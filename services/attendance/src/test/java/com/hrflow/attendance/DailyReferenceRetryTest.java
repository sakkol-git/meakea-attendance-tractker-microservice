package com.hrflow.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.attendance.daily.DailyStatusWriter;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = "hrflow.attendance.daily-status.enabled=true")
@Testcontainers
class DailyReferenceRetryTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine").withInitScript("attendance-wire-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", () -> "attendance_runtime");
        p.add("spring.datasource.password", () -> "wire-only");
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @Autowired DailyStatusWriter writer;
    @Autowired TransactionTemplate transactions;

    @Test void unknownEmployeeCommitsRetryThenConvergesAfterReferencesArrive() {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        var date = LocalDate.now(java.time.ZoneId.of("Asia/Phnom_Penh"));
        var admin = admin();
        assertThat(transactions.<Boolean>execute(tx -> writer.refresh(tenant, employee, date))).isFalse();
        assertThat(count(admin, "daily_recalculation", tenant)).isEqualTo(1);
        assertThat(count(admin, "daily_status", tenant)).isZero();
        assertThat(count(admin, "attendance_outbox", tenant)).isZero();
        AttendanceFixtures.schedule(admin, tenant, employee);
        assertThat(transactions.<Boolean>execute(tx -> writer.refresh(tenant, employee, date))).isTrue();
        assertThat(count(admin, "daily_recalculation", tenant)).isZero();
        assertThat(count(admin, "daily_status", tenant)).isEqualTo(1);
        assertThat(count(admin, "attendance_outbox", tenant)).isEqualTo(1);
        assertThat(count(admin, "attendance_evidence", tenant)).isZero();
    }

    @Test void explicitInactiveConflictStillRollsBackRatherThanBecomingAReferenceRetry() {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        var admin = admin();
        AttendanceFixtures.schedule(admin, tenant, employee);
        admin.update("UPDATE attendance.employee_projection SET status='TERMINATED' WHERE tenant_id=?", tenant);
        assertThatThrownBy(() -> transactions.execute(tx -> writer.refresh(tenant, employee, LocalDate.now())))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("409");
        assertThat(count(admin, "daily_recalculation", tenant)).isZero();
        assertThat(count(admin, "daily_status", tenant)).isZero();
        assertThat(count(admin, "attendance_outbox", tenant)).isZero();
    }

    private JdbcTemplate admin() {
        return new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
    }
    private int count(JdbcTemplate jdbc, String table, UUID tenant) {
        return jdbc.queryForObject("SELECT count(*) FROM attendance." + table + " WHERE tenant_id=?", Integer.class, tenant);
    }
}
