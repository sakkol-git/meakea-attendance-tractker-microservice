package com.hrflow.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.hrflow.attendance.daily.DailyStatusWriter;
import com.hrflow.attendance.service.DailyAttendanceService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = "hrflow.attendance.daily-status.enabled=true")
@Testcontainers
class DailyScheduleHistoryTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine").withInitScript("attendance-wire-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", () -> "attendance_runtime");
        p.add("spring.datasource.password", () -> "wire-only");
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @MockitoBean Clock clock;
    @Autowired DailyStatusWriter writer;
    @Autowired DailyAttendanceService daily;
    @Autowired TransactionTemplate transactions;
    @Autowired tools.jackson.databind.json.JsonMapper json;

    @Test void pastNoSessionDaysKeepOriginalVerifiedScheduleWhileTodayUsesTheEditedShift() throws Exception {
        var admin = admin();
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        UUID shift = AttendanceFixtures.schedule(admin, tenant, employee);
        when(clock.instant()).thenReturn(Instant.parse("2026-10-28T12:00:00Z"));
        LocalDate original = LocalDate.parse("2026-10-28");
        transactions.executeWithoutResult(tx -> writer.refresh(tenant, employee, original));
        when(clock.instant()).thenReturn(Instant.parse("2026-10-29T12:00:00Z"));
        admin.update("UPDATE attendance.shift_projection SET start_time='09:00',end_time='18:00',working_days=0,grace_minutes=20,revision=2 WHERE tenant_id=? AND shift_id=?", tenant, shift);
        transactions.executeWithoutResult(tx -> writer.refresh(tenant, employee, original));
        var historical = daily.snapshot(tenant, employee, original);
        String wire = json.writeValueAsString(historical);
        assertThat(wire).doesNotContain("workingDays", "graceMinutes\":20");
        assertThat(json.readValue(wire, com.hrflow.attendance.daily.DailySnapshot.class).scheduledEnd()).isEqualTo(historical.scheduledEnd());
        assertThat(historical.shiftRevision()).isEqualTo(1);
        assertThat(historical.scheduledStart()).isEqualTo(Instant.parse("2026-10-28T01:00:00Z"));
        assertThat(historical.overview().dayType()).isEqualTo("WORKING_DAY");
        var current = daily.snapshot(tenant, employee, original.plusDays(1));
        assertThat(current.shiftRevision()).isEqualTo(2);
        assertThat(current.overview().dayType()).isEqualTo("DAY_OFF");
        // A holiday is a separate calendar fact and still refreshes the past day using its original shift.
        admin.update("INSERT INTO attendance.holiday_projection(tenant_id,holiday_id,date,name,kind,revision) VALUES (?,?,?,'Synthetic','COMPANY',1)", tenant, UUID.randomUUID(), original);
        transactions.executeWithoutResult(tx -> writer.refresh(tenant, employee, original));
        assertThat(daily.snapshot(tenant, employee, original).overview().dayType()).isEqualTo("HOLIDAY");
        assertThat(daily.snapshot(tenant, employee, original).shiftRevision()).isEqualTo(1);
        assertThat(admin.queryForObject("SELECT count(*) FROM attendance.attendance_evidence WHERE tenant_id=?", Integer.class, tenant)).isZero();
    }

    @Test void anOldUnknownScheduleNeverBecomesAVerifiedHistoricalResultFromTodaysShift() {
        var admin = admin(); UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        AttendanceFixtures.schedule(admin, tenant, employee);
        when(clock.instant()).thenReturn(Instant.parse("2026-10-29T12:00:00Z"));
        LocalDate old = LocalDate.parse("2026-10-28");
        // Legacy materialization lacks the original grace and weekday mask, so it cannot be silently guessed.
        admin.update("INSERT INTO attendance.daily_status(tenant_id,employee_id,work_date,id,revision,payload) VALUES (?,?,?,?,1,'{}')", tenant, employee, old, UUID.randomUUID());
        assertThatThrownBy(() -> daily.snapshot(tenant, employee, old)).isInstanceOfSatisfying(org.springframework.web.ErrorResponseException.class,
                error -> assertThat(error.getStatusCode().value()).isEqualTo(503));
        assertThat(transactions.<Boolean>execute(tx -> writer.refresh(tenant, employee, old))).isFalse();
        assertThat(admin.queryForObject("SELECT count(*) FROM attendance.daily_recalculation WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(1);
        assertThat(admin.queryForObject("SELECT count(*) FROM attendance.attendance_outbox WHERE tenant_id=?", Integer.class, tenant)).isZero();
    }

    @Test void aPastDateNeverObservedBeforeCannotBeInventedFromTodaysShift() {
        var admin = admin(); UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        AttendanceFixtures.schedule(admin, tenant, employee);
        when(clock.instant()).thenReturn(Instant.parse("2026-10-29T12:00:00Z"));
        LocalDate old = LocalDate.parse("2026-10-28");
        assertThatThrownBy(() -> daily.snapshot(tenant, employee, old)).isInstanceOfSatisfying(org.springframework.web.ErrorResponseException.class,
                error -> assertThat(error.getStatusCode().value()).isEqualTo(503));
        assertThat(transactions.<Boolean>execute(tx -> writer.refresh(tenant, employee, old))).isFalse();
        assertThat(admin.queryForObject("SELECT count(*) FROM attendance.daily_schedule WHERE tenant_id=?", Integer.class, tenant)).isZero();
        assertThat(admin.queryForObject("SELECT count(*) FROM attendance.attendance_outbox WHERE tenant_id=?", Integer.class, tenant)).isZero();
    }

    @Test void changedOwnerReferencesCannotAuthorizeAnUnobservedPastDayFromAnEarlierCalendar() {
        var admin = admin(); UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        UUID shift = AttendanceFixtures.schedule(admin, tenant, employee);
        when(clock.instant()).thenReturn(Instant.parse("2026-10-27T12:00:00Z"));
        transactions.executeWithoutResult(tx -> writer.refresh(tenant, employee, LocalDate.parse("2026-10-27")));
        when(clock.instant()).thenReturn(Instant.parse("2026-10-29T12:00:00Z"));
        admin.update("UPDATE attendance.employee_projection SET revision=2 WHERE tenant_id=? AND employee_id=?", tenant, employee);
        assertThatThrownBy(() -> daily.snapshot(tenant, employee, LocalDate.parse("2026-10-28")))
                .isInstanceOf(org.springframework.web.ErrorResponseException.class);
        admin.update("UPDATE attendance.employee_projection SET revision=1 WHERE tenant_id=? AND employee_id=?", tenant, employee);
        admin.update("UPDATE attendance.shift_projection SET revision=2,start_time='09:00' WHERE tenant_id=? AND shift_id=?", tenant, shift);
        assertThatThrownBy(() -> daily.snapshot(tenant, employee, LocalDate.parse("2026-10-28")))
                .isInstanceOf(org.springframework.web.ErrorResponseException.class);
    }

    private JdbcTemplate admin() { return new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())); }
}
