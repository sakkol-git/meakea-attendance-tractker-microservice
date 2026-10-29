package com.hrflow.notification.outbox;

import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderedOutboxRelay {

    private final JdbcTemplate jdbc;
    private final OutboxPublisher publisher;

    public OrderedOutboxRelay(JdbcTemplate jdbc, OutboxPublisher publisher) {
        this.jdbc = jdbc;
        this.publisher = publisher;
    }

    public Optional<PublicationTrace> publishNextPending(UUID tenantId) {
        String query = """
                SELECT current_fact.id, current_fact.tenant_id, current_fact.topic, current_fact.aggregate_id,
                       current_fact.aggregate_revision, current_fact.payload, current_fact.created_at
                FROM notification.notification_outbox current_fact
                WHERE current_fact.tenant_id = ?
                  AND current_fact.published_at IS NULL
                  AND NOT EXISTS (
                      SELECT 1 FROM notification.notification_outbox earlier
                      WHERE earlier.tenant_id = current_fact.tenant_id
                        AND earlier.aggregate_id = current_fact.aggregate_id
                        AND earlier.aggregate_revision < current_fact.aggregate_revision
                        AND earlier.published_at IS NULL
                  )
                ORDER BY current_fact.aggregate_revision ASC, current_fact.created_at ASC
                LIMIT 1
                """;

        var matches = jdbc.query(
                query,
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
                tenantId
        );

        if (matches.isEmpty()) {
            return Optional.empty();
        }

        OutboxRecord candidate = matches.getFirst();
        PublicationTrace trace = publisher.publish(candidate);
        return Optional.of(trace);
    }
}
