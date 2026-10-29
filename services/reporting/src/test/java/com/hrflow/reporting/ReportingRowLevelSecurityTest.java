package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@Testcontainers
class ReportingRowLevelSecurityTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("hrflow")
            .withUsername("hrflow")
            .withPassword("test-only")
            .withInitScript("context-schema.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    private static Connection getAppConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), "reporting_app", "test-pass");
        conn.setAutoCommit(false);
        return conn;
    }

    @BeforeAll
    static void grantPermissions() throws SQLException {
        // Flyway migration creates tables as hrflow superuser; grant permissions to reporting_app
        try (Connection superConn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = superConn.createStatement()) {
            stmt.execute("GRANT ALL ON ALL TABLES IN SCHEMA reporting TO reporting_app");
        }
    }

    @Test
    @DisplayName("RLS: Non-superuser NOBYPASSRLS role succeeds for same-tenant reads and writes")
    void sameTenantAccessSucceeds() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        try (Connection conn = getAppConnection()) {
            // Set transaction-local tenant context (is_local = true)
            try (PreparedStatement setTenant = conn.prepareStatement("SELECT set_config('app.tenant_id', ?, true)")) {
                setTenant.setString(1, tenantA.toString());
                setTenant.execute();
            }

            // Insert attendance fact for tenantA
            try (PreparedStatement insert = conn.prepareStatement(
                    """
                    INSERT INTO reporting.attendance_fact_projection
                    (tenant_id, session_id, employee_id, work_date, status, minutes_late, minutes_worked, aggregate_revision)
                    VALUES (?, ?, ?, '2026-10-20', 'PRESENT', 0, 480, 1)
                    """)) {
                insert.setObject(1, tenantA);
                insert.setObject(2, sessionId);
                insert.setObject(3, employeeId);
                insert.executeUpdate();
            }
            conn.commit();

            // Query as tenantA in a new transaction with transaction-local context
            try (PreparedStatement setTenant = conn.prepareStatement("SELECT set_config('app.tenant_id', ?, true)")) {
                setTenant.setString(1, tenantA.toString());
                setTenant.execute();
            }
            try (PreparedStatement query = conn.prepareStatement(
                    "SELECT count(*) FROM reporting.attendance_fact_projection WHERE session_id = ?")) {
                query.setObject(1, sessionId);
                try (ResultSet rs = query.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).isEqualTo(1);
                }
            }
            conn.commit();
        }
    }

    @Test
    @DisplayName("RLS: Transaction-local set_config resets on commit and does not leak across reused connection")
    void transactionLocalContextResetsOnCommitAndCannotLeak() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        try (Connection conn = getAppConnection()) {
            // 1. Transaction 1: Set transaction-local tenant context (is_local = true)
            try (PreparedStatement setTenant = conn.prepareStatement("SELECT set_config('app.tenant_id', ?, true)")) {
                setTenant.setString(1, tenantA.toString());
                setTenant.execute();
            }

            try (PreparedStatement insert = conn.prepareStatement(
                    """
                    INSERT INTO reporting.attendance_fact_projection
                    (tenant_id, session_id, employee_id, work_date, status, minutes_late, minutes_worked, aggregate_revision)
                    VALUES (?, ?, ?, '2026-10-20', 'PRESENT', 0, 480, 1)
                    """)) {
                insert.setObject(1, tenantA);
                insert.setObject(2, sessionId);
                insert.setObject(3, employeeId);
                insert.executeUpdate();
            }

            // Commit transaction 1
            conn.commit();

            // 2. Transaction 2 on same reused connection without setting app.tenant_id:
            // Must verify transaction-local context has completely reset to empty/null
            try (PreparedStatement check = conn.prepareStatement("SELECT current_setting('app.tenant_id', true)")) {
                try (ResultSet rs = check.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    String setting = rs.getString(1);
                    assertThat(setting == null || setting.isEmpty()).isTrue();
                }
            }

            // Must verify query without tenant context fails closed (returns 0 rows, cannot leak tenantA row)
            try (PreparedStatement query = conn.prepareStatement(
                    "SELECT count(*) FROM reporting.attendance_fact_projection WHERE session_id = ?")) {
                query.setObject(1, sessionId);
                try (ResultSet rs = query.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1))
                            .as("Unset tenant context after commit on reused connection must see 0 rows")
                            .isEqualTo(0);
                }
            }
            conn.commit();
        }
    }

    @Test
    @DisplayName("RLS: Non-superuser NOBYPASSRLS role conceals foreign-tenant data completely")
    void foreignTenantReadsAreConcealed() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID sessionA = UUID.randomUUID();
        UUID sessionB = UUID.randomUUID();

        // 1. Insert tenantA row
        try (Connection connA = getAppConnection()) {
            try (PreparedStatement setTenant = connA.prepareStatement("SELECT set_config('app.tenant_id', ?, true)")) {
                setTenant.setString(1, tenantA.toString());
                setTenant.execute();
            }
            try (PreparedStatement insert = connA.prepareStatement(
                    """
                    INSERT INTO reporting.attendance_fact_projection
                    (tenant_id, session_id, employee_id, work_date, status, minutes_late, minutes_worked, aggregate_revision)
                    VALUES (?, ?, ?, '2026-10-20', 'PRESENT', 0, 480, 1)
                    """)) {
                insert.setObject(1, tenantA);
                insert.setObject(2, sessionA);
                insert.setObject(3, UUID.randomUUID());
                insert.executeUpdate();
            }
            connA.commit();
        }

        // 2. Insert tenantB row
        try (Connection connB = getAppConnection()) {
            try (PreparedStatement setTenant = connB.prepareStatement("SELECT set_config('app.tenant_id', ?, true)")) {
                setTenant.setString(1, tenantB.toString());
                setTenant.execute();
            }
            try (PreparedStatement insert = connB.prepareStatement(
                    """
                    INSERT INTO reporting.attendance_fact_projection
                    (tenant_id, session_id, employee_id, work_date, status, minutes_late, minutes_worked, aggregate_revision)
                    VALUES (?, ?, ?, '2026-10-20', 'LATE', 15, 465, 1)
                    """)) {
                insert.setObject(1, tenantB);
                insert.setObject(2, sessionB);
                insert.setObject(3, UUID.randomUUID());
                insert.executeUpdate();
            }
            connB.commit();
        }

        // 3. Connect as tenantA -> tenantB row is completely concealed
        try (Connection connA = getAppConnection()) {
            try (PreparedStatement setTenant = connA.prepareStatement("SELECT set_config('app.tenant_id', ?, true)")) {
                setTenant.setString(1, tenantA.toString());
                setTenant.execute();
            }

            // Query specific foreign session -> 0 rows returned
            try (PreparedStatement query = connA.prepareStatement(
                    "SELECT count(*) FROM reporting.attendance_fact_projection WHERE session_id = ?")) {
                query.setObject(1, sessionB);
                try (ResultSet rs = query.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).isEqualTo(0);
                }
            }

            // Query all rows without session filter -> sees only tenantA rows
            try (PreparedStatement query = connA.prepareStatement(
                    "SELECT session_id FROM reporting.attendance_fact_projection")) {
                try (ResultSet rs = query.executeQuery()) {
                    while (rs.next()) {
                        UUID foundSession = rs.getObject(1, UUID.class);
                        assertThat(foundSession).isNotEqualTo(sessionB);
                    }
                }
            }
            connA.commit();
        }
    }

    @Test
    @DisplayName("RLS: Missing tenant context fails closed (0 rows returned)")
    void missingTenantContextFailsClosed() throws Exception {
        // Connect without setting app.tenant_id
        try (Connection conn = getAppConnection()) {
            try (PreparedStatement query = conn.prepareStatement(
                    "SELECT count(*) FROM reporting.attendance_fact_projection")) {
                try (ResultSet rs = query.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1))
                            .as("Unauthenticated or missing tenant context must see 0 rows")
                            .isEqualTo(0);
                }
            }
            conn.commit();
        }
    }

    @Test
    @DisplayName("RLS: Foreign-tenant write is denied and fails closed")
    void foreignTenantWriteFailsClosed() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        try (Connection conn = getAppConnection()) {
            // Context set to tenantA
            try (PreparedStatement setTenant = conn.prepareStatement("SELECT set_config('app.tenant_id', ?, true)")) {
                setTenant.setString(1, tenantA.toString());
                setTenant.execute();
            }

            // Attempt to write a row belonging to tenantB
            try (PreparedStatement insert = conn.prepareStatement(
                    """
                    INSERT INTO reporting.attendance_fact_projection
                    (tenant_id, session_id, employee_id, work_date, status, minutes_late, minutes_worked, aggregate_revision)
                    VALUES (?, ?, ?, '2026-10-20', 'PRESENT', 0, 480, 1)
                    """)) {
                insert.setObject(1, tenantB);
                insert.setObject(2, UUID.randomUUID());
                insert.setObject(3, UUID.randomUUID());

                assertThatThrownBy(insert::executeUpdate)
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("row-level security policy");
            }
            conn.rollback();
        }
    }

    @Test
    @DisplayName("RPT-15: Application role may insert and tenant-read audit records")
    void auditLogAllowsInsertAndTenantRead() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        try (Connection conn = getAppConnection()) {
            // Set transaction-local tenant context to tenantA
            try (PreparedStatement setTenant = conn.prepareStatement("SELECT set_config('app.tenant_id', ?, true)")) {
                setTenant.setString(1, tenantA.toString());
                setTenant.execute();
            }

            // Insert audit record for tenantA
            try (PreparedStatement insert = conn.prepareStatement(
                    """
                    INSERT INTO reporting.audit_log
                    (tenant_id, actor_id, actor_email, action, resource_type, resource_id)
                    VALUES (?, ?, 'user@alpha.test', 'CREATE_EXPORT', 'REPORT', 'exp-1')
                    """)) {
                insert.setObject(1, tenantA);
                insert.setObject(2, actorId);
                int affected = insert.executeUpdate();
                assertThat(affected).isEqualTo(1);
            }
            conn.commit();

            // Read as tenantA
            try (PreparedStatement setTenant = conn.prepareStatement("SELECT set_config('app.tenant_id', ?, true)")) {
                setTenant.setString(1, tenantA.toString());
                setTenant.execute();
            }
            try (PreparedStatement query = conn.prepareStatement(
                    "SELECT count(*) FROM reporting.audit_log WHERE tenant_id = ?")) {
                query.setObject(1, tenantA);
                try (ResultSet rs = query.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).isEqualTo(1);
                }
            }
            conn.commit();

            // Read as tenantB -> 0 rows (tenant concealment)
            try (PreparedStatement setTenant = conn.prepareStatement("SELECT set_config('app.tenant_id', ?, true)")) {
                setTenant.setString(1, tenantB.toString());
                setTenant.execute();
            }
            try (PreparedStatement query = conn.prepareStatement(
                    "SELECT count(*) FROM reporting.audit_log WHERE tenant_id = ?")) {
                query.setObject(1, tenantA);
                try (ResultSet rs = query.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).isEqualTo(0);
                }
            }
            conn.commit();
        }
    }

    @Test
    @DisplayName("RPT-15: UPDATE on audit_log must fail at database level")
    void auditLogUpdateFailsAtDatabaseLevel() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        try (Connection conn = getAppConnection()) {
            try (PreparedStatement setTenant = conn.prepareStatement("SELECT set_config('app.tenant_id', ?, true)")) {
                setTenant.setString(1, tenantA.toString());
                setTenant.execute();
            }

            try (PreparedStatement insert = conn.prepareStatement(
                    """
                    INSERT INTO reporting.audit_log
                    (tenant_id, actor_id, actor_email, action, resource_type, resource_id)
                    VALUES (?, ?, 'user@alpha.test', 'CREATE_EXPORT', 'REPORT', 'exp-1')
                    """)) {
                insert.setObject(1, tenantA);
                insert.setObject(2, actorId);
                insert.executeUpdate();
            }
            conn.commit();

            // Now attempt UPDATE on audit_log as application role
            try (PreparedStatement setTenant = conn.prepareStatement("SELECT set_config('app.tenant_id', ?, true)")) {
                setTenant.setString(1, tenantA.toString());
                setTenant.execute();
            }
            try (PreparedStatement update = conn.prepareStatement(
                    "UPDATE reporting.audit_log SET action = 'TAMPERED' WHERE tenant_id = ?")) {
                update.setObject(1, tenantA);
                assertThatThrownBy(update::executeUpdate)
                        .as("UPDATE on reporting.audit_log must fail at database level")
                        .isInstanceOf(SQLException.class);
            }
            conn.rollback();
        }
    }

    @Test
    @DisplayName("RPT-15: DELETE on audit_log must fail at database level")
    void auditLogDeleteFailsAtDatabaseLevel() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        try (Connection conn = getAppConnection()) {
            try (PreparedStatement setTenant = conn.prepareStatement("SELECT set_config('app.tenant_id', ?, true)")) {
                setTenant.setString(1, tenantA.toString());
                setTenant.execute();
            }

            try (PreparedStatement insert = conn.prepareStatement(
                    """
                    INSERT INTO reporting.audit_log
                    (tenant_id, actor_id, actor_email, action, resource_type, resource_id)
                    VALUES (?, ?, 'user@alpha.test', 'CREATE_EXPORT', 'REPORT', 'exp-1')
                    """)) {
                insert.setObject(1, tenantA);
                insert.setObject(2, actorId);
                insert.executeUpdate();
            }
            conn.commit();

            // Now attempt DELETE on audit_log as application role
            try (PreparedStatement setTenant = conn.prepareStatement("SELECT set_config('app.tenant_id', ?, true)")) {
                setTenant.setString(1, tenantA.toString());
                setTenant.execute();
            }
            try (PreparedStatement delete = conn.prepareStatement(
                    "DELETE FROM reporting.audit_log WHERE tenant_id = ?")) {
                delete.setObject(1, tenantA);
                assertThatThrownBy(delete::executeUpdate)
                        .as("DELETE on reporting.audit_log must fail at database level")
                        .isInstanceOf(SQLException.class);
            }
            conn.rollback();
        }
    }
}
