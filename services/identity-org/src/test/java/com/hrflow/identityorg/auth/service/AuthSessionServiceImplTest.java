package com.hrflow.identityorg.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.hrflow.identityorg.profile.entity.Tenant;
import com.hrflow.identityorg.profile.entity.User;
import com.hrflow.identityorg.auth.entity.RefreshSession;
import com.hrflow.identityorg.exception.AuthFailedException;
import com.hrflow.identityorg.profile.repository.UserRepository;
import com.hrflow.identityorg.auth.repository.RefreshSessionRepository;
import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;

import com.hrflow.identityorg.auth.service.impl.*;
import com.hrflow.identityorg.auth.service.record.AttemptTicket;
import com.hrflow.identityorg.auth.service.record.SessionTokens;
import org.mockito.ArgumentCaptor;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthSessionServiceImplTest {
    private final UserRepository users = mock(UserRepository.class);
    private final RefreshSessionRepository refresh = mock(RefreshSessionRepository.class);
    private final JwkProviderServiceImpl access = mock(JwkProviderServiceImpl.class);
    private final AttemptThrottlerService limiter = mock(AttemptThrottlerService.class);
    private final RefreshTokenService tokenCrypto = new RefreshTokenServiceImpl();
    private AuthSessionService sessions;
    private User user;
    private final Instant now = Instant.parse("2026-10-25T12:00:00Z");

    @BeforeEach
    void setup() {
        sessions = new AuthSessionServiceImpl(users, refresh, access, limiter, new BCryptPasswordEncoder(), tokenCrypto,
                Clock.fixed(now, ZoneOffset.UTC));
        user = new User(new Tenant("demo"), "owner@example.test",
                new BCryptPasswordEncoder().encode("correct-password"), "Owner");
        when(access.issue(user)).thenReturn("signed-jwt");
        when(limiter.acquire(any(), any(), any())).thenReturn(new AttemptTicket(
                new com.hrflow.identityorg.auth.entity.AuthAttempt("a".repeat(64), now),
                new com.hrflow.identityorg.auth.entity.AuthAttempt("b".repeat(64), now)));
    }

    @Test
    void loginReturnsSignedAccessAndStoresOnlyRefreshHash() {
        when(users.findByTenantSlugAndEmailAndEnabledTrue("demo", "owner@example.test"))
                .thenReturn(Optional.of(user));
        SessionTokens tokens = sessions.login("192.0.2.1", "demo", "OWNER@example.test", "correct-password");
        assertEquals("signed-jwt", tokens.accessToken());
        assertTrue(tokens.refreshToken().length() >= 32);
        verify(refresh).save(argThat(row -> row.getUserId().equals(user.getId())
                && row.getTokenHash().equals(tokenCrypto.hashToken(tokens.refreshToken()))
                && !row.getTokenHash().equals(tokens.refreshToken())));
        ArgumentCaptor<RefreshSession> saved = ArgumentCaptor.forClass(RefreshSession.class);
        verify(refresh).save(saved.capture());
        assertEquals(user.getTenantId(), saved.getValue().getTenantId());
        assertEquals(now.plusSeconds(14L * 86400), saved.getValue().getExpiresAt());
    }

    @Test
    void unknownAccountAndWrongPasswordHaveSameFailure() {
        when(users.findByTenantSlugAndEmailAndEnabledTrue("demo", "owner@example.test"))
                .thenReturn(Optional.of(user));
        AuthFailedException wrong = assertThrows(AuthFailedException.class,
                () -> sessions.login("192.0.2.1", "demo", "owner@example.test", "wrong-password"));
        AuthFailedException missing = assertThrows(AuthFailedException.class,
                () -> sessions.login("192.0.2.1", "other", "owner@example.test", "wrong-password"));
        assertEquals(wrong.status, missing.status);
        assertEquals(wrong.getMessage(), missing.getMessage());
        verifyNoInteractions(refresh);
        verify(limiter, times(2)).failed(any());
    }

    @Test
    void consumedRefreshRevokesItsEntireFamily() {
        String raw = "a".repeat(48);
        UUID family = UUID.randomUUID();
        RefreshSession row = new RefreshSession(user.getId(), user.getTenantId(), family, tokenCrypto.hashToken(raw),
                now.plusSeconds(3600));
        when(refresh.findByTokenHash(tokenCrypto.hashToken(raw))).thenReturn(Optional.of(row));
        when(users.findByIdAndTenantIdAndEnabledTrue(user.getId(), user.getTenantId())).thenReturn(Optional.of(user));
        SessionTokens rotated = sessions.refresh(raw);
        assertNotEquals(raw, rotated.refreshToken());
        assertNotNull(row.getConsumedAt());
        assertThrows(AuthFailedException.class, () -> sessions.refresh(raw));
        verify(refresh).revokeFamily(eq(user.getTenantId()), eq(family), any());
    }

    @Test
    void logoutRevokesOnlyThePresentedFamily() {
        String raw = "b".repeat(48);
        UUID family = UUID.randomUUID();
        when(refresh.findByTokenHash(tokenCrypto.hashToken(raw))).thenReturn(Optional.of(
                new RefreshSession(user.getId(), user.getTenantId(), family, tokenCrypto.hashToken(raw),
                        now.plusSeconds(3600))));
        sessions.logout(raw);
        verify(refresh).revokeFamily(eq(user.getTenantId()), eq(family), any());
    }
}
