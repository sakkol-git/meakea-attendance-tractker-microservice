package com.hrflow.notification.outbox;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOutboxStore implements OutboxStore {

    private final JdbcTemplate jdbc;

    public JdbcOutboxStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(OutboxRecord record) {
        jdbc.update(
                """
                INSERT INTO notification.notification_outbox
                    (id, tenant_id, topic, aggregate_id, event_type, aggregate_revision, payload, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?)
                """,
                record.eventId(),
                record.tenantId(),
                record.topic(),
                record.aggregateId(),
                record.topic(),
                record.aggregateRevision(),
                record.payload(),
                Timestamp.from(record.createdAt())
        );
    }

    @Override
    public boolean isPublished(UUID eventId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notification.notification_outbox WHERE id = ? AND published_at IS NOT NULL",
                Integer.class,
                eventId
        );
        return count != null && count > 0;
    }

    @Override
    public void markPublished(UUID eventId, Instant publishedAt) {
        jdbc.update(
                "UPDATE notification.notification_outbox SET published_at = ? WHERE id = ?",
                Timestamp.from(publishedAt),
                eventId
        );
    }

    @Override
    public Optional<OutboxRecord> findById(UUID eventId) {
        return jdbc.query(
                """
                SELECT id, tenant_id, topic, aggregate_id, aggregate_revision, payload, created_at
                FROM notification.notification_outbox WHERE id = ?
                """,
                (rs, rowNum) -> new OutboxRecord(
                        rs.getObject("id", UUID.class),
                        rs.getObject("tenant_id", UUID.class),
                        rs.getString("topic"),
                        rs.getObject("aggregate_id", UUID.class),
                        rs.getLong("aggregate_revision"),
                        1,
                        rs.getObject("tenant_id", UUID.class) + ":" + rs.getObject("aggregate_id", UUID.class),
                        rs.getString("payload"),
                        rs.getTimestamp("created_at").toInstant()
                ),
                eventId
        ).stream().findFirst();
    }
}
