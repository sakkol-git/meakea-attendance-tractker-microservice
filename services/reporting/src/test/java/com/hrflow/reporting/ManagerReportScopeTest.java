package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.hrflow.contracts.organization.EmployeeSnapshot;
import com.hrflow.contracts.organization.OrganizationFact;
import com.hrflow.reporting.consumer.ReportingEmployeeConsumer;
import com.hrflow.reporting.consumer.SessionProjectionConsumer;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ManagerReportScopeTest {
    // This suite isolates transactional scope. The real replay gate is exercised by ManagerReportingKafkaIT.
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.hrflow.reporting.consumer.ReportingEmployeeBootstrap bootstrap;
    @org.junit.jupiter.api.BeforeEach void readyReferenceFixture() {
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
    @Autowired ReportingEmployeeConsumer employees;
    @Autowired SessionProjectionConsumer sessions;
    @Autowired com.hrflow.reporting.consumer.DailyProjectionConsumer dailyFacts;
    @Autowired com.hrflow.reporting.repository.AttendanceFactProjectionRepository attendanceFacts;
    @Autowired org.springframework.transaction.support.TransactionTemplate transactions;
    @Autowired com.hrflow.reporting.service.TenantSqlScope tenantScope;

    @Test void managerSeesOnlyCurrentDirectReportsAndReassignmentImmediatelyRemovesAccess() throws Exception {
        UUID tenant = UUID.randomUUID(), manager = UUID.randomUUID(), direct = UUID.randomUUID(), other = UUID.randomUUID();
        apply(employee(tenant, manager, null, 1));
        apply(employee(tenant, direct, manager, 1));
        apply(employee(tenant, other, UUID.randomUUID(), 1));
        for (UUID id : List.of(direct, other)) {
            var fact = com.hrflow.contracts.attendance.AttendanceSessionFact.newBuilder(SessionProjectionTest.fact(1, null))
                    .setTenantId(tenant.toString()).setEmployeeId(id.toString()).build();
            sessions.apply(SessionProjectionTest.record(fact, id.equals(direct) ? 0 : 1));
        }
        var caller = jwt().jwt(j -> j.subject(UUID.randomUUID().toString()).claim("employeeId", manager.toString())
                .claim("tenantId", tenant.toString()).claim("roles", List.of("MANAGER")))
                .authorities(new SimpleGrantedAuthority("ROLE_MANAGER"));
        mvc.perform(get("/reports/attendance/summary").with(caller)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.records[0].employeeId").value(direct.toString()));
        var reassigned = employee(tenant, direct, other, 2);
        apply(reassigned);
        apply(reassigned); // duplicate delivery
        apply(employee(tenant, direct, manager, 1)); // older snapshot cannot restore permission
        mvc.perform(get("/reports/attendance/summary").with(caller)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0));
        mvc.perform(get("/reports/attendance/summary").with(jwt().jwt(j -> j.subject(manager.toString())
                .claim("employeeId", manager.toString()).claim("tenantId", UUID.randomUUID().toString())
                .claim("roles", List.of("MANAGER"))).authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
                .andExpect(status().isServiceUnavailable());
    }

    static OrganizationFact employee(UUID tenant, UUID employee, UUID manager, long revision) {
        var snapshot = EmployeeSnapshot.newBuilder().setEmployeeId(employee.toString()).setEmployeeNo("E-" + employee)
                .setFullName("Synthetic employee").setStatus("ACTIVE").setAssignments(List.of())
                .setManagerId(manager == null ? null : manager.toString()).build();
        return new OrganizationFact(UUID.randomUUID().toString(), "org.employee.updated.v1", 1,
                tenant.toString(), employee.toString(), revision, Instant.now().toString(),
                UUID.randomUUID().toString(), null, snapshot);
    }

    @Test void dashboardAggregatesOnlyTheManagersCurrentTeam() throws Exception {
        UUID tenant = UUID.randomUUID(), manager = UUID.randomUUID(), direct = UUID.randomUUID(), unrelated = UUID.randomUUID();
        apply(employee(tenant, manager, null, 1));
        apply(employee(tenant, direct, manager, 1));
        apply(employee(tenant, unrelated, UUID.randomUUID(), 1));
        for (UUID id : List.of(direct, unrelated)) {
            var fact = com.hrflow.contracts.attendance.AttendanceDailyFact.newBuilder(
                    DailyReportingKafkaIT.fact(tenant, id, UUID.randomUUID(), 1, true))
                    .setStatus("ON_TIME").setClockIn(java.time.Instant.now().minusSeconds(3600))
                    .setClockOut(java.time.Instant.now()).setMinutesWorked(60).setSessionReferences(List.of(
                            com.hrflow.contracts.attendance.DailySessionReference.newBuilder()
                                    .setSessionId(UUID.randomUUID().toString()).setRevision(1).build())).build();
            dailyFacts.apply(new org.apache.kafka.clients.consumer.ConsumerRecord<>("attendance.daily-status.v1", 0, 0, tenant + ":" + id, fact));
        }
        mvc.perform(get("/reports/dashboard").with(jwt().jwt(j -> j.subject(manager.toString())
                .claim("employeeId", manager.toString()).claim("tenantId", tenant.toString()).claim("roles", List.of("MANAGER")))
                .authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalPresentToday").value(1));
    }
    private void apply(OrganizationFact fact) { employees.apply(fact.getTenantId() + ":" + fact.getAggregateId(), fact); }

    @Test void retainedRelationshipsCannotAuthorizeBeforeTheBootstrapGateIsReady() throws Exception {
        UUID tenant = UUID.randomUUID(), manager = UUID.randomUUID();
        apply(employee(tenant, manager, null, 1));
        org.mockito.Mockito.when(bootstrap.isReady()).thenReturn(false);
        mvc.perform(get("/reports/attendance/summary").with(jwt().jwt(j -> j.subject(manager.toString())
                .claim("employeeId", manager.toString()).claim("tenantId", tenant.toString())
                .claim("roles", List.of("MANAGER"))).authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("REFERENCE_NOT_READY"));
    }
}
