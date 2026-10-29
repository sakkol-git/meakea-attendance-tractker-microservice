package com.hrflow.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.hrflow.attendance.daily.ScheduleDailyChanges;
import com.hrflow.attendance.organization.OrganizationProjectionService;
import com.hrflow.contracts.organization.OrganizationFact;
import com.hrflow.contracts.organization.ShiftSnapshot;
import com.hrflow.contracts.organization.SiteSnapshot;
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

@SpringBootTest
@Testcontainers
class ScheduleFanoutBoundTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine").withInitScript("attendance-wire-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl); p.add("spring.datasource.username", () -> "attendance_runtime");
        p.add("spring.datasource.password", () -> "wire-only"); p.add("spring.flyway.user", POSTGRES::getUsername); p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @Autowired OrganizationProjectionService organization;
    @Autowired ScheduleDailyChanges changes;
    @Autowired TransactionTemplate transactions;
    @Autowired JdbcTemplate runtime;
    @MockitoBean Clock clock;

    @Test void shiftChangesResumeEveryCurrentAndFutureObservedDayAndKeepPastDaysOutOfTheQueue() {
        when(clock.instant()).thenReturn(Instant.parse("2026-10-28T18:00:00Z")); // 29 October in the captured zone.
        var admin = admin(); UUID tenant = UUID.randomUUID(), foreign = UUID.randomUUID(), shift = UUID.randomUUID();
        String payload = "{\"shiftId\":\"" + shift + "\",\"timezone\":\"Asia/Phnom_Penh\"}";
        for (UUID selected : java.util.List.of(tenant, foreign)) for (int index = 0; index < 205; index++) {
            UUID employee = UUID.randomUUID();
            for (String date : java.util.List.of("2026-10-28", "2026-10-29"))
                admin.update("INSERT INTO attendance.daily_status(tenant_id,employee_id,work_date,id,revision,payload) VALUES (?,?,?,?,1,?::jsonb)",
                        selected, employee, LocalDate.parse(date), UUID.randomUUID(), payload);
        }
        var body = new ShiftSnapshot(shift.toString(), "Synthetic", "09:00", "18:00", "Asia/Phnom_Penh", 10, 127,
                new SiteSnapshot(UUID.randomUUID().toString(), "Synthetic", 11.56, 104.92, 100.0, "Asia/Phnom_Penh", null, null), null);
        var event = new OrganizationFact(UUID.randomUUID().toString(), "org.shift.updated.v1", 1, tenant.toString(), shift.toString(), 2L,
                clock.instant().toString(), UUID.randomUUID().toString(), null, body);
        assertThatThrownBy(() -> transactions.executeWithoutResult(tx -> {
            organization.apply("org.shift.v1", tenant + ":" + shift, event); throw new IllegalStateException("rollback");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(admin.queryForObject("SELECT count(*) FROM attendance.schedule_daily_change", Integer.class)).isZero();
        organization.apply("org.shift.v1", tenant + ":" + shift, event);
        for (int expected : java.util.List.of(100, 200, 205)) {
            changes.expandNext(tenant);
            assertThat(admin.queryForObject("SELECT count(*) FROM attendance.daily_recalculation WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(expected);
        }
        changes.expandNext(tenant);
        assertThat(admin.queryForObject("SELECT count(*) FROM attendance.schedule_daily_change", Integer.class)).isZero();
        assertThat(admin.queryForObject("SELECT count(*) FROM attendance.daily_recalculation WHERE work_date='2026-10-28' OR tenant_id=?", Integer.class, foreign)).isZero();
        assertThat(runtime.queryForObject("SELECT count(*) FROM attendance.daily_recalculation", Integer.class)).isZero();
        assertThat(organization.apply("org.shift.v1", tenant + ":" + shift, event)).isEqualTo(OrganizationProjectionService.Result.DUPLICATE);
        assertThat(admin.queryForObject("SELECT count(*) FROM attendance.schedule_daily_change", Integer.class)).isZero();
    }
    private JdbcTemplate admin() { return new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())); }
}
