package com.hrflow.identityorg.auth.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrflow.identityorg.auth.repository.RefreshSessionRepository;
import com.hrflow.identityorg.auth.service.impl.PasswordServiceImpl;
import com.hrflow.identityorg.exception.AuthFailedException;
import com.hrflow.identityorg.profile.entity.Tenant;
import com.hrflow.identityorg.profile.entity.User;
import com.hrflow.identityorg.profile.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

class PasswordServiceImplTest {
    private final UserRepository users = mock(UserRepository.class);
    private final RefreshSessionRepository sessions = mock(RefreshSessionRepository.class);
    private final JwkProviderService tokens = mock(JwkProviderService.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final Instant now = Instant.parse("2026-10-26T08:00:00Z");
    private final PasswordService passwords = new PasswordServiceImpl(
            users, sessions, tokens, encoder, Clock.fixed(now, ZoneOffset.UTC));
    private User user;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        user = new User(new Tenant("demo"), "owner@example.test",
                encoder.encode("current-password"), "Owner");
        jwt = jwt(user);
        when(users.findForUpdateByIdAndTenantIdAndEnabledTrue(user.getId(), user.getTenantId()))
                .thenReturn(Optional.of(user));
        when(tokens.matches(jwt, user)).thenReturn(true);
    }

    @Test
    void changesTheAuthenticatedUsersPasswordAndRevokesRefreshSessions() {
        passwords.change(jwt, "current-password", "new-password");

        assertTrue(encoder.matches("new-password", user.getPasswordHash()));
        verify(users).save(user);
        verify(sessions).revokeUser(eq(user.getTenantId()), eq(user.getId()), eq(now));
    }

    @Test
    void rejectsTheWrongCurrentPasswordWithoutChangingCredentials() {
        assertThrows(AuthFailedException.class,
                () -> passwords.change(jwt, "wrong-password", "new-password"));

        assertTrue(encoder.matches("current-password", user.getPasswordHash()));
        verify(sessions, org.mockito.Mockito.never()).revokeUser(any(), any(), any());
    }

    @Test
    void rejectsMalformedOrMismatchedClaims() {
        assertThrows(AuthFailedException.class,
                () -> passwords.change(jwt("bad", user.getTenantId().toString()),
                        "current-password", "new-password"));
        when(tokens.matches(jwt, user)).thenReturn(false);
        assertThrows(AuthFailedException.class,
                () -> passwords.change(jwt, "current-password", "new-password"));
    }

    private static Jwt jwt(User user) {
        return jwt(user.getId().toString(), user.getTenantId().toString());
    }

    private static Jwt jwt(String subject, String tenantId) {
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "RS256"), Map.of(
                        "sub", subject,
                        "tenantId", tenantId,
                        "employeeId", "employee-id",
                        "roles", List.of("OWNER"),
                        "aud", List.of("hrflow-api")));
    }
}
