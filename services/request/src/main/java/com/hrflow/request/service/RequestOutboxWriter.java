package com.hrflow.request.service;

import com.hrflow.request.entity.GeneralRequest;
import com.hrflow.request.events.RequestSnapshotMapper;
import com.hrflow.request.repository.ApprovalStepRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class RequestOutboxWriter {
    private final EntityManager entities;
    private final JsonMapper json;
    private final RequestSnapshotMapper mapper;
    private final ApprovalStepRepository steps;

    @Transactional(propagation = Propagation.MANDATORY)
    public void snapshot(String tenant, String id, String action, String account) {
        entities.flush();
        var request = entities.find(GeneralRequest.class, id);
        if (request == null || !tenant.equals(request.getTenantId()) || account == null || account.isBlank()) {
            throw new IllegalArgumentException("Request owner or account provenance mismatch");
        }
        var body = mapper.snapshot(request, steps.findByTenantIdAndRequestIdOrderByStepOrderAsc(tenant, id));
        String key = tenant + ":" + id;
        entities.createNativeQuery("SELECT pg_advisory_xact_lock(hashtextextended(:key,0))")
                .setParameter("key", "request.stream:" + key).getSingleResult();
        UUID event = UUID.randomUUID();
        Instant now = Instant.now();
        long revision = Math.addExact(request.getVersion(), 1L);
        entities.createNativeQuery("""
                INSERT INTO request.request_outbox(id,tenant_id,aggregate_type,aggregate_id,event_type,payload,revision,
                    status,wire_version,topic,event_key,correlation_id,occurred_at,actor_account_id)
                VALUES (:event,:tenant,'GeneralRequest',:id,:action,cast(:payload as jsonb),:revision,
                    'PENDING',1,'request.workflow.v1',:key,:correlation,:occurred,:account)
                """).setParameter("event", event.toString()).setParameter("tenant", tenant).setParameter("id", id)
                .setParameter("action", action).setParameter("payload", json.writeValueAsString(body)).setParameter("revision", revision)
                .setParameter("key", key).setParameter("correlation", UUID.randomUUID()).setParameter("occurred", now)
                .setParameter("account", account).executeUpdate();
        entities.createNativeQuery("""
                INSERT INTO request.workflow_history(tenant_id,event_id,request_id,revision,actor_account_id,action,state,occurred_at)
                VALUES (:tenant,:event,:request,:revision,:account,:action,:state,:at)
                """).setParameter("tenant", tenant).setParameter("event", event).setParameter("request", id)
                .setParameter("revision", revision).setParameter("account", account).setParameter("action", action)
                .setParameter("state", request.getStatus()).setParameter("at", now).executeUpdate();
    }
}
