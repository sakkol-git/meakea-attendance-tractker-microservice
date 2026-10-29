package com.hrflow.attendance;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@AutoConfigureMockMvc
class AttendanceTenantMatrixTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("context-schema.sql");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.flyway.user", POSTGRES::getUsername);
        properties.add("spring.flyway.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void unauthenticatedRequestsAreDeniedWith401AcrossAllRoutes() throws Exception {
        UUID randomId = UUID.randomUUID();

        mvc.perform(post("/attendance/check-in")).andExpect(status().isUnauthorized());
        mvc.perform(post("/attendance/check-out")).andExpect(status().isUnauthorized());
        mvc.perform(get("/attendance/today")).andExpect(status().isUnauthorized());
        mvc.perform(get("/attendance/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/attendance/daily-status/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/attendance/daily-status")).andExpect(status().isUnauthorized());
        mvc.perform(post("/attendance/corrections")).andExpect(status().isUnauthorized());
        mvc.perform(get("/attendance/corrections")).andExpect(status().isUnauthorized());
        mvc.perform(post("/attendance/corrections/{id}/approve", randomId)).andExpect(status().isUnauthorized());
        mvc.perform(post("/attendance/corrections/{id}/reject", randomId)).andExpect(status().isUnauthorized());
    }

    @Test
    void missingTenantClaimIsDeniedWith403() throws Exception {
        UUID employeeId = UUID.randomUUID();
        JwtRequestPostProcessor noTenant = jwt().jwt(token -> token
                .subject(employeeId.toString())
                .claim("roles", java.util.List.of("EMPLOYEE"))
        ).authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));

        mvc.perform(get("/attendance/today").with(noTenant)).andExpect(status().isForbidden());
        mvc.perform(get("/attendance/me").with(noTenant)).andExpect(status().isForbidden());
        mvc.perform(get("/attendance/daily-status/me").with(noTenant)).andExpect(status().isForbidden());
        mvc.perform(get("/attendance/corrections").with(noTenant)).andExpect(status().isForbidden());
    }

    @Test
    void approvalAndRejectionRequireElevatedRoles() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID employee = UUID.randomUUID();
        UUID correctionId = UUID.randomUUID();

        JwtRequestPostProcessor employeeOnly = jwt().jwt(token -> token
                .subject(employee.toString())
                .claim("tenantId", tenant.toString())
                .claim("employeeId", employee.toString())
                .claim("roles", java.util.List.of("EMPLOYEE"))
        ).authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));

        mvc.perform(post("/attendance/corrections/{id}/approve", correctionId)
                .with(employeeOnly)
                .header("If-Match", "\"0\""))
                .andExpect(status().isForbidden());

        mvc.perform(post("/attendance/corrections/{id}/reject", correctionId)
                .with(employeeOnly)
                .header("If-Match", "\"0\""))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeCannotSeeCrossTenantAttendanceData() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID employeeA = UUID.randomUUID();
        UUID employeeB = UUID.randomUUID();
        AttendanceFixtures.schedule(jdbc, tenantA, employeeA);
        AttendanceFixtures.schedule(jdbc, tenantB, employeeB);

        // Employee A checks in under tenant A
        String body = """
                {"latitude":11.56,"longitude":104.92,"accuracyMeters":10.0,"deviceModel":"Pixel 8"}
                """;
        mvc.perform(post("/attendance/check-in").with(employee(tenantA, employeeA))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        // Employee B under tenant B queries endpoints -> sees 0 data
        mvc.perform(get("/attendance/me").with(employee(tenantB, employeeB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items").isEmpty());

        mvc.perform(get("/attendance/today").with(employee(tenantB, employeeB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessions").isEmpty());

        mvc.perform(get("/attendance/corrections").with(employee(tenantB, employeeB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    private static JwtRequestPostProcessor employee(UUID tenantId, UUID employeeId) {
        return jwt().jwt(token -> token
                .subject(employeeId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("employeeId", employeeId.toString())
                .claim("roles", java.util.List.of("EMPLOYEE"))
        ).authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
    }
}
