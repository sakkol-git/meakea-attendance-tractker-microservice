package com.hrflow.identityorg.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hrflow.identityorg.exception.AuthFailedException;
import com.hrflow.identityorg.auth.service.AuthSessionService;
import com.hrflow.identityorg.auth.service.PasswordService;
import com.hrflow.identityorg.auth.service.record.SessionTokens;
import com.hrflow.identityorg.profile.dto.ProfilePatchRequest;
import com.hrflow.identityorg.profile.dto.UserProfileResponse;
import com.hrflow.identityorg.profile.service.UserProfileService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(properties = {
        "hrflow.auth.local-bootstrap=true",
        "hrflow.auth.dev-password=synthetic-local-password",
        "spring.kafka.listener.auto-startup=false"
})
class IdentityPersistenceIT {
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
