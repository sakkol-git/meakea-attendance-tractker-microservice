package com.hrflow.leave.service;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeaveOutboxWriter {

    private final EntityManager entityManager;
    private final JsonMapper jsonMapper;
    private final com.hrflow.leave.events.LeaveSnapshotMapper snapshots;

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordEvent(String tenantId, String aggregateType, String aggregateId,
                            String eventType, Object payload, long revision) {
        if ("LEAVE_REQUEST".equals(aggregateType) && payload instanceof com.hrflow.leave.entity.LeaveRequest) {
            recordSnapshot(tenantId, aggregateId, eventType);
            return;
        }
        String payloadJson = jsonMapper.writeValueAsString(payload);
        entityManager.createNativeQuery("""
                INSERT INTO leave.leave_outbox
                    (id, tenant_id, aggregate_type, aggregate_id, event_type, payload, revision, status)
                VALUES
                    (:id, :tenantId, :aggType, :aggId, :eventType, CAST(:payload AS JSONB), :rev, 'PENDING')
                """)
                .setParameter("id", UUID.randomUUID().toString())
                .setParameter("tenantId", tenantId)
                .setParameter("aggType", aggregateType)
                .setParameter("aggId", aggregateId)
                .setParameter("eventType", eventType)
                .setParameter("payload", payloadJson)
                .setParameter("rev", revision)
                .executeUpdate();
    }

    private void recordSnapshot(String tenant, String id, String ownerEventType) {
        // Flush/inspect the managed owner row; caller-supplied stale revisions or detached objects are not wire authority.
        entityManager.flush();
        var request = entityManager.find(com.hrflow.leave.entity.LeaveRequest.class, id);
        if (request == null || !tenant.equals(request.getTenantId())) throw new IllegalArgumentException("Leave owner identity mismatch");
        var dates = entityManager.createQuery("SELECT d.date FROM LeaveDay d WHERE d.tenantId=:tenant AND d.requestId=:id ORDER BY d.date",
                java.time.LocalDate.class).setParameter("tenant", tenant).setParameter("id", id).getResultList();
        String key = tenant + ":" + request.getEmployeeId();
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtextextended(:key,0))")
                .setParameter("key", "leave.stream:" + key).getSingleResult();
        entityManager.createNativeQuery("""
                INSERT INTO leave.leave_outbox(id,tenant_id,aggregate_type,aggregate_id,event_type,payload,revision,
                    status,wire_version,topic,event_key,correlation_id,occurred_at)
                VALUES (:event,:tenant,'LEAVE_REQUEST',:id,:type,cast(:payload as jsonb),:revision,
                    'PENDING',1,'leave.request.v1',:key,:correlation,:occurred)
                """).setParameter("event", UUID.randomUUID().toString()).setParameter("tenant", tenant)
                .setParameter("id", id).setParameter("type", ownerEventType)
                .setParameter("payload", jsonMapper.writeValueAsString(snapshots.snapshot(request, dates)))
                .setParameter("revision", Math.addExact(request.getVersion(), 1L)).setParameter("key", key)
                .setParameter("correlation", UUID.randomUUID()).setParameter("occurred", java.time.Instant.now()).executeUpdate();
    }
}
