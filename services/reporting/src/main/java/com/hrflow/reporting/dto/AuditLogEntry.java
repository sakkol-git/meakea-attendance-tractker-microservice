package com.hrflow.reporting.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditLogEntry(
        UUID id,
        UUID tenantId,
        UUID actorId,
        String actorEmail,
        String action,
        String resourceType,
        String resourceId,
        String details,
        String ipAddress,
        Instant createdAt
) {}
