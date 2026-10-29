package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hrflow.reporting.entity.ExportJob;
import com.hrflow.reporting.repository.ExportJobRepository;
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
class ReportingRouteAndKafkaTest {

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
    ExportJobRepository exportJobRepository;

    @Test
    @DisplayName("GET /reports/dashboard requires authentication and returns 401 when anonymous")
    void dashboardRequiresAuthentication() throws Exception {
        mvc.perform(get("/reports/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /reports/dashboard denies plain employees")
    void dashboardDeniesPlainEmployees() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        mvc.perform(get("/reports/dashboard").with(employee(tenantId, employeeId)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /reports/dashboard returns KPIs for HR Admin")
    void dashboardReturnsKpisForHrAdmin() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        mvc.perform(get("/reports/dashboard").with(hrAdmin(tenantId, adminId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPresentToday").isNumber())
                .andExpect(jsonPath("$.totalLateToday").isNumber())
                .andExpect(jsonPath("$.totalAbsentToday").isNumber());
    }

    @Test
    @DisplayName("GET /reports/attendance/summary denies access to plain employees without manager or admin role")
    void attendanceSummaryRoleCheck() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        mvc.perform(get("/reports/attendance/summary").with(employee(tenantId, employeeId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCannotReadTenantWideReportsWithoutCurrentTeamProjection() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        for (String path : List.of("/reports/dashboard", "/reports/attendance/summary")) {
            mvc.perform(get(path).with(jwt().jwt(token -> token.subject(actor.toString())
                    .claim("tenantId", tenant.toString()).claim("employeeId", actor.toString())
                    .claim("roles", List.of("MANAGER")))
                    .authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
                    .andExpect(status().isServiceUnavailable());
        }
    }

    @Test
    void exportStatusIsPrivateToTheRequestingHrAdmin() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        ExportJob job = exportJobRepository.save(new ExportJob(tenant, owner, "ATTENDANCE"));
        mvc.perform(get("/reports/exports/" + job.getId()).with(hrAdmin(tenant, UUID.randomUUID())))
                .andExpect(status().isNotFound());
        mvc.perform(get("/reports/exports/" + job.getId()).with(hrAdmin(tenant, owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        job.markRunning();
        exportJobRepository.saveAndFlush(job);
        mvc.perform(get("/reports/exports/" + job.getId()).with(hrAdmin(tenant, owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2));
        mvc.perform(get("/reports/exports").with(hrAdmin(tenant, UUID.randomUUID())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalCount").value(0));
        mvc.perform(get("/reports/exports/" + job.getId()).with(jwt().jwt(token -> token
                .subject(owner.toString()).claim("tenantId", tenant.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void malformedIdentityClaimsDoNotBecomeServerErrors() throws Exception {
        mvc.perform(get("/reports/dashboard").with(jwt().jwt(token -> token
                .claim("tenantId", "invalid").claim("roles", List.of("HR_ADMIN")))
                .authorities(new SimpleGrantedAuthority("ROLE_HR_ADMIN"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /reports/attendance/summary succeeds for HR Admin")
    void attendanceSummarySucceedsForAdmin() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        mvc.perform(get("/reports/attendance/summary").with(hrAdmin(tenantId, adminId)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /reports/exports creates an async export job and returns 202 Accepted")
    void createExportJobSucceeds() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        String payload = """
                {
                  "report": "ATTENDANCE",
                  "format": "CSV"
                }
                """;

        mvc.perform(post("/reports/exports").with(hrAdmin(tenantId, adminId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.downloadUrl").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.fileName").doesNotHaveJsonPath());
    }

    @Test
    @DisplayName("GET /reports/exports/{id} returns 404 for cross-tenant access")
    void exportJobTenantIsolation() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID adminA = UUID.randomUUID();
        UUID adminB = UUID.randomUUID();

        ExportJob jobA = exportJobRepository.save(new ExportJob(tenantA, adminA, "ATTENDANCE"));

        // Tenant B trying to access Tenant A's export job should receive 404 (or 403)
        mvc.perform(get("/reports/exports/" + jobA.getId()).with(hrAdmin(tenantB, adminB)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /reports/attendance/summary returns an object DTO and not a raw array")
    void attendanceSummaryReturnsObjectNotArray() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        mvc.perform(get("/reports/attendance/summary").with(hrAdmin(tenantId, adminId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records").isArray())
                .andExpect(jsonPath("$.totalCount").isNumber());
    }

    @Test
    @DisplayName("GET /reports/attendance/summary rejects inverted date range with 400")
    void attendanceSummaryRejectsInvertedDateRange() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        mvc.perform(get("/reports/attendance/summary")
                .param("from", "2026-10-30")
                .param("to", "2026-10-01")
                .with(hrAdmin(tenantId, adminId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /reports/exports returns an object DTO with items and not a raw array")
    void listExportsReturnsObjectNotArray() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        mvc.perform(get("/reports/exports").with(hrAdmin(tenantId, adminId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalCount").isNumber());
    }

    @Test
    @DisplayName("POST /reports/exports rejects invalid report type with 400")
    void createExportRejectsInvalidReportType() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        String payload = """
                {
                  "report": "INVALID_REPORT",
                  "format": "CSV"
                }
                """;

        mvc.perform(post("/reports/exports").with(hrAdmin(tenantId, adminId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /reports/exports uses date range from/to and returns no uncontracted download link")
    void createExportPreservesDateRangeAndReturnsNoUnavailableLink() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        String payload = """
                {
                  "report": "ATTENDANCE",
                  "format": "CSV",
                  "from": "2026-10-01",
                  "to": "2026-10-15"
                }
                """;

        mvc.perform(post("/reports/exports").with(hrAdmin(tenantId, adminId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.downloadUrl").doesNotExist());
    }

    @Test
    @DisplayName("GET /reports/exports/{id}/download hides unknown jobs")
    void unknownDownloadReturns404() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        mvc.perform(get("/reports/exports/" + UUID.randomUUID() + "/download").with(hrAdmin(tenantId, adminId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/reports/dashboard is not mapped directly on reporting service and returns 404")
    void legacyV1PrefixNotMappedDirectlyOnReportingService() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        mvc.perform(get("/api/v1/reports/dashboard").with(hrAdmin(tenantId, adminId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /reports/attendance/summary rejects invalid sort field with 400 Problem response")
    void attendanceSummaryRejectsInvalidSortField() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        mvc.perform(get("/reports/attendance/summary")
                .param("sortBy", "invalidSortField")
                .with(hrAdmin(tenantId, adminId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    @DisplayName("GET /reports/attendance/summary succeeds for allowed sort fields")
    void attendanceSummarySucceedsForAllowedSortFields() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        for (String sort : List.of("workDate", "status", "minutesLate", "minutesWorked")) {
            mvc.perform(get("/reports/attendance/summary")
                    .param("sortBy", sort)
                    .with(hrAdmin(tenantId, adminId)))
                    .andExpect(status().isOk());
        }
    }

    private static JwtRequestPostProcessor employee(UUID tenantId, UUID employeeId) {
        return jwt().jwt(token -> token
                .subject(employeeId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("employeeId", employeeId.toString())
                .claim("roles", List.of("EMPLOYEE"))
        ).authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
    }

    private static JwtRequestPostProcessor hrAdmin(UUID tenantId, UUID adminId) {
        return jwt().jwt(token -> token
                .subject(adminId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("employeeId", adminId.toString())
                .claim("roles", List.of("HR_ADMIN"))
        ).authorities(new SimpleGrantedAuthority("ROLE_HR_ADMIN"));
    }
}
