package com.hrflow.reporting.dto;

import java.time.Instant;
import java.util.UUID;

public record FreshnessStatus(
        UUID tenantId,
        String topic,
        boolean isHealthy,
        int pendingIntents,
        long lagSeconds,
        Instant lastProcessedAt
) {}
