package com.hrflow.leave;

import static org.assertj.core.api.Assertions.assertThat;

import com.hrflow.leave.dto.CreateLeaveRequestDto;
import com.hrflow.leave.entity.LeaveBalance;
import com.hrflow.leave.entity.LeaveType;
import com.hrflow.leave.repository.LeaveBalanceRepository;
import com.hrflow.leave.repository.LeaveTypeRepository;
import com.hrflow.leave.service.LeaveDecisions;
import com.hrflow.leave.service.LeaveSubmissionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
class LeaveDecisionConcurrencyTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("context-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", POSTGRES::getUsername);
        p.add("spring.datasource.password", POSTGRES::getPassword);
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @MockitoBean com.hrflow.leave.organization.LeaveEmployeeBootstrap employeeBootstrap;
    @MockitoBean com.hrflow.leave.organization.LeaveCalendarBootstrap calendarBootstrap;
    @Autowired LeaveSubmissionService submissions;
    @Autowired LeaveDecisions decisions;
    @Autowired LeaveTypeRepository types;
    @Autowired LeaveBalanceRepository balances;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean java.time.Clock clock;
    @org.junit.jupiter.api.BeforeEach void fixedPolicyClock() {
        org.mockito.Mockito.when(clock.instant()).thenReturn(java.time.Instant.parse("2026-10-28T00:00:00Z"));
    }

    @org.junit.jupiter.api.Test void simultaneousDifferentLeaveTypesCannotReserveTheSameHalf() throws Exception {
        org.mockito.Mockito.when(employeeBootstrap.isReady()).thenReturn(true);
        org.mockito.Mockito.when(calendarBootstrap.isReady()).thenReturn(true);
        String tenant = UUID.randomUUID().toString(), employee = UUID.randomUUID().toString();
        for (String type : List.of("ANNUAL", "SICK")) {
            types.save(new LeaveType(UUID.randomUUID().toString(), tenant, type, type, new BigDecimal("20.00"), true, true, false, 0, BigDecimal.ZERO));
            balances.save(new LeaveBalance(UUID.randomUUID().toString(), tenant, employee, type, 2026,
                    new BigDecimal("20.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("20.00")));
        }
        LeaveReferenceFixtures.calendar(jdbc, tenant, employee, 127);
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var outcomes = new java.util.ArrayList<java.util.concurrent.Future<Integer>>();
            for (String type : List.of("ANNUAL", "SICK")) outcomes.add(executor.submit(() -> {
                start.await();
                try {
                    var day = LocalDate.parse("2026-11-02");
                    submissions.submitLeaveRequest(tenant, employee, "Synthetic", new CreateLeaveRequestDto(type, day, day, "FULL_DAY", "Reason", "Title"));
                    return 201;
                } catch (org.springframework.web.ErrorResponseException rejected) { return rejected.getStatusCode().value(); }
            }));
            start.countDown();
            assertThat(List.of(outcomes.get(0).get(10, TimeUnit.SECONDS), outcomes.get(1).get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(jdbc.queryForObject("SELECT sum(pending) FROM leave.leave_balance WHERE tenant_id=?", BigDecimal.class, tenant)).isEqualByComparingTo("1.00");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM leave.leave_request WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM leave.day_reservation WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(2);
    }

    @org.junit.jupiter.api.Test void elapsedApprovedLeaveCannotBeRefundedThroughWholeRequestCancellation() {
        org.mockito.Mockito.when(employeeBootstrap.isReady()).thenReturn(true);
        org.mockito.Mockito.when(calendarBootstrap.isReady()).thenReturn(true);
        String tenant = UUID.randomUUID().toString(), employee = UUID.randomUUID().toString();
        types.save(new LeaveType(UUID.randomUUID().toString(), tenant, "ANNUAL", "Annual", new BigDecimal("20.00"), true, true, false, 0, BigDecimal.ZERO));
        balances.save(new LeaveBalance(UUID.randomUUID().toString(), tenant, employee, "ANNUAL", 2026,
                new BigDecimal("20.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("20.00")));
        LeaveReferenceFixtures.calendar(jdbc, tenant, employee, 127);
        var past = LocalDate.parse("2026-10-26");
        var pending = submissions.submitLeaveRequest(tenant, employee, "Synthetic", new CreateLeaveRequestDto("ANNUAL", past, past, "FULL_DAY", "Reason", "Title"));
        var approved = decisions.approveLeaveRequest(tenant, UUID.randomUUID().toString(), List.of("HR_ADMIN"), pending.id(), "\"" + pending.version() + "\"", null);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> decisions.cancelLeaveRequest(tenant, employee, approved.id(), "\"" + approved.version() + "\"", "Changed plans"))
                .isInstanceOfSatisfying(org.springframework.web.server.ResponseStatusException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(422));
        assertThat(jdbc.queryForObject("SELECT used FROM leave.leave_balance WHERE tenant_id=?", BigDecimal.class, tenant)).isEqualByComparingTo("1.00");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM leave.leave_outbox WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(2);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"APPROVE", "REJECT", "CANCEL_PENDING", "CANCEL_APPROVED"})
    void simultaneousDecisionsSettleExactlyOnceAndLoserGetsAStaleOrConflictResponse(String mode) throws Exception {
        org.mockito.Mockito.when(employeeBootstrap.isReady()).thenReturn(true);
        org.mockito.Mockito.when(calendarBootstrap.isReady()).thenReturn(true);
        String tenant = UUID.randomUUID().toString(), employee = UUID.randomUUID().toString();
        types.save(new LeaveType(UUID.randomUUID().toString(), tenant, "ANNUAL", "Annual", new BigDecimal("20.00"), true, true, false, 0, BigDecimal.ZERO));
        balances.save(new LeaveBalance(UUID.randomUUID().toString(), tenant, employee, "ANNUAL", 2026,
                new BigDecimal("20.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("20.00")));
        LeaveReferenceFixtures.calendar(jdbc, tenant, employee, 127);
        var day = LocalDate.parse("2026-11-02");
        var pending = submissions.submitLeaveRequest(tenant, employee, "Synthetic", new CreateLeaveRequestDto("ANNUAL", day, day, "FULL_DAY", "Reason", "Title"));
        var target = "CANCEL_APPROVED".equals(mode) ? decisions.approveLeaveRequest(tenant, UUID.randomUUID().toString(), List.of("HR_ADMIN"),
                pending.id(), "\"" + pending.version() + "\"", null) : pending;
        var failures = new java.util.concurrent.ConcurrentLinkedQueue<String>();
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var lockedBalance = java.sql.DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var executor = Executors.newFixedThreadPool(2)) {
            lockedBalance.setAutoCommit(false);
            try (var lock = lockedBalance.prepareStatement("SELECT id FROM leave.leave_balance WHERE tenant_id=? FOR UPDATE")) {
                lock.setString(1, tenant);
                lock.executeQuery().close();
            }
            var action = (java.util.concurrent.Callable<Integer>) () -> {
                start.await();
                try {
                    String tag = "\"" + target.version() + "\"";
                    switch (mode) {
                        case "APPROVE" -> decisions.approveLeaveRequest(tenant, UUID.randomUUID().toString(), List.of("HR_ADMIN"), target.id(), tag, null);
                        case "REJECT" -> decisions.rejectLeaveRequest(tenant, UUID.randomUUID().toString(), List.of("HR_ADMIN"), target.id(), tag, null);
                        default -> decisions.cancelLeaveRequest(tenant, employee, target.id(), tag, "Changed plans");
                    }
                    return 200;
                } catch (org.springframework.web.ErrorResponseException failure) { return failure.getStatusCode().value(); }
                catch (Exception unexpected) { failures.add(unexpected.getClass().getSimpleName() + ": " + unexpected.getMessage()); return 500; }
            };
            var first = executor.submit(action);
            var second = executor.submit(action);
            start.countDown();
            try {
                // The external balance lock lets both commands reach a real database lock wait.
                // After the fix, one waits on the request and one on the balance instead of both using stale request state.
                org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(5)).until(() ->
                        jdbc.queryForObject("SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock' AND (query ILIKE '%leave_balance%' OR query ILIKE '%leave_request%')", Integer.class) >= 2);
            } finally { lockedBalance.commit(); }
            var results = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertThat(results.stream().filter(status -> status == 200).count()).as("Unexpected failures: %s", failures).isEqualTo(1);
            assertThat(results).as("Unexpected failures: %s", failures).allMatch(status -> status == 200 || status == 409 || status == 412);
        }
        assertThat(jdbc.queryForObject("SELECT used FROM leave.leave_balance WHERE tenant_id=?", BigDecimal.class, tenant)).isEqualByComparingTo("APPROVE".equals(mode) ? "1.00" : "0.00");
        assertThat(jdbc.queryForObject("SELECT pending FROM leave.leave_balance WHERE tenant_id=?", BigDecimal.class, tenant)).isEqualByComparingTo("0.00");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM leave.leave_ledger WHERE tenant_id=? AND entry_type='USED'", Integer.class, tenant)).isEqualTo("APPROVE".equals(mode) || "CANCEL_APPROVED".equals(mode) ? 1 : 0);
    }
}
