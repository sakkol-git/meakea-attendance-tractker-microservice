package com.hrflow.identityorg.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "refresh_session", schema = "identity_org")
public class RefreshSession {
    @Id public UUID id;
    public UUID userId;
    public UUID familyId;
    @Column(columnDefinition = "char(64)")
    @JdbcTypeCode(SqlTypes.CHAR)
    public String tokenHash;
    public Instant expiresAt;
    public Instant consumedAt;
    public Instant revokedAt;

    protected RefreshSession() {}

    public RefreshSession(UUID userId, UUID familyId, String tokenHash, Instant expiresAt) {
        id = UUID.randomUUID();
        this.userId = userId;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }
}
