package com.hrflow.leave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.leave.dto.CreateLeaveRequestDto;
import com.hrflow.leave.dto.DecisionRequest;
import com.hrflow.leave.dto.LeaveRecordResponse;
import com.hrflow.leave.entity.LeaveBalance;
import com.hrflow.leave.entity.LeaveLedger;
import com.hrflow.leave.entity.LeaveType;
import com.hrflow.leave.repository.LeaveBalanceRepository;
import com.hrflow.leave.repository.LeaveLedgerRepository;
import com.hrflow.leave.repository.LeaveRequestRepository;
import com.hrflow.leave.repository.LeaveTypeRepository;
import com.hrflow.leave.service.LeaveSubmissionService;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class ApproveOrRejectOnceTest {
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.hrflow.leave.organization.LeaveCalendarBootstrap calendarBootstrap;
    @org.junit.jupiter.api.BeforeEach void calendarReferenceReady() {
        org.mockito.Mockito.when(calendarBootstrap.isReady()).thenReturn(true);
    }

    @Autowired com.hrflow.leave.service.LeaveDecisions decisions;
    // Isolates authorization/transaction tests; the actual broker replay is verified separately.
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.hrflow.leave.organization.LeaveEmployeeBootstrap bootstrap;
    @org.junit.jupiter.api.BeforeEach void readyReferenceGate() {
        org.mockito.Mockito.when(bootstrap.isReady()).thenReturn(true);
    }


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

    @Autowired
    LeaveSubmissionService leaveService;

    @Autowired
    LeaveTypeRepository leaveTypeRepository;

    @Autowired
    LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    LeaveLedgerRepository leaveLedgerRepository;

    @Autowired
    LeaveRequestRepository leaveRequestRepository;

    @Autowired
    DataSource dataSource;

    private final String tenantId = "tenant-approval-test";
    private final String employeeId = "emp-requester-01";
    private final String managerId = "mgr-approver-02";

    @BeforeEach
    void setUp() throws Exception {
        var references = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        LeaveReferenceFixtures.employee(references, tenantId, managerId, null);
        LeaveReferenceFixtures.employee(references, tenantId, employeeId, managerId);
        LeaveReferenceFixtures.calendar(references, tenantId, employeeId, 31);
        try (Connection conn = dataSource.getConnection()) {
            try (Statement st = conn.createStatement()) {
                st.execute("SET app.tenant_id = '" + tenantId + "'");
                st.execute("DELETE FROM leave.leave_outbox WHERE tenant_id = '" + tenantId + "'");
                st.execute("DELETE FROM leave.leave_day WHERE tenant_id = '" + tenantId + "'");
                st.execute("DELETE FROM leave.leave_request WHERE tenant_id = '" + tenantId + "'");
                st.execute("DELETE FROM leave.leave_ledger WHERE tenant_id = '" + tenantId + "'");
                st.execute("DELETE FROM leave.leave_balance WHERE tenant_id = '" + tenantId + "'");
                st.execute("DELETE FROM leave.leave_type WHERE tenant_id = '" + tenantId + "'");
            }
        }

        leaveTypeRepository.save(new LeaveType(
                UUID.randomUUID().toString(),
                tenantId,
                "ANNUAL",
                "Annual Leave",
                new BigDecimal("20.00"),
                true,
                true,
                false,
                0,
                new BigDecimal("5.00")
        ));

        leaveBalanceRepository.save(new LeaveBalance(
                UUID.randomUUID().toString(),
                tenantId,
                employeeId,
                "ANNUAL",
                2026,
                new BigDecimal("15.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("15.00")
        ));
    }

    @Test
    @DisplayName("Self-approval is forbidden and non-approver role is denied")
    void selfApprovalAndNonApproverRoleDenied() {
        // Submit 2-day leave: 2026-07-06 (Mon) to 2026-07-07 (Tue)
        CreateLeaveRequestDto dto = new CreateLeaveRequestDto(
                "ANNUAL",
                LocalDate.of(2026, 7, 6),
                LocalDate.of(2026, 7, 7),
                "FULL_DAY",
                "Trip",
                "Vacation"
        );
        LeaveRecordResponse request = leaveService.submitLeaveRequest(tenantId, employeeId, "Requester", dto);

        // 1. Employee tries to approve their own request with manager role -> 403 Forbidden
        assertThatThrownBy(() -> decisions.approveLeaveRequest(
                tenantId,
                employeeId, // self!
                List.of("ROLE_MANAGER"),
                request.id(),
                "\"" + request.version() + "\"",
                new DecisionRequest("Self approve", "Note")
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> {
                    ResponseStatusException rse = (ResponseStatusException) e;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(rse.getReason()).contains("Self-approval is forbidden");
                });

        // 2. Someone with only ROLE_EMPLOYEE tries to approve -> 403 Forbidden
        assertThatThrownBy(() -> decisions.approveLeaveRequest(
                tenantId,
                managerId,
                List.of("ROLE_EMPLOYEE"),
                request.id(),
                "\"" + request.version() + "\"",
                new DecisionRequest("Approved", "Note")
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> {
                    ResponseStatusException rse = (ResponseStatusException) e;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    @Test
    @DisplayName("Stale If-Match header returns 412 Precondition Failed")
    void staleIfMatchRejected() {
        CreateLeaveRequestDto dto = new CreateLeaveRequestDto(
                "ANNUAL",
                LocalDate.of(2026, 7, 6),
                LocalDate.of(2026, 7, 7),
                "FULL_DAY",
                "Trip",
                "Vacation"
        );
        LeaveRecordResponse request = leaveService.submitLeaveRequest(tenantId, employeeId, "Requester", dto);

        // Wrong ETag version ("999")
        assertThatThrownBy(() -> decisions.approveLeaveRequest(
                tenantId,
                managerId,
                List.of("ROLE_MANAGER"),
                request.id(),
                "\"999\"",
                new DecisionRequest("Approved", "Note")
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> {
                    ResponseStatusException rse = (ResponseStatusException) e;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
                });
    }

    @Test
    @DisplayName("Approval moves pending to used once and duplicate decision is rejected with 409")
    void approvalMovesPendingToUsedAndDuplicateDecisionRejected() {
        CreateLeaveRequestDto dto = new CreateLeaveRequestDto(
                "ANNUAL",
                LocalDate.of(2026, 7, 6),
                LocalDate.of(2026, 7, 7),
                "FULL_DAY",
                "Trip",
                "Vacation"
        );
        LeaveRecordResponse request = leaveService.submitLeaveRequest(tenantId, employeeId, "Requester", dto);

        // Balance before approval: 13 available, 2 pending, 0 used
        LeaveBalance preApprovalBalance = leaveBalanceRepository
                .findByTenantIdAndEmployeeIdAndLeaveTypeCodeAndYear(tenantId, employeeId, "ANNUAL", 2026)
                .orElseThrow();
        assertThat(preApprovalBalance.getAvailable()).isEqualByComparingTo(new BigDecimal("13.00"));
        assertThat(preApprovalBalance.getPending()).isEqualByComparingTo(new BigDecimal("2.00"));
        assertThat(preApprovalBalance.getUsed()).isEqualByComparingTo(BigDecimal.ZERO);

        // Manager approves request
        LeaveRecordResponse approved = decisions.approveLeaveRequest(
                tenantId,
                managerId,
                List.of("ROLE_MANAGER"),
                request.id(),
                "\"" + request.version() + "\"",
                new DecisionRequest("Enjoy your time off", "Approved by manager")
        );

        assertThat(approved.status()).isEqualTo("APPROVED");
        assertThat(approved.approverId()).isEqualTo(managerId);
        assertThat(approved.version()).isEqualTo(request.version() + 1);

        // Balance after approval: 13 available, 0 pending, 2 used
        LeaveBalance postApprovalBalance = leaveBalanceRepository
                .findByTenantIdAndEmployeeIdAndLeaveTypeCodeAndYear(tenantId, employeeId, "ANNUAL", 2026)
                .orElseThrow();
        assertThat(postApprovalBalance.getAvailable()).isEqualByComparingTo(new BigDecimal("13.00"));
        assertThat(postApprovalBalance.getPending()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(postApprovalBalance.getUsed()).isEqualByComparingTo(new BigDecimal("2.00"));

        // Verify ledger has USED entry
        List<LeaveLedger> ledgers = leaveLedgerRepository.findByTenantIdAndReferenceId(tenantId, request.id());
        assertThat(ledgers).hasSize(2); // RESERVATION and USED
        assertThat(ledgers.stream().anyMatch(l -> "USED".equals(l.getEntryType()))).isTrue();

        // Second approval attempt returns 409 Conflict
        assertThatThrownBy(() -> decisions.approveLeaveRequest(
                tenantId,
                managerId,
                List.of("ROLE_MANAGER"),
                request.id(),
                "\"" + approved.version() + "\"",
                new DecisionRequest("Duplicate", "Duplicate")
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> {
                    ResponseStatusException rse = (ResponseStatusException) e;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    @DisplayName("Rejection releases pending balance to available and updates status")
    void rejectionReleasesPendingToAvailable() {
        CreateLeaveRequestDto dto = new CreateLeaveRequestDto(
                "ANNUAL",
                LocalDate.of(2026, 7, 8),
                LocalDate.of(2026, 7, 9),
                "FULL_DAY",
                "Trip",
                "Vacation"
        );
        LeaveRecordResponse request = leaveService.submitLeaveRequest(tenantId, employeeId, "Requester", dto);

        LeaveRecordResponse rejected = decisions.rejectLeaveRequest(
                tenantId,
                managerId,
                List.of("ROLE_MANAGER"),
                request.id(),
                "\"" + request.version() + "\"",
                new DecisionRequest("Staff shortage during release week", "Please reschedule")
        );

        assertThat(rejected.status()).isEqualTo("REJECTED");
        assertThat(rejected.approverId()).isEqualTo(managerId);

        // Balance after rejection: all 15 available, 0 pending, 0 used
        LeaveBalance postRejectBalance = leaveBalanceRepository
                .findByTenantIdAndEmployeeIdAndLeaveTypeCodeAndYear(tenantId, employeeId, "ANNUAL", 2026)
                .orElseThrow();
        assertThat(postRejectBalance.getAvailable()).isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(postRejectBalance.getPending()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(postRejectBalance.getUsed()).isEqualByComparingTo(BigDecimal.ZERO);

        // Verify ledger has RELEASE entry
        List<LeaveLedger> ledgers = leaveLedgerRepository.findByTenantIdAndReferenceId(tenantId, request.id());
        assertThat(ledgers.stream().anyMatch(l -> "RELEASE".equals(l.getEntryType()))).isTrue();
    }
}
