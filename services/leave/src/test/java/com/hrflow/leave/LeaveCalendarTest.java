package com.hrflow.leave;

import static org.assertj.core.api.Assertions.assertThat;

import com.hrflow.leave.dto.CreateLeaveRequestDto;
import com.hrflow.leave.entity.LeaveBalance;
import com.hrflow.leave.entity.LeaveType;
import com.hrflow.leave.repository.LeaveBalanceRepository;
import com.hrflow.leave.repository.LeaveTypeRepository;
import com.hrflow.leave.service.LeaveSubmissionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class LeaveCalendarTest {
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.hrflow.leave.organization.LeaveCalendarBootstrap calendarBootstrap;
    @org.junit.jupiter.api.BeforeEach void calendarReferenceReady() {
        org.mockito.Mockito.when(calendarBootstrap.isReady()).thenReturn(true);
    }

    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("context-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", POSTGRES::getUsername);
        p.add("spring.datasource.password", POSTGRES::getPassword);
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @Autowired LeaveSubmissionService submissions;
    @Autowired LeaveTypeRepository types;
    @Autowired LeaveBalanceRepository balances;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired com.hrflow.leave.organization.LeaveEmployeeConsumer employees;
    @Autowired com.hrflow.leave.organization.LeaveCalendarConsumer calendars;
    @Autowired com.hrflow.leave.service.LeaveDecisions decisions;

    @Test void complementaryHalfDaysMayReserveTheSameWorkingDateButAnOverlappingFullDayCannot() {
        String tenant = UUID.randomUUID().toString(), employee = UUID.randomUUID().toString();
        allocate(tenant, employee);
        LeaveReferenceFixtures.calendar(jdbc, tenant, employee, 127);
        var day = LocalDate.parse("2026-11-02");
        var morning = submissions.submitLeaveRequest(tenant, employee, "Synthetic", new CreateLeaveRequestDto("ANNUAL", day, day, "MORNING", "Reason", "Title"));
        var afternoon = submissions.submitLeaveRequest(tenant, employee, "Synthetic", new CreateLeaveRequestDto("ANNUAL", day, day, "AFTERNOON", "Reason", "Title"));
        assertThat(morning.days().add(afternoon.days())).isEqualByComparingTo("1.00");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> submissions.submitLeaveRequest(tenant, employee, "Synthetic",
                new CreateLeaveRequestDto("ANNUAL", day, day, "FULL_DAY", "Reason", "Title")))
                .isInstanceOfSatisfying(org.springframework.web.server.ResponseStatusException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(409));
        assertThat(balances.findByTenantIdAndEmployeeId(tenant, employee).getFirst().getPending()).isEqualByComparingTo("1.00");
        decisions.cancelLeaveRequest(tenant, employee, morning.id(), "\"" + morning.version() + "\"", "Changed plans");
        assertThat(jdbc.queryForList("SELECT portion FROM leave.day_reservation WHERE tenant_id=?", String.class, tenant)).containsExactly("AFTERNOON");
        var replacement = submissions.submitLeaveRequest(tenant, employee, "Synthetic", new CreateLeaveRequestDto("ANNUAL", day, day, "MORNING", "Reason", "Title"));
        assertThat(replacement.days()).isEqualByComparingTo("0.50");
    }
    @Autowired org.springframework.transaction.support.TransactionTemplate transactions;

    @Test void upgradeCanRestoreUnknownAssignmentsOnlyFromTheExactAlreadyReceivedOwnerRevision() {
        var tenant = UUID.randomUUID();
        var employee = UUID.randomUUID();
        allocate(tenant.toString(), employee.toString());
        String shift = LeaveReferenceFixtures.calendar(jdbc, tenant.toString(), employee.toString(), 127);
        var fact = LeaveReferenceFixtures.fact(tenant, employee, null, 1);
        ((com.hrflow.contracts.organization.EmployeeSnapshot) fact.getPayload()).setAssignments(java.util.List.of(
                new com.hrflow.contracts.organization.AssignmentSnapshot(UUID.randomUUID().toString(), shift, "2000-01-01", null)));
        jdbc.update("UPDATE leave.employee_projection SET assignments=null WHERE tenant_id=?", tenant.toString());
        jdbc.update("INSERT INTO leave.organization_inbox(tenant_id,event_id) VALUES (?,?)", tenant.toString(), UUID.fromString(fact.getEventId()));
        employees.apply(tenant + ":" + employee, fact);
        assertThat(jdbc.queryForObject("SELECT assignments::text FROM leave.employee_projection WHERE tenant_id=?", String.class, tenant.toString())).contains(shift);
        jdbc.update("UPDATE leave.employee_projection SET assignments=null,revision=2 WHERE tenant_id=?", tenant.toString());
        employees.apply(tenant + ":" + employee, fact);
        assertThat(jdbc.queryForObject("SELECT assignments::text FROM leave.employee_projection WHERE tenant_id=?", String.class, tenant.toString())).isNull();
    }

    @Test void invalidRoutingAndRollbackLeaveNeitherCalendarNorReceipt() {
        UUID tenant = UUID.randomUUID(), holiday = UUID.randomUUID();
        var fact = new com.hrflow.contracts.organization.OrganizationFact(UUID.randomUUID().toString(), "org.holiday.added.v1", 1,
                tenant.toString(), holiday.toString(), 1L, java.time.Instant.now().toString(), UUID.randomUUID().toString(), null,
                new com.hrflow.contracts.organization.HolidaySnapshot(holiday.toString(), "2026-11-02", "Fixture", "COMPANY", null));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> calendars.apply("org.holiday.v1", "wrong", fact)).isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> transactions.executeWithoutResult(tx -> {
            calendars.apply("org.holiday.v1", tenant + ":" + holiday, fact);
            throw new IllegalStateException("rollback calendar");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM leave.organization_inbox WHERE tenant_id=?", Integer.class, tenant.toString())).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM leave.holiday_projection WHERE tenant_id=?", Integer.class, tenant.toString())).isZero();
    }
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.hrflow.leave.organization.LeaveEmployeeBootstrap bootstrap;
    @org.junit.jupiter.api.BeforeEach void replayReady() { org.mockito.Mockito.when(bootstrap.isReady()).thenReturn(true); }

    @Test void sundayShiftAndTenantHolidayDriveChargeAndActualHoursForFullAndHalfDays() {
        String tenant = UUID.randomUUID().toString(), employee = UUID.randomUUID().toString();
        allocate(tenant, employee);
        String shift = LeaveReferenceFixtures.calendar(jdbc, tenant, employee, 65); // Sunday and Monday.
        jdbc.update("UPDATE leave.shift_projection SET start_time='10:00',end_time='16:00' WHERE tenant_id=? AND shift_id=?", tenant, shift);
        jdbc.update("INSERT INTO leave.holiday_projection(tenant_id,holiday_id,date,deleted,revision) VALUES (?,?,'2026-11-09',false,1)", tenant, UUID.randomUUID().toString());
        var full = submissions.submitLeaveRequest(tenant, employee, "Synthetic", new CreateLeaveRequestDto("ANNUAL",
                LocalDate.parse("2026-11-08"), LocalDate.parse("2026-11-09"), "FULL_DAY", "Reason", "Title"));
        assertThat(full.days()).isEqualByComparingTo("1.00");
        assertThat(jdbc.queryForList("SELECT date FROM leave.leave_day WHERE tenant_id=?", LocalDate.class, tenant)).containsExactly(LocalDate.parse("2026-11-08"));
        assertThat(jdbc.queryForObject("SELECT scheduled_seconds FROM leave.leave_day WHERE tenant_id=?", Long.class, tenant)).isEqualTo(21600);
        jdbc.update("INSERT INTO leave.holiday_projection(tenant_id,holiday_id,date,deleted,revision) VALUES (?,?,'2026-11-15',false,1)", UUID.randomUUID().toString(), UUID.randomUUID().toString());
        var half = submissions.submitLeaveRequest(tenant, employee, "Synthetic", new CreateLeaveRequestDto("ANNUAL",
                LocalDate.parse("2026-11-15"), LocalDate.parse("2026-11-15"), "MORNING", "Reason", "Title"));
        assertThat(half.days()).isEqualByComparingTo("0.50");
        assertThat(jdbc.queryForObject("SELECT scheduled_seconds FROM leave.leave_day WHERE tenant_id=? AND request_id=?", Long.class, tenant, half.id())).isEqualTo(10800);
    }

    private void allocate(String tenant, String employee) {
        types.save(new LeaveType(UUID.randomUUID().toString(), tenant, "ANNUAL", "Annual", new BigDecimal("20.00"), true, true, false, 0, BigDecimal.ZERO));
        balances.save(new LeaveBalance(UUID.randomUUID().toString(), tenant, employee, "ANNUAL", 2026,
                new BigDecimal("20.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("20.00")));
    }

    @Test void missingEmployeeCalendarCannotReserveBalanceUsingAnAssumedWeekdaySchedule() {
        String tenant = UUID.randomUUID().toString(), employee = UUID.randomUUID().toString();
        types.save(new LeaveType(UUID.randomUUID().toString(), tenant, "ANNUAL", "Annual", new BigDecimal("20.00"),
                true, true, false, 0, BigDecimal.ZERO));
        balances.save(new LeaveBalance(UUID.randomUUID().toString(), tenant, employee, "ANNUAL", 2026,
                new BigDecimal("20.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("20.00")));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> submissions.submitLeaveRequest(tenant, employee, "Synthetic",
                new CreateLeaveRequestDto("ANNUAL", LocalDate.parse("2026-11-02"), LocalDate.parse("2026-11-02"), "FULL_DAY", "Reason", "Title")))
                .isInstanceOfSatisfying(org.springframework.web.ErrorResponseException.class,
                        error -> assertThat(error.getStatusCode().value()).isEqualTo(503));
        assertThat(balances.findByTenantIdAndEmployeeId(tenant, employee).getFirst().getAvailable()).isEqualByComparingTo("20.00");
    }
}
