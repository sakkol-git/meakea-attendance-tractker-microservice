package com.hrflow.leave.events;

import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
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

/** Single-leader owner relay: private metadata discovery, scoped payload work, broker acknowledgment before commit. */
@Component
@Slf4j
@ConditionalOnProperty(name = "hrflow.leave.relay.enabled", havingValue = "true")
public class LeaveOutboxRelay {
    private final String url, user, password;
    private final TransactionTemplate transactions;
    private final com.hrflow.leave.service.TenantSqlScope scope;
    private final EntityManager entityManager;
    private final LeaveEventCodec codec;
    private final KafkaTemplate<Object, Object> kafka;
    private Connection leader;
    private String lastTenant;

    public LeaveOutboxRelay(@Value("${spring.datasource.url}") String url,
            @Value("${hrflow.leave.relay.job-user:}") String user,
            @Value("${hrflow.leave.relay.job-password:}") String password, TransactionTemplate transactions,
            com.hrflow.leave.service.TenantSqlScope scope, EntityManager entityManager,
            LeaveEventCodec codec, KafkaTemplate<Object, Object> kafka) {
        if (user.isBlank() || password.isBlank()) throw new IllegalStateException("Private leave job role required");
        this.url = url;
        this.user = user;
        this.password = password;
        this.transactions = transactions;
        this.scope = scope;
        this.entityManager = entityManager;
        this.codec = codec;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${hrflow.leave.relay.poll-ms:250}", initialDelayString = "${hrflow.leave.relay.initial-delay-ms:1000}")
    public synchronized void poll() {
        try {
            if (!lead()) return;
            String tenant = next(lastTenant);
            if (tenant == null) tenant = next(null);
            lastTenant = tenant;
            if (tenant != null) {
                String selected = tenant;
                transactions.executeWithoutResult(tx -> publish(selected));
            }
        } catch (Exception failure) {
            log.warn("Leave publication remains pending after {}", failure.getClass().getSimpleName());
            closeLeader();
        }
    }

    private boolean lead() throws SQLException {
        if (leader != null && leader.isValid(2)) return true;
        closeLeader();
        var properties = new java.util.Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", password);
        properties.setProperty("connectTimeout", "5");
        properties.setProperty("socketTimeout", "5");
        Connection candidate = DriverManager.getConnection(url, properties);
        try (var statement = candidate.prepareStatement("SELECT pg_try_advisory_lock(73104220)")) {
            try (var rows = statement.executeQuery()) {
                rows.next();
                if (!rows.getBoolean(1)) { candidate.close(); return false; }
            }
        } catch (SQLException failure) { candidate.close(); throw failure; }
        leader = candidate;
        return true;
    }

    private String next(String cursor) throws SQLException {
        try (var statement = leader.prepareStatement("""
                SELECT tenant_id FROM leave_jobs.tenant_registry
                WHERE (cast(? as text) IS NULL OR tenant_id>?) ORDER BY tenant_id LIMIT 1
                """)) {
            statement.setString(1, cursor);
            statement.setString(2, cursor);
            try (var rows = statement.executeQuery()) { return rows.next() ? rows.getString(1) : null; }
        }
    }

    private void publish(String tenant) {
        scope.use(tenant);
        @SuppressWarnings("unchecked")
        List<Object[]> pending = entityManager.createNativeQuery("""
                SELECT c.id,c.aggregate_id,c.revision,c.event_key,c.correlation_id,c.occurred_at,c.payload::text
                FROM leave.leave_outbox c WHERE c.tenant_id=:tenant AND c.wire_version=1 AND c.status='PENDING'
                  AND NOT EXISTS (SELECT 1 FROM leave.leave_outbox earlier WHERE earlier.tenant_id=c.tenant_id
                    AND earlier.wire_version=1 AND earlier.status='PENDING' AND earlier.event_key=c.event_key
                    AND earlier.stream_position<c.stream_position)
                ORDER BY c.stream_position LIMIT 1 FOR UPDATE OF c SKIP LOCKED
                """).setParameter("tenant", tenant).getResultList();
        if (pending.isEmpty()) return;
        Object[] row = pending.getFirst();
        var stored = new LeaveEventCodec.Stored((String) row[0], tenant, (String) row[1],
                ((Number) row[2]).longValue(), (String) row[3], (UUID) row[4], instant(row[5]), (String) row[6]);
        try {
            if (!leader.isValid(1)) throw new SQLException("Leave relay leadership lost");
            kafka.send("leave.request.v1", stored.key(), codec.encode(stored)).get(3, TimeUnit.SECONDS);
            if (!leader.isValid(1)) throw new SQLException("Leave relay leadership lost");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Leave publication interrupted", interrupted);
        } catch (Exception failure) { throw new IllegalStateException("Leave broker acknowledgment unavailable", failure); }
        entityManager.createNativeQuery("UPDATE leave.leave_outbox SET status='PUBLISHED',published_at=now() WHERE tenant_id=:tenant AND id=:id AND status='PENDING'")
                .setParameter("tenant", tenant).setParameter("id", stored.id()).executeUpdate();
    }

    private static Instant instant(Object value) {
        if (value instanceof Instant time) return time;
        if (value instanceof java.sql.Timestamp time) return time.toInstant();
        if (value instanceof java.time.OffsetDateTime time) return time.toInstant();
        throw new IllegalArgumentException("Invalid leave event timestamp");
    }

    @PreDestroy public synchronized void closeLeader() {
        if (leader != null) {
            try { leader.close(); }
            catch (SQLException ignored) { log.warn("Leave leader connection failed to close"); }
        }
        leader = null;
    }
}
