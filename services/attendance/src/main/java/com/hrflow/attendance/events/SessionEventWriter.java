package com.hrflow.attendance.events;

import com.hrflow.attendance.entity.AttendanceSession;
import com.hrflow.attendance.service.TenantSqlScope;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class SessionEventWriter {
    public static final String TOPIC = "attendance.session.v1";
    public static final String TYPE = "AttendanceSessionSnapshot.v1";
    private final EntityManager entityManager;
    private final TenantSqlScope scope;
    private final SessionPayloadMapper mapper;
    private final JsonMapper json;
    private final org.springframework.beans.factory.ObjectProvider<com.hrflow.attendance.daily.DailyStatusWriter> daily;

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(AttendanceSession session, String source) {
        if (session.getWorkDate() == null || !Set.of("TAP", "MANUAL").contains(source)) {
            throw new IllegalStateException("A session fact requires a captured schedule basis and explicit source");
        }
        scope.use(session.getTenantId());
        scope.lockEmployee(session.getTenantId(), session.getEmployeeId());
        // Serialize stream insertion through commit, including distinct sessions for one employee.
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtextextended(:stream, 0))")
                .setParameter("stream", "attendance.stream:" + session.getTenantId() + ":" + session.getEmployeeId())
                .getSingleResult();
        long revision = Math.addExact(session.getVersion(), 1L);
        // Identity/revision metadata belongs here; MapStruct only copies the approved payload.
        entityManager.createNativeQuery("""
                INSERT INTO attendance.attendance_outbox
                    (id,tenant_id,topic,event_id,aggregate_id,aggregate_revision,payload,
                     schema_version,event_type,correlation_id,occurred_at,event_key)
                VALUES (:id,:tenant,:topic,:event,:aggregate,:revision,cast(:payload as jsonb),
                        1,:type,:correlation,:occurred,:key)
                """).setParameter("id", UUID.randomUUID()).setParameter("tenant", session.getTenantId())
                .setParameter("topic", TOPIC).setParameter("event", UUID.randomUUID())
                .setParameter("aggregate", session.getId()).setParameter("revision", revision)
                .setParameter("payload", json.writeValueAsString(mapper.snapshot(session, source)))
                .setParameter("type", TYPE).setParameter("correlation", UUID.randomUUID())
                .setParameter("occurred", Instant.now())
                .setParameter("key", session.getTenantId() + ":" + session.getEmployeeId()).executeUpdate();
        daily.ifAvailable(writer -> writer.refresh(session.getTenantId(), session.getEmployeeId(), session.getWorkDate()));
    }
}
