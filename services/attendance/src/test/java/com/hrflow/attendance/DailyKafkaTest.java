package com.hrflow.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

import com.hrflow.attendance.dto.CheckInRequest;
import com.hrflow.attendance.events.AttendanceOutboxRelay;
import com.hrflow.attendance.service.AttendanceService;
import com.hrflow.contracts.attendance.AttendanceDailyFact;
import com.hrflow.contracts.attendance.AttendanceSessionFact;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

@Testcontainers
@SpringBootTest(properties = {"hrflow.attendance.daily-status.enabled=true", "hrflow.attendance.relay.enabled=true",
        "hrflow.attendance.cutoff.enabled=true", "hrflow.attendance.cutoff.initial-delay-ms=3600000",
        "hrflow.attendance.cutoff-consumer.enabled=true",
        "hrflow.attendance.relay.initial-delay-ms=3600000", "spring.kafka.listener.auto-startup=true"})
class DailyKafkaTest {
    static final Network NETWORK = Network.newNetwork();
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("attendance-wire-schema.sql");
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
        var schemas = Map.of("time.shift-cutoff.v1", com.hrflow.contracts.attendance.ShiftCutoffFact.getClassSchema(),
                "attendance.session.v1", AttendanceSessionFact.getClassSchema(),
                "attendance.daily-status.v1", AttendanceDailyFact.getClassSchema());
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", KAFKA.getBootstrapServers()))) {
            admin.createTopics(schemas.keySet().stream().map(topic -> new NewTopic(topic, 1, (short) 1)).toList()).all().get();
        }
        for (var entry : schemas.entrySet()) {
            org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            var registered = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                    URI.create(registryUrl() + "/subjects/" + entry.getKey() + "-value/versions"))
                    .header("Content-Type", "application/vnd.schemaregistry.v1+json")
                    .POST(HttpRequest.BodyPublishers.ofString(JsonMapper.builder().build().writeValueAsString(
                            Map.of("schema", entry.getValue().toString())))).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(registered.statusCode()).as("Schema Registry registration response: %s", registered.body()).isEqualTo(200);
            });
        }
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", () -> "attendance_runtime");
        p.add("spring.datasource.password", () -> "wire-only");
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
        p.add("hrflow.attendance.relay.job-user", () -> "attendance_job");
        p.add("hrflow.attendance.relay.job-password", () -> "wire-only");
        p.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        p.add("spring.kafka.properties.schema.registry.url", DailyKafkaTest::registryUrl);
    }
    @Autowired AttendanceService attendance;
    @Autowired AttendanceOutboxRelay relay;
    @SuppressWarnings("rawtypes") @MockitoSpyBean(name = "kafkaTemplate") KafkaTemplate kafka;
    @MockitoSpyBean java.time.Clock clock;
    @Autowired com.hrflow.attendance.daily.ShiftCutoffWorker cutoff;

    @Test void noTapCutoffTraversesRegisteredKafkaAndCreatesFinalizedAbsenceWithNoEvidence() {
        var admin = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        org.mockito.Mockito.doReturn(DailyAttendanceTest.at("17:30")).when(clock).instant();
        try {
            AttendanceFixtures.schedule(admin, tenant, employee);
            org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
                cutoff.poll();
                relay.poll();
                assertThat(admin.queryForList("SELECT payload->'overview'->>'status' FROM attendance.daily_status WHERE tenant_id=?",
                        String.class, tenant)).containsExactly("ABSENT");
            });
            assertThat(admin.queryForObject("SELECT payload->'overview'->>'finalized' FROM attendance.daily_status WHERE tenant_id=?", String.class, tenant)).isEqualTo("true");
            assertThat(admin.queryForObject("SELECT count(*) FROM attendance.cutoff_inbox WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(1);
            assertThat(admin.queryForObject("SELECT count(*) FROM attendance.attendance_evidence WHERE tenant_id=?", Integer.class, tenant)).isZero();
        } finally { org.mockito.Mockito.reset(clock); }
    }

    @Test void realCommandsPublishDistinctDailyAvroAndFailedDailySendRetainsEmployeeStreamOrder() throws Exception {
        var admin = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        AttendanceFixtures.schedule(admin, tenant, employee);
        var input = new CheckInRequest(null, null, null, null);
        attendance.checkIn(tenant, employee, employee, UUID.randomUUID().toString(), input);
        attendance.checkOut(tenant, employee, employee, UUID.randomUUID().toString(), "\"0\"", input);
        var fail = new AtomicBoolean(true);
        doAnswer(call -> fail.getAndSet(false) ? CompletableFuture.failedFuture(new IllegalStateException("broker denied daily send"))
                : call.callRealMethod()).when(kafka).send(eq("attendance.daily-status.v1"), eq(tenant + ":" + employee), any());
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(10)).until(() -> { relay.poll(); return !fail.get(); });
        assertThat(admin.queryForObject("SELECT count(*) FROM attendance.attendance_outbox WHERE tenant_id=? AND sent_at IS NOT NULL", Integer.class, tenant)).isEqualTo(1);
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            relay.poll();
            assertThat(admin.queryForObject("SELECT count(*) FROM attendance.attendance_outbox WHERE tenant_id=? AND topic='attendance.daily-status.v1' AND sent_at IS NOT NULL", Integer.class, tenant)).isEqualTo(2);
        });
        var facts = consume(tenant);
        assertThat(facts).extracting(AttendanceDailyFact::getAggregateVersion).containsExactly(1L, 2L);
        assertThat(facts.getFirst().getOpenSession()).isTrue();
        assertThat(facts.getFirst().getMinutesWorked()).isNull();
        assertThat(facts.getLast().getOpenSession()).isFalse();
        assertThat(facts.getLast().getMinutesWorked()).isZero();
        assertThat(facts.getLast().getSessionReferences().getFirst().getRevision()).isEqualTo(2);
        assertThat(facts.getFirst().getAggregateId()).isEqualTo(facts.getLast().getAggregateId());
        assertThat(facts.getLast().toString()).doesNotContain("latitude", "deviceModel");
    }

    private List<AttendanceDailyFact> consume(UUID tenant) {
        var config = Map.<String, Object>of("bootstrap.servers", KAFKA.getBootstrapServers(), "group.id", UUID.randomUUID().toString(),
                "auto.offset.reset", "earliest", "enable.auto.commit", false, "key.deserializer", org.apache.kafka.common.serialization.StringDeserializer.class,
                "value.deserializer", io.confluent.kafka.serializers.KafkaAvroDeserializer.class, "schema.registry.url", registryUrl(), "specific.avro.reader", true);
        try (var consumer = new KafkaConsumer<String, AttendanceDailyFact>(config)) {
            consumer.subscribe(List.of("attendance.daily-status.v1"));
            List<AttendanceDailyFact> facts = new ArrayList<>();
            long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
            while (facts.size() < 2 && System.nanoTime() < deadline) consumer.poll(Duration.ofMillis(250)).forEach(record -> {
                if (tenant.toString().equals(record.value().getTenantId())) facts.add(record.value());
            });
            assertThat(facts).hasSize(2);
            return facts;
        }
    }
}
