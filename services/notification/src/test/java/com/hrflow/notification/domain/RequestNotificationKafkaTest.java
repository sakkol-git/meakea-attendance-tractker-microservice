package com.hrflow.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.hrflow.contracts.organization.EmployeeSnapshot;
import com.hrflow.contracts.organization.OrganizationFact;
import com.hrflow.contracts.request.RequestWorkflowFact;
import com.hrflow.notification.organization.NotificationEmployeeBootstrap;
import com.hrflow.notification.service.NotificationService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = {"hrflow.events.recovery.enabled=true", "hrflow.notification.domain-consumer.enabled=true", "spring.kafka.listener.auto-startup=true"})
@Testcontainers
class RequestNotificationKafkaTest {
    static final Network NETWORK = Network.newNetwork();
    @Container static final ConfluentKafkaContainer KAFKA = new ConfluentKafkaContainer("confluentinc/cp-kafka:8.2.0")
            .withNetwork(NETWORK).withNetworkAliases("kafka").withListener("kafka:19092");
    @Container static final GenericContainer<?> REGISTRY = new GenericContainer<>("confluentinc/cp-schema-registry:8.2.0")
            .withNetwork(NETWORK).dependsOn(KAFKA).withExposedPorts(8081).withEnv("SCHEMA_REGISTRY_HOST_NAME", "registry")
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://kafka:19092")
            .waitingFor(Wait.forHttp("/subjects").forStatusCode(200));
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine").withInitScript("context-schema.sql");
    static final UUID TENANT = UUID.randomUUID(), EMPLOYEE = UUID.randomUUID(), MANAGER = UUID.randomUUID(), HR = UUID.randomUUID();

