package com.hrflow.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
class ApproveIndependentScopeTest {

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
    void approvalEnforcesManagerRolePreventsSelfApprovalAndAppliesEffectiveTimes() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID employee = UUID.randomUUID();
        UUID manager = UUID.randomUUID();
        AttendanceFixtures.schedule(jdbc, tenant, employee);
        AttendanceFixtures.reportsTo(jdbc, tenant, employee, manager);

        // 1. Seed raw evidence and session
        UUID evidenceId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant rawClockIn = Instant.parse("2026-11-16T08:30:00Z");
        Instant rawClockOut = Instant.parse("2026-11-16T17:00:00Z");

        jdbc.update("""
                INSERT INTO attendance.attendance_evidence
                    (id, tenant_id, employee_id, client_observation_id, recorded_at, latitude, longitude, accuracy_meters, device_model)
                VALUES (?, ?, ?, ?, ?, 11.56, 104.92, 10.0, 'Pixel 8')
                """, evidenceId, tenant, employee, UUID.randomUUID(), Timestamp.from(rawClockIn));

        jdbc.update("""
                INSERT INTO attendance.attendance_session
                    (id, tenant_id, employee_id, clock_in, clock_out, status, version, in_evidence_id)
                VALUES (?, ?, ?, ?, ?, 'COMPLETED', 0, ?)
                """, sessionId, tenant, employee, Timestamp.from(rawClockIn), Timestamp.from(rawClockOut), evidenceId);

        // 2. Seed a pending correction request
        UUID correctionId = UUID.randomUUID();
        Instant proposedClockIn = Instant.parse("2026-11-16T09:00:00Z");
        Instant proposedClockOut = Instant.parse("2026-11-16T18:00:00Z");

        jdbc.update("""
                INSERT INTO attendance.attendance_correction
                    (id, tenant_id, employee_id, session_id, date, proposed_clock_in, proposed_clock_out, reason, status, version)
                VALUES (?, ?, ?, ?, '2026-11-16', ?, ?, 'Traffic delay and overtime', 'PENDING', 0)
                """, correctionId, tenant, employee, sessionId, Timestamp.from(proposedClockIn), Timestamp.from(proposedClockOut));

        // 3. Employee attempts to self-approve with EMPLOYEE role -> 403 Forbidden
        mvc.perform(post("/attendance/corrections/{id}/approve", correctionId)
                .with(employee(tenant, employee))
                .header("If-Match", "\"0\""))
                .andExpect(status().isForbidden());

        // 4. Employee with elevated role still CANNOT self-approve own correction -> 403 Forbidden
        mvc.perform(post("/attendance/corrections/{id}/approve", correctionId)
                .with(manager(tenant, employee)) // actor is the requester!
                .header("If-Match", "\"0\""))
                .andExpect(status().isForbidden());

        // 5. Manager provides stale If-Match -> 412 Precondition Failed
        mvc.perform(post("/attendance/corrections/{id}/approve", correctionId)
                .with(manager(tenant, manager))
                .header("If-Match", "\"99\""))
                .andExpect(status().isPreconditionFailed());

        // 6. Independent manager approves with correct If-Match -> 200 OK
        mvc.perform(post("/attendance/corrections/{id}/approve", correctionId)
                .with(manager(tenant, manager))
                .header("If-Match", "\"0\""))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1\""))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewedBy").value(manager.toString()))
                .andExpect(jsonPath("$.reviewedAt").isNotEmpty());

        // 7. Verify Effective Session times are updated
        Timestamp updatedClockIn = jdbc.queryForObject(
                "SELECT clock_in FROM attendance.attendance_session WHERE id = ?",
                Timestamp.class, sessionId);
        Timestamp updatedClockOut = jdbc.queryForObject(
                "SELECT clock_out FROM attendance.attendance_session WHERE id = ?",
                Timestamp.class, sessionId);
        assertNotNull(updatedClockIn);
        assertNotNull(updatedClockOut);
        assertEquals(proposedClockIn, updatedClockIn.toInstant(), "Effective clock_in must be updated to proposed time");
        assertEquals(proposedClockOut, updatedClockOut.toInstant(), "Effective clock_out must be updated to proposed time");

        // 8. Verify Raw Evidence is STILL INTACT and untouched
        UUID sessionEvidenceId = jdbc.queryForObject(
                "SELECT in_evidence_id FROM attendance.attendance_session WHERE id = ?",
                UUID.class, sessionId);
        assertEquals(evidenceId, sessionEvidenceId, "Raw evidence link must remain intact");

        Timestamp rawEvidenceTime = jdbc.queryForObject(
                "SELECT recorded_at FROM attendance.attendance_evidence WHERE id = ?",
                Timestamp.class, evidenceId);
        assertNotNull(rawEvidenceTime);
        assertEquals(rawClockIn, rawEvidenceTime.toInstant(), "Raw evidence recorded_at must be immutable");

        // 9. Second concurrent decision on already approved correction -> 409 Conflict
        mvc.perform(post("/attendance/corrections/{id}/approve", correctionId)
                .with(manager(tenant, manager))
                .header("If-Match", "\"1\""))
                .andExpect(status().isConflict());

        // 10. Outbox event recorded for approved correction
        Integer outboxCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM attendance.attendance_outbox WHERE tenant_id = ? AND aggregate_id = ?",
                Integer.class, tenant, correctionId);
        assertEquals(1, outboxCount, "Approved correction must produce an outbox event");
    }

    @Test
    void unrelatedManagerAndSelfWithDifferentAccountIdCannotDecideCorrections() throws Exception {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID(), assigned = UUID.randomUUID();
        AttendanceFixtures.schedule(jdbc, tenant, employee);
        UUID correction = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO attendance.attendance_correction
                  (id,tenant_id,employee_id,date,reason,status,version)
                VALUES (?,?,?,'2026-11-16','Synthetic correction','PENDING',0)
                """, correction, tenant, employee);
        mvc.perform(post("/attendance/corrections/{id}/approve", correction)
                .with(manager(tenant, assigned)).header("If-Match", "\"0\""))
                .andExpect(status().isServiceUnavailable());
        var selfWithAccountId = jwt().jwt(token -> token.subject(UUID.randomUUID().toString())
                .claim("tenantId", tenant.toString()).claim("employeeId", employee.toString())
                .claim("roles", java.util.List.of("HR_ADMIN")))
                .authorities(new SimpleGrantedAuthority("ROLE_HR_ADMIN"));
        mvc.perform(post("/attendance/corrections/{id}/approve", correction)
                .with(selfWithAccountId).header("If-Match", "\"0\""))
                .andExpect(status().isForbidden());
        AttendanceFixtures.reportsTo(jdbc, tenant, employee, assigned);
        UUID unrelated = UUID.randomUUID();
        mvc.perform(post("/attendance/corrections/{id}/reject", correction)
                .with(manager(tenant, unrelated)).header("If-Match", "\"0\""))
                .andExpect(status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/attendance/corrections")
                .with(manager(tenant, unrelated))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/attendance/corrections")
                .with(manager(tenant, assigned))).andExpect(jsonPath("$.total").value(1));
        assertEquals("PENDING", jdbc.queryForObject("SELECT status FROM attendance.attendance_correction WHERE id=?",
                String.class, correction));
        mvc.perform(post("/attendance/corrections/{id}/reject", correction)
                .with(manager(tenant, assigned)).header("If-Match", "\"0\""))
                .andExpect(status().isOk());
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
