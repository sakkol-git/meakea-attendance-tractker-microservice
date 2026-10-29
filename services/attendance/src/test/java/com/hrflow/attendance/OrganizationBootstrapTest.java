package com.hrflow.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hrflow.attendance.organization.OrganizationBootstrap;
import com.hrflow.attendance.organization.OrganizationProjectionService;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
class OrganizationBootstrapTest {
    private static final String REGISTRY = "mock://attendance-organization-bootstrap-it";
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("context-schema.sql");
    @Container
    static final ConfluentKafkaContainer KAFKA = new ConfluentKafkaContainer("confluentinc/cp-kafka:8.2.0");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.flyway.user", POSTGRES::getUsername);
        properties.add("spring.flyway.password", POSTGRES::getPassword);
        properties.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        properties.add("spring.kafka.properties.schema.registry.url", () -> REGISTRY);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired OrganizationProjectionService projections;

    @Test
    void newAttendanceInstanceReplaysCompactedOwnerSnapshotAndRejectsBadReplay() throws Exception {
        try (AdminClient admin = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()))) {
            admin.createTopics(List.of(
                    new NewTopic("org.employee.v1", 1, (short) 1),
                    new NewTopic("org.shift.v1", 1, (short) 1),
                    new NewTopic("org.holiday.v1", 1, (short) 1))).all().get();
        }
        UUID tenant = UUID.randomUUID();
        UUID holiday = UUID.randomUUID();
        OrganizationFact snapshot = fact(tenant, holiday, "Company Day");
        try (KafkaProducer<String, OrganizationFact> producer = producer()) {
            producer.send(new ProducerRecord<>("org.holiday.v1", tenant + ":" + holiday, snapshot)).get();
        }
        OrganizationBootstrap bootstrap = bootstrap();
        bootstrap.run(null);
        assertEquals(1, count("attendance.holiday_projection", tenant));
        assertEquals(1, count("attendance.organization_inbox", tenant));
        bootstrap.run(null);
        assertEquals(1, count("attendance.organization_inbox", tenant));

        try (KafkaProducer<String, OrganizationFact> producer = producer()) {
            producer.send(new ProducerRecord<>("org.holiday.v1", "wrong-key",
                    fact(tenant, UUID.randomUUID(), "Bad Day"))).get();
        }
        assertThrows(IllegalArgumentException.class, () -> bootstrap.run(null));
        assertEquals(1, count("attendance.holiday_projection", tenant));
    }

    private OrganizationBootstrap bootstrap() {
        OrganizationBootstrap runner = new OrganizationBootstrap(projections);
        ReflectionTestUtils.setField(runner, "brokers", KAFKA.getBootstrapServers());
        ReflectionTestUtils.setField(runner, "registry", REGISTRY);
        ReflectionTestUtils.setField(runner, "timeoutMillis", 15000L);
        return runner;
    }

    private KafkaProducer<String, OrganizationFact> producer() {
        Properties config = new Properties();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put("schema.registry.url", REGISTRY);
        config.put("auto.register.schemas", true);
        return new KafkaProducer<>(config);
    }

    private OrganizationFact fact(UUID tenant, UUID holiday, String name) {
        return new OrganizationFact(UUID.randomUUID().toString(), "org.holiday.added.v1", 1,
                tenant.toString(), holiday.toString(), 1L, "2026-10-27T00:00:00Z",
                UUID.randomUUID().toString(), null,
                new HolidaySnapshot(holiday.toString(), "2026-11-09", name, "COMPANY", null));
    }

    private int count(String table, UUID tenant) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE tenant_id = ?", Integer.class, tenant);
    }
}
