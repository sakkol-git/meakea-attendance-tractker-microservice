package com.hrflow.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RequestPageTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine").withInitScript("context-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", () -> "request_app");
        p.add("spring.datasource.password", () -> "test-pass");
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate runtime;
    @MockitoBean com.hrflow.request.organization.RequestEmployeeBootstrap replay;

    @Test void pagesCountOnlyOwnRecordsAndApplyStableOwnerFiltersBeforeTheLimit() throws Exception {
        String tenant = id(), employee = id(), other = id();
        var admin = admin();
        for (int index = 0; index < 27; index++) row(admin, tenant, employee, "Own " + index, index);
        row(admin, tenant, other, "Foreign employee", 100);
        row(admin, id(), employee, "Foreign tenant", 100);
        var actor = actor(tenant, employee, "EMPLOYEE");
        mvc.perform(get("/requests/me").with(actor).param("page", "1").param("size", "25").param("sort", "oldest"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(2)).andExpect(jsonPath("$.total").value(27))
                .andExpect(jsonPath("$.page").value(1)).andExpect(jsonPath("$.items[0].title").value("Own 25"));
        mvc.perform(get("/requests/me").with(actor).param("page", "0").param("size", "25").param("search", "%"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/requests/me").with(actor).param("page", "0").param("status", "APPROVED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        for (String page : List.of("-1", "2147483647")) mvc.perform(get("/requests/me").with(actor).param("page", page).param("size", "100"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/requests/me").with(actor)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(27));
        assertThat(runtime.queryForObject("SELECT count(*) FROM request.general_request", Integer.class)).isZero();
    }

    @Test void inboxPagesRequireTheCurrentAssignedStepAndRevokedManagersDisappearBeforeCounting() throws Exception {
        var admin = admin();
        String tenant = id(), employee = id(), manager = id(), request = row(admin, tenant, employee, "Private", 0);
        admin.update("UPDATE request.general_request SET status='PENDING_APPROVAL' WHERE id=?", request);
        admin.update("INSERT INTO request.employee_projection(tenant_id,employee_id,manager_id,active,revision) VALUES (?,?,?,true,1),(?,?,null,true,1)", tenant, employee, manager, tenant, manager);
        admin.update("INSERT INTO request.approval_step(id,tenant_id,request_id,step_order,approver_id,approver_role,status) VALUES (?,?,?,1,?,'MANAGER','PENDING'),(?,?,?,2,null,'HR_ADMIN','PENDING')", id(), tenant, request, manager, id(), tenant, request);
        org.mockito.Mockito.when(replay.isReady()).thenReturn(true);
        mvc.perform(get("/requests/inbox").with(actor(tenant, manager, "MANAGER")).param("page", "0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/requests/inbox").with(actor(tenant, id(), "MANAGER")).param("page", "0"))
                .andExpect(status().isServiceUnavailable());
        mvc.perform(get("/requests/inbox").with(actor(tenant, id(), "HR_ADMIN")).param("page", "0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        admin.update("UPDATE request.employee_projection SET manager_id=? WHERE tenant_id=? AND employee_id=?", id(), tenant, employee);
        mvc.perform(get("/requests/inbox").with(actor(tenant, manager, "MANAGER")).param("page", "0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }

    @Test void explicitCatalogPagesPreserveLegacyArraysAndActiveTenantScope() throws Exception {
        var admin = admin(); String tenant = id(), employee = id();
        for (int index = 0; index < 3; index++) {
            admin.update("INSERT INTO request.request_type(id,tenant_id,code,name,category,active) VALUES (?,?,?,?,'GENERAL',?)", id(), tenant, "T" + index, "Type " + index, index < 2);
            admin.update("INSERT INTO request.material_item(id,tenant_id,code,name,category,active) VALUES (?,?,?,?,'EQUIPMENT',?)", id(), tenant, "M" + index, "Material " + index, index < 2);
        }
        for (String path : List.of("/requests/types", "/materials")) {
            mvc.perform(get(path).with(actor(tenant, employee, "EMPLOYEE")).param("page", "1").param("size", "1"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2)).andExpect(jsonPath("$.items.length()").value(1));
            mvc.perform(get(path).with(actor(tenant, employee, "EMPLOYEE"))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        }
    }

    private String row(JdbcTemplate jdbc, String tenant, String employee, String title, int seconds) {
        String id = id();
        jdbc.update("INSERT INTO request.general_request(id,tenant_id,employee_id,employee_name,type,title,reason,status,quantity,version,created_at,updated_at) VALUES (?,?,?,'Synthetic','OTHER',?,'Private','DRAFT',1,0,?::timestamptz,now())", id, tenant, employee, title, java.time.Instant.parse("2026-10-01T00:00:00Z").plusSeconds(seconds).toString());
        return id;
    }
    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor actor(String tenant, String employee, String role) { return jwt().jwt(token -> token.subject(id()).claim("tenantId", tenant).claim("employeeId", employee).claim("roles", List.of(role))); }
    private static String id() { return UUID.randomUUID().toString(); }
    private JdbcTemplate admin() { return new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())); }
}
