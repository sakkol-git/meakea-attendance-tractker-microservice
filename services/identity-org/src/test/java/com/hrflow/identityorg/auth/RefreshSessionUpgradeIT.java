package com.hrflow.identityorg.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class RefreshSessionUpgradeIT {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("identity-init.sql");

    @Test
    void upgradesExistingRefreshRowsWithoutLosingTenantOwnership() throws Exception {
        migration().target(MigrationVersion.fromVersion("4")).load().migrate();
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            try (var insertTenant = connection.prepareStatement(
                    "INSERT INTO identity_org.auth_tenant(id, slug) VALUES (?, 'upgrade-test')")) {
                insertTenant.setObject(1, tenantId);
                insertTenant.executeUpdate();
            }
            try (var insertUser = connection.prepareStatement("""
                    INSERT INTO identity_org.auth_user
                        (id, tenant_id, tenant_slug, employee_id, email, password_hash,
                         display_name, roles, time_zone, enabled)
                    VALUES (?, ?, 'upgrade-test', ?, 'owner@example.test', 'hash',
                            'Owner', 'OWNER', 'UTC', true)
                    """)) {
                insertUser.setObject(1, userId);
                insertUser.setObject(2, tenantId);
                insertUser.setObject(3, UUID.randomUUID());
                insertUser.executeUpdate();
            }
            try (var insertSession = connection.prepareStatement("""
                    INSERT INTO identity_org.refresh_session
                        (id, user_id, family_id, token_hash, expires_at)
                    VALUES (?, ?, ?, ?, now() + interval '1 day')
                    """)) {
                insertSession.setObject(1, sessionId);
                insertSession.setObject(2, userId);
                insertSession.setObject(3, UUID.randomUUID());
                insertSession.setString(4, "a".repeat(64));
                insertSession.executeUpdate();
            }
        }

        migration().load().migrate();

        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var query = connection.prepareStatement(
                     "SELECT tenant_id FROM identity_org.refresh_session WHERE id = ?")) {
            query.setObject(1, sessionId);
            try (var result = query.executeQuery()) {
                result.next();
                assertEquals(tenantId, result.getObject(1, UUID.class));
            }
        }
    }

    private static org.flywaydb.core.api.configuration.FluentConfiguration migration() {
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas("identity_org")
                .defaultSchema("identity_org")
                .createSchemas(false)
                .locations("classpath:db/migration");
    }
}
