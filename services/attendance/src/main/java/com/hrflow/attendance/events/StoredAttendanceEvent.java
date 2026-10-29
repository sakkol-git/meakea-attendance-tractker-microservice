package com.hrflow.attendance.events;

import java.time.Instant;
import java.util.UUID;

public record StoredAttendanceEvent(UUID id, UUID eventId, UUID tenant, String topic, String key, UUID aggregate,
        long revision, String type, UUID correlation, Instant occurredAt, String payload) {}
