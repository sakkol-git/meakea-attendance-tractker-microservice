package com.hrflow.identityorg.organization.service;

import com.hrflow.identityorg.organization.service.OrganizationFactMapper.OutboxEntry;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

/** One active relay; the dedicated job connection owns leadership and UUID-only discovery. */
@Slf4j
@Component
@ConditionalOnProperty(name = "hrflow.organization.relay.enabled", havingValue = "true")
public class OrganizationOutboxRelay {
    private static final int LEADER_LOCK = 73104217;
    private final String jdbcUrl;
    private final String jobUser;
    private final String jobPassword;
    private final TransactionTemplate transactions;
    private final TenantSqlScope scope;
    private final EntityManager entityManager;
    private final OrganizationFactMapper mapper;
    private final KafkaTemplate<Object, Object> kafka;
    private Connection leader;
    private UUID lastTenant;

    public OrganizationOutboxRelay(@Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${hrflow.organization.relay.job-user:}") String jobUser,
            @Value("${hrflow.organization.relay.job-password:}") String jobPassword,
            TransactionTemplate transactions, TenantSqlScope scope, EntityManager entityManager,
            OrganizationFactMapper mapper, KafkaTemplate<Object, Object> kafka) {
        if (jobUser.isBlank() || jobPassword.isBlank()) {
            throw new IllegalStateException("Organization relay requires a private job database role");
        }
        this.jdbcUrl = jdbcUrl;
        this.jobUser = jobUser;
        this.jobPassword = jobPassword;
        this.transactions = transactions;
        this.scope = scope;
        this.entityManager = entityManager;
        this.mapper = mapper;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${hrflow.organization.relay.poll-ms:250}",
            initialDelayString = "${hrflow.organization.relay.initial-delay-ms:1000}")
    public synchronized void poll() {
        try {
            if (!lead()) {
                log.debug("Organization relay standby");
                return;
            }
            UUID tenant = nextTenant();
            if (tenant == null) {
                log.debug("Organization relay has no registered tenant");
                return;
            }
            transactions.executeWithoutResult(status -> publishOne(tenant));
        } catch (Exception error) {
            log.warn("Organization relay stopped this poll; pending fact remains retryable", error);
            closeLeader();
        }
    }

    private boolean lead() throws SQLException {
        if (leader != null && leader.isValid(2)) return true;
        closeLeader();
        Connection candidate = DriverManager.getConnection(jdbcUrl, jobUser, jobPassword);
        try (var query = candidate.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
            query.setInt(1, LEADER_LOCK);
            try (var rows = query.executeQuery()) {
                rows.next();
                if (!rows.getBoolean(1)) {
                    candidate.close();
                    return false;
                }
            }
        } catch (SQLException error) {
            candidate.close();
            throw error;
        }
        leader = candidate;
        return true;
    }

    private UUID nextTenant() throws SQLException {
        UUID next = findTenantAfter(lastTenant);
        if (next == null) next = findTenantAfter(null);
        lastTenant = next;
        return next;
    }

    private UUID findTenantAfter(UUID cursor) throws SQLException {
        String sql = """
                SELECT tenant_id FROM identity_jobs.organization_tenant_registry
                WHERE lifecycle = 'ACTIVE' AND (?::uuid IS NULL OR tenant_id > ?::uuid)
                ORDER BY tenant_id LIMIT 1
                """;
        try (PreparedStatement query = leader.prepareStatement(sql)) {
            query.setObject(1, cursor);
            query.setObject(2, cursor);
            try (ResultSet rows = query.executeQuery()) {
                return rows.next() ? rows.getObject(1, UUID.class) : null;
            }
        }
    }

    private void publishOne(UUID tenant) {
        scope.use(tenant);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT id, tenant_id, topic, aggregate_id, aggregate_revision, event_type,
                       correlation_id, causation_id, payload::text, occurred_at
                FROM identity_org.organization_outbox current_fact
                WHERE tenant_id = :tenant AND published_at IS NULL
                  AND NOT EXISTS (
                    SELECT 1 FROM identity_org.organization_outbox earlier
                    WHERE earlier.tenant_id = current_fact.tenant_id
                      AND earlier.topic = current_fact.topic
                      AND earlier.aggregate_id = current_fact.aggregate_id
                      AND earlier.aggregate_revision < current_fact.aggregate_revision
                      AND earlier.published_at IS NULL)
                ORDER BY occurred_at, id LIMIT 1 FOR UPDATE OF current_fact SKIP LOCKED
                """).setParameter("tenant", tenant).getResultList();
        if (rows.isEmpty()) {
            log.debug("Organization relay found no pending row for registered tenant");
            return;
        }
        Object[] value = rows.getFirst();
        OutboxEntry fact = new OutboxEntry((UUID) value[0], (UUID) value[1], (String) value[2],
                (UUID) value[3], ((Number) value[4]).longValue(), (String) value[5],
                (UUID) value[6], (UUID) value[7], (String) value[8],
                instant(value[9]));
        try {
            kafka.send(fact.topic(), fact.tenantId() + ":" + fact.aggregateId(), mapper.map(fact))
                    .get(3, TimeUnit.SECONDS);
        } catch (Exception error) {
            throw new IllegalStateException("Broker did not acknowledge organization fact", error);
        }
        entityManager.createNativeQuery("""
                UPDATE identity_org.organization_outbox
                SET published_at = now(), delivery_attempts = delivery_attempts + 1
                WHERE tenant_id = :tenant AND id = :id AND published_at IS NULL
                """).setParameter("tenant", tenant).setParameter("id", fact.id()).executeUpdate();
    }

    private Instant instant(Object value) {
        if (value instanceof OffsetDateTime dateTime) return dateTime.toInstant();
        if (value instanceof Timestamp timestamp) return timestamp.toInstant();
        if (value instanceof Instant result) return result;
        throw new IllegalArgumentException("Outbox timestamp has an unsupported type");
    }

    @PreDestroy
    public synchronized void closeLeader() {
        if (leader == null) return;
        try {
            leader.close();
        } catch (SQLException ignored) {
            log.warn("Organization relay leader connection failed to close");
        } finally {
            leader = null;
        }
    }
}
