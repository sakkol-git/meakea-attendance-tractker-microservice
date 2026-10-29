package com.hrflow.identityorg.organization.service;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class OrganizationOutbox {
    private final EntityManager entityManager;
    private final JsonMapper json;

    /** Append the owner fact in the same transaction as the aggregate change. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void append(UUID tenantId, String topic, UUID aggregateId, String type,
            OrganizationSnapshots.Snapshot payload) {
        Number revision = (Number) entityManager.createNativeQuery("""
                INSERT INTO identity_org.organization_revision(tenant_id, topic, aggregate_id, revision)
                VALUES (:tenant, :topic, :aggregate, 1)
                ON CONFLICT (tenant_id, topic, aggregate_id)
                DO UPDATE SET revision = identity_org.organization_revision.revision + 1
                RETURNING revision
                """)
                .setParameter("tenant", tenantId).setParameter("topic", topic)
                .setParameter("aggregate", aggregateId).getSingleResult();
        entityManager.createNativeQuery("""
                INSERT INTO identity_org.organization_outbox
                    (id, tenant_id, topic, aggregate_id, aggregate_revision, event_type,
                     correlation_id, payload, occurred_at)
                VALUES (:id, :tenant, :topic, :aggregate, :revision, :type,
                        :correlation, cast(:payload as jsonb), :occurred)
                """)
                .setParameter("id", UUID.randomUUID())
                .setParameter("tenant", tenantId).setParameter("topic", topic)
                .setParameter("aggregate", aggregateId).setParameter("revision", revision.longValue())
                .setParameter("type", type).setParameter("correlation", UUID.randomUUID())
                .setParameter("payload", json.writeValueAsString(payload))
                .setParameter("occurred", Instant.now()).executeUpdate();
    }
}
