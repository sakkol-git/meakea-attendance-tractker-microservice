package com.hrflow.leave.conflict;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hrflow.contracts.attendance.AttendanceDailyFact;
import com.hrflow.contracts.attendance.DailyLeaveReference;
import com.hrflow.contracts.attendance.DailySessionReference;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class LeaveAttendanceReviewTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine").withInitScript("context-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl); p.add("spring.datasource.username", () -> "leave_app");
        p.add("spring.datasource.password", () -> "test-pass"); p.add("spring.flyway.user", POSTGRES::getUsername); p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @Autowired LeaveAttendanceReview review;
    @Autowired TransactionTemplate transactions;
    @Autowired JdbcTemplate runtime;
    @Autowired MockMvc mvc;
    static final LocalDate DATE = LocalDate.parse("2026-11-02");

    @Test void onlyTheActuallyOverlappingHalfNeedsHrReviewAndDebitStaysUnchanged() throws Exception {
        UUID tenant = id(), employee = id(), morning = id(), afternoon = id();
        seed(tenant, employee, morning, "MORNING", "APPROVED", 7);
        seed(tenant, employee, afternoon, "AFTERNOON", "APPROVED", 7);
        var fact = fact(tenant, employee, morning, "MORNING", 8, true);
        fact.setLeaveReferences(List.of(fact.getLeaveReferences().getFirst(), reference(afternoon, "AFTERNOON", 8, false)));
        assertThatThrownBy(() -> transactions.executeWithoutResult(tx -> { review.apply(key(fact), fact); throw new IllegalStateException("rollback"); }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(state(morning)).isEqualTo("APPROVED");
        review.apply(key(fact), fact); review.apply(key(fact), fact);
        assertThat(state(morning)).isEqualTo("NEEDS_REVIEW"); assertThat(state(afternoon)).isEqualTo("APPROVED");
        assertThat(admin().queryForObject("SELECT sum(used) FROM leave.leave_balance WHERE tenant_id=?", java.math.BigDecimal.class, tenant.toString())).isEqualByComparingTo("1");
        assertThat(admin().queryForObject("SELECT count(*) FROM leave.leave_ledger WHERE tenant_id=?", Integer.class, tenant.toString())).isZero();
        assertThat(admin().queryForObject("SELECT count(*) FROM leave.leave_day WHERE tenant_id=?", Integer.class, tenant.toString())).isEqualTo(2);
        assertThat(admin().queryForObject("SELECT count(*) FROM leave.leave_outbox WHERE tenant_id=? AND payload->>'state'='NEEDS_REVIEW'", Integer.class, tenant.toString())).isEqualTo(1);
        mvc.perform(get("/leave/requests/pending").param("page", "0").param("status", "NEEDS_REVIEW")
                .with(jwt().jwt(jwt -> jwt.subject(id().toString()).claim("tenantId", tenant.toString()).claim("employeeId", id().toString()).claim("roles", List.of("HR_ADMIN")))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].status").value("NEEDS_REVIEW"));
        assertThat(runtime.queryForObject("SELECT count(*) FROM leave.leave_request", Integer.class)).isZero();
    }

    @Test void unknownLegacyOverlapStaleApprovalAndCancelledLeaveNeverCreateAReviewOrRefund() {
        for (Boolean overlap : new Boolean[] {null, false, true}) {
            UUID tenant = id(), employee = id(), leave = id();
            seed(tenant, employee, leave, "FULL_DAY", overlap == Boolean.TRUE ? "CANCELLED" : "APPROVED", 9);
            var fact = fact(tenant, employee, leave, "FULL_DAY", 8, overlap);
            review.apply(key(fact), fact);
            assertThat(state(leave)).isEqualTo(overlap == Boolean.TRUE ? "CANCELLED" : "APPROVED");
            assertThat(admin().queryForObject("SELECT count(*) FROM leave.leave_outbox WHERE tenant_id=?", Integer.class, tenant.toString())).isZero();
        }
        UUID tenant = id(), employee = id(), leave = id(); seed(tenant, employee, leave, "MORNING", "APPROVED", 7);
        var wrongEmployee = fact(tenant, id(), leave, "MORNING", 8, true);
        assertThatThrownBy(() -> review.apply(key(wrongEmployee), wrongEmployee)).isInstanceOf(IllegalArgumentException.class);
        assertThat(state(leave)).isEqualTo("APPROVED");
        var stale = fact(tenant, employee, leave, "MORNING", 7, true); review.apply(key(stale), stale);
        assertThat(state(leave)).isEqualTo("APPROVED");
        var foreign = fact(id(), employee, leave, "MORNING", 8, true);
        assertThatThrownBy(() -> review.apply(key(foreign), foreign)).isInstanceOf(org.springframework.web.ErrorResponseException.class);
        assertThat(state(leave)).isEqualTo("APPROVED");
    }

    private void seed(UUID tenant, UUID employee, UUID leave, String portion, String state, long version) {
        admin().update("INSERT INTO leave.leave_request(id,tenant_id,employee_id,type,start_date,end_date,portion,days,status,version,paid_charge) VALUES (?,?,?,'ANNUAL',?,?,?,0.5,?,?,true)",
                leave.toString(), tenant.toString(), employee.toString(), DATE, DATE, portion, state, version);
        admin().update("INSERT INTO leave.leave_day(id,tenant_id,request_id,employee_id,date,portion,scheduled_seconds) VALUES (?,?,?,?,?,?,14400)",
                id().toString(), tenant.toString(), leave.toString(), employee.toString(), DATE, portion);
        admin().update("INSERT INTO leave.leave_balance(id,tenant_id,employee_id,leave_type_code,year,allocated,used,pending,available) VALUES (?,?,?,'ANNUAL',2026,20,1,0,19) ON CONFLICT DO NOTHING", id().toString(), tenant.toString(), employee.toString());
    }
    private String state(UUID leave) { return admin().queryForObject("SELECT status FROM leave.leave_request WHERE id=?", String.class, leave.toString()); }
    private JdbcTemplate admin() { return new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())); }
    static String key(AttendanceDailyFact fact) { return fact.getTenantId() + ":" + fact.getEmployeeId(); }
    static UUID id() { return UUID.randomUUID(); }
    static DailyLeaveReference reference(UUID leave, String portion, long revision, Boolean work) {
        return DailyLeaveReference.newBuilder().setLeaveId(leave.toString()).setPortion(portion).setRevision(revision).setWorkRecorded(work).build();
    }
    static AttendanceDailyFact fact(UUID tenant, UUID employee, UUID leave, String portion, long leaveRevision, Boolean work) {
        return AttendanceDailyFact.newBuilder().setEventId(id().toString()).setType("AttendanceDailySnapshot.v1").setSchemaVersion(1)
                .setTenantId(tenant.toString()).setAggregateId(id().toString()).setAggregateVersion(1L).setOccurredAt(Instant.now())
                .setCorrelationId(id().toString()).setEmployeeId(employee.toString()).setWorkDate(DATE).setTimezone("Asia/Phnom_Penh")
                .setStatus("REVIEW_REQUIRED").setDayType("PARTIAL_LEAVE").setClockIn(Instant.parse("2026-11-02T01:00:00Z"))
                .setClockOut(Instant.parse("2026-11-02T02:00:00Z")).setLate(false).setEarlyDeparture(false).setReviewRequired(true)
                .setReviewReason("Attendance recorded during approved leave").setFinalized(true).setOpenSession(false).setMinutesWorked(60).setMinutesLate(0)
                .setShiftId(id().toString()).setShiftRevision(1L).setLeaveReferences(List.of(reference(leave, portion, leaveRevision, work)))
                .setSessionReferences(List.of(new DailySessionReference(id().toString(), 1L))).build();
    }
}
