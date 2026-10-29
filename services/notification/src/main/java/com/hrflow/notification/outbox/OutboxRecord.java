package com.hrflow.notification.outbox;

import java.time.Instant;
import java.util.UUID;

public record OutboxRecord(
        UUID eventId,
        UUID tenantId,
        String topic,
        UUID aggregateId,
        long aggregateRevision,
        int schemaVersion,
        String partitionKey,
        String payload,
        Instant createdAt
) {
    public OutboxRecord {
        if (eventId == null || tenantId == null || topic == null || aggregateId == null || partitionKey == null) {
            throw new IllegalArgumentException("Outbox record must have non-null keys");
        }
    }
}
