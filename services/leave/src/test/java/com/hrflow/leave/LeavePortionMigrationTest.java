package com.hrflow.leave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class LeavePortionMigrationTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("leave-upgrade-schema.sql");

    @Test void nonBypassOwnerUpgradePreservesLegacyReservationsAndRestoresForcedTenantPolicies() throws Exception {
        flyway("4").migrate();
        String first = UUID.randomUUID().toString(), second = UUID.randomUUID().toString(), employee = UUID.randomUUID().toString();
        String full = UUID.randomUUID().toString(), morning = UUID.randomUUID().toString();
        try (var owner = DriverManager.getConnection(POSTGRES.getJdbcUrl(), "leave_migrator", "upgrade-only")) {
            seed(owner, first, employee, full, "FULL_DAY");
            seed(owner, second, employee, morning, "MORNING");
        }
        flyway("5").migrate();
        flyway("5").validate();
        try (var runtime = DriverManager.getConnection(POSTGRES.getJdbcUrl(), "leave_upgrade_runtime", "runtime-only")) {
            assertThat(count(runtime)).isZero();
            scope(runtime, first);
            assertThat(count(runtime)).isEqualTo(2);
            assertThatThrownBy(() -> {
                try (var insert = runtime.prepareStatement("INSERT INTO leave.day_reservation(tenant_id,employee_id,date,portion,request_id) VALUES (?,?,'2026-11-02','MORNING',?)")) {
                    insert.setString(1, first); insert.setString(2, employee); insert.setString(3, full); insert.executeUpdate();
                }
            }).isInstanceOf(java.sql.SQLException.class).satisfies(error -> assertThat(((java.sql.SQLException) error).getSQLState()).isEqualTo("23505"));
            scope(runtime, second);
            assertThat(count(runtime)).isEqualTo(1);
            try (var flags = runtime.createStatement().executeQuery("""
                    SELECT bool_and(c.relrowsecurity AND c.relforcerowsecurity)
                    FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                    WHERE n.nspname='leave' AND c.relname IN ('leave_day','leave_request','day_reservation')
                    """)) { flags.next(); assertThat(flags.getBoolean(1)).isTrue(); }
        }
    }

    private Flyway flyway(String target) {
        return Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), "leave_migrator", "upgrade-only")
                .schemas("leave").defaultSchema("leave").target(target).load();
    }
    private void seed(Connection connection, String tenant, String employee, String request, String portion) throws Exception {
        scope(connection, tenant);
        try (var insert = connection.prepareStatement("""
                INSERT INTO leave.leave_request(id,tenant_id,employee_id,type,start_date,end_date,portion,days,status)
                VALUES (?,?,?,'ANNUAL','2026-11-02','2026-11-02',?,1,'APPROVED')
                """)) {
            insert.setString(1, request); insert.setString(2, tenant); insert.setString(3, employee); insert.setString(4, portion); insert.executeUpdate();
        }
        try (var insert = connection.prepareStatement("""
                INSERT INTO leave.leave_day(id,tenant_id,request_id,employee_id,date,portion,hours)
                VALUES (?,?,?,?,'2026-11-02',?,8)
                """)) {
            insert.setString(1, UUID.randomUUID().toString()); insert.setString(2, tenant); insert.setString(3, request);
            insert.setString(4, employee); insert.setString(5, portion); insert.executeUpdate();
        }
    }
    private void scope(Connection connection, String tenant) throws Exception {
        try (var query = connection.prepareStatement("SELECT set_config('app.tenant_id',?,false)")) { query.setString(1, tenant); query.execute(); }
    }
    private int count(Connection connection) throws Exception {
        try (var result = connection.createStatement().executeQuery("SELECT count(*) FROM leave.day_reservation")) { result.next(); return result.getInt(1); }
    }
}
