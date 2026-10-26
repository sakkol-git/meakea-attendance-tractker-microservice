package com.hrflow.identityorg.auth.service.impl;

import com.hrflow.identityorg.profile.entity.User;
import com.hrflow.identityorg.auth.entity.RefreshSession;
import com.hrflow.identityorg.exception.AuthFailedException;
import com.hrflow.identityorg.profile.repository.UserRepository;
import com.hrflow.identityorg.auth.repository.RefreshSessionRepository;
import java.time.Instant;
import java.time.Clock;
import java.util.UUID;

import com.hrflow.identityorg.auth.service.AttemptThrottlerService;
import com.hrflow.identityorg.auth.service.AuthSessionService;
import com.hrflow.identityorg.auth.service.JwkProviderService;
import com.hrflow.identityorg.auth.service.RefreshTokenService;
import com.hrflow.identityorg.auth.service.record.AttemptTicket;
import com.hrflow.identityorg.auth.service.record.AuthTokenPolicy;
import com.hrflow.identityorg.auth.service.record.SessionTokens;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthSessionServiceImpl implements AuthSessionService {
    private static final String DUMMY_PASSWORD_HASH =
            "$2y$10$LPWqz26BIRZrctpaseKVUeJjo5Fk5Y1XALIIqHaaFm4BCvcAP.0Hu";
    private final UserRepository users;
    private final RefreshSessionRepository sessionsRepository;
    private final JwkProviderService access;
    private final AttemptThrottlerService limiter;
    private final PasswordEncoder passwords;
    private final RefreshTokenService tokenCrypto;
    private final Clock clock;

    @Override
    @Transactional(noRollbackFor = AuthFailedException.class)
    public SessionTokens login(String sourceAddress, String tenant, String email, String password) {
        AttemptTicket attempt = limiter.acquire(sourceAddress, tenant, email);
        var user = users.findByTenantSlugAndEmailAndEnabledTrue(
                tenant.toLowerCase(java.util.Locale.ROOT), email.toLowerCase(java.util.Locale.ROOT));
        boolean valid = passwords.matches(password, user.map(User::getPasswordHash).orElse(DUMMY_PASSWORD_HASH));
        if (user.isEmpty() || !valid) {
            limiter.failed(attempt);
            throw AuthFailedException.invalid("Invalid credentials");
        }
        limiter.succeeded(attempt);
        return mint(user.orElseThrow(), UUID.randomUUID());
    }

    @Override
    @Transactional(noRollbackFor = AuthFailedException.class)
    public SessionTokens refresh(String rawToken) {
        if (rawToken == null || rawToken.length() < 32) throw AuthFailedException.invalid("Invalid Refresh Token");
        RefreshSession session = sessionsRepository.findByTokenHash(tokenCrypto.hashToken(rawToken))
                .orElseThrow(()-> AuthFailedException.invalid("Refresh Token does not exist"));
        Instant now = clock.instant();
        if (session.getConsumedAt() != null) {
            sessionsRepository.revokeFamily(session.getTenantId(), session.getFamilyId(), now);
            throw AuthFailedException.invalid("Refresh Token has expired");
        }
        if (session.getRevokedAt() != null || !session.getExpiresAt().isAfter(now)) throw AuthFailedException.invalid("Session expired");
        User user = users.findByIdAndTenantIdAndEnabledTrue(session.getUserId(), session.getTenantId())
                .orElseThrow(()-> AuthFailedException.invalid("User does not exist"));
        session.consume(now);
        return mint(user, session.getFamilyId());
    }

    @Override
    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.length() < 32) throw AuthFailedException.invalid("Invalid Refresh Token");
        RefreshSession session = sessionsRepository.findByTokenHash(tokenCrypto.hashToken(rawToken))
                .orElseThrow(()-> AuthFailedException.invalid("Refresh Token does not exist"));
        sessionsRepository.revokeFamily(session.getTenantId(), session.getFamilyId(), clock.instant());
    }



    private SessionTokens mint(User user, UUID family) {
        String rawToken = tokenCrypto.generateRawToken();
        sessionsRepository.save(new RefreshSession(user.getId(), user.getTenantId(), family, tokenCrypto.hashToken(rawToken),
                clock.instant().plus(AuthTokenPolicy.REFRESH_LIFETIME)));
        return new SessionTokens(access.issue(user), rawToken);
    }
}
