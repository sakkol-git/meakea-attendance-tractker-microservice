package com.hrflow.attendance;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CorrectionReadContractTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine").withInitScript("attendance-wire-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl); p.add("spring.datasource.username", () -> "attendance_runtime");
        p.add("spring.datasource.password", () -> "wire-only"); p.add("spring.flyway.user", POSTGRES::getUsername); p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @Autowired MockMvc mvc;
    @Autowired com.hrflow.attendance.service.AttendanceService attendance;

    @Test void detailReturnsReviewedRevisionOnlyToTheEmployeeCurrentManagerOrHr() throws Exception {
        var admin = admin(); UUID tenant = id(), employee = id(), manager = id();
        AttendanceFixtures.schedule(admin, tenant, employee);
        AttendanceFixtures.reportsTo(admin, tenant, employee, manager);
        UUID correction = row(admin, tenant, employee, 0);
        mvc.perform(get("/attendance/corrections/{id}", correction).with(actor(tenant, employee, "EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.employeeId").value(employee.toString())).andExpect(jsonPath("$.version").value(0));
        mvc.perform(get("/attendance/corrections/{id}", correction).with(actor(tenant, manager, "MANAGER"))).andExpect(status().isOk());
        mvc.perform(get("/attendance/corrections/{id}", correction).with(actor(tenant, id(), "HR_ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/attendance/corrections/{id}", correction).with(actor(tenant, id(), "EMPLOYEE"))).andExpect(status().isForbidden());
        mvc.perform(get("/attendance/corrections/{id}", correction).with(actor(id(), employee, "HR_ADMIN"))).andExpect(status().isNotFound());
        AttendanceFixtures.reportsTo(admin, tenant, employee, id());
        mvc.perform(get("/attendance/corrections/{id}", correction).with(actor(tenant, manager, "MANAGER"))).andExpect(status().isForbidden());
    }

    @Test void listPagesScopeAndFilterBeforeCountAndDoNotReportAnInvalidEmptyPageSize() throws Exception {
        var admin = admin(); UUID tenant = id(), employee = id(), manager = id(), other = id();
        AttendanceFixtures.schedule(admin, tenant, employee);
        AttendanceFixtures.reportsTo(admin, tenant, employee, manager);
        for (int index = 0; index < 27; index++) row(admin, tenant, employee, index);
        row(admin, tenant, other, 100); row(admin, id(), employee, 100);
        var actor = actor(tenant, employee, "EMPLOYEE");
        mvc.perform(get("/attendance/corrections").with(actor).param("page", "1").param("size", "25").param("sort", "oldest"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(27)).andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.page").value(1)).andExpect(jsonPath("$.size").value(25)).andExpect(jsonPath("$.items[0].reason").value("Reason 25"));
        mvc.perform(get("/attendance/corrections").with(actor).param("search", "%"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0)).andExpect(jsonPath("$.size").value(25));
        mvc.perform(get("/attendance/corrections").with(actor).param("status", "APPROVED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/attendance/corrections").with(actor(tenant, manager, "MANAGER")).param("page", "0").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(27)).andExpect(jsonPath("$.items.length()").value(1));
        mvc.perform(get("/attendance/corrections").with(actor).param("page", "-1")).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/attendance/corrections").with(actor).param("size", "101")).andExpect(status().isUnprocessableEntity());
    }

    @Test void aNewCorrectionKeepsTheActualSubmissionTimeSessionsAfterLaterCheckoutAndLegacyStaysUnknown() throws Exception {
        var admin = admin(); UUID tenant = id(), employee = id();
        AttendanceFixtures.schedule(admin, tenant, employee);
        var input = new com.hrflow.attendance.dto.CheckInRequest(null, null, null, null);
        var opened = attendance.checkIn(tenant, employee, employee, id().toString(), input);
        var day = opened.clockIn().atZone(java.time.ZoneId.of("Asia/Phnom_Penh")).toLocalDate();
        String body = "{\"sessionId\":\"" + opened.id() + "\",\"date\":\"" + day + "\",\"proposedClockIn\":\"" + opened.clockIn().minusSeconds(60) + "\",\"reason\":\"Correct original tap\"}";
        var actor = actor(tenant, employee, "EMPLOYEE");
        String result = mvc.perform(post("/attendance/corrections").with(actor).header("Idempotency-Key", id().toString())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.previousSessions[0].id").value(opened.id().toString()))
                .andExpect(jsonPath("$.previousSessions[0].version").value(0)).andReturn().getResponse().getContentAsString();
        String correction = com.jayway.jsonpath.JsonPath.read(result, "$.id");
        org.assertj.core.api.Assertions.assertThat(admin.queryForObject("SELECT schema_version FROM attendance.attendance_outbox WHERE aggregate_id=? AND topic='attendance.correction.v1'", Integer.class, UUID.fromString(correction))).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(admin.queryForObject("SELECT payload::text FROM attendance.attendance_outbox WHERE aggregate_id=? AND topic='attendance.correction.v1'", String.class, UUID.fromString(correction)))
                .doesNotContain("Correct original tap", "previousSessions", "proposedClockIn");
        org.assertj.core.api.Assertions.assertThat(admin.queryForObject("SELECT jsonb_typeof(previous_sessions) FROM attendance.attendance_correction WHERE id=?", String.class, UUID.fromString(correction))).isEqualTo("array");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> admin.update("UPDATE attendance.attendance_correction SET previous_sessions='[]'::jsonb WHERE id=?", UUID.fromString(correction)))
                .isInstanceOf(org.springframework.dao.DataAccessException.class).hasMessageContaining("immutable");
        attendance.checkOut(tenant, employee, employee, id().toString(), "\"0\"", input);
        mvc.perform(get("/attendance/corrections/{id}", correction).with(actor)).andExpect(status().isOk())
                .andExpect(jsonPath("$.previousSessions[0].clockOut").isEmpty()).andExpect(jsonPath("$.previousSessions[0].version").value(0));
        UUID legacy = row(admin, tenant, employee, 1);
        mvc.perform(get("/attendance/corrections/{id}", legacy).with(actor)).andExpect(status().isOk()).andExpect(jsonPath("$.previousSessions").isEmpty());
        String none = "{\"date\":\"2001-01-02\",\"reason\":\"Missing tap\"}";
        mvc.perform(post("/attendance/corrections").with(actor).header("Idempotency-Key", id().toString()).contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(none))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.previousSessions.length()").value(0));
    }

    private UUID row(JdbcTemplate jdbc, UUID tenant, UUID employee, int seconds) {
        UUID id = id(); jdbc.update("INSERT INTO attendance.attendance_correction(id,tenant_id,employee_id,date,reason,status,version,created_at) VALUES (?,?,?,?,'Reason '||?,'PENDING',0,?::timestamptz)", id, tenant, employee, LocalDate.parse("2026-10-28"), Integer.toString(seconds), Instant.parse("2026-10-28T00:00:00Z").plusSeconds(seconds).toString()); return id;
    }
    private static UUID id() { return UUID.randomUUID(); }
    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor actor(UUID tenant, UUID employee, String role) {
        return jwt().jwt(token -> token.subject(id().toString()).claim("tenantId", tenant.toString()).claim("employeeId", employee.toString()).claim("roles", List.of(role)));
    }
    private JdbcTemplate admin() { return new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())); }
}
