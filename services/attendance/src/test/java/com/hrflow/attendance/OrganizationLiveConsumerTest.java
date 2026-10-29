package com.hrflow.attendance;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.hrflow.contracts.organization.HolidaySnapshot;
import com.hrflow.contracts.organization.OrganizationFact;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(properties = {
        "hrflow.attendance.organization-consumer.enabled=true",
        "hrflow.attendance.organization-consumer.bootstrap-timeout-ms=15000"
})
class OrganizationLiveConsumerTest {
    private static final String REGISTRY = "mock://attendance-live-consumer-it";
    private static final UUID BOOTSTRAP_TENANT = UUID.randomUUID();
    private static final UUID BOOTSTRAP_HOLIDAY = UUID.randomUUID();

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("context-schema.sql");

    @Container
    static final ConfluentKafkaContainer KAFKA = new ConfluentKafkaContainer("confluentinc/cp-kafka:8.2.0");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry properties) {
        POSTGRES.start();
        KAFKA.start();
        seedKafkaBeforeBootstrap();

        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.flyway.user", POSTGRES::getUsername);
        properties.add("spring.flyway.password", POSTGRES::getPassword);
        properties.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        properties.add("spring.kafka.properties.schema.registry.url", () -> REGISTRY);
        properties.add("spring.kafka.properties.auto.register.schemas", () -> "true");
    }

    private static void seedKafkaBeforeBootstrap() {
        try (AdminClient admin = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()))) {
            admin.createTopics(List.of(
                    new NewTopic("org.employee.v1", 1, (short) 1),
                    new NewTopic("org.shift.v1", 1, (short) 1),
                    new NewTopic("org.holiday.v1", 1, (short) 1))).all().get();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to pre-create Kafka topics", exception);
        }

        try (KafkaProducer<String, OrganizationFact> producer = createProducer()) {
            OrganizationFact snapshot = new OrganizationFact(
                    UUID.randomUUID().toString(), "org.holiday.added.v1", 1,
                    BOOTSTRAP_TENANT.toString(), BOOTSTRAP_HOLIDAY.toString(), 1L,
                    "2026-10-27T00:00:00Z", UUID.randomUUID().toString(), null,
                    new HolidaySnapshot(BOOTSTRAP_HOLIDAY.toString(), "2026-11-09", "Bootstrap Day", "COMPANY", null));
            producer.send(new ProducerRecord<>("org.holiday.v1",
                    BOOTSTRAP_TENANT + ":" + BOOTSTRAP_HOLIDAY, snapshot)).get();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to seed initial Kafka snapshot", exception);
        }
    }

    private static KafkaProducer<String, OrganizationFact> createProducer() {
        Properties config = new Properties();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put("schema.registry.url", REGISTRY);
        config.put("auto.register.schemas", true);
        return new KafkaProducer<>(config);
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ApplicationAvailability availability;

    @Test
    void bootstrapReplaysCompactedTopicBeforeReadinessAndLiveListenerReceivesSubsequentEvents() throws Exception {
        assertEquals(ReadinessState.ACCEPTING_TRAFFIC, availability.getReadinessState());
        assertEquals(1, count("attendance.holiday_projection", BOOTSTRAP_TENANT));
        assertEquals(1, count("attendance.organization_inbox", BOOTSTRAP_TENANT));
        assertEquals("Bootstrap Day", jdbc.queryForObject(
                "SELECT name FROM attendance.holiday_projection WHERE tenant_id = ? AND holiday_id = ?",
                String.class, BOOTSTRAP_TENANT, BOOTSTRAP_HOLIDAY));

        UUID liveHoliday = UUID.randomUUID();
        OrganizationFact liveFact = new OrganizationFact(
                UUID.randomUUID().toString(), "org.holiday.added.v1", 1,
                BOOTSTRAP_TENANT.toString(), liveHoliday.toString(), 1L,
                "2026-10-27T00:00:00Z", UUID.randomUUID().toString(), null,
                new HolidaySnapshot(liveHoliday.toString(), "2026-11-10", "Live Holiday", "COMPANY", null));

        try (KafkaProducer<String, OrganizationFact> producer = createProducer()) {
            producer.send(new ProducerRecord<>("org.holiday.v1",
                    BOOTSTRAP_TENANT + ":" + liveHoliday, liveFact)).get();
        }

        await().atMost(10, SECONDS).untilAsserted(() -> {
            assertEquals(2, count("attendance.holiday_projection", BOOTSTRAP_TENANT));
            assertEquals(2, count("attendance.organization_inbox", BOOTSTRAP_TENANT));
            assertEquals("Live Holiday", jdbc.queryForObject(
                    "SELECT name FROM attendance.holiday_projection WHERE tenant_id = ? AND holiday_id = ?",
                    String.class, BOOTSTRAP_TENANT, liveHoliday));
        });
    }

    private int count(String table, UUID tenant) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE tenant_id = ?",
                Integer.class, tenant);
    }
}
