package com.hrflow.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
class AttendanceReferenceTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("context-schema.sql");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.flyway.user", POSTGRES::getUsername);
        properties.add("spring.flyway.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void missingEmployeeOrAssignedShiftWritesNoAttendanceState() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID employee = UUID.randomUUID();
        mvc.perform(post("/attendance/check-in").with(employee(tenant, employee))
                .header("Idempotency-Key", UUID.randomUUID()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("REFERENCE_NOT_READY"));
        UUID shift = AttendanceFixtures.schedule(jdbc, tenant, employee);
        jdbc.update("DELETE FROM attendance.shift_projection WHERE tenant_id = ? AND shift_id = ?", tenant, shift);
        mvc.perform(post("/attendance/check-in").with(employee(tenant, employee))
                .header("Idempotency-Key", UUID.randomUUID()))
                .andExpect(status().isServiceUnavailable());
        for (String table : new String[]{"attendance_session", "attendance_evidence", "attendance_outbox", "command_receipt"}) {
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM attendance." + table
                    + " WHERE tenant_id = ?", Integer.class, tenant));
        }
    }

    @Test
    void dailyStatusUsesOnlyTheEmployeesAssignedShift() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID employee = UUID.randomUUID();
        AttendanceFixtures.schedule(jdbc, tenant, other);
        UUID assigned = AttendanceFixtures.schedule(jdbc, tenant, employee);
        jdbc.update("UPDATE attendance.shift_projection SET start_time='10:30',end_time='19:30',grace_minutes=7 "
                + "WHERE tenant_id=? AND shift_id=?", tenant, assigned);
        mvc.perform(get("/attendance/daily-status/me").with(employee(tenant, employee))
                .param("date", java.time.LocalDate.now(java.time.ZoneId.of("Asia/Phnom_Penh")).toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.shiftStart").value("10:30:00"))
                .andExpect(jsonPath("$.graceMinutes").value(7));
        mvc.perform(post("/attendance/check-in").with(employee(tenant, employee))
                .header("Idempotency-Key", UUID.randomUUID())).andExpect(status().isCreated());
        var captured = jdbc.queryForObject("SELECT scheduled_start FROM attendance.attendance_session WHERE tenant_id=?",
                java.sql.Timestamp.class, tenant);
        jdbc.update("UPDATE attendance.shift_projection SET start_time='11:30',end_time='20:30' "
                + "WHERE tenant_id=? AND shift_id=?", tenant, assigned);
        mvc.perform(post("/attendance/check-out").with(employee(tenant, employee))
                .header("Idempotency-Key", UUID.randomUUID()).header("If-Match", "\"0\""))
                .andExpect(status().isOk());
        assertEquals(captured, jdbc.queryForObject(
                "SELECT scheduled_start FROM attendance.attendance_session WHERE tenant_id=?", java.sql.Timestamp.class, tenant));
    }

    @Test
    void databaseRejectsPartialScheduleBasis() {
        assertThrows(org.springframework.dao.DataAccessException.class, () -> jdbc.update("""
                INSERT INTO attendance.attendance_session
                    (id,tenant_id,employee_id,clock_in,work_date,timezone,shift_id,
                     scheduled_start,scheduled_end,grace_minutes)
                VALUES (?,?,?,now(),current_date,'UTC',?,now(),now()+interval '8 hours',10)
                """, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
    }

    private static JwtRequestPostProcessor employee(UUID tenant, UUID employee) {
        return jwt().jwt(token -> token.subject(employee.toString()).claim("employeeId", employee.toString())
                .claim("tenantId", tenant.toString())).authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
    }
}
