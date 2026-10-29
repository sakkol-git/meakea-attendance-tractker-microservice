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
class CancelCompensatingLedgerTest {
    @org.springframework.test.context.bean.override.mockito.MockitoBean java.time.Clock clock;
    @org.junit.jupiter.api.BeforeEach void beforeTheApprovedFutureLeave() {
        org.mockito.Mockito.when(clock.instant()).thenReturn(java.time.Instant.parse("2026-08-01T00:00:00Z"));
    }
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

    private final String tenantId = "tenant-cancel-test";
    private final String employeeId = "emp-requester-99";
    private final String otherEmployeeId = "emp-other-100";
    private final String managerId = "mgr-lead-01";

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
                new BigDecimal("12.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("12.00")
        ));
    }

    @Test
    @DisplayName("Cancel pending request releases pending balance and repeat cancellation fails with 409")
    void cancelPendingRequestReleasesBalanceAndRepeatFails() {
        CreateLeaveRequestDto dto = new CreateLeaveRequestDto(
                "ANNUAL",
                LocalDate.of(2026, 8, 3),
                LocalDate.of(2026, 8, 4),
                "FULL_DAY",
                "Doctor appointment",
                "Personal"
        );
        LeaveRecordResponse request = leaveService.submitLeaveRequest(tenantId, employeeId, "Requester", dto);

        // Cancel pending request
        LeaveRecordResponse cancelled = decisions.cancelLeaveRequest(
                tenantId,
                employeeId,
                request.id(),
                "\"" + request.version() + "\"",
                "Changed plans"
        );

        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(cancelled.version()).isEqualTo(request.version() + 1);

        // Balance restored to 12 available, 0 pending, 0 used
        LeaveBalance balance = leaveBalanceRepository
                .findByTenantIdAndEmployeeIdAndLeaveTypeCodeAndYear(tenantId, employeeId, "ANNUAL", 2026)
                .orElseThrow();
        assertThat(balance.getAvailable()).isEqualByComparingTo(new BigDecimal("12.00"));
        assertThat(balance.getPending()).isEqualByComparingTo(BigDecimal.ZERO);

        // Repeating cancel fails with 409 Conflict
        assertThatThrownBy(() -> decisions.cancelLeaveRequest(
                tenantId,
                employeeId,
                request.id(),
                "\"" + cancelled.version() + "\"",
                "Repeat cancel"
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> {
                    ResponseStatusException rse = (ResponseStatusException) e;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    @DisplayName("Cancel approved request writes compensating ledger and preserves original history")
    void cancelApprovedRequestCompensatesAndPreservesHistory() {
        CreateLeaveRequestDto dto = new CreateLeaveRequestDto(
                "ANNUAL",
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 11),
                "FULL_DAY",
                "Conference",
                "Work trip"
        );
        LeaveRecordResponse request = leaveService.submitLeaveRequest(tenantId, employeeId, "Requester", dto);

        // Manager approves request
        LeaveRecordResponse approved = decisions.approveLeaveRequest(
                tenantId,
                managerId,
                List.of("ROLE_MANAGER"),
                request.id(),
                "\"" + request.version() + "\"",
                new DecisionRequest("Approved", "Approved")
        );

        // Now employee cancels approved request
        LeaveRecordResponse cancelled = decisions.cancelLeaveRequest(
                tenantId,
                employeeId,
                request.id(),
                "\"" + approved.version() + "\"",
                "Conference rescheduled"
        );

        assertThat(cancelled.status()).isEqualTo("CANCELLED");

        // Balance restored: 12 available, 0 used, 0 pending
        LeaveBalance balance = leaveBalanceRepository
                .findByTenantIdAndEmployeeIdAndLeaveTypeCodeAndYear(tenantId, employeeId, "ANNUAL", 2026)
                .orElseThrow();
        assertThat(balance.getAvailable()).isEqualByComparingTo(new BigDecimal("12.00"));
        assertThat(balance.getUsed()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(balance.getPending()).isEqualByComparingTo(BigDecimal.ZERO);

        // Full audit trail preserved in ledger: RESERVATION, USED, and COMPENSATION
        List<LeaveLedger> ledgers = leaveLedgerRepository.findByTenantIdAndReferenceId(tenantId, request.id());
        assertThat(ledgers).hasSize(3);
        List<String> types = ledgers.stream().map(LeaveLedger::getEntryType).toList();
        assertThat(types).containsExactlyInAnyOrder("RESERVATION", "USED", "COMPENSATION");
    }

    @Test
    @DisplayName("Non-owner employee cannot cancel request")
    void foreignEmployeeCannotCancelRequest() {
        CreateLeaveRequestDto dto = new CreateLeaveRequestDto(
                "ANNUAL",
                LocalDate.of(2026, 8, 17),
                LocalDate.of(2026, 8, 18),
                "FULL_DAY",
                "Vacation",
                "Trip"
        );
        LeaveRecordResponse request = leaveService.submitLeaveRequest(tenantId, employeeId, "Requester", dto);

        assertThatThrownBy(() -> decisions.cancelLeaveRequest(
                tenantId,
                otherEmployeeId, // different employee!
                request.id(),
                "\"" + request.version() + "\"",
                "Sneaky cancel"
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> {
                    ResponseStatusException rse = (ResponseStatusException) e;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }
}
