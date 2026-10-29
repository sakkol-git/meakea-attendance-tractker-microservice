package com.hrflow.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@Testcontainers
class ConstrainOwnerOutboxConsumerInboxTest {

    @Container
    static final PostgreSQLContainer<?> database = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("hrflow")
            .withUsername("hrflow")
            .withPassword("test-only")
            .withInitScript("context-schema.sql");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", database::getJdbcUrl);
        properties.add("spring.datasource.username", database::getUsername);
        properties.add("spring.datasource.password", database::getPassword);
        properties.add("spring.flyway.user", database::getUsername);
        properties.add("spring.flyway.password", database::getPassword);
    }

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("Clean migration passes and duplicate consumer receipt fails its constraint")
    void duplicateConsumerReceiptFailsConstraint() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        String consumerName = "notification-consumer";
        String topic = "org.attendance.session.created.v1";

        try (Connection conn = jdbc.getDataSource().getConnection()) {
            conn.createStatement().execute("SET ROLE notification_app");
            conn.createStatement().execute("SET app.tenant_id = '" + tenantId + "'");

            // First receipt succeeds
            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO notification.notification_inbox (consumer_name, tenant_id, event_id, topic, aggregate_id) "
                            + "VALUES (?, ?, ?, ?, ?)")) {
                insert.setString(1, consumerName);
                insert.setObject(2, tenantId);
                insert.setObject(3, eventId);
                insert.setString(4, topic);
                insert.setObject(5, aggregateId);
                insert.executeUpdate();
            }

            // Duplicate receipt for same (consumer, tenant, event) must fail primary key constraint
            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO notification.notification_inbox (consumer_name, tenant_id, event_id, topic, aggregate_id) "
                            + "VALUES (?, ?, ?, ?, ?)")) {
                insert.setString(1, consumerName);
                insert.setObject(2, tenantId);
                insert.setObject(3, eventId);
                insert.setString(4, topic);
                insert.setObject(5, aggregateId);
                assertThatThrownBy(insert::executeUpdate)
                        .isInstanceOf(java.sql.SQLException.class)
                        .hasMessageContaining("duplicate key");
            }
        }
    }

    @Test
    @DisplayName("Row level security hides foreign tenant rows")
    void foreignTenantRowsRemainHiddenUnderRLS() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID employeeA = UUID.randomUUID();
        UUID employeeB = UUID.randomUUID();

        try (Connection conn = jdbc.getDataSource().getConnection()) {
            conn.createStatement().execute("SET ROLE notification_app");

            // Under Tenant A, insert a notification record
            try (var setTenant = conn.prepareStatement("SET app.tenant_id = '" + tenantA + "'")) {
                setTenant.execute();
            }
            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO notification.notification_record (tenant_id, recipient_id, title, body) VALUES (?, ?, ?, ?)")) {
                insert.setObject(1, tenantA);
                insert.setObject(2, employeeA);
                insert.setString(3, "Welcome Tenant A");
                insert.setString(4, "First message");
                insert.executeUpdate();
            }

            // Verify Tenant A sees 1 record
            try (PreparedStatement query = conn.prepareStatement("SELECT count(*) FROM notification.notification_record")) {
                try (ResultSet rs = query.executeQuery()) {
                    rs.next();
                    assertThat(rs.getLong(1)).isEqualTo(1L);
                }
            }

            // Switch to Tenant B
            try (var setTenant = conn.prepareStatement("SET app.tenant_id = '" + tenantB + "'")) {
                setTenant.execute();
            }

            // Verify Tenant B sees 0 records
            try (PreparedStatement query = conn.prepareStatement("SELECT count(*) FROM notification.notification_record")) {
                try (ResultSet rs = query.executeQuery()) {
                    rs.next();
                    assertThat(rs.getLong(1)).isEqualTo(0L);
                }
            }

            // Insert under Tenant B
            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO notification.notification_record (tenant_id, recipient_id, title, body) VALUES (?, ?, ?, ?)")) {
                insert.setObject(1, tenantB);
                insert.setObject(2, employeeB);
                insert.setString(3, "Welcome Tenant B");
                insert.setString(4, "Second message");
                insert.executeUpdate();
            }

            // Verify Tenant B sees only its 1 record
            try (PreparedStatement query = conn.prepareStatement("SELECT count(*) FROM notification.notification_record")) {
                try (ResultSet rs = query.executeQuery()) {
                    rs.next();
                    assertThat(rs.getLong(1)).isEqualTo(1L);
                }
            }

            // Switch back to Tenant A
            try (var setTenant = conn.prepareStatement("SET app.tenant_id = '" + tenantA + "'")) {
                setTenant.execute();
            }
            try (PreparedStatement query = conn.prepareStatement("SELECT count(*) FROM notification.notification_record")) {
                try (ResultSet rs = query.executeQuery()) {
                    rs.next();
                    assertThat(rs.getLong(1)).isEqualTo(1L);
                }
            }
        }
    }
}
