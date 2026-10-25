package com.hrflow.identityorg.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthSessions {
    private final AuthUserRepository users;
    private final RefreshSessionRepository sessions;
    private final AccessTokens access;
    private final AttemptLimiter limiter;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
    private final String dummyHash = passwords.encode("unused-credential");
    private final SecureRandom random = new SecureRandom();

    public AuthSessions(AuthUserRepository users, RefreshSessionRepository sessions,
                        AccessTokens access, AttemptLimiter limiter) {
        this.users = users;
        this.sessions = sessions;
        this.access = access;
        this.limiter = limiter;
    }

    @Transactional(noRollbackFor = AuthFailure.class)
    public TokenPair login(String tenant, String email, String password) {
        AuthAttempt attempt = limiter.acquire(tenant, email);
        AuthUser user = users.findByTenantSlugAndEmailAndEnabledTrue(
                tenant.toLowerCase(), email.toLowerCase()).orElse(null);
        boolean valid = passwords.matches(password, user == null ? dummyHash : user.passwordHash);
        if (!valid || user == null) {
            limiter.failed(attempt);
            throw AuthFailure.invalid();
        }
        limiter.succeeded(attempt);
        return mint(user, UUID.randomUUID());
    }

    @Transactional(noRollbackFor = AuthFailure.class)
    public TokenPair refresh(String rawToken) {
        if (rawToken == null || rawToken.length() < 32) throw AuthFailure.invalid();
        RefreshSession session = sessions.findByTokenHash(hash(rawToken)).orElseThrow(AuthFailure::invalid);
        Instant now = Instant.now();
        if (session.consumedAt != null) {
            sessions.revokeFamily(session.familyId, now);
            throw AuthFailure.invalid();
        }
        if (session.revokedAt != null || !session.expiresAt.isAfter(now)) throw AuthFailure.invalid();
        AuthUser user = users.findById(session.userId).filter(value -> value.enabled)
                .orElseThrow(AuthFailure::invalid);
        session.consumedAt = now;
        return mint(user, session.familyId);
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.length() < 32) throw AuthFailure.invalid();
        RefreshSession session = sessions.findByTokenHash(hash(rawToken)).orElseThrow(AuthFailure::invalid);
        sessions.revokeFamily(session.familyId, Instant.now());
    }

    public Map<String, Object> me(UUID userId, org.springframework.security.oauth2.jwt.Jwt jwt) {
        AuthUser user = users.findById(userId).orElseThrow(AuthFailure::invalid);
        if (!access.matches(jwt, user)) throw AuthFailure.invalid();
        return access.profile(user);
    }

    private TokenPair mint(AuthUser user, UUID family) {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.save(new RefreshSession(user.id, family, hash(raw), Instant.now().plusSeconds(30L * 86400)));
        return new TokenPair(access.issue(user), raw);
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    public record TokenPair(String accessToken, String refreshToken) {}
}
