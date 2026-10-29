package com.hrflow.request;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hrflow.request.entity.MaterialItem;
import com.hrflow.request.entity.RequestType;
import com.hrflow.request.repository.MaterialItemRepository;
import com.hrflow.request.repository.RequestTypeRepository;
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
class RequestTenantMatrixTest {

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
        registry.add("attachment.storage.dir", () -> "/tmp/hrflow-test-matrix-" + System.currentTimeMillis());
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    RequestTypeRepository requestTypeRepository;

    @Autowired
    MaterialItemRepository materialItemRepository;

    @Test
    @DisplayName("Unauthenticated requests are denied with 401 across all request routes")
    void unauthenticatedRequestsDeniedWith401() throws Exception {
        String randomId = UUID.randomUUID().toString();

        mvc.perform(get("/requests/types")).andExpect(status().isUnauthorized());
        mvc.perform(get("/materials")).andExpect(status().isUnauthorized());
        mvc.perform(post("/requests").header("Idempotency-Key", UUID.randomUUID().toString()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/requests/{id}", randomId).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(get("/requests/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/requests/inbox")).andExpect(status().isUnauthorized());
        mvc.perform(get("/requests/{id}", randomId)).andExpect(status().isUnauthorized());
        mvc.perform(post("/requests/{id}/submit", randomId).header("Idempotency-Key", UUID.randomUUID().toString())).andExpect(status().isUnauthorized());
        mvc.perform(post("/requests/{id}/approve", randomId).header("Idempotency-Key", UUID.randomUUID().toString())).andExpect(status().isUnauthorized());
        mvc.perform(post("/requests/{id}/reject", randomId).header("Idempotency-Key", UUID.randomUUID().toString())).andExpect(status().isUnauthorized());
        mvc.perform(post("/requests/{id}/cancel", randomId).header("Idempotency-Key", UUID.randomUUID().toString())).andExpect(status().isUnauthorized());
        mvc.perform(post("/requests/{id}/fulfill", randomId).header("Idempotency-Key", UUID.randomUUID().toString())).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Missing tenant claim is denied with 403 Forbidden across request routes")
    void missingTenantClaimDeniedWith403() throws Exception {
        UUID employeeId = UUID.randomUUID();
        JwtRequestPostProcessor noTenant = jwt().jwt(token -> token
                .subject(employeeId.toString())
                .claim("roles", List.of("EMPLOYEE"))
        ).authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));

        mvc.perform(get("/requests/types").with(noTenant)).andExpect(status().isForbidden());
        mvc.perform(get("/materials").with(noTenant)).andExpect(status().isForbidden());
        mvc.perform(get("/requests/me").with(noTenant)).andExpect(status().isForbidden());
        mvc.perform(get("/requests/inbox").with(noTenant)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cross-tenant boundary isolation: Tenant B cannot view Tenant A requests or catalogue")
    void crossTenantIsolation() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID empA = UUID.randomUUID();
        UUID empB = UUID.randomUUID();

        // Seed request type and material item for Tenant A
        requestTypeRepository.save(RequestType.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(tenantA.toString())
                .code("EQUIPMENT")
                .name("Equipment Request")
                .category("EQUIPMENT")
                .active(true)
                .build());

        materialItemRepository.save(MaterialItem.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(tenantA.toString())
                .code("LAPTOP-PRO")
                .name("Developer Laptop")
                .category("EQUIPMENT")
                .advisoryStock(10)
                .unit("UNIT")
                .active(true)
                .build());

        // Employee A creates a draft request in Tenant A
        String reqBody = """
                {
                  "type": "OTHER",
                  "title": "Office Relocation Needs",
                  "reason": "Moving to level 5",
                  "quantity": 1
                }
                """;

        String createdResponse = mvc.perform(post("/requests").header("Idempotency-Key", UUID.randomUUID().toString()).with(employee(tenantA, empA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(reqBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        // Employee B in Tenant B queries /requests/me -> sees 0 items
        mvc.perform(get("/requests/me").with(employee(tenantB, empB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        // Employee B in Tenant B queries /materials -> sees 0 materials
        mvc.perform(get("/materials").with(employee(tenantB, empB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("Requester isolation: Unrelated employee in same tenant cannot see another employee's request")
    void unrelatedEmployeeInSameTenantCannotSeeRequest() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID empA = UUID.randomUUID();
        UUID empB = UUID.randomUUID();

        String reqBody = """
                {
                  "type": "OTHER",
                  "title": "Private Medical Request",
                  "reason": "Special accommodation",
                  "quantity": 1
                }
                """;

        String createdJson = mvc.perform(post("/requests").header("Idempotency-Key", UUID.randomUUID().toString()).with(employee(tenant, empA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(reqBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String requestId = com.jayway.jsonpath.JsonPath.read(createdJson, "$.id");

        // Employee B (not requester, not approver, not HR) tries to view request detail -> 403 Forbidden
        mvc.perform(get("/requests/{id}", requestId).with(employee(tenant, empB)))
                .andExpect(status().isForbidden());
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
