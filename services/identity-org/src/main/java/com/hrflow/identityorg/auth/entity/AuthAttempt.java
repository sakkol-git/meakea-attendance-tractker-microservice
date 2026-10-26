package com.hrflow.identityorg.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auth_attempt", schema = "identity_org")
@Getter
@NoArgsConstructor
public class AuthAttempt {
    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "failures", nullable = false)
    private int failures;

    @Column(name = "window_started")
    private Instant windowStarted;

    @Column(name = "blocked_until")
    private Instant blockedUntil;

    public AuthAttempt(String id, Instant windowStarted) {
        this.id = id;
        this.windowStarted = windowStarted;
    }

    public void recordFailure(Instant now, int threshold, java.time.Duration window) {
        if (windowStarted == null || !windowStarted.plus(window).isAfter(now)) {
            windowStarted = now;
            failures = 1;
            blockedUntil = null;
        } else {
            failures++;
        }
        if (failures >= threshold) blockedUntil = now.plus(window);
    }

    public void reset(Instant now) {
        failures = 0;
        blockedUntil = null;
        windowStarted = now;
    }
}
