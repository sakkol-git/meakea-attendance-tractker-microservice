package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class ReportingMigrationVerificationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("hrflow")
            .withUsername("hrflow")
            .withPassword("test-only")
            .withInitScript("context-schema.sql");

    @BeforeEach
    void resetSchema() throws Exception {
        try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP SCHEMA IF EXISTS reporting CASCADE");
            stmt.execute("DROP SCHEMA IF EXISTS reporting_jobs CASCADE");
            stmt.execute("CREATE SCHEMA reporting");
            stmt.execute("GRANT ALL ON SCHEMA reporting TO hrflow");
            stmt.execute("GRANT USAGE ON SCHEMA reporting TO reporting_app");
            stmt.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA reporting GRANT ALL ON TABLES TO reporting_app");
        }
    }

    @Test
    @DisplayName("Migration: Existing V1 database upgrades to V2, existing rows remain valid, and Flyway validation passes")
    void v1DatabaseUpgradesToV2AndPreservesExistingData() throws Exception {
        String jdbcUrl = postgres.getJdbcUrl();
        String username = postgres.getUsername();
        String password = postgres.getPassword();

        // 1. Migrate up to V1 only
        Flyway flywayV1 = Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .schemas("reporting")
                .defaultSchema("reporting")
                .target("1")
                .load();
        flywayV1.migrate();

        // 2. Insert an export job row using V1 schema
        UUID tenantId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            try (PreparedStatement setTenant = conn.prepareStatement("SELECT set_config('app.tenant_id', ?, false)")) {
                setTenant.setString(1, tenantId.toString());
                setTenant.execute();
            }

            try (PreparedStatement insert = conn.prepareStatement(
                    """
                    INSERT INTO reporting.export_job
                    (id, tenant_id, requester_id, export_type, status, row_count, version)
                    VALUES (?, ?, ?, 'ATTENDANCE_SUMMARY', 'QUEUED', 0, 0)
                    """)) {
                insert.setObject(1, jobId);
                insert.setObject(2, tenantId);
                insert.setObject(3, requesterId);
                insert.executeUpdate();
            }
        }

        // 3. Migrate from V1 to V2
        Flyway flywayV2 = Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .schemas("reporting")
                .defaultSchema("reporting")
                .load();
        flywayV2.migrate();
        flywayV2.validate(); // Flyway validation succeeds

        // 4. Verify existing row remains intact and valid with from_date/to_date as NULL
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            try (PreparedStatement setTenant = conn.prepareStatement("SELECT set_config('app.tenant_id', ?, false)")) {
                setTenant.setString(1, tenantId.toString());
                setTenant.execute();
            }

            try (PreparedStatement query = conn.prepareStatement(
                    "SELECT id, status, from_date, to_date FROM reporting.export_job WHERE id = ?")) {
                query.setObject(1, jobId);
                try (ResultSet rs = query.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("status")).isEqualTo("QUEUED");
                    assertThat(rs.getDate("from_date")).isNull();
                    assertThat(rs.getDate("to_date")).isNull();
                }
            }

            // 5. Insert new row with V2 columns populated
            UUID newJobId = UUID.randomUUID();
            try (PreparedStatement insertV2 = conn.prepareStatement(
                    """
                    INSERT INTO reporting.export_job
                    (id, tenant_id, requester_id, export_type, from_date, to_date, status, row_count, version)
                    VALUES (?, ?, ?, 'ATTENDANCE_SUMMARY', '2026-10-01', '2026-10-15', 'QUEUED', 0, 0)
                    """)) {
                insertV2.setObject(1, newJobId);
                insertV2.setObject(2, tenantId);
                insertV2.setObject(3, requesterId);
                insertV2.executeUpdate();
            }

            try (PreparedStatement query = conn.prepareStatement(
                    "SELECT from_date, to_date FROM reporting.export_job WHERE id = ?")) {
                query.setObject(1, newJobId);
                try (ResultSet rs = query.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getDate("from_date")).isEqualTo(java.sql.Date.valueOf("2026-10-01"));
                    assertThat(rs.getDate("to_date")).isEqualTo(java.sql.Date.valueOf("2026-10-15"));
                }
            }
        }
    }

    @Test
    @DisplayName("Migration: Fresh installation applies all migrations and preserves export rows")
    void freshInstallationPreservesExportRows() throws Exception {
        String jdbcUrl = postgres.getJdbcUrl();
        String username = postgres.getUsername();
        String password = postgres.getPassword();

        // 1. Fresh installation runs all current migrations.
        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .schemas("reporting")
                .defaultSchema("reporting")
                .load();
        var result = flyway.migrate();
        assertThat(result.migrationsExecuted).isEqualTo(9);
        flyway.validate();

        UUID tenantId = UUID.randomUUID();
        UUID legacyJobId = UUID.randomUUID();
        UUID rangeJobId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            try (PreparedStatement setTenant = conn.prepareStatement("SELECT set_config('app.tenant_id', ?, false)")) {
                setTenant.setString(1, tenantId.toString());
                setTenant.execute();
            }

            // 2. Insert row using legacy columns only (without date filter)
            try (PreparedStatement insertLegacy = conn.prepareStatement(
                    """
                    INSERT INTO reporting.export_job
                    (id, tenant_id, requester_id, export_type, status, row_count, version)
                    VALUES (?, ?, ?, 'ATTENDANCE_SUMMARY', 'QUEUED', 0, 0)
                    """)) {
                insertLegacy.setObject(1, legacyJobId);
                insertLegacy.setObject(2, tenantId);
                insertLegacy.setObject(3, requesterId);
                insertLegacy.executeUpdate();
            }

            // 3. Insert row using date filter columns (V2 columns)
            try (PreparedStatement insertRange = conn.prepareStatement(
                    """
                    INSERT INTO reporting.export_job
                    (id, tenant_id, requester_id, export_type, from_date, to_date, status, row_count, version)
                    VALUES (?, ?, ?, 'ATTENDANCE_SUMMARY', '2026-10-01', '2026-10-15', 'COMPLETED', 15, 1)
                    """)) {
                insertRange.setObject(1, rangeJobId);
                insertRange.setObject(2, tenantId);
                insertRange.setObject(3, requesterId);
                insertRange.executeUpdate();
            }

            // 4. Verify legacy export job row is preserved with NULL dates
            try (PreparedStatement queryLegacy = conn.prepareStatement(
                    "SELECT id, status, from_date, to_date, row_count FROM reporting.export_job WHERE id = ?")) {
                queryLegacy.setObject(1, legacyJobId);
                try (ResultSet rs = queryLegacy.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("status")).isEqualTo("QUEUED");
                    assertThat(rs.getDate("from_date")).isNull();
                    assertThat(rs.getDate("to_date")).isNull();
                    assertThat(rs.getInt("row_count")).isEqualTo(0);
                }
            }

            // 5. Verify date-range export job row is preserved with populated dates
            try (PreparedStatement queryRange = conn.prepareStatement(
                    "SELECT id, status, from_date, to_date, row_count FROM reporting.export_job WHERE id = ?")) {
                queryRange.setObject(1, rangeJobId);
                try (ResultSet rs = queryRange.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("status")).isEqualTo("COMPLETED");
                    assertThat(rs.getDate("from_date")).isEqualTo(java.sql.Date.valueOf("2026-10-01"));
                    assertThat(rs.getDate("to_date")).isEqualTo(java.sql.Date.valueOf("2026-10-15"));
                    assertThat(rs.getInt("row_count")).isEqualTo(15);
                }
            }
        }
    }
}
