package com.hrflow.identityorg.organization;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
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
@SpringBootTest(properties = {
        "hrflow.auth.local-bootstrap=true",
        "hrflow.auth.dev-password=synthetic-local-password",
        "spring.kafka.listener.auto-startup=false"
})
@AutoConfigureMockMvc
class SchedulePersistenceTest {
    private static final Path KEY_DIRECTORY = temporaryKeys();

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("identity-init.sql");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        properties.add("spring.flyway.user", POSTGRES::getUsername);
        properties.add("spring.flyway.password", POSTGRES::getPassword);
        properties.add("hrflow.auth.key-directory", KEY_DIRECTORY::toString);
    }

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MockMvc mvc;

    @Test
    void siteAndShiftHttpEnforceTimezoneRevisionReplayAndTenantBoundary() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        jdbc.update("INSERT INTO identity_org.auth_tenant(id, slug) VALUES (?, ?)", tenantA, "site-a-" + tenantA);
        jdbc.update("INSERT INTO identity_org.auth_tenant(id, slug) VALUES (?, ?)", tenantB, "site-b-" + tenantB);
        String site = """
                {"name":"Main Office","latitude":11.56,"longitude":104.92,
                 "radiusM":100,"timezone":"Asia/Phnom_Penh"}
                """;
        String key = UUID.randomUUID().toString();
        mvc.perform(post("/work-sites").with(owner(tenantA)).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(site))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.timezone").value("Asia/Phnom_Penh"));
        mvc.perform(post("/work-sites").with(owner(tenantA)).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(site))
                .andExpect(status().isCreated());
        UUID siteId = jdbc.queryForObject("""
                SELECT id FROM identity_org.work_site WHERE tenant_id = ? AND name = 'Main Office'
                """, UUID.class, tenantA);
        mvc.perform(get("/work-sites").with(owner(tenantB)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        String shift = """
                {"name":"Day Shift","startTime":"08:00","endTime":"17:00",
                 "graceMinutes":10,"workingDays":31,"siteId":"%s","timezone":"Asia/Phnom_Penh"}
                """.formatted(siteId);
        mvc.perform(post("/shifts").with(owner(tenantA)).header("Idempotency-Key", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content(shift))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.workingDays").value(31));
        mvc.perform(post("/shifts").with(owner(tenantA)).header("Idempotency-Key", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content(shift.replace("17:00", "06:00")))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/shifts").with(owner(tenantB)).header("Idempotency-Key", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content(shift))
                .andExpect(status().isUnprocessableEntity());
        UUID shiftId = jdbc.queryForObject("""
                SELECT id FROM identity_org.shift WHERE tenant_id = ? AND name = 'Day Shift'
                """, UUID.class, tenantA);
        mvc.perform(patch("/shifts/{id}", shiftId).with(owner(tenantA))
                .header("If-Match", "\"99\"").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Revised\"}"))
                .andExpect(status().isPreconditionFailed());
        mvc.perform(patch("/shifts/{id}", shiftId).with(owner(tenantA))
                .header("If-Match", "\"0\"").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Revised\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        assertEquals(java.util.List.of(1L, 2L), jdbc.queryForList("""
                SELECT aggregate_revision FROM identity_org.organization_outbox
                WHERE tenant_id = ? AND topic = 'org.shift.v1' AND aggregate_id = ?
                ORDER BY aggregate_revision
                """, Long.class, tenantA, shiftId));
        mvc.perform(patch("/work-sites/{id}", siteId).with(owner(tenantA))
                .header("If-Match", "\"0\"").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Revised Office\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/work-sites").with(owner(tenantA)).header("Idempotency-Key", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content(site.replace("Main Office", "Spare Office")))
                .andExpect(status().isCreated());
        UUID spare = jdbc.queryForObject("SELECT id FROM identity_org.work_site WHERE tenant_id = ? AND name = 'Spare Office'",
                UUID.class, tenantA);
        mvc.perform(delete("/work-sites/{id}", spare).with(owner(tenantA)).header("If-Match", "\"0\""))
                .andExpect(status().isNoContent());
        mvc.perform(get("/audit-logs").with(owner(tenantA)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(6));
    }

    @Test
    void assignmentsAndHolidaysRejectOverlapAndForeignReaders() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID foreign = UUID.randomUUID();
        jdbc.update("INSERT INTO identity_org.auth_tenant(id, slug) VALUES (?, ?)", tenant, "flow-a-" + tenant);
        jdbc.update("INSERT INTO identity_org.auth_tenant(id, slug) VALUES (?, ?)", foreign, "flow-b-" + foreign);
        UUID department = UUID.randomUUID();
        UUID position = UUID.randomUUID();
        UUID employee = UUID.randomUUID();
        UUID site = UUID.randomUUID();
        UUID shift = UUID.randomUUID();
        jdbc.update("INSERT INTO identity_org.department(id, tenant_id, code, name) VALUES (?, ?, 'D', 'Dept')", department, tenant);
        jdbc.update("INSERT INTO identity_org.position(id, tenant_id, code, name) VALUES (?, ?, 'P', 'Position')", position, tenant);
        jdbc.update("""
                INSERT INTO identity_org.employee(id, tenant_id, employee_code, name, email, department_id, position_id)
                VALUES (?, ?, 'E', 'Employee', 'shift@example.test', ?, ?)
                """, employee, tenant, department, position);
        jdbc.update("""
                INSERT INTO identity_org.work_site(id, tenant_id, name, latitude, longitude, radius_m, timezone)
                VALUES (?, ?, 'Main', 11.56, 104.92, 100, 'Asia/Phnom_Penh')
                """, site, tenant);
        jdbc.update("""
                INSERT INTO identity_org.shift(id, tenant_id, name, start_time, end_time,
                    grace_minutes, working_days, site_id, timezone)
                VALUES (?, ?, 'Day', '08:00', '17:00', 10, 31, ?, 'Asia/Phnom_Penh')
                """, shift, tenant, site);
        String body = """
                {"shiftId":"%s","validFrom":"2026-11-01","validTo":null}
                """.formatted(shift);
        String key = UUID.randomUUID().toString();
        mvc.perform(post("/employees/{id}/shifts", employee).with(owner(tenant))
                .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.shiftSnapshot.site.name").value("Main"));
        mvc.perform(post("/employees/{id}/shifts", employee).with(owner(tenant))
                .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/employees/{id}/shifts", employee).with(owner(tenant))
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        UUID assignment = jdbc.queryForObject("""
                SELECT id FROM identity_org.employee_shift WHERE tenant_id = ? AND employee_id = ?
                """, UUID.class, tenant, employee);
        mvc.perform(get("/employees/{id}/shifts", employee).with(employee(tenant, employee)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/employees/{id}/shifts", employee).with(employee(foreign, UUID.randomUUID())))
                .andExpect(status().isForbidden());
        mvc.perform(post("/employees/{id}/shifts", employee).with(employee(tenant, employee))
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(get("/shift-assignments").with(owner(foreign)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(post("/shift-assignments/{id}/close", assignment).with(owner(tenant))
                .header("If-Match", "\"99\"").contentType(MediaType.APPLICATION_JSON)
                .content("{\"validTo\":\"2026-11-09\"}"))
                .andExpect(status().isPreconditionFailed());
        mvc.perform(post("/shift-assignments/{id}/close", assignment).with(owner(tenant))
                .header("If-Match", "\"0\"").contentType(MediaType.APPLICATION_JSON)
                .content("{\"validTo\":\"2026-11-09\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        mvc.perform(post("/employees/{id}/shifts", employee).with(owner(tenant))
                .header("Idempotency-Key", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("2026-11-01", "2026-11-10")))
                .andExpect(status().isCreated());
        mvc.perform(post("/holidays").with(owner(tenant)).header("Idempotency-Key", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Company Day\",\"date\":\"2026-11-09\",\"kind\":\"COMPANY\"}"))
                .andExpect(status().isCreated());
        UUID holiday = jdbc.queryForObject("""
                SELECT id FROM identity_org.holiday WHERE tenant_id = ? AND date = DATE '2026-11-09'
                """, UUID.class, tenant);
        mvc.perform(patch("/holidays/{id}", holiday).with(owner(tenant))
                .header("If-Match", "\"99\"").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Revised Company Day\"}"))
                .andExpect(status().isPreconditionFailed());
        mvc.perform(patch("/holidays/{id}", holiday).with(owner(tenant))
                .header("If-Match", "\"0\"").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Revised Company Day\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        assertEquals(java.util.List.of(1L, 2L), jdbc.queryForList("""
                SELECT aggregate_revision FROM identity_org.organization_outbox
                WHERE tenant_id = ? AND topic = 'org.holiday.v1' AND aggregate_id = ?
                ORDER BY aggregate_revision
                """, Long.class, tenant, holiday));
        assertEquals(java.util.List.of(1L, 2L, 3L), jdbc.queryForList("""
                SELECT aggregate_revision FROM identity_org.organization_outbox
                WHERE tenant_id = ? AND topic = 'org.employee.v1' AND aggregate_id = ?
                ORDER BY aggregate_revision
                """, Long.class, tenant, employee));
        mvc.perform(get("/holidays").with(employee(foreign, UUID.randomUUID())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(delete("/holidays/{id}", holiday).with(owner(tenant)).header("If-Match", "\"1\""))
                .andExpect(status().isNoContent());
        mvc.perform(get("/audit-logs").with(owner(tenant)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(6));
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor
    owner(UUID tenantId) {
        return jwt().jwt(token -> token.subject("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa")
                        .claim("tenantId", tenantId.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_OWNER"));
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor
    employee(UUID tenantId, UUID employeeId) {
        return jwt().jwt(token -> token.subject("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb")
                        .claim("tenantId", tenantId.toString()).claim("employeeId", employeeId.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
    }

    @Test
    void siteAndShiftConstraintsRejectInvalidCoordinatesAndOvernightWindows() {
        UUID tenant = UUID.randomUUID();
        UUID site = UUID.randomUUID();
        jdbc.update("INSERT INTO identity_org.auth_tenant(id, slug) VALUES (?, ?)", tenant, "schedule-" + tenant);
        jdbc.update("""
                INSERT INTO identity_org.work_site(id, tenant_id, name, latitude, longitude, radius_m, timezone)
                VALUES (?, ?, 'Main', 11.56, 104.92, 100, 'Asia/Phnom_Penh')
                """, site, tenant);
        assertThrows(Exception.class, () -> jdbc.update("""
                INSERT INTO identity_org.work_site(id, tenant_id, name, latitude, longitude, radius_m, timezone)
                VALUES (?, ?, 'Invalid', 91, 104.92, 100, 'Asia/Phnom_Penh')
                """, UUID.randomUUID(), tenant));
        assertThrows(Exception.class, () -> jdbc.update("""
                INSERT INTO identity_org.shift(id, tenant_id, name, start_time, end_time,
                    grace_minutes, working_days, site_id, timezone)
                VALUES (?, ?, 'Night', '22:00', '06:00', 10, 31, ?, 'Asia/Phnom_Penh')
                """, UUID.randomUUID(), tenant, site));
        assertThrows(Exception.class, () -> jdbc.update("""
                INSERT INTO identity_org.shift(id, tenant_id, name, start_time, end_time,
                    grace_minutes, working_days, site_id, timezone)
                VALUES (?, ?, 'Bad mask', '08:00', '17:00', 10, 0, ?, 'Asia/Phnom_Penh')
                """, UUID.randomUUID(), tenant, site));
    }

    @Test
    void assignmentAndHolidayTablesEnforceTenantReferencesAndCalendarUniqueness() {
        UUID tenant = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        jdbc.update("INSERT INTO identity_org.auth_tenant(id, slug) VALUES (?, ?)", tenant, "period-a-" + tenant);
        jdbc.update("INSERT INTO identity_org.auth_tenant(id, slug) VALUES (?, ?)", other, "period-b-" + other);
        UUID department = UUID.randomUUID();
        UUID position = UUID.randomUUID();
        UUID employee = UUID.randomUUID();
        UUID site = UUID.randomUUID();
        UUID shift = UUID.randomUUID();
        jdbc.update("INSERT INTO identity_org.department(id, tenant_id, code, name) VALUES (?, ?, 'D', 'Dept')", department, tenant);
        jdbc.update("INSERT INTO identity_org.position(id, tenant_id, code, name) VALUES (?, ?, 'P', 'Position')", position, tenant);
        jdbc.update("""
                INSERT INTO identity_org.employee(id, tenant_id, employee_code, name, email, department_id, position_id)
                VALUES (?, ?, 'E', 'Employee', 'employee@example.test', ?, ?)
                """, employee, tenant, department, position);
        jdbc.update("""
                INSERT INTO identity_org.work_site(id, tenant_id, name, latitude, longitude, radius_m, timezone)
                VALUES (?, ?, 'Other site', 11.56, 104.92, 100, 'Asia/Phnom_Penh')
                """, site, other);
        jdbc.update("""
                INSERT INTO identity_org.shift(id, tenant_id, name, start_time, end_time,
                    grace_minutes, working_days, site_id, timezone)
                VALUES (?, ?, 'Other shift', '08:00', '17:00', 10, 31, ?, 'Asia/Phnom_Penh')
                """, shift, other, site);
        UUID ownSite = UUID.randomUUID();
        UUID ownShift = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO identity_org.work_site(id, tenant_id, name, latitude, longitude, radius_m, timezone)
                VALUES (?, ?, 'Own site', 11.56, 104.92, 100, 'Asia/Phnom_Penh')
                """, ownSite, tenant);
        jdbc.update("""
                INSERT INTO identity_org.shift(id, tenant_id, name, start_time, end_time,
                    grace_minutes, working_days, site_id, timezone)
                VALUES (?, ?, 'Own shift', '08:00', '17:00', 10, 31, ?, 'Asia/Phnom_Penh')
                """, ownShift, tenant, ownSite);
        String insert = """
                INSERT INTO identity_org.employee_shift
                    (id, tenant_id, employee_id, shift_id, valid_from, shift_snapshot)
                VALUES (?, ?, ?, ?, DATE '2026-11-01', '{}'::jsonb)
                """;
        jdbc.update(insert, UUID.randomUUID(), tenant, employee, ownShift);
        assertThrows(Exception.class, () -> jdbc.update("""
                INSERT INTO identity_org.employee_shift
                    (id, tenant_id, employee_id, shift_id, valid_from, shift_snapshot)
                VALUES (?, ?, ?, ?, DATE '2026-11-02', '{}'::jsonb)
                """, UUID.randomUUID(), tenant, employee, ownShift));
        assertThrows(Exception.class, () -> jdbc.update(insert, UUID.randomUUID(), tenant, employee, shift));
        jdbc.update("""
                INSERT INTO identity_org.holiday(id, tenant_id, date, name, kind)
                VALUES (?, ?, DATE '2026-11-09', 'Company Day', 'COMPANY')
                """, UUID.randomUUID(), tenant);
        assertThrows(Exception.class, () -> jdbc.update("""
                INSERT INTO identity_org.holiday(id, tenant_id, date, name, kind)
                VALUES (?, ?, DATE '2026-11-09', 'Duplicate Day', 'COMPANY')
                """, UUID.randomUUID(), tenant));
    }

    private static Path temporaryKeys() {
        try {
            return Files.createTempDirectory("hrflow-schedule-keys-");
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }
}
