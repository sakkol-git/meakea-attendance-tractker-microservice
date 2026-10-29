package com.hrflow.identityorg.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_log", schema = "identity_org")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdentityAudit {
    @Id private UUID id;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(name = "actor_id") private UUID actorId;
    @Column(nullable = false) private String action;
    @Column(nullable = false) private String entity;
    @Column(name = "entity_id") private UUID entityId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb") private Map<String, Object> diff;
    @Column(nullable = false) private Instant at;
    @Column(name = "correlation_id", nullable = false) private UUID correlationId;

    public IdentityAudit(UUID tenantId, UUID actorId, String action, String entity,
            UUID entityId, Map<String, Object> diff, Instant at) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.actorId = actorId;
        this.action = action;
        this.entity = entity;
        this.entityId = entityId;
        this.diff = Map.copyOf(diff);
        this.at = at;
        this.correlationId = UUID.randomUUID();
    }
}
