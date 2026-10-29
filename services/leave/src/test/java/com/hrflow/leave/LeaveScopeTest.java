package com.hrflow.leave;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hrflow.leave.entity.LeaveBalance;
import com.hrflow.leave.entity.LeaveRequest;
import com.hrflow.leave.entity.LeaveType;
import com.hrflow.leave.repository.LeaveBalanceRepository;
import com.hrflow.leave.repository.LeaveRequestRepository;
import com.hrflow.leave.repository.LeaveTypeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class LeaveScopeTest {
    // Isolates authorization/transaction tests; the actual broker replay is verified separately.
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.hrflow.leave.organization.LeaveEmployeeBootstrap bootstrap;
    @org.junit.jupiter.api.BeforeEach void readyReferenceGate() {
        org.mockito.Mockito.when(bootstrap.isReady()).thenReturn(true);
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
    @Autowired MockMvc mvc;
    @Autowired LeaveTypeRepository types;
    @Autowired LeaveRequestRepository requests;
    @Autowired LeaveBalanceRepository balances;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired com.hrflow.leave.organization.LeaveEmployeeConsumer references;
    @Autowired org.springframework.transaction.support.TransactionTemplate transactions;

    @Test void pagedLeaveListsFilterAndCountOnlyTheCallersAuthorizedRows() throws Exception {
        UUID tenant = UUID.randomUUID(), manager = UUID.randomUUID(), direct = UUID.randomUUID(), stranger = UUID.randomUUID();
        LeaveReferenceFixtures.employee(jdbc, tenant.toString(), manager.toString(), null);
        LeaveReferenceFixtures.employee(jdbc, tenant.toString(), direct.toString(), manager.toString());
        LeaveReferenceFixtures.employee(jdbc, tenant.toString(), stranger.toString(), UUID.randomUUID().toString());
        for (int index = 0; index < 3; index++) {
            var row = new LeaveRequest(UUID.randomUUID().toString(), tenant.toString(), direct.toString(),
                    "Direct", "ANNUAL", "Rest " + index, LocalDate.parse("2026-11-02"), LocalDate.parse("2026-11-02"),
                    "FULL_DAY", BigDecimal.ONE, "Private");
            requests.saveAndFlush(row);
            jdbc.update("UPDATE leave.leave_request SET created_at=?::timestamptz WHERE id=?", "2026-01-0" + (index + 1) + "T00:00:00Z", row.getId());
        }
        requests.saveAndFlush(new LeaveRequest(UUID.randomUUID().toString(), tenant.toString(), stranger.toString(),
                "Stranger", "ANNUAL", "Private", LocalDate.parse("2026-11-02"), LocalDate.parse("2026-11-02"),
                "FULL_DAY", BigDecimal.ONE, "Private"));
        mvc.perform(get("/leave/requests/pending").param("page", "0").param("size", "2")
                .with(caller(tenant, manager, manager, "MANAGER")))
                .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.total").value(3))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.items[0].title").value("Rest 0"));
        mvc.perform(get("/leave/requests/pending").param("page", "1").param("size", "2")
                .with(caller(tenant, manager, manager, "MANAGER")))
                .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.items.length()").value(1));
        mvc.perform(get("/leave/requests/me").param("page", "0").param("size", "2").param("sort", "newest")
                .with(caller(tenant, direct, direct, "EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.total").value(3))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.items[0].title").value("Rest 2"));
        mvc.perform(get("/leave/requests/me").param("page", "0").param("search", "rest 1").param("minAgeDays", "1")
                .with(caller(tenant, direct, direct, "EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.total").value(1));
        mvc.perform(get("/leave/requests/me").param("page", "0").param("search", "%")
                .with(caller(tenant, direct, direct, "EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.total").value(0));
        jdbc.update("UPDATE leave.employee_projection SET manager_id=NULL WHERE tenant_id=? AND employee_id=?", tenant.toString(), direct.toString());
        mvc.perform(get("/leave/requests/pending").param("page", "0").with(caller(tenant, manager, manager, "MANAGER")))
                .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.total").value(0));
        mvc.perform(get("/leave/requests/pending").param("page", "0").with(caller(tenant, stranger, stranger, "EMPLOYEE")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/leave/requests/me").param("page", "0").param("sort", "unknown").with(caller(tenant, direct, direct, "EMPLOYEE")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test void crossYearPreviewAndSubmissionHaveNoReservationLedgerOrEventEffects() throws Exception {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        pending(tenant, employee);
        String body = "{\"type\":\"ANNUAL\",\"startDate\":\"2026-12-31\",\"endDate\":\"2027-01-01\",\"portion\":\"FULL_DAY\"}";
        for (String path : List.of("/leave/requests/preview", "/leave/requests")) {
            mvc.perform(post(path).with(caller(tenant, employee, employee, "EMPLOYEE"))
                    .header("Idempotency-Key", UUID.randomUUID()).contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnprocessableEntity());
        }
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT count(*) FROM leave.leave_request WHERE tenant_id=?", Integer.class, tenant.toString())).isEqualTo(1);
        for (String table : List.of("leave_outbox", "leave_ledger", "leave_day")) {
            org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT count(*) FROM leave." + table + " WHERE tenant_id=?", Integer.class, tenant.toString())).isZero();
        }
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT available FROM leave.leave_balance WHERE tenant_id=?", BigDecimal.class, tenant.toString()))
                .isEqualByComparingTo("19.00");
    }

    @Test void organizationSnapshotReceiptAndRelationshipRollbackTogetherAndStaleCannotRestoreManager() {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID(), manager = UUID.randomUUID(), replacement = UUID.randomUUID();
        var original = LeaveReferenceFixtures.fact(tenant, employee, manager, 1);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> references.apply("wrong", original)).isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> transactions.executeWithoutResult(tx -> {
            references.apply(tenant + ":" + employee, original);
            throw new IllegalStateException("rollback reference");
        })).isInstanceOf(IllegalStateException.class);
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT count(*) FROM leave.organization_inbox WHERE tenant_id=?", Integer.class, tenant.toString())).isZero();
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT count(*) FROM leave.employee_projection WHERE tenant_id=?", Integer.class, tenant.toString())).isZero();
        references.apply(tenant + ":" + employee, original);
        var newer = LeaveReferenceFixtures.fact(tenant, employee, replacement, 2);
        references.apply(tenant + ":" + employee, newer);
        references.apply(tenant + ":" + employee, newer);
        references.apply(tenant + ":" + employee, LeaveReferenceFixtures.fact(tenant, employee, manager, 1));
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT manager_id FROM leave.employee_projection WHERE tenant_id=?", String.class, tenant.toString())).isEqualTo(replacement.toString());
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT count(*) FROM leave.organization_inbox WHERE tenant_id=?", Integer.class, tenant.toString())).isEqualTo(3);
    }

    @Test void retainedRelationshipsCannotAuthorizeBeforeStartupReplayCompletes() throws Exception {
        UUID tenant = UUID.randomUUID(), manager = UUID.randomUUID();
        LeaveReferenceFixtures.employee(jdbc, tenant.toString(), manager.toString(), null);
        org.mockito.Mockito.when(bootstrap.isReady()).thenReturn(false);
        mvc.perform(get("/leave/requests/pending").with(caller(tenant, manager, manager, "MANAGER")))
                .andExpect(status().isServiceUnavailable());
    }

    @Test void pendingAndDetailUseCurrentTeamAndReassignmentImmediatelyRemovesAccess() throws Exception {
        UUID tenant = UUID.randomUUID(), manager = UUID.randomUUID(), direct = UUID.randomUUID(), other = UUID.randomUUID();
        String id = pending(tenant, direct);
        LeaveReferenceFixtures.employee(jdbc, tenant.toString(), manager.toString(), null);
        LeaveReferenceFixtures.employee(jdbc, tenant.toString(), direct.toString(), manager.toString());
        LeaveReferenceFixtures.employee(jdbc, tenant.toString(), other.toString(), UUID.randomUUID().toString());
        mvc.perform(get("/leave/requests/pending").with(caller(tenant, UUID.randomUUID(), manager, "MANAGER")))
                .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].id").value(id));
        mvc.perform(get("/leave/requests/{id}", id).with(caller(tenant, manager, manager, "MANAGER"))).andExpect(status().isOk());
        jdbc.update("UPDATE leave.employee_projection SET manager_id=? WHERE tenant_id=? AND employee_id=?", other.toString(), tenant.toString(), direct.toString());
        mvc.perform(post("/leave/requests/{id}/approve", id).header("Idempotency-Key", UUID.randomUUID().toString()).with(caller(tenant, manager, manager, "MANAGER")).header("If-Match", "\"1\""))
                .andExpect(status().isForbidden());
        mvc.perform(get("/leave/requests/{id}", id).with(caller(tenant, manager, manager, "MANAGER"))).andExpect(status().isNotFound());
        mvc.perform(get("/leave/requests/pending").with(caller(tenant, manager, manager, "MANAGER")))
                .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$").isEmpty());
    }

    @Test void aManagerRoleWithoutKnownRelationshipCannotApproveAnotherEmployee() throws Exception {
        UUID tenant = UUID.randomUUID(), target = UUID.randomUUID(), manager = UUID.randomUUID();
        String id = pending(tenant, target);
        mvc.perform(post("/leave/requests/{id}/approve", id).header("Idempotency-Key", UUID.randomUUID().toString()).with(caller(tenant, manager, manager, "MANAGER"))
                .header("If-Match", "\"1\"")).andExpect(status().isServiceUnavailable());
    }

    @Test void separateAccountSubjectCannotBypassSelfApprovalCheck() throws Exception {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        String id = pending(tenant, employee);
        mvc.perform(post("/leave/requests/{id}/approve", id).header("Idempotency-Key", UUID.randomUUID().toString()).with(caller(tenant, UUID.randomUUID(), employee, "HR_ADMIN"))
                .header("If-Match", "\"1\"")).andExpect(status().isForbidden());
    }

    @Test void anotherEmployeeInTheSameTenantCannotReadPrivateLeaveDetail() throws Exception {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID(), stranger = UUID.randomUUID();
        String id = pending(tenant, employee);
        mvc.perform(get("/leave/requests/{id}", id).with(caller(tenant, stranger, stranger, "EMPLOYEE")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/leave/requests/{id}", id).with(caller(tenant, employee, employee, "EMPLOYEE")))
                .andExpect(status().isOk());
    }

    private String pending(UUID tenant, UUID employee) {
        types.save(new LeaveType(UUID.randomUUID().toString(), tenant.toString(), "ANNUAL", "Annual", new BigDecimal("20.00"),
                true, true, false, 0, BigDecimal.ZERO));
        balances.save(new LeaveBalance(UUID.randomUUID().toString(), tenant.toString(), employee.toString(), "ANNUAL", 2026,
                new BigDecimal("20.00"), BigDecimal.ZERO, BigDecimal.ONE, new BigDecimal("19.00")));
        return requests.saveAndFlush(new LeaveRequest(UUID.randomUUID().toString(), tenant.toString(), employee.toString(),
                "Synthetic", "ANNUAL", "Private", LocalDate.parse("2026-11-02"), LocalDate.parse("2026-11-02"),
                "FULL_DAY", BigDecimal.ONE, "Private detail")).getId();
    }
    static JwtRequestPostProcessor caller(UUID tenant, UUID actor, UUID employee, String role) {
        return jwt().jwt(token -> token.subject(actor.toString()).claim("tenantId", tenant.toString())
                .claim("employeeId", employee.toString()).claim("roles", List.of(role)))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
