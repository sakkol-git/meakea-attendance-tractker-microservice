package com.hrflow.identityorg.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.hrflow.identityorg.organization.dto.NamedCreateRequest;
import com.hrflow.identityorg.organization.service.CatalogService;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(properties = {"hrflow.auth.local-bootstrap=true",
        "hrflow.auth.dev-password=synthetic-local-password", "spring.kafka.listener.auto-startup=false"})
@AutoConfigureMockMvc
class IdentityAuditTest {
    private static final Path KEYS = keys();

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("identity-init.sql");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        properties.add("spring.flyway.user", POSTGRES::getUsername);
        properties.add("spring.flyway.password", POSTGRES::getPassword);
        properties.add("hrflow.auth.key-directory", KEYS::toString);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired CatalogService catalogs;
    @Autowired PlatformTransactionManager transactions;

    @Test
    void ownerCanReadAnEmptyTenantAudit() throws Exception {
        mvc.perform(get("/audit-logs").with(actor(UUID.randomUUID(), UUID.randomUUID(), "OWNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void catalogCreateAndReplayProduceOneTenantScopedAuditEntry() throws Exception {
        UUID tenant = tenant();
        UUID actor = UUID.randomUUID();
        UUID foreign = tenant();
        String key = UUID.randomUUID().toString();
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(post("/departments").with(actor(tenant, actor, "OWNER"))
                    .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"People\",\"code\":\"PEOPLE\"}"))
                    .andExpect(status().isCreated());
        }
        mvc.perform(get("/audit-logs").with(actor(tenant, actor, "OWNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].actorId").value(actor.toString()))
                .andExpect(jsonPath("$.items[0].action").value("DEPARTMENT_CREATED"))
                .andExpect(jsonPath("$.items[0].entity").value("department"))
                .andExpect(jsonPath("$.items[0].diff.name.after").value("People"));
        mvc.perform(get("/audit-logs").with(actor(foreign, actor, "OWNER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/audit-logs").with(actor(tenant, actor, "OWNER"))
                .param("actor", actor.toString()).param("action", "department_created")
                .param("entity", "department").param("actorId", actor.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/audit-logs").with(actor(tenant, actor, "OWNER")).param("target", "%"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM identity_org.audit_log WHERE tenant_id = ?", Integer.class, tenant));
    }

    @Test
    void employeeChangesRecordActorAndDiffAndStaleWritesRecordNothing() throws Exception {
        UUID tenant = tenant();
        UUID actor = UUID.randomUUID();
        UUID department = UUID.randomUUID();
        UUID position = UUID.randomUUID();
        jdbc.update("INSERT INTO identity_org.department(id,tenant_id,code,name) VALUES (?,?,'D','People')",
                department, tenant);
        jdbc.update("INSERT INTO identity_org.position(id,tenant_id,code,name) VALUES (?,?,'P','Engineer')",
                position, tenant);
        String key = UUID.randomUUID().toString();
        String body = """
                {"employeeNo":"E-1","name":"Before Name","email":"employee@example.test",
                 "departmentId":"%s","positionId":"%s"}
                """.formatted(department, position);
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(post("/employees").with(actor(tenant, actor, "OWNER"))
                    .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated());
        }
        UUID employee = jdbc.queryForObject("SELECT id FROM identity_org.employee WHERE tenant_id = ?",
                UUID.class, tenant);
        mvc.perform(patch("/employees/" + employee).with(actor(tenant, actor, "OWNER"))
                .header("If-Match", "\"0\"").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"After Name\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/employees/" + employee).with(actor(tenant, actor, "OWNER"))
                .header("If-Match", "\"0\"").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Stale Name\"}"))
                .andExpect(status().isPreconditionFailed());
        mvc.perform(get("/audit-logs").with(actor(tenant, actor, "OWNER")).param("entity", "employee"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].actorId").value(actor.toString()))
                .andExpect(jsonPath("$.items[0].action").value("EMPLOYEE_UPDATED"))
                .andExpect(jsonPath("$.items[0].diff.name.before").value("Before Name"))
                .andExpect(jsonPath("$.items[0].diff.name.after").value("After Name"))
                .andExpect(jsonPath("$.items[0].diff.email").doesNotHaveJsonPath());
    }

    @Test
    void auditAndCommandRollBackTogether() {
        UUID tenant = tenant();
        UUID actor = UUID.randomUUID();
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactions).execute(status -> {
            catalogs.createPosition(tenant, actor, UUID.randomUUID().toString(),
                    new NamedCreateRequest("Temporary", "TMP"));
            throw new IllegalStateException("synthetic failure after the owner mutation");
        }));
        for (String table : new String[]{"position", "command_receipt", "audit_log"}) {
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM identity_org." + table
                    + " WHERE tenant_id = ?", Integer.class, tenant));
        }
    }

    @Test
    void filtersApplyBeforeStablePaginationAndIncludeTheWholeUtcEndDate() throws Exception {
        UUID tenant = tenant();
        UUID actor = UUID.randomUUID();
        for (int number = 1; number <= 3; number++) {
            UUID id = new UUID(0, number);
            String at = number == 3 ? "2026-10-02T00:00:00Z" : "2026-10-01T23:59:59Z";
            jdbc.update("""
                    INSERT INTO identity_org.audit_log
                    (tenant_id,id,actor_id,action,entity,entity_id,diff,at,correlation_id)
                    VALUES (?,?,?,'DEPARTMENT_CREATED','department',?,'{}',?::timestamptz,?)
                    """, tenant, id, actor, id, at, UUID.randomUUID());
        }
        mvc.perform(get("/audit-logs").with(actor(tenant, actor, "OWNER"))
                .param("from", "2026-10-01").param("to", "2026-10-01")
                .param("page", "1").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(new UUID(0, 1).toString()));
        mvc.perform(get("/audit-logs").with(actor(tenant, actor, "OWNER"))
                .param("target", new UUID(0, 3).toString()).param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(new UUID(0, 3).toString()));
    }

    @Test
    void auditRowsCannotBeChangedAndTenantRlsFailsClosed() throws Exception {
        UUID tenant = tenant();
        mvc.perform(post("/positions").with(actor(tenant, UUID.randomUUID(), "OWNER"))
                .header("Idempotency-Key", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Engineer\",\"code\":\"ENG\"}"))
                .andExpect(status().isCreated());
        assertThrows(org.springframework.dao.DataAccessException.class,
                () -> jdbc.update("UPDATE identity_org.audit_log SET action = 'REWRITTEN' WHERE tenant_id = ?", tenant));
        assertThrows(org.springframework.dao.DataAccessException.class,
                () -> jdbc.update("DELETE FROM identity_org.audit_log WHERE tenant_id = ?", tenant));
        assertThrows(org.springframework.dao.DataAccessException.class,
                () -> jdbc.execute("TRUNCATE identity_org.audit_log"));
        jdbc.execute("CREATE ROLE audit_reader LOGIN PASSWORD 'test-only' NOBYPASSRLS");
        jdbc.execute("GRANT USAGE ON SCHEMA identity_org TO audit_reader");
        jdbc.execute("GRANT SELECT ON identity_org.audit_log TO audit_reader");
        try (var connection = java.sql.DriverManager.getConnection(POSTGRES.getJdbcUrl(), "audit_reader", "test-only")) {
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                try (var rows = statement.executeQuery("SELECT count(*) FROM identity_org.audit_log")) {
                    rows.next();
                    assertEquals(0, rows.getInt(1));
                }
                statement.execute("SELECT set_config('app.tenant_id', '" + tenant + "', true)");
                try (var rows = statement.executeQuery("SELECT count(*) FROM identity_org.audit_log")) {
                    rows.next();
                    assertEquals(1, rows.getInt(1));
                }
                connection.commit();
                try (var rows = statement.executeQuery("SELECT count(*) FROM identity_org.audit_log")) {
                    rows.next();
                    assertEquals(0, rows.getInt(1));
                }
            }
        }
    }

    @Test
    void auditRequiresOwnerAndValidTenantAndPagination() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        mvc.perform(get("/audit-logs")).andExpect(status().isUnauthorized());
        for (String role : new String[]{"EMPLOYEE", "MANAGER", "HR_ADMIN"}) {
            mvc.perform(get("/audit-logs").with(actor(tenant, actor, role)))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/audit-logs").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OWNER"))))
                .andExpect(status().isUnauthorized());
        for (String query : new String[]{"page=-1", "size=0", "size=101", "from=2026-10-30&to=2026-10-01"}) {
            mvc.perform(get("/audit-logs?" + query).with(actor(tenant, actor, "OWNER")))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    private UUID tenant() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO identity_org.auth_tenant(id, slug) VALUES (?, ?)", id, "audit-" + id);
        return id;
    }

    private static JwtRequestPostProcessor actor(UUID tenant, UUID actor, String role) {
        return jwt().jwt(token -> token.subject(actor.toString()).claim("tenantId", tenant.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private static Path keys() {
        try {
            return Files.createTempDirectory("hrflow-audit-test-keys-");
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
