package com.hrflow.attendance.events;

import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** Owner-local, single-leader, acknowledged publication with UUID-only tenant discovery. */
@Component
@Slf4j
@ConditionalOnProperty(name = "hrflow.attendance.relay.enabled", havingValue = "true")
public class AttendanceOutboxRelay {
    private final com.hrflow.attendance.jobs.AttendanceJobLeader leader;
    private final TransactionTemplate transactions;
    private final com.hrflow.attendance.service.TenantSqlScope scope;
    private final EntityManager entityManager;
    private final AttendanceEventCodec codec;
    private final KafkaTemplate<Object, Object> kafka;

    public AttendanceOutboxRelay(@Value("${spring.datasource.url}") String url,
            @Value("${hrflow.attendance.relay.job-user:}") String jobUser,
            @Value("${hrflow.attendance.relay.job-password:}") String jobPassword,
            TransactionTemplate transactions, com.hrflow.attendance.service.TenantSqlScope scope,
            EntityManager entityManager, AttendanceEventCodec codec, KafkaTemplate<Object, Object> kafka) {
        this.leader = new com.hrflow.attendance.jobs.AttendanceJobLeader(url, jobUser, jobPassword, 73104218L);
        this.transactions = transactions;
        this.scope = scope;
        this.entityManager = entityManager;
        this.codec = codec;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${hrflow.attendance.relay.poll-ms:250}",
            initialDelayString = "${hrflow.attendance.relay.initial-delay-ms:1000}")
    public synchronized void poll() {
        try {
            UUID tenant = leader.nextTenant();
            if (tenant != null) {
                UUID selected = tenant;
                transactions.executeWithoutResult(ignored -> publishOne(selected));
            }
        } catch (Exception failure) {
            if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("Attendance publication remains pending after {}", failure.getClass().getSimpleName());
            closeLeader();
        }
    }

    private void publishOne(UUID tenant) {
        scope.use(tenant);
        @SuppressWarnings("unchecked")
        List<Object[]> pending = entityManager.createNativeQuery("""
                SELECT c.id,c.event_id,c.tenant_id,c.topic,c.event_key,c.aggregate_id,
                       c.aggregate_revision,c.event_type,c.correlation_id,c.occurred_at,c.payload::text
                FROM attendance.attendance_outbox c
                WHERE c.tenant_id=:tenant AND c.schema_version=1 AND c.sent_at IS NULL
                  AND NOT EXISTS (SELECT 1 FROM attendance.attendance_outbox earlier
                    WHERE earlier.tenant_id=c.tenant_id AND earlier.event_key=c.event_key
                      AND earlier.schema_version=1 AND earlier.sent_at IS NULL
                      AND earlier.stream_position<c.stream_position)
                ORDER BY c.stream_position LIMIT 1 FOR UPDATE OF c SKIP LOCKED
                """).setParameter("tenant", tenant).getResultList();
        if (pending.isEmpty()) return;
        Object[] row = pending.getFirst();
        var stored = new StoredAttendanceEvent((UUID) row[0], (UUID) row[1], (UUID) row[2],
                (String) row[3], (String) row[4], (UUID) row[5], ((Number) row[6]).longValue(),
                (String) row[7], (UUID) row[8], instant(row[9]), (String) row[10]);
        try {
            if (!leader.valid()) throw new SQLException("Attendance relay lost leadership");
            kafka.send(stored.topic(), stored.key(), codec.encode(stored)).get(3, TimeUnit.SECONDS);
            if (!leader.valid()) throw new SQLException("Attendance relay lost leadership");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Attendance publication interrupted", interrupted);
        } catch (Exception failure) {
            throw new IllegalStateException("Attendance broker acknowledgment unavailable", failure);
        }
        entityManager.createNativeQuery("""
                UPDATE attendance.attendance_outbox SET sent_at=now()
                WHERE tenant_id=:tenant AND id=:id AND sent_at IS NULL
                """).setParameter("tenant", tenant).setParameter("id", stored.id()).executeUpdate();
    }

    private static Instant instant(Object value) {
        if (value instanceof Instant time) return time;
        if (value instanceof Timestamp time) return time.toInstant();
        if (value instanceof OffsetDateTime time) return time.toInstant();
        throw new IllegalArgumentException("Invalid outbox timestamp");
    }

    @PreDestroy
    public synchronized void closeLeader() {
        try { leader.close(); }
        catch (SQLException ignored) { log.warn("Attendance leader connection failed to close"); }
    }
}
