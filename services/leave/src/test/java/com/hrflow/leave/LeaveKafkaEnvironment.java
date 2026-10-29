package com.hrflow.leave;

import static org.assertj.core.api.Assertions.assertThat;

import com.hrflow.contracts.leave.LeaveRequestFact;
import com.hrflow.leave.dto.CreateLeaveRequestDto;
import com.hrflow.leave.dto.DecisionRequest;
import com.hrflow.leave.service.LeaveSubmissionService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.KafkaConsumer;
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

@Testcontainers
abstract class LeaveKafkaEnvironment {
    static final UUID TEAM_TENANT = UUID.randomUUID(), TEAM_MANAGER = UUID.randomUUID(), TEAM_EMPLOYEE = UUID.randomUUID(), TEAM_SHIFT = UUID.randomUUID(), TEAM_HOLIDAY = UUID.randomUUID();
    static final Network NETWORK = Network.newNetwork();
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("leave-live-schema.sql");
    @Container static final ConfluentKafkaContainer KAFKA = new ConfluentKafkaContainer("confluentinc/cp-kafka:8.2.0")
            .withNetwork(NETWORK).withNetworkAliases("kafka").withListener("kafka:19092");
    @Container static final GenericContainer<?> REGISTRY = new GenericContainer<>("confluentinc/cp-schema-registry:8.2.0")
            .withNetwork(NETWORK).withExposedPorts(8081).dependsOn(KAFKA)
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", "registry")
            .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://kafka:19092")
            .waitingFor(Wait.forHttp("/subjects").forStatusCode(200));
    static String registryUrl() { return "http://" + REGISTRY.getHost() + ":" + REGISTRY.getMappedPort(8081); }

