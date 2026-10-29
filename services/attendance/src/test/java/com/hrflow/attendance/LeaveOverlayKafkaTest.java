package com.hrflow.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.hrflow.attendance.dto.CheckInRequest;
import com.hrflow.attendance.service.AttendanceService;
import com.hrflow.contracts.leave.LeaveRequestFact;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
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
@SpringBootTest(properties = {"hrflow.attendance.leave-consumer.enabled=true", "spring.kafka.listener.auto-startup=true"})
class LeaveOverlayKafkaTest {
    static final Network NETWORK = Network.newNetwork();
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("context-schema.sql");
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
            admin.createTopics(List.of(new NewTopic("leave.request.v1", 1, (short) 1))).all().get();
        }
        var registration = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                URI.create(registryUrl() + "/subjects/leave.request.v1-value/versions"))
                .header("Content-Type", "application/vnd.schemaregistry.v1+json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonMapper.builder().build().writeValueAsString(
                        Map.of("schema", LeaveRequestFact.getClassSchema().toString())))).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(registration.statusCode()).isEqualTo(200);
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", POSTGRES::getUsername);
        p.add("spring.datasource.password", POSTGRES::getPassword);
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
        p.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        p.add("spring.kafka.properties.schema.registry.url", LeaveOverlayKafkaTest::registryUrl);
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired AttendanceService attendance;
    @Autowired KafkaTemplate<Object, Object> kafka;

    @Test void actualLeaveSnapshotsOverlayOnlyOwnerDatesAndWorkRequiresReviewWithoutDeletingEvidence() throws Exception {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID(), leave = UUID.randomUUID();
        AttendanceFixtures.schedule(jdbc, tenant, employee);
        var today = LocalDate.now(java.time.ZoneId.of("Asia/Phnom_Penh"));
        var approved = fact(tenant, employee, leave, today, 1, "APPROVED");
        send(approved);
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(attendance.dailyStatusMe(tenant, employee, today).status()).isEqualTo("ON_LEAVE"));
        attendance.checkIn(tenant, employee, employee, UUID.randomUUID().toString(), new CheckInRequest(null, null, null, null));
        var working = attendance.dailyStatusMe(tenant, employee, today);
        assertThat(working.reviewRequired()).isTrue();
        assertThat(working.reviewReason()).contains("approved leave");
        assertThat(working.clockIn()).isNotNull();
        send(fact(tenant, employee, leave, today, 2, "NEEDS_REVIEW"));
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(revision(tenant, leave)).isEqualTo(2));
        send(approved);
        send(fact(tenant, employee, leave, today, 1, "CANCELLED"));
        send(fact(tenant, employee, leave, today, 3, "CANCELLED"));
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(attendance.dailyStatusMe(tenant, employee, today).reviewRequired()).isFalse());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM attendance.attendance_evidence WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(1);
    }

    private long revision(UUID tenant, UUID leave) {
        return jdbc.queryForObject("SELECT revision FROM attendance.leave_projection WHERE tenant_id=? AND leave_id=?", Long.class, tenant, leave);
    }
    private void send(LeaveRequestFact fact) throws Exception {
        kafka.send("leave.request.v1", fact.getTenantId() + ":" + fact.getEmployeeId(), fact).get();
    }
    static LeaveRequestFact fact(UUID tenant, UUID employee, UUID leave, LocalDate date, long revision, String state) {
        return LeaveRequestFact.newBuilder().setEventId(UUID.randomUUID().toString()).setType("LeaveRequestSnapshot.v1")
                .setSchemaVersion(1).setTenantId(tenant.toString()).setAggregateId(leave.toString()).setAggregateVersion(revision)
                .setOccurredAt(Instant.now()).setCorrelationId(UUID.randomUUID().toString()).setEmployeeId(employee.toString())
                .setLeaveType("ANNUAL").setStartDate(date.minusDays(1)).setEndDate(date.plusDays(1)).setPortion("FULL_DAY")
                .setChargedDays("1.00").setWorkingDates(List.of(date)).setState(state).build();
    }
}
