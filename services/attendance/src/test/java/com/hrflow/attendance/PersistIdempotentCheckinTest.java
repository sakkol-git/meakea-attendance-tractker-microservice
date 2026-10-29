package com.hrflow.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@AutoConfigureMockMvc
class PersistIdempotentCheckinTest {

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
    @Autowired JdbcTemplate jdbc;
    @Autowired tools.jackson.databind.json.JsonMapper json;

    @Test
    void checkInAndCheckOutEnforceIdempotencyRevisionAndTenantBoundary() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID employeeA = UUID.randomUUID();
        UUID employeeB = UUID.randomUUID();
        AttendanceFixtures.schedule(jdbc, tenantA, employeeA);
        AttendanceFixtures.schedule(jdbc, tenantB, employeeB);

        String body = """
                {"latitude":11.56,"longitude":104.92,"accuracyMeters":10.0,"deviceModel":"Pixel 8"}
                """;
        String key = UUID.randomUUID().toString();

        // 1. Missing Idempotency-Key on check-in -> 428 Precondition Required
        mvc.perform(post("/attendance/check-in").with(employee(tenantA, employeeA))
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isPreconditionRequired());

        // 2. Successful check-in
        String response = mvc.perform(post("/attendance/check-in").with(employee(tenantA, employeeA))
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.clockIn").isNotEmpty())
                .andExpect(jsonPath("$.clockOut").isEmpty())
                .andExpect(jsonPath("$.version").value(0))
                .andReturn().getResponse().getContentAsString();

        String eventPayload = jdbc.queryForObject(
                "SELECT payload::text FROM attendance.attendance_outbox WHERE tenant_id = ?",
                String.class, tenantA);
        assertTrue(json.readTree(eventPayload).has("workDate"),
                "Committed session intent must carry the captured work date, not only the HTTP response DTO");
        assertTrue(json.readTree(eventPayload).has("scheduledStart"),
                "Committed session intent must carry its original schedule basis");

        // 3. Replay with same key and body -> 201 Created with exact same content
        mvc.perform(post("/attendance/check-in").with(employee(tenantA, employeeA))
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.version").value(0));

        // 4. Same key with changed body -> 409 Conflict
        mvc.perform(post("/attendance/check-in").with(employee(tenantA, employeeA))
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(body.replace("Pixel 8", "Galaxy S24")))
                .andExpect(status().isConflict());

        // 5. Different key while session is already open -> 409 Conflict
        mvc.perform(post("/attendance/check-in").with(employee(tenantA, employeeA))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());

        // 6. Check today and my sessions for tenantA employeeA
        mvc.perform(get("/attendance/today").with(employee(tenantA, employeeA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.sessions").isArray());

        mvc.perform(get("/attendance/me").with(employee(tenantA, employeeA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

        // 7. TenantB employeeB sees 0 sessions (tenant boundary isolation)
        mvc.perform(get("/attendance/me").with(employee(tenantB, employeeB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));

        mvc.perform(get("/attendance/today").with(employee(tenantB, employeeB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessions").isEmpty());

        // 8. TenantB employee attempts check-out before check-in -> 422 Unprocessable Entity
        mvc.perform(post("/attendance/check-out").with(employee(tenantB, employeeB))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("If-Match", "\"0\"")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());

        // 9. TenantA employee check-out with missing If-Match -> 428 Precondition Required
        String outKey = UUID.randomUUID().toString();
        mvc.perform(post("/attendance/check-out").with(employee(tenantA, employeeA))
                .header("Idempotency-Key", outKey)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isPreconditionRequired());

        // 10. TenantA employee check-out with stale If-Match -> 412 Precondition Failed
        mvc.perform(post("/attendance/check-out").with(employee(tenantA, employeeA))
                .header("Idempotency-Key", outKey)
                .header("If-Match", "\"99\"")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isPreconditionFailed());

        // 11. TenantA employee check-out with matching If-Match -> 200 OK
        mvc.perform(post("/attendance/check-out").with(employee(tenantA, employeeA))
                .header("Idempotency-Key", outKey)
                .header("If-Match", "\"0\"")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1\""))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.clockOut").isNotEmpty())
                .andExpect(jsonPath("$.version").value(1));

        // 12. Replay check-out with same key and body -> 200 OK
        mvc.perform(post("/attendance/check-out").with(employee(tenantA, employeeA))
                .header("Idempotency-Key", outKey)
                .header("If-Match", "\"0\"")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // 13. Verify outbox has revision 1 (created) and revision 2 (closed)
        assertEquals(2, jdbc.queryForObject("""
                SELECT count(*) FROM attendance.attendance_outbox WHERE tenant_id = ?
                """, Integer.class, tenantA));
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor
    employee(UUID tenantId, UUID employeeId) {
        return jwt().jwt(token -> token.subject(employeeId.toString())
                        .claim("tenantId", tenantId.toString())
                        .claim("employeeId", employeeId.toString())
                        .claim("roles", java.util.List.of("EMPLOYEE")))
                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
    }
}
