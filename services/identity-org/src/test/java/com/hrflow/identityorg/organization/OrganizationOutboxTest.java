package com.hrflow.identityorg.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hrflow.identityorg.organization.dto.EmployeeCreateRequest;
import com.hrflow.identityorg.organization.dto.EmployeePatchRequest;
import com.hrflow.identityorg.organization.dto.NamedCreateRequest;
import com.hrflow.identityorg.organization.service.CatalogService;
import com.hrflow.identityorg.organization.service.EmployeeService;
import com.hrflow.identityorg.organization.service.OrganizationSnapshots;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

@Testcontainers
@SpringBootTest(properties = {
        "hrflow.auth.local-bootstrap=true",
        "hrflow.auth.dev-password=synthetic-local-password",
        "spring.kafka.listener.auto-startup=false"
})
class OrganizationOutboxTest {
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

    @Autowired JdbcTemplate jdbc;
    @Autowired CatalogService catalogs;
    @Autowired EmployeeService employees;
    @Autowired JsonMapper json;

    @Test
    void employeeMutationAndOwnerIntentCommitTogether() {
        UUID tenant = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        jdbc.update("INSERT INTO identity_org.auth_tenant(id, slug) VALUES (?, ?)", tenant, "outbox-" + tenant);
        var department = catalogs.createDepartment(tenant, actor, UUID.randomUUID().toString(),
                new NamedCreateRequest("People", "PEOPLE"));
        var position = catalogs.createPosition(tenant, actor, UUID.randomUUID().toString(),
                new NamedCreateRequest("Analyst", "ANALYST"));
        var request = new EmployeeCreateRequest("Test Employee", "test@example.test", "EMP-1",
                department.id(), position.id(), null, null);
        String key = UUID.randomUUID().toString();
        var created = employees.create(tenant, actor, key, request);
        assertEquals(1, count(tenant));
        String payload = jdbc.queryForObject("""
                SELECT payload::text FROM identity_org.organization_outbox
                WHERE tenant_id = ? AND topic = 'org.employee.v1'
                """, String.class, tenant);
        var snapshot = json.readValue(payload, OrganizationSnapshots.Employee.class);
        assertEquals("EMP-1", snapshot.employeeNo());
        assertEquals("People", snapshot.departmentName());
        assertEquals(created, employees.create(tenant, actor, key, request));
        assertEquals(1, count(tenant));
        var patch = new EmployeePatchRequest();
        patch.setName("Renamed Employee");
        employees.patch(tenant, actor, created.id(), '"' + Long.toString(created.version()) + '"', patch);
        assertEquals(2, count(tenant));
        assertEquals(java.util.List.of(1L, 2L), jdbc.queryForList("""
                SELECT aggregate_revision FROM identity_org.organization_outbox
                WHERE tenant_id = ? AND topic = 'org.employee.v1'
                ORDER BY aggregate_revision
                """, Long.class, tenant));
        assertThrows(Exception.class, () -> employees.create(tenant, actor, UUID.randomUUID().toString(), request));
        assertEquals(2, count(tenant));
    }

    @Test
    void outboxPayloadRequiresTenantContextForNonOwnerConnection() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        jdbc.update("INSERT INTO identity_org.auth_tenant(id, slug) VALUES (?, ?)", tenantA, "outbox-a-" + tenantA);
        jdbc.update("INSERT INTO identity_org.auth_tenant(id, slug) VALUES (?, ?)", tenantB, "outbox-b-" + tenantB);
        for (UUID tenant : java.util.List.of(tenantA, tenantB)) {
            jdbc.update("""
                    INSERT INTO identity_org.organization_outbox
                        (id, tenant_id, topic, aggregate_id, aggregate_revision,
                         event_type, correlation_id, payload, occurred_at)
                    VALUES (?, ?, 'org.employee.v1', ?, 1,
                            'org.employee.created.v1', ?, '{}'::jsonb, now())
                    """, UUID.randomUUID(), tenant, UUID.randomUUID(), UUID.randomUUID());
        }
        jdbc.execute("CREATE ROLE hrflow_outbox_rls_test NOLOGIN");
        jdbc.execute("GRANT USAGE ON SCHEMA identity_org TO hrflow_outbox_rls_test");
        jdbc.execute("GRANT SELECT ON identity_org.organization_outbox TO hrflow_outbox_rls_test");
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM identity_jobs.organization_tenant_registry WHERE tenant_id = ?
                """, Integer.class, tenantA));
        try (Connection connection = jdbc.getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            assertEquals(0, scopedOutboxCount(connection, null));
            connection.rollback();
            try (var role = connection.createStatement()) {
                role.execute("SET LOCAL ROLE hrflow_outbox_rls_test");
            }
            assertThrows(java.sql.SQLException.class, () -> {
                try (var query = connection.createStatement()) {
                    query.executeQuery("SELECT count(*) FROM identity_jobs.organization_tenant_registry");
                }
            });
            connection.rollback();
            assertEquals(1, scopedOutboxCount(connection, tenantA));
            connection.rollback();
            assertEquals(1, scopedOutboxCount(connection, tenantB));
            connection.rollback();
            assertEquals(0, scopedOutboxCount(connection, null));
            connection.rollback();
        }
    }

    @Test
    void concurrentRetryCommitsOneEmployeeAndOneIntent() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        jdbc.update("INSERT INTO identity_org.auth_tenant(id, slug) VALUES (?, ?)", tenant, "race-" + tenant);
        var department = catalogs.createDepartment(tenant, actor, UUID.randomUUID().toString(),
                new NamedCreateRequest("People", "PEOPLE"));
        var position = catalogs.createPosition(tenant, actor, UUID.randomUUID().toString(),
                new NamedCreateRequest("Analyst", "ANALYST"));
        var request = new EmployeeCreateRequest("Race Employee", "race@example.test", "EMP-RACE",
                department.id(), position.id(), null, null);
        String key = UUID.randomUUID().toString();
        CountDownLatch start = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> {
                start.await();
                return employees.create(tenant, actor, key, request);
            });
            var second = workers.submit(() -> {
                start.await();
                return employees.create(tenant, actor, key, request);
            });
            start.countDown();
            assertEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        }
        assertEquals(1, count(tenant));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM identity_org.employee WHERE tenant_id = ?
                """, Integer.class, tenant));
    }

    private int scopedOutboxCount(Connection connection, UUID tenant) throws Exception {
        try (var statement = connection.createStatement()) {
            statement.execute("SET LOCAL ROLE hrflow_outbox_rls_test");
        }
        if (tenant != null) {
            try (PreparedStatement context = connection.prepareStatement("SELECT set_config('app.tenant_id', ?, true)")) {
                context.setString(1, tenant.toString());
                context.execute();
            }
        }
        try (var query = connection.createStatement();
                ResultSet rows = query.executeQuery("SELECT count(*) FROM identity_org.organization_outbox")) {
            rows.next();
            return rows.getInt(1);
        }
    }

    private int count(UUID tenant) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM identity_org.organization_outbox
                WHERE tenant_id = ? AND topic = 'org.employee.v1'
                """, Integer.class, tenant);
    }

    private static Path temporaryKeys() {
        try {
            return Files.createTempDirectory("hrflow-outbox-keys-");
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }
}
