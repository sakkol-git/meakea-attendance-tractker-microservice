package com.hrflow.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

import com.hrflow.attendance.dto.CheckInRequest;
import com.hrflow.attendance.events.AttendanceOutboxRelay;
import com.hrflow.attendance.service.AttendanceService;
import com.hrflow.contracts.attendance.AttendanceSessionFact;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
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
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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
@SpringBootTest(properties = {"hrflow.attendance.relay.enabled=true",
        "hrflow.attendance.relay.initial-delay-ms=3600000", "spring.kafka.listener.auto-startup=false"})
class SessionKafkaTest {
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

    @DynamicPropertySource static void properties(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", POSTGRES::getUsername);
        p.add("spring.datasource.password", POSTGRES::getPassword);
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
        p.add("hrflow.attendance.relay.job-user", () -> "attendance_test_job");
        p.add("hrflow.attendance.relay.job-password", () -> "test-only-password");
        p.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        p.add("spring.kafka.properties.schema.registry.url", SessionKafkaTest::registryUrl);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired AttendanceService attendance;
    @Autowired AttendanceOutboxRelay relay;
    @Autowired com.hrflow.attendance.service.AttendanceCorrectionService corrections;
    @SuppressWarnings("rawtypes")
    @MockitoSpyBean(name = "kafkaTemplate") KafkaTemplate kafka;

    @Test void actualOwnerCommandsPublishRegisteredAvroInOrderOnlyAfterBrokerAcknowledgment() throws Exception {
        jdbc.execute("CREATE ROLE attendance_test_job LOGIN PASSWORD 'test-only-password' NOSUPERUSER NOBYPASSRLS");
        jdbc.execute("GRANT USAGE ON SCHEMA attendance_jobs TO attendance_test_job");
        jdbc.execute("GRANT SELECT ON attendance_jobs.tenant_registry TO attendance_test_job");
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", KAFKA.getBootstrapServers()))) {
            admin.createTopics(List.of(new NewTopic("attendance.session.v1", 1, (short) 1), new NewTopic("attendance.correction.v1", 1, (short) 1))).all().get();
        }
        var registration = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                URI.create(registryUrl() + "/subjects/attendance.session.v1-value/versions"))
                .header("Content-Type", "application/vnd.schemaregistry.v1+json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonMapper.builder().build().writeValueAsString(
                        Map.of("schema", AttendanceSessionFact.getClassSchema().toString())))).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(registration.statusCode()).isEqualTo(200);
        var correctionRegistration = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                URI.create(registryUrl() + "/subjects/attendance.correction.v1-value/versions"))
                .header("Content-Type", "application/vnd.schemaregistry.v1+json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonMapper.builder().build().writeValueAsString(Map.of("schema",
                        com.hrflow.contracts.attendance.AttendanceCorrectionFact.getClassSchema().toString())))).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(correctionRegistration.statusCode()).isEqualTo(200);
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        AttendanceFixtures.schedule(jdbc, tenant, employee);
        var input = new CheckInRequest(11.56, 104.92, 10.0, "Synthetic device");
        var opened = attendance.checkIn(tenant, employee, employee, UUID.randomUUID().toString(), input);
        UUID reviewer = UUID.randomUUID();
        var correction = corrections.requestCorrection(tenant, employee, employee, UUID.randomUUID().toString(),
                new com.hrflow.attendance.dto.CorrectionRequest(opened.id(),
                        opened.clockIn().atZone(java.time.ZoneId.of("Asia/Phnom_Penh")).toLocalDate(),
                        opened.clockIn().minusSeconds(60), null, "Synthetic correction"));
        corrections.approveCorrection(tenant,
                new com.hrflow.attendance.service.CorrectionAccess.Caller(reviewer, reviewer, true, false),
                correction.id(), "\"" + correction.version() + "\"");
        AtomicBoolean failFirst = new AtomicBoolean(true);
        doAnswer(call -> failFirst.getAndSet(false)
                ? CompletableFuture.failedFuture(new IllegalStateException("injected broker failure"))
                : call.callRealMethod()).when(kafka).send(eq("attendance.session.v1"), any(), any());
        try {
            relay.poll();
            assertThat(sent(tenant)).isZero();
            relay.poll();
            assertThat(sent(tenant)).isEqualTo(1);
            relay.poll();
            attendance.checkOut(tenant, employee, employee, UUID.randomUUID().toString(),
                    "\"1\"", input);
            relay.poll();
            for (int attempt = 0; attempt < 5; attempt++) relay.poll();
            assertThat(sent(tenant)).isEqualTo(3);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM attendance.attendance_outbox WHERE tenant_id=? AND topic='attendance.correction.v1' AND sent_at IS NOT NULL", Integer.class, tenant)).isEqualTo(2);
            try (var consumer = new KafkaConsumer<String, com.hrflow.contracts.attendance.AttendanceCorrectionFact>(Map.of(
                    "bootstrap.servers", KAFKA.getBootstrapServers(), "group.id", "correction-test-" + UUID.randomUUID(),
                    "auto.offset.reset", "earliest", "enable.auto.commit", false, "key.deserializer", StringDeserializer.class,
                    "value.deserializer", KafkaAvroDeserializer.class, "schema.registry.url", registryUrl(), "specific.avro.reader", true))) {
                consumer.subscribe(List.of("attendance.correction.v1"));
                var received = new ArrayList<com.hrflow.contracts.attendance.AttendanceCorrectionFact>();
                long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
                while (received.size() < 2 && System.nanoTime() < deadline) for (var record : consumer.poll(Duration.ofMillis(250))) {
                    com.hrflow.contracts.attendance.AttendanceCorrectionWire.validate(record.key(), record.value());
                    received.add(record.value());
                }
                assertThat(received).extracting(com.hrflow.contracts.attendance.AttendanceCorrectionFact::getState).containsExactly("PENDING", "APPROVED");
                assertThat(received.getFirst().toString()).doesNotContain("Synthetic correction", "proposed", "previousSessions", "latitude");
            }
            List<AttendanceSessionFact> facts = consume(tenant, employee);
            assertThat(facts).extracting(AttendanceSessionFact::getAggregateVersion).containsExactly(1L, 2L, 3L);
            assertThat(facts).extracting(AttendanceSessionFact::getState).containsExactly("OPEN", "OPEN", "COMPLETED");
            assertThat(facts).extracting(AttendanceSessionFact::getSource).containsExactly("TAP", "MANUAL", "MANUAL");
            assertThat(facts.get(0).getClockOut()).isNull();
            assertThat(facts.get(2).getClockOut()).isNotNull();
            assertThat(facts.get(1).getScheduledStart()).isEqualTo(facts.get(0).getScheduledStart());
            assertThat(facts.get(1).toString()).doesNotContain("Synthetic device", "latitude", "longitude");
        } finally { reset(kafka); relay.closeLeader(); }
    }

    private int sent(UUID tenant) {
        return jdbc.queryForObject("SELECT count(*) FROM attendance.attendance_outbox WHERE tenant_id=? AND topic='attendance.session.v1' AND sent_at IS NOT NULL",
                Integer.class, tenant);
    }

    private List<AttendanceSessionFact> consume(UUID tenant, UUID employee) {
        Map<String, Object> config = new java.util.HashMap<>(Map.of(
                "bootstrap.servers", KAFKA.getBootstrapServers(), "group.id", "session-test-" + UUID.randomUUID(),
                "auto.offset.reset", "earliest", "enable.auto.commit", false,
                "key.deserializer", StringDeserializer.class, "value.deserializer", KafkaAvroDeserializer.class,
                "schema.registry.url", registryUrl(), "specific.avro.reader", true));
        try (var consumer = new KafkaConsumer<String, AttendanceSessionFact>(config)) {
            consumer.subscribe(List.of("attendance.session.v1"));
            List<AttendanceSessionFact> facts = new ArrayList<>();
            long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
            while (facts.size() < 3 && System.nanoTime() < deadline) {
                for (var record : consumer.poll(Duration.ofMillis(500))) {
                    assertThat(record.key()).isEqualTo(tenant + ":" + employee);
                    facts.add(record.value());
                }
            }
            assertThat(facts).hasSize(3);
            return facts;
        }
    }
}
