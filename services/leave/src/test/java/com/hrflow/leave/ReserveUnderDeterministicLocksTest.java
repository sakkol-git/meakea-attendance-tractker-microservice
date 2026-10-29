package com.hrflow.leave;

import static org.assertj.core.api.Assertions.assertThat;

import com.hrflow.leave.dto.CreateLeaveRequestDto;
import com.hrflow.leave.dto.LeaveRecordResponse;
import com.hrflow.leave.entity.LeaveBalance;
import com.hrflow.leave.entity.LeaveLedger;
import com.hrflow.leave.entity.LeaveRequest;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class ReserveUnderDeterministicLocksTest {
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.hrflow.leave.organization.LeaveCalendarBootstrap calendarBootstrap;
    @org.junit.jupiter.api.BeforeEach void calendarReferenceReady() {
        org.mockito.Mockito.when(calendarBootstrap.isReady()).thenReturn(true);
    }

    @org.springframework.test.context.bean.override.mockito.MockitoBean com.hrflow.leave.organization.LeaveEmployeeBootstrap bootstrap;
    @org.junit.jupiter.api.BeforeEach void referenceReady() { org.mockito.Mockito.when(bootstrap.isReady()).thenReturn(true); }


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

    private final String tenantId = "tenant-locks-test";
    private final String employeeId = "emp-concurrent-01";

    @BeforeEach
    void setUp() throws Exception {
        LeaveReferenceFixtures.calendar(new org.springframework.jdbc.core.JdbcTemplate(dataSource), tenantId, employeeId, 31);
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

        // Create Leave Type
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

        // Create Leave Balance: allocated = 10, available = 10, pending = 0, used = 0
        leaveBalanceRepository.save(new LeaveBalance(
                UUID.randomUUID().toString(),
                tenantId,
                employeeId,
                "ANNUAL",
                2026,
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("10.00")
        ));
    }

    @Test
    @DisplayName("Two parallel reservations never overspend; insufficient second request rolls back completely")
    void parallelReservationsNeverOverspend() throws Exception {
        // Request 1: 2026-06-01 to 2026-06-08 (6 working days: June 1, 2, 3, 4, 5, 8)
        CreateLeaveRequestDto req1 = new CreateLeaveRequestDto(
                "ANNUAL",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 8),
                "FULL_DAY",
                "Trip to beach",
                "Summer Vacay 1"
        );

        // Request 2: 2026-06-15 to 2026-06-22 (6 working days: June 15, 16, 17, 18, 19, 22)
        CreateLeaveRequestDto req2 = new CreateLeaveRequestDto(
                "ANNUAL",
                LocalDate.of(2026, 6, 15),
                LocalDate.of(2026, 6, 22),
                "FULL_DAY",
                "Mountain hiking",
                "Summer Vacay 2"
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<LeaveRecordResponse> results = Collections.synchronizedList(new ArrayList<>());

        executor.submit(() -> {
            try {
                startLatch.await();
                LeaveRecordResponse res = leaveService.submitLeaveRequest(tenantId, employeeId, "Test Employee", req1);
                results.add(res);
                successCount.incrementAndGet();
            } catch (ResponseStatusException e) {
                failureCount.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                LeaveRecordResponse res = leaveService.submitLeaveRequest(tenantId, employeeId, "Test Employee", req2);
                results.add(res);
                successCount.incrementAndGet();
            } catch (ResponseStatusException e) {
                failureCount.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        });

        // Trigger concurrent execution
        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        // Exactly ONE must succeed and ONE must fail
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);
        assertThat(results).hasSize(1);

        // Verify balance in database: 10 - 6 = 4 available, 6 pending
        LeaveBalance finalBalance = leaveBalanceRepository
                .findByTenantIdAndEmployeeIdAndLeaveTypeCodeAndYear(tenantId, employeeId, "ANNUAL", 2026)
                .orElseThrow();

        assertThat(finalBalance.getAvailable()).isEqualByComparingTo(new BigDecimal("4.00"));
        assertThat(finalBalance.getPending()).isEqualByComparingTo(new BigDecimal("6.00"));
        assertThat(finalBalance.getUsed()).isEqualByComparingTo(BigDecimal.ZERO);

        // Verify ledger has only 1 reservation row (the failing request rolled back without writing a ledger row)
        List<LeaveLedger> ledgers = leaveLedgerRepository.findByTenantIdAndEmployeeIdOrderByCreatedAtDesc(tenantId, employeeId);
        assertThat(ledgers).hasSize(1);
        assertThat(ledgers.get(0).getEntryType()).isEqualTo("RESERVATION");
        assertThat(ledgers.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("6.00"));

        // Verify requests table has only 1 request
        List<LeaveRequest> requests = leaveRequestRepository.findByTenantIdAndEmployeeIdOrderByCreatedAtDesc(tenantId, employeeId);
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).getStatus()).isEqualTo("PENDING");
        assertThat(requests.get(0).getDays()).isEqualByComparingTo(new BigDecimal("6.00"));
    }
}
