package com.hrflow.identityorg.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthSessionsTest {
    private final AuthUserRepository users = mock(AuthUserRepository.class);
    private final RefreshSessionRepository refresh = mock(RefreshSessionRepository.class);
    private final AccessTokens access = mock(AccessTokens.class);
    private final AttemptLimiter limiter = mock(AttemptLimiter.class);
    private AuthSessions sessions;
    private AuthUser user;

    @BeforeEach
    void setup() {
        sessions = new AuthSessions(users, refresh, access, limiter);
        user = new AuthUser(new AuthTenant("demo"), "owner@example.test",
                new BCryptPasswordEncoder().encode("correct-password"), "Owner");
        when(access.issue(user)).thenReturn("signed-jwt");
        when(limiter.acquire(any(), any())).thenReturn(new AuthAttempt());
    }

    @Test
    void loginReturnsSignedAccessAndStoresOnlyRefreshHash() {
        when(users.findByTenantSlugAndEmailAndEnabledTrue("demo", "owner@example.test"))
                .thenReturn(Optional.of(user));
        AuthSessions.TokenPair tokens = sessions.login("demo", "OWNER@example.test", "correct-password");
        assertEquals("signed-jwt", tokens.accessToken());
        assertTrue(tokens.refreshToken().length() >= 32);
        verify(refresh).save(argThat(row -> row.userId.equals(user.id)
                && row.tokenHash.equals(hash(tokens.refreshToken()))
                && !row.tokenHash.equals(tokens.refreshToken())));
    }

    @Test
    void unknownAccountAndWrongPasswordHaveSameFailure() {
        when(users.findByTenantSlugAndEmailAndEnabledTrue("demo", "owner@example.test"))
                .thenReturn(Optional.of(user));
        AuthFailure wrong = assertThrows(AuthFailure.class,
                () -> sessions.login("demo", "owner@example.test", "wrong-password"));
        AuthFailure missing = assertThrows(AuthFailure.class,
                () -> sessions.login("other", "owner@example.test", "wrong-password"));
        assertEquals(wrong.status, missing.status);
        assertEquals(wrong.getMessage(), missing.getMessage());
        verifyNoInteractions(refresh);
        verify(limiter, times(2)).failed(any());
    }

    @Test
    void consumedRefreshRevokesItsEntireFamily() {
        String raw = "a".repeat(48);
        UUID family = UUID.randomUUID();
        RefreshSession row = new RefreshSession(user.id, family, hash(raw), Instant.now().plusSeconds(3600));
        when(refresh.findByTokenHash(hash(raw))).thenReturn(Optional.of(row));
        when(users.findById(user.id)).thenReturn(Optional.of(user));
        AuthSessions.TokenPair rotated = sessions.refresh(raw);
        assertNotEquals(raw, rotated.refreshToken());
        assertNotNull(row.consumedAt);
        assertThrows(AuthFailure.class, () -> sessions.refresh(raw));
        verify(refresh).revokeFamily(eq(family), any());
    }

    @Test
    void logoutRevokesOnlyThePresentedFamily() {
        String raw = "b".repeat(48);
        UUID family = UUID.randomUUID();
        when(refresh.findByTokenHash(hash(raw))).thenReturn(Optional.of(
                new RefreshSession(user.id, family, hash(raw), Instant.now().plusSeconds(3600))));
        sessions.logout(raw);
        verify(refresh).revokeFamily(eq(family), any());
    }

    private static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }
}
