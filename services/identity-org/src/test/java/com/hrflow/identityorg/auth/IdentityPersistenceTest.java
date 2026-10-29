package com.hrflow.identityorg.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hrflow.identityorg.exception.AuthFailedException;
import com.hrflow.identityorg.auth.service.AuthSessionService;
import com.hrflow.identityorg.auth.service.PasswordService;
import com.hrflow.identityorg.auth.service.record.SessionTokens;
import com.hrflow.identityorg.profile.dto.ProfilePatchRequest;
import com.hrflow.identityorg.profile.dto.UserProfileResponse;
import com.hrflow.identityorg.profile.service.UserProfileService;
import com.hrflow.identityorg.organization.dto.NamedCreateRequest;
import com.hrflow.identityorg.organization.repository.EmployeeRepository;
import com.hrflow.identityorg.organization.service.CatalogService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
class IdentityPersistenceTest {
    private static final Path KEY_DIRECTORY = createKeyDirectory();

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
    AuthSessionService sessions;

    @Autowired
    UserProfileService profiles;

    @Autowired
    JwtDecoder jwtDecoder;

    @Autowired
    PasswordService passwords;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    MockMvc mvc;

    @Autowired
    CatalogService catalogs;

    @Autowired
    EmployeeRepository employees;

    @Test
    void migrationsMatchJpaAndRefreshSessionsCarryTenantOwnership() {
        Integer tenantColumns = jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                WHERE table_schema = 'identity_org'
                  AND table_name = 'refresh_session'
                  AND column_name = 'tenant_id'
                """, Integer.class);
        assertEquals(1, tenantColumns);

        assertThrows(Exception.class, () -> jdbc.update("""
                INSERT INTO identity_org.refresh_session
                    (id, user_id, tenant_id, family_id, token_hash, expires_at)
                SELECT gen_random_uuid(), account.id, gen_random_uuid(), gen_random_uuid(), ?, now() + interval '1 day'
                FROM identity_org.auth_user account LIMIT 1
                """, "f".repeat(64)));
    }

    @Test
    void employeeMasterRejectsForeignReferencesAndManagerCycles() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        jdbc.update("INSERT INTO identity_org.auth_tenant(id, slug) VALUES (?, ?)", tenantA, "org-a-" + tenantA);
        jdbc.update("INSERT INTO identity_org.auth_tenant(id, slug) VALUES (?, ?)", tenantB, "org-b-" + tenantB);
        UUID departmentA = UUID.randomUUID();
        UUID positionA = UUID.randomUUID();
        UUID departmentB = UUID.randomUUID();
        UUID positionB = UUID.randomUUID();
        jdbc.update("INSERT INTO identity_org.department(id, tenant_id, code, name) VALUES (?, ?, ?, ?)",
                departmentA, tenantA, "A", "Department A");
        jdbc.update("INSERT INTO identity_org.position(id, tenant_id, code, name) VALUES (?, ?, ?, ?)",
                positionA, tenantA, "A", "Position A");
        jdbc.update("INSERT INTO identity_org.department(id, tenant_id, code, name) VALUES (?, ?, ?, ?)",
                departmentB, tenantB, "B", "Department B");
        jdbc.update("INSERT INTO identity_org.position(id, tenant_id, code, name) VALUES (?, ?, ?, ?)",
                positionB, tenantB, "B", "Position B");
        UUID first = UUID.randomUUID();
        assertThrows(Exception.class, () -> jdbc.update("""
                INSERT INTO identity_org.employee
                    (id, tenant_id, employee_code, name, email, department_id, position_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, first, tenantA, "E1", "First", "first@example.test", departmentB, positionA));
        assertThrows(Exception.class, () -> jdbc.update("""
                INSERT INTO identity_org.employee
                    (id, tenant_id, employee_code, name, email, department_id, position_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, first, tenantA, "E1", "First", "first@example.test", departmentA, positionB));
        jdbc.update("""
                INSERT INTO identity_org.employee
                    (id, tenant_id, employee_code, name, email, department_id, position_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, first, tenantA, "E1", "First", "first@example.test", departmentA, positionA);
        UUID second = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO identity_org.employee
                    (id, tenant_id, employee_code, name, email, department_id, position_id, manager_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, second, tenantA, "E2", "Second", "second@example.test", departmentA, positionA, first);
        assertThrows(Exception.class, () -> jdbc.update(
                "UPDATE identity_org.employee SET manager_id = ? WHERE id = ?", second, first));
        assertThrows(Exception.class, () -> jdbc.update(
                "UPDATE identity_org.employee SET manager_id = id WHERE id = ?", first));
    }

    @Test
    void personalRowsRequireTransactionTenantForNonOwnerAndDoNotLeakAcrossPoolReuse() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        jdbc.update("INSERT INTO identity_org.auth_tenant(id, slug) VALUES (?, ?)", tenantA, "rls-a-" + tenantA);
        jdbc.update("INSERT INTO identity_org.auth_tenant(id, slug) VALUES (?, ?)", tenantB, "rls-b-" + tenantB);
        jdbc.execute("CREATE ROLE hrflow_rls_test NOLOGIN");
        jdbc.execute("GRANT USAGE ON SCHEMA identity_org TO hrflow_rls_test");
        jdbc.execute("GRANT SELECT, INSERT ON identity_org.department TO hrflow_rls_test");
        jdbc.update("INSERT INTO identity_org.department(id, tenant_id, code, name) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), tenantA, "A", "Tenant A");
        jdbc.update("INSERT INTO identity_org.department(id, tenant_id, code, name) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), tenantB, "B", "Tenant B");
        try (Connection connection = jdbc.getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            assertEquals(0, scopedDepartmentCount(connection, null));
            connection.rollback();
            assertEquals(1, scopedDepartmentCount(connection, tenantA));
            assertThrows(SQLException.class, () -> {
                try (PreparedStatement insert = connection.prepareStatement("""
                        INSERT INTO identity_org.department(id, tenant_id, code, name)
                        VALUES (?, ?, 'FOREIGN', 'Foreign tenant')
                        """)) {
                    insert.setObject(1, UUID.randomUUID());
                    insert.setObject(2, tenantB);
                    insert.executeUpdate();
                }
            });
            connection.rollback();
            assertEquals(1, scopedDepartmentCount(connection, tenantB));
            connection.rollback();
            assertEquals(0, scopedDepartmentCount(connection, null));
            connection.rollback();
        }
    }

    private int scopedDepartmentCount(Connection connection, UUID tenantId) throws Exception {
        try (var role = connection.createStatement()) {
            role.execute("SET LOCAL ROLE hrflow_rls_test");
        }
        if (tenantId != null) {
            try (PreparedStatement context = connection.prepareStatement("SELECT set_config('app.tenant_id', ?, true)")) {
                context.setString(1, tenantId.toString());
                context.execute();
            }
        }
        try (var query = connection.createStatement();
                ResultSet rows = query.executeQuery("SELECT count(*) FROM identity_org.department")) {
            rows.next();
            return rows.getInt(1);
        }
    }

    @Test
    void employeeHttpScopesTenantAndEnforcesRoleAndRevision() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        jdbc.update("INSERT INTO identity_org.auth_tenant(id, slug) VALUES (?, ?)", tenantA, "http-a-" + tenantA);
        jdbc.update("INSERT INTO identity_org.auth_tenant(id, slug) VALUES (?, ?)", tenantB, "http-b-" + tenantB);
        UUID actor = UUID.randomUUID();
        var department = catalogs.createDepartment(tenantA, actor, UUID.randomUUID().toString(),
                new NamedCreateRequest("People", "PEOPLE"));
        var position = catalogs.createPosition(tenantA, actor, UUID.randomUUID().toString(),
                new NamedCreateRequest("Analyst", "ANALYST"));
        String body = """
                {"name":"Test Person","email":"person@example.test","employeeNo":"EMP-101",
                 "departmentId":"%s","positionId":"%s"}
                """.formatted(department.id(), position.id());
        String commandKey = UUID.randomUUID().toString();
        mvc.perform(post("/employees").with(owner(tenantA)).header("Idempotency-Key", commandKey)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.employeeNo").value("EMP-101"));
        mvc.perform(post("/employees").with(owner(tenantA)).header("Idempotency-Key", commandKey)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.employeeNo").value("EMP-101"));
        mvc.perform(post("/employees").with(owner(tenantA)).header("Idempotency-Key", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONFLICT"));
        UUID employeeId = employees.findByTenantId(tenantA, org.springframework.data.domain.PageRequest.of(0, 1))
                .getContent().getFirst().getId();
        mvc.perform(get("/employees").with(owner(tenantB)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/employees/{id}", employeeId).with(owner(tenantB)))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/employees/{id}", employeeId).with(owner(tenantA))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Changed\"}"))
                .andExpect(status().isPreconditionRequired());
        mvc.perform(patch("/employees/{id}", employeeId).with(owner(tenantA))
                .header("If-Match", "\"99\"").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Changed\"}"))
                .andExpect(status().isPreconditionFailed());
        mvc.perform(patch("/employees/{id}", employeeId).with(owner(tenantA))
                .header("If-Match", "\"0\"").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Changed\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        mvc.perform(post("/employees").with(owner(tenantA)).header("Idempotency-Key", commandKey)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.version").value(0));
        mvc.perform(post("/employees").with(owner(tenantA)).header("Idempotency-Key", commandKey)
                .contentType(MediaType.APPLICATION_JSON).content(body.replace("EMP-101", "EMP-102")))
                .andExpect(status().isConflict());
        mvc.perform(post("/employees").with(owner(tenantA))
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isPreconditionRequired());
        String reportBody = """
                {"name":"Direct Report","email":"report@example.test","employeeNo":"EMP-102",
                 "departmentId":"%s","positionId":"%s","managerId":"%s"}
                """.formatted(department.id(), position.id(), employeeId);
        mvc.perform(post("/employees").with(owner(tenantA)).header("Idempotency-Key", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content(reportBody))
                .andExpect(status().isCreated());
        UUID reportId = jdbc.queryForObject("""
                SELECT id FROM identity_org.employee WHERE tenant_id = ? AND employee_code = 'EMP-102'
                """, UUID.class, tenantA);
        mvc.perform(patch("/employees/{id}", reportId).with(owner(tenantA))
                .header("If-Match", "\"0\"").contentType(MediaType.APPLICATION_JSON)
                .content("{\"managerId\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.managerId").isEmpty());
        mvc.perform(get("/employees").with(jwt().jwt(token -> token.claim("tenantId", tenantA.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void catalogHttpScopesTenantAndEnforcesRoleAndIdempotency() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        jdbc.update("INSERT INTO identity_org.auth_tenant(id, slug) VALUES (?, ?)", tenantA, "cat-a-" + tenantA);
        jdbc.update("INSERT INTO identity_org.auth_tenant(id, slug) VALUES (?, ?)", tenantB, "cat-b-" + tenantB);

        String deptBody = "{\"name\":\"Engineering\",\"code\":\"ENG\"}";
        String deptKey = UUID.randomUUID().toString();

        // Missing Idempotency-Key
        mvc.perform(post("/departments").with(owner(tenantA))
                .contentType(MediaType.APPLICATION_JSON).content(deptBody))
                .andExpect(status().isPreconditionRequired());

        // Forbidden for ROLE_EMPLOYEE
        mvc.perform(post("/departments").with(jwt().jwt(token -> token.claim("tenantId", tenantA.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE")))
                .header("Idempotency-Key", deptKey)
                .contentType(MediaType.APPLICATION_JSON).content(deptBody))
                .andExpect(status().isForbidden());

        // Create department
        mvc.perform(post("/departments").with(owner(tenantA))
                .header("Idempotency-Key", deptKey)
                .contentType(MediaType.APPLICATION_JSON).content(deptBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("ENG"))
                .andExpect(jsonPath("$.name").value("Engineering"));

        // Replay department creation with same key and body
        mvc.perform(post("/departments").with(owner(tenantA))
                .header("Idempotency-Key", deptKey)
                .contentType(MediaType.APPLICATION_JSON).content(deptBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("ENG"));

        // Different body with same key -> 409 Conflict
        mvc.perform(post("/departments").with(owner(tenantA))
                .header("Idempotency-Key", deptKey)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Other\",\"code\":\"OTH\"}"))
                .andExpect(status().isConflict());

        // List departments for tenantA
        mvc.perform(get("/departments").with(owner(tenantA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].code").value("ENG"));

        // TenantB should see 0 departments
        mvc.perform(get("/departments").with(owner(tenantB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));

        // Employee cannot list departments
        mvc.perform(get("/departments").with(jwt().jwt(token -> token.claim("tenantId", tenantA.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
                .andExpect(status().isForbidden());

        // Positions: Missing Idempotency-Key
        String posBody = "{\"name\":\"Software Engineer\",\"code\":\"SWE\"}";
        String posKey = UUID.randomUUID().toString();

        mvc.perform(post("/positions").with(owner(tenantA))
                .contentType(MediaType.APPLICATION_JSON).content(posBody))
                .andExpect(status().isPreconditionRequired());

        // Create position
        mvc.perform(post("/positions").with(owner(tenantA))
                .header("Idempotency-Key", posKey)
                .contentType(MediaType.APPLICATION_JSON).content(posBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SWE"))
                .andExpect(jsonPath("$.name").value("Software Engineer"));

        // Replay position creation
        mvc.perform(post("/positions").with(owner(tenantA))
                .header("Idempotency-Key", posKey)
                .contentType(MediaType.APPLICATION_JSON).content(posBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SWE"));

        // Conflict on changed body with same key
        mvc.perform(post("/positions").with(owner(tenantA))
                .header("Idempotency-Key", posKey)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Other Pos\",\"code\":\"OTH\"}"))
                .andExpect(status().isConflict());

        // List positions for tenantA
        mvc.perform(get("/positions").with(owner(tenantA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].code").value("SWE"));

        // TenantB should see 0 positions
        mvc.perform(get("/positions").with(owner(tenantB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));

        // Employee cannot list positions
        mvc.perform(get("/positions").with(jwt().jwt(token -> token.claim("tenantId", tenantA.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
                .andExpect(status().isForbidden());
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor
    owner(UUID tenantId) {
        return jwt().jwt(token -> token.subject("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa")
                        .claim("tenantId", tenantId.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_OWNER"));
    }

    @Test
    void concurrentRefreshAllowsOneRotationThenRevokesTheFamily() throws Exception {
        SessionTokens login = sessions.login("192.0.2.1", "demo", "owner@example.test",
                "synthetic-local-password");
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<SessionTokens> rotate = () -> {
                start.await();
                return sessions.refresh(login.refreshToken());
            };
            Future<SessionTokens> first = executor.submit(rotate);
            Future<SessionTokens> second = executor.submit(rotate);
            start.countDown();
            java.util.List<SessionTokens> successes = new java.util.ArrayList<>();
            int failures = 0;
            for (Future<SessionTokens> result : java.util.List.of(first, second)) {
                try {
                    successes.add(result.get());
                } catch (java.util.concurrent.ExecutionException failure) {
                    assertTrue(failure.getCause() instanceof AuthFailedException);
                    failures++;
                }
            }
            assertEquals(1, successes.size());
            assertEquals(1, failures);
            assertThrows(AuthFailedException.class, () -> sessions.refresh(successes.getFirst().refreshToken()));
        }
    }

    @Test
    void thresholdFailureCommitsTheBlockAndReturnsRetryAfter() {
        for (int attempt = 1; attempt < 5; attempt++) {
            AuthFailedException invalid = assertThrows(AuthFailedException.class, () -> sessions.login(
                    "198.51.100.10", "demo", "missing@example.test", "wrong-password"));
            assertEquals(401, invalid.status.value());
        }
        AuthFailedException threshold = assertThrows(AuthFailedException.class, () -> sessions.login(
                "198.51.100.10", "demo", "missing@example.test", "wrong-password"));
        assertEquals(429, threshold.status.value());
        assertTrue(threshold.retryAfterSeconds > 0);

        AuthFailedException stillBlocked = assertThrows(AuthFailedException.class, () -> sessions.login(
                "198.51.100.10", "demo", "missing@example.test", "wrong-password"));
        assertEquals(429, stillBlocked.status.value());
    }

    @Test
    void authenticatedUserCanReadAndPersistProfileChanges() {
        SessionTokens login = sessions.login("203.0.113.20", "demo", "owner@example.test",
                "synthetic-local-password");
        var jwt = jwtDecoder.decode(login.accessToken());

        UserProfileResponse updated = profiles.update(jwt,
                new ProfilePatchRequest("Updated Owner", "Asia/Bangkok"));

        assertEquals("Updated Owner", updated.name());
        assertEquals("Asia/Bangkok", updated.timeZone());
        assertEquals(updated, profiles.me(jwt));
        assertEquals("Updated Owner", jdbc.queryForObject("""
                SELECT display_name FROM identity_org.auth_user
                WHERE id = ?::uuid AND tenant_id = ?::uuid
                """, String.class, updated.userId(), updated.tenantId()));
        assertEquals("Asia/Bangkok", jdbc.queryForObject("""
                SELECT time_zone FROM identity_org.auth_user
                WHERE id = ?::uuid AND tenant_id = ?::uuid
                """, String.class, updated.userId(), updated.tenantId()));
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void passwordChangePersistsTheNewHashAndRevokesExistingRefreshSessions() {
        SessionTokens login = sessions.login("203.0.113.21", "demo", "owner@example.test",
                "synthetic-local-password");
        var jwt = jwtDecoder.decode(login.accessToken());

        passwords.change(jwt, "synthetic-local-password", "replacement-password");

        String storedHash = jdbc.queryForObject("""
                SELECT password_hash FROM identity_org.auth_user
                WHERE id = ?::uuid AND tenant_id = ?::uuid
                """, String.class, jwt.getSubject(), jwt.getClaimAsString("tenantId"));
        assertTrue(passwordEncoder.matches("replacement-password", storedHash));
        assertEquals(0, jdbc.queryForObject("""
                SELECT count(*) FROM identity_org.refresh_session
                WHERE user_id = ?::uuid AND tenant_id = ?::uuid AND revoked_at IS NULL
                """, Integer.class, jwt.getSubject(), jwt.getClaimAsString("tenantId")));
    }

    private static Path createKeyDirectory() {
        try {
            return Files.createTempDirectory("hrflow-identity-keys-");
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }
}
