package com.hrflow.identityorg.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "auth_attempt", schema = "identity_org")
public class AuthAttempt {
    @Id @Column(length = 64) public String id;
    public int failures;
    public Instant windowStarted;
    public Instant blockedUntil;

    protected AuthAttempt() {}
}
