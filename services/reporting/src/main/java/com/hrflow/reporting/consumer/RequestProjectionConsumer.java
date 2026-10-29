package com.hrflow.reporting.consumer;

import com.hrflow.contracts.request.RequestWorkflowFact;
import com.hrflow.contracts.request.RequestWorkflowWire;
import com.hrflow.reporting.mapper.RequestReportMapper;
import com.hrflow.reporting.service.TenantSqlScope;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RequestProjectionConsumer {
    private final EntityManager entities;
    private final TenantSqlScope scope;
    private final RequestReportMapper mapper;

    @Transactional
    public void apply(ConsumerRecord<String, RequestWorkflowFact> record) {
        if (!"request.workflow.v1".equals(record.topic()) || record.partition() < 0 || record.offset() < 0) {
            throw new IllegalArgumentException("Invalid request topic or position");
        }
        var id = RequestWorkflowWire.validate(record.key(), record.value());
        var event = mapper.event(record.value());
        scope.use(id.tenant());
        entities.createNativeQuery("SELECT pg_advisory_xact_lock(hashtextextended(:key,0))")
                .setParameter("key", "reporting.request:" + id.tenant() + ":" + id.request()).getSingleResult();
        @SuppressWarnings("unchecked")
        java.util.List<Object[]> existing = entities.createNativeQuery("""
                SELECT requester_id,request_type,created_at FROM reporting.request_fact_projection
                WHERE tenant_id=:tenant AND request_id=:request
                """).setParameter("tenant", id.tenant()).setParameter("request", id.request()).getResultList();
        if (!existing.isEmpty() && existing.getFirst()[2] != null) {
            Object[] previous = existing.getFirst();
            var original = instant(previous[2]);
            if (!id.employee().equals(previous[0]) || !event.requestType().equals(previous[1]) || !event.createdAt().equals(original)) {
                throw new IllegalArgumentException("Immutable request owner basis changed");
            }
        }
        int receipt = entities.createNativeQuery("""
                INSERT INTO reporting.reporting_inbox(consumer_name,tenant_id,event_id,topic,aggregate_id,aggregate_revision)
                VALUES ('reporting-request-v1',:tenant,:event,'request.workflow.v1',:request,:revision) ON CONFLICT DO NOTHING
                """).setParameter("tenant", id.tenant()).setParameter("event", id.event()).setParameter("request", id.request())
                .setParameter("revision", event.aggregateRevision()).executeUpdate();
        if (receipt == 1) entities.createNativeQuery("""
                INSERT INTO reporting.request_fact_projection(tenant_id,request_id,requester_id,request_type,status,aggregate_revision,created_at)
                VALUES (:tenant,:request,:employee,:type,:state,:revision,:created)
                ON CONFLICT(tenant_id,request_id) DO UPDATE SET requester_id=EXCLUDED.requester_id,request_type=EXCLUDED.request_type,
                    status=EXCLUDED.status,aggregate_revision=EXCLUDED.aggregate_revision,created_at=EXCLUDED.created_at,updated_at=now()
                WHERE reporting.request_fact_projection.aggregate_revision<EXCLUDED.aggregate_revision
                   OR (reporting.request_fact_projection.aggregate_revision=EXCLUDED.aggregate_revision AND reporting.request_fact_projection.created_at IS NULL)
                """).setParameter("tenant", id.tenant()).setParameter("request", id.request()).setParameter("employee", id.employee())
                .setParameter("type", event.requestType()).setParameter("state", event.status()).setParameter("revision", event.aggregateRevision())
                .setParameter("created", event.createdAt()).executeUpdate();
        entities.createNativeQuery("""
                INSERT INTO reporting.checkpoint_progress(tenant_id,topic,partition_num,last_offset,last_processed_at,pending_intents)
                VALUES (:tenant,'request.workflow.v1',:partition,:offset,now(),0)
                ON CONFLICT(tenant_id,topic,partition_num) DO UPDATE SET last_offset=EXCLUDED.last_offset,last_processed_at=EXCLUDED.last_processed_at
                WHERE reporting.checkpoint_progress.last_offset<EXCLUDED.last_offset
                """).setParameter("tenant", id.tenant()).setParameter("partition", record.partition()).setParameter("offset", record.offset()).executeUpdate();
    }
    private static java.time.Instant instant(Object value) {
        if (value instanceof java.time.Instant time) return time;
        if (value instanceof java.sql.Timestamp time) return time.toInstant();
        if (value instanceof java.time.OffsetDateTime time) return time.toInstant();
        throw new IllegalArgumentException("Invalid original request timestamp");
    }

}
