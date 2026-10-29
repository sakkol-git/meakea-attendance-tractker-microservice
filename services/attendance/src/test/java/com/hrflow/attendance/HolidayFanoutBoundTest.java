package com.hrflow.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.attendance.daily.HolidayDailyChanges;
import com.hrflow.attendance.organization.OrganizationProjectionService;
import com.hrflow.contracts.organization.HolidaySnapshot;
import com.hrflow.contracts.organization.OrganizationFact;
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

@SpringBootTest
@Testcontainers
class HolidayFanoutBoundTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine").withInitScript("attendance-wire-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", () -> "attendance_runtime");
        p.add("spring.datasource.password", () -> "wire-only");
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @Autowired OrganizationProjectionService organization;
    @Autowired HolidayDailyChanges changes;
    @Autowired TransactionTemplate transactions;
    @Autowired JdbcTemplate runtime;

    @Test void aLargeHolidayMoveResumesAllObservedKeysWithoutDroppingRowsOrCrossingTenants() {
        var admin = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        UUID tenant = UUID.randomUUID(), foreign = UUID.randomUUID(), holiday = UUID.randomUUID();
        LocalDate first = LocalDate.parse("2026-11-02"), second = first.plusDays(1);
        // Metadata traversal fixture only; actual day computation/evidence has its separate owner integration case.
        for (UUID selected : java.util.List.of(tenant, foreign)) {
            UUID employee = null;
            for (int index = 0; index < 205; index++) {
                if (index % 2 == 0) employee = UUID.randomUUID();
                admin.update("INSERT INTO attendance.daily_status(tenant_id,employee_id,work_date,id,revision,payload) VALUES (?,?,?,?,1,'{}')",
                        selected, employee, index % 2 == 0 ? first : second, UUID.randomUUID());
            }
        }
        admin.update("INSERT INTO attendance.holiday_projection(tenant_id,holiday_id,date,name,kind,revision) VALUES (?,?,?,'Synthetic','COMPANY',1)", tenant, holiday, first);
        var event = new OrganizationFact(UUID.randomUUID().toString(), "org.holiday.updated.v1", 1,
                tenant.toString(), holiday.toString(), 2L, java.time.Instant.now().toString(), UUID.randomUUID().toString(), null,
                new HolidaySnapshot(holiday.toString(), second.toString(), "Synthetic", "COMPANY", null));
        assertThatThrownBy(() -> transactions.executeWithoutResult(tx -> {
            organization.apply("org.holiday.v1", tenant + ":" + holiday, event);
            throw new IllegalStateException("interrupt");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(admin.queryForObject("SELECT count(*) FROM attendance.holiday_daily_change", Integer.class)).isZero();
        organization.apply("org.holiday.v1", tenant + ":" + holiday, event);
        for (int expected : java.util.List.of(100, 200, 205)) {
            changes.expandNext(tenant);
            assertThat(admin.queryForObject("SELECT count(*) FROM attendance.daily_recalculation WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(expected);
        }
        changes.expandNext(tenant);
        assertThat(admin.queryForObject("SELECT count(*) FROM attendance.holiday_daily_change", Integer.class)).isZero();
        assertThat(admin.queryForObject("SELECT count(*) FROM attendance.daily_recalculation WHERE tenant_id=?", Integer.class, foreign)).isZero();
        assertThat(runtime.queryForObject("SELECT count(*) FROM attendance.daily_recalculation", Integer.class)).isZero();
        assertThat(organization.apply("org.holiday.v1", tenant + ":" + holiday, event)).isEqualTo(OrganizationProjectionService.Result.DUPLICATE);
        assertThat(admin.queryForObject("SELECT count(*) FROM attendance.holiday_daily_change", Integer.class)).isZero();
    }
}
