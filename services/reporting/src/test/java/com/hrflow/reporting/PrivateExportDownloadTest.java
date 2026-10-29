package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.hrflow.reporting.service.CsvExportService;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PrivateExportDownloadTest {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("context-schema.sql");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired CsvExportService exports;

    @Test
    void completedCsvRequiresSameTenantRequesterAndCurrentHrAdminRole() throws Exception {
        UUID tenant = UUID.randomUUID(), requester = UUID.randomUUID();
        var job = exports.createExportJob(tenant, requester, "ATTENDANCE");
        String route = "/reports/exports/" + job.getId() + "/download";
        var caller = jwt().jwt(j -> j.subject(requester.toString()).claim("tenantId", tenant.toString())
                .claim("roles", List.of("HR_ADMIN"))).authorities(new SimpleGrantedAuthority("ROLE_HR_ADMIN"));

        mvc.perform(get(route).with(caller)).andExpect(status().isConflict());
        exports.executeExport(tenant, job.getId(), List.of("employee,date", "sample,2026-10-28"), false, null);
        mvc.perform(get(route).with(caller)).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"" + job.getId() + ".csv\""))
                .andExpect(content().string("employee,date\nsample,2026-10-28\n"));
        mvc.perform(get(route)).andExpect(status().isUnauthorized());
        mvc.perform(get(route).with(jwt().jwt(j -> j.subject(requester.toString())
                .claim("tenantId", tenant.toString())).authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
                .andExpect(status().isForbidden());
        for (UUID foreignTenant : List.of(tenant, UUID.randomUUID())) {
            mvc.perform(get(route).with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString())
                    .claim("tenantId", foreignTenant.toString())).authorities(new SimpleGrantedAuthority("ROLE_HR_ADMIN"))))
                    .andExpect(status().isNotFound());
        }
        // A missing completed file is visible as unavailable, never an empty successful download.
        Files.delete(exports.getStorageRoot().resolve(job.getId() + ".csv"));
        mvc.perform(get(route).with(caller)).andExpect(status().isServiceUnavailable());
    }

    @Test
    void symbolicLinkCannotExposeAFileOutsidePrivateStorage() throws Exception {
        UUID tenant = UUID.randomUUID(), requester = UUID.randomUUID();
        var job = exports.createExportJob(tenant, requester, "ATTENDANCE");
        exports.executeExport(tenant, job.getId(), List.of("safe"), false, null);
        var file = exports.getStorageRoot().resolve(job.getId() + ".csv");
        var secret = Files.createTempFile("hrflow-download-boundary-", ".txt");
        Files.writeString(secret, "must-not-be-served");
        try {
            Files.delete(file);
            Files.createSymbolicLink(file, secret);
            var response = mvc.perform(get("/reports/exports/" + job.getId() + "/download")
                    .with(jwt().jwt(j -> j.subject(requester.toString()).claim("tenantId", tenant.toString()))
                            .authorities(new SimpleGrantedAuthority("ROLE_HR_ADMIN"))))
                    .andExpect(status().isServiceUnavailable()).andReturn().getResponse();
            assertThat(response.getContentAsString()).doesNotContain("must-not-be-served", secret.toString());
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(secret);
        }
    }
}
