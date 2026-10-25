package com.hrflow.identityorg.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AttemptLimiter {
    private final AuthAttemptRepository attempts;

    public AttemptLimiter(AuthAttemptRepository attempts) {
        this.attempts = attempts;
    }

    AuthAttempt acquire(String tenant, String email) {
        Instant now = Instant.now();
        String id = hash(tenant.toLowerCase() + ":" + email.toLowerCase());
        attempts.ensure(id, now);
        AuthAttempt row = attempts.findLocked(id).orElseThrow();
        if (row.blockedUntil != null && row.blockedUntil.isAfter(now)) {
            throw new AuthFailure(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_ATTEMPTS",
                    "Wait before trying to sign in again.");
        }
        if (row.windowStarted.isBefore(now.minus(15, ChronoUnit.MINUTES))) {
            row.windowStarted = now;
            row.failures = 0;
            row.blockedUntil = null;
        }
        return row;
    }

    void failed(AuthAttempt row) {
        row.failures++;
        if (row.failures >= 5) row.blockedUntil = Instant.now().plus(15, ChronoUnit.MINUTES);
    }

    void succeeded(AuthAttempt row) {
        row.failures = 0;
        row.blockedUntil = null;
        row.windowStarted = Instant.now();
    }

    private static String hash(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }
}
