package com.hrflow.notification.outbox;

import java.time.Instant;
import java.util.UUID;

public record PublicationTrace(
        UUID eventId,
        UUID tenantId,
        long aggregateRevision,
        int schemaVersion,
        String partitionKey,
        Instant publishedAt
) {}
