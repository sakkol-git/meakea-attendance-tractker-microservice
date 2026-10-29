package com.hrflow.identityorg.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEntry(UUID id, UUID actorId, String actor, String action, String entity,
        UUID entityId, Map<String, Object> diff, Map<String, Object> changes, Instant at) {}
