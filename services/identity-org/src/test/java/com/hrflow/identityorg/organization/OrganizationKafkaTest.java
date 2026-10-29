package com.hrflow.identityorg.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hrflow.contracts.organization.HolidaySnapshot;
import com.hrflow.contracts.organization.OrganizationFact;
import com.hrflow.identityorg.organization.service.OrganizationOutboxRelay;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
        "hrflow.auth.local-bootstrap=true",
        "hrflow.auth.dev-password=synthetic-local-password",
        "hrflow.organization.relay.enabled=true",
        "hrflow.organization.relay.initial-delay-ms=3600000",
        "spring.kafka.listener.auto-startup=false"
})
class OrganizationKafkaTest {
    private static final Path KEY_DIRECTORY = temporaryKeys();
    private static final String REGISTRY = "mock://hrflow-organization-it";

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("identity-init.sql");
    @Container
    static final ConfluentKafkaContainer KAFKA = new ConfluentKafkaContainer("confluentinc/cp-kafka:8.2.0");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        properties.add("spring.flyway.user", POSTGRES::getUsername);
        properties.add("spring.flyway.password", POSTGRES::getPassword);
        properties.add("hrflow.organization.relay.job-user", POSTGRES::getUsername);
        properties.add("hrflow.organization.relay.job-password", POSTGRES::getPassword);
        properties.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        properties.add("spring.kafka.properties.schema.registry.url", () -> REGISTRY);
        properties.add("spring.kafka.producer.properties.auto.register.schemas", () -> "true");
        properties.add("hrflow.auth.key-directory", KEY_DIRECTORY::toString);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired OrganizationOutboxRelay relay;

    @Test
    void acknowledgedOrganizationFactIsConsumableFromKafka() throws Exception {
        try (AdminClient admin = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()))) {
            admin.createTopics(List.of(new NewTopic("org.holiday.v1", 1, (short) 1))).all().get();
        }
        UUID tenant = UUID.randomUUID();
        UUID holiday = UUID.randomUUID();
        UUID event = UUID.randomUUID();
        jdbc.update("INSERT INTO identity_org.auth_tenant(id, slug) VALUES (?, ?)", tenant, "kafka-" + tenant);
        jdbc.update("""
                INSERT INTO identity_org.organization_outbox
                    (id, tenant_id, topic, aggregate_id, aggregate_revision, event_type,
                     correlation_id, payload, occurred_at)
                VALUES (?, ?, 'org.holiday.v1', ?, 1, 'org.holiday.added.v1', ?,
                        ?::jsonb, now())
                """, event, tenant, holiday, UUID.randomUUID(), """
                {"holidayId":"%s","date":"2026-11-09","name":"Company Day","kind":"COMPANY"}
                """.formatted(holiday));
        for (int i = 0; i < 12 && sentAt(event) == null; i++) relay.poll();
        assertNotNull(sentAt(event));

        Properties config = new Properties();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "organization-it-" + UUID.randomUUID());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        config.put("schema.registry.url", REGISTRY);
        config.put("specific.avro.reader", true);
        try (KafkaConsumer<String, OrganizationFact> consumer = new KafkaConsumer<>(config)) {
            consumer.subscribe(List.of("org.holiday.v1"));
            var records = consumer.poll(Duration.ofSeconds(10));
            assertEquals(1, records.count());
            var delivered = records.iterator().next();
            assertEquals(tenant + ":" + holiday, delivered.key());
            assertEquals(event.toString(), delivered.value().getEventId());
            assertTrue(delivered.value().getPayload() instanceof HolidaySnapshot);
        }
    }

    private Timestamp sentAt(UUID event) {
        return jdbc.queryForObject("""
                SELECT published_at FROM identity_org.organization_outbox WHERE id = ?
                """, Timestamp.class, event);
    }

    private static Path temporaryKeys() {
        try {
            return Files.createTempDirectory("hrflow-kafka-keys-");
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }
}