    static String registry() { return "http://" + REGISTRY.getHost() + ":" + REGISTRY.getMappedPort(8081); }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry p) throws Exception {
        var schemas = Map.of("org.employee.v1", OrganizationFact.getClassSchema(), "request.workflow.v1", RequestWorkflowFact.getClassSchema(),
                "leave.request.v1", com.hrflow.contracts.leave.LeaveRequestFact.getClassSchema(),
                "attendance.daily-status.v1", com.hrflow.contracts.attendance.AttendanceDailyFact.getClassSchema(),
                "attendance.correction.v1", com.hrflow.contracts.attendance.AttendanceCorrectionFact.getClassSchema());
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", KAFKA.getBootstrapServers()))) {
            var topics = new java.util.ArrayList<NewTopic>();
            for (String source : schemas.keySet()) {
                topics.add(new NewTopic(source, 1, (short) 1));
                for (String suffix : List.of(".retry-0", ".retry-1", ".retry-2", ".dlq")) topics.add(new NewTopic(source + suffix, 1, (short) 1));
            }
            admin.createTopics(topics).all().get();
        }
        var http = HttpClient.newHttpClient(); var json = JsonMapper.builder().build();
        for (var schema : schemas.entrySet()) {
            await().atMost(Duration.ofSeconds(45)).untilAsserted(() -> {
                var response = http.send(HttpRequest.newBuilder(URI.create(registry() + "/subjects/" + schema.getKey() + "-value/versions"))
                        .header("Content-Type", "application/vnd.schemaregistry.v1+json")
                        .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("schema", schema.getValue().toString())))).build(), HttpResponse.BodyHandlers.ofString());
                assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
            });
        }
        try (var producer = new KafkaProducer<String, Object>(producer())) {
            for (var fact : List.of(employee(EMPLOYEE, MANAGER, List.of("EMPLOYEE")), employee(MANAGER, null, List.of("EMPLOYEE", "MANAGER")), employee(HR, null, List.of("EMPLOYEE", "HR_ADMIN"))))
                producer.send(new ProducerRecord<>("org.employee.v1", TENANT + ":" + fact.getAggregateId(), fact)).get();
        }
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl); p.add("spring.datasource.username", () -> "notification_app");
        p.add("spring.datasource.password", () -> "test-pass"); p.add("spring.flyway.user", POSTGRES::getUsername); p.add("spring.flyway.password", POSTGRES::getPassword);
        p.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers); p.add("spring.kafka.properties.schema.registry.url", RequestNotificationKafkaTest::registry);
    }
    @Autowired NotificationEmployeeBootstrap bootstrap;
    @Autowired NotificationService notices;
    @Autowired com.hrflow.events.BrokerLagMonitor brokerLag;

    @Test void actualAvroReplayAndWorkflowEventsDeliverOnceBeforeOffsetsAdvance() throws Exception {
        assertThat(bootstrap.isReady()).isTrue();
        UUID request = UUID.randomUUID();
        var first = RequestNotificationTest.fact(TENANT, EMPLOYEE, request, MANAGER, 1, "PENDING_APPROVAL", "PENDING");
        long end;
        try (var producer = new KafkaProducer<String, Object>(producer())) {
            producer.send(new ProducerRecord<>("request.workflow.v1", TENANT + ":" + request, first)).get();
            producer.send(new ProducerRecord<>("request.workflow.v1", TENANT + ":" + request, first)).get();
            await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(notices.listNotifications(TENANT, MANAGER, 0, 25).total()).isEqualTo(1));
            producer.send(new ProducerRecord<>("request.workflow.v1", TENANT + ":" + request,
                    RequestNotificationTest.fact(TENANT, EMPLOYEE, request, MANAGER, 2, "PENDING_APPROVAL", "APPROVED"))).get();
            await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(notices.listNotifications(TENANT, HR, 0, 25).total()).isEqualTo(1));
            producer.send(new ProducerRecord<>("request.workflow.v1", TENANT + ":" + request,
                    RequestNotificationTest.fact(TENANT, EMPLOYEE, request, MANAGER, 3, "APPROVED", "APPROVED"))).get();
            end = producer.send(new ProducerRecord<>("request.workflow.v1", TENANT + ":" + request,
                    RequestNotificationTest.fact(TENANT, EMPLOYEE, request, MANAGER, 1, "PENDING_APPROVAL", "PENDING"))).get().offset() + 1;
        }
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", KAFKA.getBootstrapServers()))) {
            await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
                var committed = admin.listConsumerGroupOffsets("hrflow-notification-request-v1").partitionsToOffsetAndMetadata().get();
                assertThat(committed.get(new TopicPartition("request.workflow.v1", 0)).offset()).isGreaterThanOrEqualTo(end);
            });
        }
        var admin = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        assertThat(admin.queryForObject("SELECT count(*) FROM notification.notification_record WHERE tenant_id=?", Integer.class, TENANT)).isEqualTo(3);
        assertThat(notices.listNotifications(TENANT, EMPLOYEE, 0, 25).total()).isEqualTo(1);
        assertThat(notices.listNotifications(UUID.randomUUID(), HR, 0, 25).total()).isZero();
        brokerLag.poll();
        assertThat(brokerLag.lag()).isGreaterThanOrEqualTo(0);
        verifyLeaveDelivery();
        verifyCorrectionDelivery();
        UUID recoveredManager = NotificationRecoveryFixture.verify();
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(notices.listNotifications(TENANT, recoveredManager, 0, 25).total()).isEqualTo(1));
    }


    private void verifyLeaveDelivery() throws Exception {
        UUID leave = UUID.randomUUID();
        var day = java.time.LocalDate.parse("2026-11-02");
        var pending = com.hrflow.contracts.leave.LeaveRequestFact.newBuilder()
                .setEventId(UUID.randomUUID().toString()).setType("LeaveRequestSnapshot.v1").setSchemaVersion(1)
                .setTenantId(TENANT.toString()).setAggregateId(leave.toString()).setAggregateVersion(1L)
                .setOccurredAt(Instant.now()).setCorrelationId(UUID.randomUUID().toString()).setEmployeeId(EMPLOYEE.toString())
                .setLeaveType("ANNUAL").setStartDate(day).setEndDate(day).setPortion("FULL_DAY").setChargedDays("1")
                .setWorkingDates(List.of(day)).setState("PENDING").build();
        long end;
        try (var producer = new KafkaProducer<String, Object>(producer())) {
            producer.send(new ProducerRecord<>("leave.request.v1", TENANT + ":" + EMPLOYEE, pending)).get();
            producer.send(new ProducerRecord<>("leave.request.v1", TENANT + ":" + EMPLOYEE, pending)).get();
            var approved = com.hrflow.contracts.leave.LeaveRequestFact.newBuilder(pending)
                    .setEventId(UUID.randomUUID().toString()).setAggregateVersion(2L).setState("APPROVED").build();
            producer.send(new ProducerRecord<>("leave.request.v1", TENANT + ":" + EMPLOYEE, approved)).get();
            var review = com.hrflow.contracts.leave.LeaveRequestFact.newBuilder(approved)
                    .setEventId(UUID.randomUUID().toString()).setAggregateVersion(3L).setState("NEEDS_REVIEW").build();
            producer.send(new ProducerRecord<>("leave.request.v1", TENANT + ":" + EMPLOYEE, review)).get();
            end = producer.send(new ProducerRecord<>("leave.request.v1", TENANT + ":" + EMPLOYEE, pending)).get().offset() + 1;
        }
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", KAFKA.getBootstrapServers()))) {
            await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
                var committed = admin.listConsumerGroupOffsets("hrflow-notification-leave-v1").partitionsToOffsetAndMetadata().get();
                assertThat(committed.get(new TopicPartition("leave.request.v1", 0)).offset()).isGreaterThanOrEqualTo(end);
            });
        }
        var admin = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        assertThat(admin.queryForObject("SELECT count(*) FROM notification.notification_record WHERE tenant_id=? AND kind='leave' AND record_id=?",
                Integer.class, TENANT, leave.toString())).isEqualTo(5);
        assertThat(notices.listNotifications(TENANT, HR, 0, 25).items().stream()
                .filter(notice -> notice.recordId().equals(leave.toString()))).hasSize(2);
    }

    private void verifyCorrectionDelivery() throws Exception {
        UUID correction = UUID.randomUUID();
        var pending = com.hrflow.contracts.attendance.AttendanceCorrectionFact.newBuilder()
                .setEventId(UUID.randomUUID().toString()).setType("AttendanceCorrectionSnapshot.v1").setSchemaVersion(1)
                .setTenantId(TENANT.toString()).setAggregateId(correction.toString()).setAggregateVersion(1L)
                .setOccurredAt(Instant.now()).setCorrelationId(UUID.randomUUID().toString()).setEmployeeId(EMPLOYEE.toString())
                .setWorkDate(java.time.LocalDate.parse("2026-11-02")).setState("PENDING").build();
        long end;
        try (var producer = new KafkaProducer<String, Object>(producer())) {
            producer.send(new ProducerRecord<>("attendance.correction.v1", TENANT + ":" + EMPLOYEE, pending)).get();
            producer.send(new ProducerRecord<>("attendance.correction.v1", TENANT + ":" + EMPLOYEE, pending)).get();
            var approved = com.hrflow.contracts.attendance.AttendanceCorrectionFact.newBuilder(pending)
                    .setEventId(UUID.randomUUID().toString()).setAggregateVersion(2L).setState("APPROVED").build();
            producer.send(new ProducerRecord<>("attendance.correction.v1", TENANT + ":" + EMPLOYEE, approved)).get();
            end = producer.send(new ProducerRecord<>("attendance.correction.v1", TENANT + ":" + EMPLOYEE, pending)).get().offset() + 1;
        }
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", KAFKA.getBootstrapServers()))) {
            await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
                var committed = admin.listConsumerGroupOffsets("hrflow-notification-correction-v1").partitionsToOffsetAndMetadata().get();
                assertThat(committed.get(new TopicPartition("attendance.correction.v1", 0)).offset()).isGreaterThanOrEqualTo(end);
            });
        }
        var admin = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        assertThat(admin.queryForObject("SELECT count(*) FROM notification.notification_record WHERE tenant_id=? AND kind='correction' AND record_id=?", Integer.class, TENANT, correction.toString())).isEqualTo(3);
    }

    static Map<String, Object> producer() { return Map.of("bootstrap.servers", KAFKA.getBootstrapServers(), "key.serializer", org.apache.kafka.common.serialization.StringSerializer.class,
            "value.serializer", io.confluent.kafka.serializers.KafkaAvroSerializer.class, "schema.registry.url", registry(), "auto.register.schemas", false, "acks", "all"); }
    static OrganizationFact employee(UUID id, UUID manager, List<String> roles) {
        var value = EmployeeSnapshot.newBuilder().setEmployeeId(id.toString()).setEmployeeNo("SYNTHETIC").setFullName("Synthetic")
                .setStatus("ACTIVE").setAssignments(List.of()).setUserId(id.toString()).setAccountRoles(roles).setManagerId(manager == null ? null : manager.toString()).build();
        return new OrganizationFact(UUID.randomUUID().toString(), "org.employee.updated.v1", 1, TENANT.toString(), id.toString(), 1L,
                Instant.now().toString(), UUID.randomUUID().toString(), null, value);
    }
}