    @DynamicPropertySource static void properties(DynamicPropertyRegistry p) throws Exception {
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", KAFKA.getBootstrapServers()))) {
            admin.createTopics(List.of(new NewTopic("leave.request.v1", 1, (short) 1), new NewTopic("attendance.daily-status.v1", 1, (short) 1), new NewTopic("org.employee.v1", 1, (short) 1), new NewTopic("org.shift.v1", 1, (short) 1), new NewTopic("org.holiday.v1", 1, (short) 1))).all().get();
        }
        var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(registryUrl()
                        + "/subjects/leave.request.v1-value/versions"))
                .header("Content-Type", "application/vnd.schemaregistry.v1+json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonMapper.builder().build().writeValueAsString(
                        Map.of("schema", LeaveRequestFact.getClassSchema().toString())))).build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IllegalStateException("Leave schema registration failed");
        String dailySubject = registryUrl() + "/subjects/attendance.daily-status.v1-value/versions";
        var json = JsonMapper.builder().build();
        var configured = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(registryUrl() + "/config/attendance.daily-status.v1-value"))
                .header("Content-Type", "application/vnd.schemaregistry.v1+json")
                .PUT(HttpRequest.BodyPublishers.ofString("{\"compatibility\":\"FULL_TRANSITIVE\"}")).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(configured.statusCode()).isEqualTo(200);
        for (String schema : List.of(java.nio.file.Files.readString(java.nio.file.Path.of("../../libs/hrflow-contracts/src/test/resources/attendance-daily-fact.original-v1.avsc")),
                com.hrflow.contracts.attendance.AttendanceDailyFact.getClassSchema().toString())) {
            org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                var registered = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(dailySubject))
                        .header("Content-Type", "application/vnd.schemaregistry.v1+json")
                        .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("schema", schema)))).build(), HttpResponse.BodyHandlers.ofString());
                assertThat(registered.statusCode()).as(registered.body()).isEqualTo(200);
            });
        }
        var organization = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(registryUrl()
                        + "/subjects/org.employee.v1-value/versions"))
                .header("Content-Type", "application/vnd.schemaregistry.v1+json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonMapper.builder().build().writeValueAsString(Map.of("schema",
                        com.hrflow.contracts.organization.OrganizationFact.getClassSchema().toString())))).build(), HttpResponse.BodyHandlers.ofString());
        if (organization.statusCode() != 200) throw new IllegalStateException("Employee schema registration failed");
        for (String topic : List.of("org.shift.v1", "org.holiday.v1")) {
            var registered = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(registryUrl() + "/subjects/" + topic + "-value/versions"))
                    .header("Content-Type", "application/vnd.schemaregistry.v1+json")
                    .POST(HttpRequest.BodyPublishers.ofString(JsonMapper.builder().build().writeValueAsString(Map.of("schema",
                            com.hrflow.contracts.organization.OrganizationFact.getClassSchema().toString())))).build(), HttpResponse.BodyHandlers.ofString());
            if (registered.statusCode() != 200) throw new IllegalStateException("Calendar schema registration failed");
        }

        try (var producer = new org.apache.kafka.clients.producer.KafkaProducer<String, com.hrflow.contracts.organization.OrganizationFact>(
                Map.of("bootstrap.servers", KAFKA.getBootstrapServers(), "key.serializer", org.apache.kafka.common.serialization.StringSerializer.class,
                        "value.serializer", io.confluent.kafka.serializers.KafkaAvroSerializer.class, "schema.registry.url", registryUrl(),
                        "auto.register.schemas", false, "acks", "all"))) {
            for (var fact : List.of(LeaveReferenceFixtures.fact(TEAM_TENANT, TEAM_MANAGER, null, 1),
                    LeaveReferenceFixtures.fact(TEAM_TENANT, TEAM_EMPLOYEE, TEAM_MANAGER, 1))) {
                if (TEAM_EMPLOYEE.toString().equals(fact.getAggregateId())) {
                    ((com.hrflow.contracts.organization.EmployeeSnapshot) fact.getPayload()).setAssignments(List.of(
                            new com.hrflow.contracts.organization.AssignmentSnapshot(UUID.randomUUID().toString(), TEAM_SHIFT.toString(), "2000-01-01", null)));
                }
                producer.send(new org.apache.kafka.clients.producer.ProducerRecord<>("org.employee.v1", fact.getTenantId() + ":" + fact.getAggregateId(), fact)).get();
            }
            for (var fact : List.of(shift(), holiday(1, false))) {
                String topic = fact.getPayload() instanceof com.hrflow.contracts.organization.ShiftSnapshot ? "org.shift.v1" : "org.holiday.v1";
                producer.send(new org.apache.kafka.clients.producer.ProducerRecord<>(topic, fact.getTenantId() + ":" + fact.getAggregateId(), fact)).get();
            }
        }

        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", () -> "leave_app");
        p.add("spring.datasource.password", () -> "test-pass");
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
        p.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        p.add("spring.kafka.properties.schema.registry.url", LeaveKafkaEnvironment::registryUrl);
        p.add("hrflow.leave.relay.job-user", () -> "leave_job");
        p.add("hrflow.leave.relay.job-password", () -> "job-test-pass");
    }
    static com.hrflow.contracts.organization.OrganizationFact shift() {
        var body = com.hrflow.contracts.organization.ShiftSnapshot.newBuilder().setShiftId(TEAM_SHIFT.toString()).setName("Fixture")
                .setStartTime("10:00").setEndTime("16:00").setTimezone("Asia/Phnom_Penh").setGraceMinutes(10).setWorkingDays(65)
                .setSite(com.hrflow.contracts.organization.SiteSnapshot.newBuilder().setSiteId(UUID.randomUUID().toString()).setName("Fixture")
                        .setLatitude(11.56).setLongitude(104.92).setRadiusM(100).setTimezone("Asia/Phnom_Penh").build()).build();
        return new com.hrflow.contracts.organization.OrganizationFact(UUID.randomUUID().toString(), "org.shift.defined.v1", 1,
                TEAM_TENANT.toString(), TEAM_SHIFT.toString(), 1L, java.time.Instant.now().toString(), UUID.randomUUID().toString(), null, body);
    }
    static com.hrflow.contracts.organization.OrganizationFact holiday(long revision, boolean removed) {
        var body = new com.hrflow.contracts.organization.HolidaySnapshot(TEAM_HOLIDAY.toString(), "2026-11-09", "Fixture", "COMPANY",
                removed ? java.time.Instant.now().toString() : null);
        return new com.hrflow.contracts.organization.OrganizationFact(UUID.randomUUID().toString(), removed ? "org.holiday.removed.v1" : "org.holiday.added.v1", 1,
                TEAM_TENANT.toString(), TEAM_HOLIDAY.toString(), revision, java.time.Instant.now().toString(), UUID.randomUUID().toString(), null, body);
    }

}
