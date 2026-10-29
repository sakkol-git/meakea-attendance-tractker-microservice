package com.hrflow.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.contracts.organization.EmployeeSnapshot;
import com.hrflow.contracts.organization.OrganizationFact;
import com.hrflow.contracts.request.RequestApprovalStepFact;
import com.hrflow.contracts.request.RequestWorkflowFact;
import com.hrflow.notification.organization.NotificationEmployeeBootstrap;
import com.hrflow.notification.organization.NotificationEmployeeConsumer;
import com.hrflow.notification.service.NotificationService;
import java.time.Instant;
import java.util.List;
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
class RequestNotificationTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine").withInitScript("context-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl); p.add("spring.datasource.username", () -> "notification_app");
        p.add("spring.datasource.password", () -> "test-pass"); p.add("spring.flyway.user", POSTGRES::getUsername); p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @Autowired RequestNotices requests;
    @Autowired LeaveNotices leaves;
    @Autowired CorrectionNotices corrections;
    @Autowired NotificationEmployeeConsumer organization;
    @Autowired NotificationService notices;
    @Autowired TransactionTemplate transactions;
    @Autowired JdbcTemplate runtime;
    @MockitoBean NotificationEmployeeBootstrap bootstrap;

    @Test void currentStageRoutesToVerifiedRecipientsAndCriticalApprovalCannotBeDisabledOrDuplicated() {
        UUID tenant = id(), employee = id(), manager = id(), hr = id(), request = id();
        team(tenant, employee, manager, hr);
        var preferences = notices.getPreferences(tenant, manager);
        notices.updatePreferences(tenant, manager, new com.hrflow.notification.dto.UpdatePreferencesRequest(false, null, false, null, false, null), Long.toString(preferences.version()));
        var first = fact(tenant, employee, request, manager, 1, "PENDING_APPROVAL", "PENDING");
        requests.apply(tenant + ":" + request, first);
        requests.apply(tenant + ":" + request, first);
        assertThat(notices.listNotifications(tenant, manager, 0, 25).total()).isEqualTo(1);
        assertThat(notices.getPreferences(tenant, manager).inApp()).isFalse();
        assertThat(notices.listNotifications(tenant, hr, 0, 25).total()).isZero();
        var second = fact(tenant, employee, request, manager, 2, "PENDING_APPROVAL", "APPROVED");
        requests.apply(tenant + ":" + request, second);
        assertThat(notices.listNotifications(tenant, hr, 0, 25).total()).isEqualTo(1);
        assertThat(notices.listNotifications(tenant, employee, 0, 25).total()).isZero();
        requests.apply(tenant + ":" + request, fact(tenant, employee, request, manager, 3, "APPROVED", "APPROVED"));
        assertThat(notices.listNotifications(tenant, employee, 0, 25).total()).isEqualTo(1);
        requests.apply(tenant + ":" + request, fact(tenant, employee, request, manager, 1, "PENDING_APPROVAL", "PENDING"));
        assertThat(notices.listNotifications(tenant, manager, 0, 25).total()).isEqualTo(1);
        var record = notices.listNotifications(tenant, hr, 0, 25).items().getFirst();
        assertThat(record.kind()).isEqualTo("request"); assertThat(record.recordId()).isEqualTo(request.toString());
        assertThat(record.body()).doesNotContain("Synthetic employee", "Private reason");
        assertThat(runtime.queryForObject("SELECT count(*) FROM notification.notification_record", Integer.class)).isZero();
    }

    @Test void unavailableRecipientOrRollbackLeavesNoReceiptAndCanRetryAfterReferencesArrive() {
        UUID tenant = id(), employee = id(), manager = id(), request = id();
        org.mockito.Mockito.when(bootstrap.isReady()).thenReturn(true);
        employee(tenant, employee, manager, List.of("EMPLOYEE"));
        var event = fact(tenant, employee, request, manager, 1, "PENDING_APPROVAL", "PENDING");
        assertThatThrownBy(() -> requests.apply(tenant + ":" + request, event)).isInstanceOf(org.springframework.web.ErrorResponseException.class);
        assertThat(admin().queryForObject("SELECT count(*) FROM notification.notification_inbox WHERE consumer_name='notification-domain' AND tenant_id=?", Integer.class, tenant)).isZero();
        employee(tenant, manager, null, List.of("EMPLOYEE", "MANAGER"));
        assertThatThrownBy(() -> transactions.executeWithoutResult(tx -> { requests.apply(tenant + ":" + request, event); throw new IllegalStateException("interrupt"); }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(admin().queryForObject("SELECT count(*) FROM notification.notification_record WHERE tenant_id=?", Integer.class, tenant)).isZero();
        requests.apply(tenant + ":" + request, event);
        assertThat(notices.listNotifications(tenant, manager, 0, 25).total()).isEqualTo(1);
    }

    @Test void leaveApprovalAndReviewUseAuthorizedRecipientsWithoutOwningAnyBalance() {
        UUID tenant = id(), employee = id(), manager = id(), hr = id(), leave = id();
        team(tenant, employee, manager, hr);
        var date = java.time.LocalDate.parse("2026-11-02");
        var pending = com.hrflow.contracts.leave.LeaveRequestFact.newBuilder().setEventId(id().toString()).setType("LeaveRequestSnapshot.v1").setSchemaVersion(1)
                .setTenantId(tenant.toString()).setAggregateId(leave.toString()).setAggregateVersion(1L).setOccurredAt(Instant.now()).setCorrelationId(id().toString())
                .setEmployeeId(employee.toString()).setLeaveType("ANNUAL").setStartDate(date).setEndDate(date).setPortion("FULL_DAY")
                .setChargedDays("1").setWorkingDates(List.of(date)).setState("PENDING").build();
        leaves.apply(tenant + ":" + employee, pending);
        leaves.apply(tenant + ":" + employee, pending);
        assertThat(notices.listNotifications(tenant, manager, 0, 25).total()).isEqualTo(1);
        assertThat(notices.listNotifications(tenant, hr, 0, 25).total()).isEqualTo(1);
        var approved = com.hrflow.contracts.leave.LeaveRequestFact.newBuilder(pending).setEventId(id().toString()).setState("APPROVED").setAggregateVersion(2L).build();
        leaves.apply(tenant + ":" + employee, approved);
        assertThat(notices.listNotifications(tenant, employee, 0, 25).items().getFirst().kind()).isEqualTo("leave");
        var review = com.hrflow.contracts.leave.LeaveRequestFact.newBuilder(approved).setEventId(id().toString()).setState("NEEDS_REVIEW").setAggregateVersion(3L).build();
        leaves.apply(tenant + ":" + employee, review);
        assertThat(notices.listNotifications(tenant, hr, 0, 25).total()).isEqualTo(2);
        assertThat(notices.listNotifications(tenant, employee, 0, 25).total()).isEqualTo(2);
        assertThatThrownBy(() -> leaves.apply(id() + ":" + employee, pending)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void correctionApprovalsArePrivateCriticalAndOutcomesCannotDuplicateOrReopenApproval() {
        UUID tenant = id(), employee = id(), manager = id(), hr = id(), correction = id();
        team(tenant, employee, manager, hr);
        var pending = com.hrflow.contracts.attendance.AttendanceCorrectionFact.newBuilder()
                .setEventId(id().toString()).setType("AttendanceCorrectionSnapshot.v1").setSchemaVersion(1)
                .setTenantId(tenant.toString()).setAggregateId(correction.toString()).setAggregateVersion(1L)
                .setOccurredAt(Instant.now()).setCorrelationId(id().toString()).setEmployeeId(employee.toString())
                .setWorkDate(java.time.LocalDate.parse("2026-11-02")).setState("PENDING").build();
        corrections.apply(tenant + ":" + employee, pending); corrections.apply(tenant + ":" + employee, pending);
        assertThat(notices.listNotifications(tenant, manager, 0, 25).total()).isEqualTo(1);
        assertThat(notices.listNotifications(tenant, hr, 0, 25).total()).isEqualTo(1);
        assertThat(notices.listNotifications(tenant, employee, 0, 25).total()).isZero();
        var rejected = com.hrflow.contracts.attendance.AttendanceCorrectionFact.newBuilder(pending)
                .setEventId(id().toString()).setState("REJECTED").setAggregateVersion(2L).build();
        corrections.apply(tenant + ":" + employee, rejected);
        corrections.apply(tenant + ":" + employee, pending);
        var notice = notices.listNotifications(tenant, employee, 0, 25).items().getFirst();
        assertThat(notice.kind()).isEqualTo("correction"); assertThat(notice.recordId()).isEqualTo(correction.toString());
        assertThat(notice.title()).isEqualTo("Attendance correction rejected");
        assertThat(notices.listNotifications(tenant, manager, 0, 25).total()).isEqualTo(1);
        assertThatThrownBy(() -> corrections.apply(id() + ":" + employee, rejected)).isInstanceOf(IllegalArgumentException.class);
    }

    private void team(UUID tenant, UUID employee, UUID manager, UUID hr) {
        org.mockito.Mockito.when(bootstrap.isReady()).thenReturn(true);
        employee(tenant, employee, manager, List.of("EMPLOYEE", "HR_ADMIN"));
        employee(tenant, manager, null, List.of("EMPLOYEE", "MANAGER"));
        employee(tenant, hr, null, List.of("EMPLOYEE", "HR_ADMIN"));
    }
    private void employee(UUID tenant, UUID employee, UUID manager, List<String> roles) {
        var snapshot = EmployeeSnapshot.newBuilder().setEmployeeId(employee.toString()).setEmployeeNo("SYNTHETIC").setFullName("Synthetic employee")
                .setStatus("ACTIVE").setAssignments(List.of()).setUserId(id().toString()).setAccountRoles(roles).setManagerId(manager == null ? null : manager.toString()).build();
        organization.apply(tenant + ":" + employee, new OrganizationFact(id().toString(), "org.employee.updated.v1", 1, tenant.toString(), employee.toString(), 1L, Instant.now().toString(), id().toString(), null, snapshot));
    }
    static RequestWorkflowFact fact(UUID tenant, UUID employee, UUID request, UUID manager, long revision, String state, String managerState) {
        var steps = List.of(new RequestApprovalStepFact(1, "MANAGER", manager.toString(), managerState),
                new RequestApprovalStepFact(2, "HR_ADMIN", null, state.equals("APPROVED") ? "APPROVED" : "PENDING"));
        return RequestWorkflowFact.newBuilder().setEventId(id().toString()).setType("RequestWorkflowSnapshot.v1").setSchemaVersion(1)
                .setTenantId(tenant.toString()).setAggregateId(request.toString()).setAggregateVersion(revision).setOccurredAt(Instant.now())
                .setCorrelationId(id().toString()).setEmployeeId(employee.toString()).setRequestType("OTHER").setState(state)
                .setCreatedAt(Instant.parse("2026-10-01T00:00:00Z")).setSteps(steps).build();
    }
    static UUID id() { return UUID.randomUUID(); }
    private JdbcTemplate admin() { return new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())); }
}
