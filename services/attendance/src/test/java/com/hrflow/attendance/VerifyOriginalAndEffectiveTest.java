package com.hrflow.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
class VerifyOriginalAndEffectiveTest {

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
    void originalRawTimesRemainVisibleAfterApprovedEffectiveCorrection() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID employee = UUID.randomUUID();
        UUID manager = UUID.randomUUID();
        AttendanceFixtures.schedule(jdbc, tenant, employee);
        AttendanceFixtures.reportsTo(jdbc, tenant, employee, manager);

        // 1. Employee checks in via standard flow
        String checkInBody = """
                {"latitude":11.562,"longitude":104.921,"accuracyMeters":12.5,"deviceModel":"iPhone 15"}
                """;
        String inResponse = mvc.perform(post("/attendance/check-in").with(employee(tenant, employee))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content(checkInBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String sessionIdStr = JsonPath.read(inResponse, "$.id");
        UUID sessionId = UUID.fromString(sessionIdStr);
        String initialClockIn = JsonPath.read(inResponse, "$.clockIn");

        // 2. Employee checks out
        String checkOutBody = """
                {"latitude":11.563,"longitude":104.922,"accuracyMeters":10.0,"deviceModel":"iPhone 15"}
                """;
        mvc.perform(post("/attendance/check-out").with(employee(tenant, employee))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("If-Match", "\"0\"")
                .contentType(MediaType.APPLICATION_JSON).content(checkOutBody))
                .andExpect(status().isOk());

        // 3. Capture raw evidence timestamps before correction
        Timestamp rawRecordedAt = jdbc.queryForObject(
                "SELECT recorded_at FROM attendance.attendance_evidence e JOIN attendance.attendance_session s ON e.id = s.in_evidence_id WHERE s.id = ?",
                Timestamp.class, sessionId);
        assertNotNull(rawRecordedAt);
        String originalBasis = jdbc.queryForObject("SELECT original_schedule::text FROM attendance.attendance_session WHERE id=?",
                String.class, sessionId);

        // 4. Employee proposes correction (e.g. badge error, shift was actually 09:00 - 18:00)
        String correctionBody = """
                {
                    "sessionId": "%s",
                    "date": "2026-11-16",
                    "proposedClockIn": "2026-11-16T09:00:00Z",
                    "proposedClockOut": "2026-11-16T18:00:00Z",
                    "reason": "Scanner misread badge at gate"
                }
                """.formatted(sessionId);

        String correctionResp = mvc.perform(post("/attendance/corrections").with(employee(tenant, employee))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content(correctionBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String correctionIdStr = JsonPath.read(correctionResp, "$.id");
        UUID correctionId = UUID.fromString(correctionIdStr);

        // 5. Manager approves correction
        mvc.perform(post("/attendance/corrections/{id}/approve", correctionId)
                .with(manager(tenant, manager))
                .header("If-Match", "\"0\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // 6. Assert Effective session times reflect the approved correction
        mvc.perform(get("/attendance/me").with(employee(tenant, employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].clockIn").value("2026-11-16T09:00:00Z"))
                .andExpect(jsonPath("$.items[0].clockOut").value("2026-11-16T18:00:00Z"));

        // 7. Assert Raw Evidence remains intact, immutable, and retains original telemetry
        Timestamp rawAfterApproval = jdbc.queryForObject(
                "SELECT recorded_at FROM attendance.attendance_evidence e JOIN attendance.attendance_session s ON e.id = s.in_evidence_id WHERE s.id = ?",
                Timestamp.class, sessionId);
        assertEquals(rawRecordedAt, rawAfterApproval, "Original evidence timestamp must remain immutable");

        Double latitude = jdbc.queryForObject(
                "SELECT latitude FROM attendance.attendance_evidence e JOIN attendance.attendance_session s ON e.id = s.in_evidence_id WHERE s.id = ?",
                Double.class, sessionId);
        assertEquals(11.562, latitude, 0.001, "Original evidence telemetry must remain preserved");
        assertEquals(originalBasis, jdbc.queryForObject("SELECT original_schedule::text FROM attendance.attendance_session WHERE id=?",
                String.class, sessionId), "Human approval must retain the original captured schedule");
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM attendance.session_revision WHERE tenant_id=? AND session_id=?",
                Integer.class, tenant, sessionId), "Open, completed and manual interpretations must remain append-only");
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataAccessException.class,
                () -> jdbc.update("DELETE FROM attendance.session_revision WHERE session_id=?", sessionId));

        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM attendance.attendance_outbox WHERE tenant_id=? AND aggregate_id=?
                  AND schema_version=1 AND payload->>'source'='MANUAL'
                  AND payload->>'workDate'='2026-11-16' AND payload->>'state'='COMPLETED'
                """, Integer.class, tenant, sessionId), "Approved effective times must converge downstream");

        // 8. Assert correction record maintains audited explanation and decisions
        mvc.perform(get("/attendance/corrections").with(employee(tenant, employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].reason").value("Scanner misread badge at gate"))
                .andExpect(jsonPath("$.items[0].status").value("APPROVED"))
                .andExpect(jsonPath("$.items[0].reviewedBy").value(manager.toString()));
    }

    private static JwtRequestPostProcessor employee(UUID tenantId, UUID employeeId) {
        return jwt().jwt(token -> token
                .subject(employeeId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("employeeId", employeeId.toString())
                .claim("roles", java.util.List.of("EMPLOYEE"))
        ).authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
    }

    private static JwtRequestPostProcessor manager(UUID tenantId, UUID managerId) {
        return jwt().jwt(token -> token
                .subject(managerId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("employeeId", managerId.toString())
                .claim("roles", java.util.List.of("MANAGER"))
        ).authorities(new SimpleGrantedAuthority("ROLE_MANAGER"));
    }
}
