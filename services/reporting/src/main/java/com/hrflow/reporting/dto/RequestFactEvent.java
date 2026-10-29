package com.hrflow.reporting.dto;

import java.util.UUID;

public record RequestFactEvent(
        UUID eventId,
        UUID tenantId,
        UUID requestId,
        UUID requesterId,
        String requestType,
        String status,
        long aggregateRevision,
        java.time.Instant createdAt
) {
    public RequestFactEvent(UUID eventId, UUID tenantId, UUID requestId, UUID requesterId,
            String requestType, String status, long aggregateRevision) {
        this(eventId, tenantId, requestId, requesterId, requestType, status, aggregateRevision, null);
    }
}
