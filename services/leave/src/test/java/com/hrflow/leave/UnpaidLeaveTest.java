package com.hrflow.leave;

import static org.assertj.core.api.Assertions.assertThat;

import com.hrflow.leave.dto.CreateLeaveRequestDto;
import com.hrflow.leave.entity.LeaveType;
import com.hrflow.leave.repository.LeaveTypeRepository;
import com.hrflow.leave.service.LeaveDecisions;
import com.hrflow.leave.service.LeaveSubmissionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class UnpaidLeaveTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("context-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", POSTGRES::getUsername);
        p.add("spring.datasource.password", POSTGRES::getPassword);
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @MockitoBean com.hrflow.leave.organization.LeaveEmployeeBootstrap employees;
    @MockitoBean com.hrflow.leave.organization.LeaveCalendarBootstrap calendars;
    @MockitoBean java.time.Clock clock;
    @Autowired LeaveSubmissionService submissions;
    @Autowired LeaveDecisions decisions;
    @Autowired LeaveTypeRepository types;
    @Autowired JdbcTemplate jdbc;

    @Test void evidenceRequiredTypeCannotReserveLeaveWithoutThePrivateEvidenceWorkflow() {
        org.mockito.Mockito.when(employees.isReady()).thenReturn(true);
        org.mockito.Mockito.when(calendars.isReady()).thenReturn(true);
        org.mockito.Mockito.when(clock.instant()).thenReturn(java.time.Instant.parse("2026-10-28T00:00:00Z"));
        String tenant = UUID.randomUUID().toString(), employee = UUID.randomUUID().toString();
        types.save(new LeaveType(UUID.randomUUID().toString(), tenant, "PROOF", "Evidence required", BigDecimal.ZERO, false, true, true, 0, BigDecimal.ZERO));
        LeaveReferenceFixtures.calendar(jdbc, tenant, employee, 127);
        var date = LocalDate.parse("2026-11-02");
        var input = new CreateLeaveRequestDto("PROOF", date, date, "FULL_DAY", "Private", "Request");
        assertThat(submissions.previewLeaveRequest(tenant, employee, input).evidenceRequired()).isTrue();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> submissions.submitLeaveRequest(tenant, employee, "Synthetic", input))
                .isInstanceOfSatisfying(org.springframework.web.ErrorResponseException.class,
                        error -> assertThat(error.getStatusCode().value()).isEqualTo(422));
        for (String table : List.of("leave_request", "leave_ledger", "leave_outbox", "day_reservation"))
            assertThat(jdbc.queryForObject("SELECT count(*) FROM leave." + table + " WHERE tenant_id=?", Integer.class, tenant)).isZero();
    }

    @Test void unpaidApprovalAndCancellationUseCapturedModeAndKeepHistoryWithoutAnEntitlementBalance() {
        org.mockito.Mockito.when(employees.isReady()).thenReturn(true);
        org.mockito.Mockito.when(calendars.isReady()).thenReturn(true);
        org.mockito.Mockito.when(clock.instant()).thenReturn(java.time.Instant.parse("2026-10-28T00:00:00Z"));
        String tenant = UUID.randomUUID().toString(), employee = UUID.randomUUID().toString();
        types.save(new LeaveType(UUID.randomUUID().toString(), tenant, "UNPAID", "Unpaid", BigDecimal.ZERO, false, true, false, 0, BigDecimal.ZERO));
        LeaveReferenceFixtures.calendar(jdbc, tenant, employee, 127);
        var date = LocalDate.parse("2026-11-02");
        var request = new CreateLeaveRequestDto("UNPAID", date, date, "FULL_DAY", "Reason", "Title");
        var preview = submissions.previewLeaveRequest(tenant, employee, request);
        assertThat(preview.chargeableDays()).isEqualByComparingTo("1.00");
        assertThat(preview.balanceBefore()).isNull();
        assertThat(preview.balanceAfter()).isNull();
        assertThat(preview.warnings()).doesNotContain("INSUFFICIENT_BALANCE");
        var pending = submissions.submitLeaveRequest(tenant, employee, "Synthetic", request);
        jdbc.update("UPDATE leave.leave_type SET paid=true WHERE tenant_id=?", tenant);
        var approved = decisions.approveLeaveRequest(tenant, UUID.randomUUID().toString(), List.of("HR_ADMIN"), pending.id(), "\"" + pending.version() + "\"", null);
        decisions.cancelLeaveRequest(tenant, employee, approved.id(), "\"" + approved.version() + "\"", "Changed plans");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM leave.leave_balance WHERE tenant_id=?", Integer.class, tenant)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM leave.day_reservation WHERE tenant_id=?", Integer.class, tenant)).isZero();
        assertThat(jdbc.queryForList("SELECT entry_type FROM leave.leave_ledger WHERE tenant_id=? ORDER BY created_at,id", String.class, tenant))
                .containsExactly("UNPAID_RESERVATION", "UNPAID_USED", "UNPAID_COMPENSATION");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM leave.leave_outbox WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(3);
    }
}
