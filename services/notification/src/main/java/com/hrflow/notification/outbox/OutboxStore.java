package com.hrflow.notification.outbox;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OutboxStore {
    void save(OutboxRecord record);
    boolean isPublished(UUID eventId);
    void markPublished(UUID eventId, Instant publishedAt);
    Optional<OutboxRecord> findById(UUID eventId);
}
