package com.hrflow.leave;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hrflow.leave.entity.LeaveBalance;
import com.hrflow.leave.entity.LeaveType;
import com.hrflow.leave.repository.LeaveBalanceRepository;
import com.hrflow.leave.repository.LeaveTypeRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@AutoConfigureMockMvc
@Testcontainers
class LeaveTenantMatrixTest {
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.hrflow.leave.organization.LeaveCalendarBootstrap calendarBootstrap;
    @org.junit.jupiter.api.BeforeEach void calendarReferenceReady() {
        org.mockito.Mockito.when(calendarBootstrap.isReady()).thenReturn(true);
    }

    // Isolates authorization/transaction tests; the actual broker replay is verified separately.
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.hrflow.leave.organization.LeaveEmployeeBootstrap bootstrap;
    @org.junit.jupiter.api.BeforeEach void readyReferenceGate() {
        org.mockito.Mockito.when(bootstrap.isReady()).thenReturn(true);
    }

    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

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
    MockMvc mvc;

    @Autowired
    LeaveTypeRepository leaveTypeRepository;

    @Autowired
    LeaveBalanceRepository leaveBalanceRepository;

    @Test
    @DisplayName("Unauthenticated requests are denied with 401 across all leave routes")
    void unauthenticatedRequestsDeniedWith401() throws Exception {
        String randomId = UUID.randomUUID().toString();

        mvc.perform(get("/leave/types")).andExpect(status().isUnauthorized());
        mvc.perform(get("/leave/balances/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/leave/requests/preview").header("Idempotency-Key", UUID.randomUUID().toString()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/leave/requests").header("Idempotency-Key", UUID.randomUUID().toString()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(get("/leave/requests/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/leave/requests/pending")).andExpect(status().isUnauthorized());
        mvc.perform(get("/leave/requests/{id}", randomId)).andExpect(status().isUnauthorized());
        mvc.perform(post("/leave/requests/{id}/approve", randomId).header("Idempotency-Key", UUID.randomUUID().toString())).andExpect(status().isUnauthorized());
        mvc.perform(post("/leave/requests/{id}/reject", randomId).header("Idempotency-Key", UUID.randomUUID().toString())).andExpect(status().isUnauthorized());
        mvc.perform(post("/leave/requests/{id}/cancel", randomId).header("Idempotency-Key", UUID.randomUUID().toString())).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Missing tenant claim is denied with 403 Forbidden across leave routes")
    void missingTenantClaimDeniedWith403() throws Exception {
        UUID employeeId = UUID.randomUUID();
        JwtRequestPostProcessor noTenant = jwt().jwt(token -> token
                .subject(employeeId.toString())
                .claim("roles", List.of("EMPLOYEE"))
        ).authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));

        mvc.perform(get("/leave/types").with(noTenant)).andExpect(status().isForbidden());
        mvc.perform(get("/leave/balances/me").with(noTenant)).andExpect(status().isForbidden());
        mvc.perform(get("/leave/requests/me").with(noTenant)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Pending approval list, approve and reject require elevated manager roles")
    void pendingApprovalRequireElevatedRole() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID employee = UUID.randomUUID();
        String randomId = UUID.randomUUID().toString();

        JwtRequestPostProcessor employeeOnly = jwt().jwt(token -> token
                .subject(employee.toString())
                .claim("tenantId", tenant.toString())
                .claim("employeeId", employee.toString())
                .claim("roles", List.of("EMPLOYEE"))
        ).authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));

        mvc.perform(get("/leave/requests/pending").with(employeeOnly))
                .andExpect(status().isForbidden());

        mvc.perform(post("/leave/requests/{id}/approve", randomId).header("Idempotency-Key", UUID.randomUUID().toString()).with(employeeOnly))
                .andExpect(status().isForbidden());

        mvc.perform(post("/leave/requests/{id}/reject", randomId).header("Idempotency-Key", UUID.randomUUID().toString()).with(employeeOnly))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cross-tenant boundary isolation: Tenant B cannot view or access Tenant A leave records")
    void crossTenantIsolation() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID empA = UUID.randomUUID();
        UUID empB = UUID.randomUUID();

        // Seed leave type and balance for Tenant A
        leaveTypeRepository.save(new LeaveType(
                UUID.randomUUID().toString(),
                tenantA.toString(),
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
                tenantA.toString(),
                empA.toString(),
                "ANNUAL",
                2026,
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("10.00")
        ));

        LeaveReferenceFixtures.calendar(jdbc, tenantA.toString(), empA.toString(), 31);

        // Employee A creates a leave request
        String reqBody = """
                {
                  "type": "ANNUAL",
                  "startDate": "2026-09-01",
                  "endDate": "2026-09-02",
                  "portion": "FULL_DAY",
                  "reason": "Personal rest",
                  "title": "September Break"
                }
                """;

        String createdResponse = mvc.perform(post("/leave/requests").header("Idempotency-Key", UUID.randomUUID().toString()).with(employee(tenantA, empA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(reqBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        // Employee B in Tenant B queries /leave/requests/me -> sees 0 items
        mvc.perform(get("/leave/requests/me").with(employee(tenantB, empB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        // Employee B in Tenant B queries /leave/balances/me -> sees 0 balances
        mvc.perform(get("/leave/balances/me").with(employee(tenantB, empB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        UUID tenantBManager = UUID.randomUUID();
        LeaveReferenceFixtures.employee(jdbc, tenantB.toString(), tenantBManager.toString(), null);
        // Tenant B manager queries /leave/requests/pending -> sees 0 pending requests
        mvc.perform(get("/leave/requests/pending").with(manager(tenantB, tenantBManager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    private static JwtRequestPostProcessor employee(UUID tenantId, UUID employeeId) {
        return jwt().jwt(token -> token
                .subject(employeeId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("employeeId", employeeId.toString())
                .claim("name", "Employee " + employeeId)
                .claim("roles", List.of("EMPLOYEE"))
        ).authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
    }

    private static JwtRequestPostProcessor manager(UUID tenantId, UUID managerId) {
        return jwt().jwt(token -> token
                .subject(managerId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("employeeId", managerId.toString())
                .claim("name", "Manager " + managerId)
                .claim("roles", List.of("MANAGER"))
        ).authorities(new SimpleGrantedAuthority("ROLE_MANAGER"));
    }
}
