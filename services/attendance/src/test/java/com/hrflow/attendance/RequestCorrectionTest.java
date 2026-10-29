package com.hrflow.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@AutoConfigureMockMvc
class RequestCorrectionTest {

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

    @Test
    void requestCorrectionPreservesRawEvidenceAndEnforcesIntegrity() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID employeeA = UUID.randomUUID();
        UUID employeeB = UUID.randomUUID();

        // Seed an attendance session with raw evidence for employeeA
        UUID evidenceId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant rawClockIn = Instant.parse("2026-11-16T08:30:00Z");
        Instant rawClockOut = Instant.parse("2026-11-16T17:00:00Z");

        jdbc.update("""
                INSERT INTO attendance.attendance_evidence
                    (id, tenant_id, employee_id, client_observation_id, recorded_at, latitude, longitude, accuracy_meters, device_model)
                VALUES (?, ?, ?, ?, ?, 11.56, 104.92, 10.0, 'Pixel 8')
                """, evidenceId, tenantA, employeeA, UUID.randomUUID(), Timestamp.from(rawClockIn));

        jdbc.update("""
                INSERT INTO attendance.attendance_session
                    (id, tenant_id, employee_id, clock_in, clock_out, status, version, in_evidence_id)
                VALUES (?, ?, ?, ?, ?, 'COMPLETED', 0, ?)
                """, sessionId, tenantA, employeeA, Timestamp.from(rawClockIn), Timestamp.from(rawClockOut), evidenceId);

        // 1. Invalid payload: proposed clock-out precedes proposed clock-in -> 422
        String invertedBody = """
                {
                    "sessionId": "%s",
                    "date": "2026-11-16",
                    "proposedClockIn": "2026-11-16T09:00:00Z",
                    "proposedClockOut": "2026-11-16T08:00:00Z",
                    "reason": "Typo in clock out"
                }
                """.formatted(sessionId);

        mvc.perform(post("/attendance/corrections").with(employee(tenantA, employeeA))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content(invertedBody))
                .andExpect(status().isUnprocessableEntity());

        // 2. Foreign session ID (belonging to another tenant/employee) -> 404
        String foreignSessionBody = """
                {
                    "sessionId": "%s",
                    "date": "2026-11-16",
                    "proposedClockIn": "2026-11-16T09:00:00Z",
                    "proposedClockOut": "2026-11-16T18:00:00Z",
                    "reason": "Targeting other employee session"
                }
                """.formatted(sessionId);

        mvc.perform(post("/attendance/corrections").with(employee(tenantB, employeeB))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content(foreignSessionBody))
                .andExpect(status().isNotFound());

        // 3. Valid correction request by owner -> 201 Created with ETag "0"
        String validBody = """
                {
                    "sessionId": "%s",
                    "date": "2026-11-16",
                    "proposedClockIn": "2026-11-16T09:00:00Z",
                    "proposedClockOut": "2026-11-16T18:00:00Z",
                    "reason": "Forgot badge in morning, security manually let me in"
                }
                """.formatted(sessionId);

        String response = mvc.perform(post("/attendance/corrections").with(employee(tenantA, employeeA))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content(validBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.employeeId").value(employeeA.toString()))
                .andExpect(jsonPath("$.reason").value("Forgot badge in morning, security manually let me in"))
                .andReturn().getResponse().getContentAsString();

        // 4. Verify Raw Evidence is UNCHANGED in database
        Integer evidenceCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM attendance.attendance_evidence WHERE id = ?",
                Integer.class, evidenceId);
        assertEquals(1, evidenceCount, "Raw evidence must remain intact in database");

        // Raw session times are still original prior to approval
        Timestamp currentSessionClockIn = jdbc.queryForObject(
                "SELECT clock_in FROM attendance.attendance_session WHERE id = ?",
                Timestamp.class, sessionId);
        assertNotNull(currentSessionClockIn);
        assertEquals(rawClockIn, currentSessionClockIn.toInstant());

        // 5. Outbox record created for correction requested
        Integer outboxCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM attendance.attendance_outbox WHERE tenant_id = ? AND topic = 'attendance.correction.v1'",
                Integer.class, tenantA);
        assertEquals(1, outboxCount, "Outbox event must be recorded for correction request");

        // 6. Tenant isolation on listing corrections
        mvc.perform(get("/attendance/corrections").with(employee(tenantA, employeeA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

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
