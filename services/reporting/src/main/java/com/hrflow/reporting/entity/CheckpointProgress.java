package com.hrflow.reporting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "checkpoint_progress", schema = "reporting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CheckpointProgress {

    @EmbeddedId
    private CheckpointId id;

    @Column(name = "last_offset", nullable = false)
    private long lastOffset;

    @Column(name = "last_processed_at", nullable = false)
    private Instant lastProcessedAt;

    @Column(name = "pending_intents", nullable = false)
    private int pendingIntents;

    public CheckpointProgress(UUID tenantId, String topic, int partitionNum, long lastOffset, Instant lastProcessedAt, int pendingIntents) {
        validate(lastOffset, lastProcessedAt, pendingIntents);
        this.id = new CheckpointId(tenantId, topic, partitionNum);
        this.lastOffset = lastOffset;
        this.lastProcessedAt = lastProcessedAt;
        this.pendingIntents = pendingIntents;
    }

    public static void validate(long offset, Instant processedAt, int pending) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative: " + offset);
        }
        if (pending < 0) {
            throw new IllegalArgumentException("Pending intents cannot be negative: " + pending);
        }
        if (processedAt == null) {
            throw new IllegalArgumentException("Checkpoint timestamp cannot be null");
        }
        if (processedAt.isAfter(Instant.now().plusSeconds(300))) {
            throw new IllegalArgumentException("Checkpoint timestamp cannot be in the future: " + processedAt);
        }
    }

    /**
     * Updates checkpoint progress enforcing strict monotonicity and idempotency.
     *
     * @param offset proposed new offset
     * @param processedAt proposed new processing timestamp
     * @param pending proposed new pending intents count
     * @return true if updated, false if duplicate offset ignored as idempotent no-op
     * @throws IllegalArgumentException if offset decreases, pending is negative, or timestamp is invalid
     */
    public boolean updateProgress(long offset, Instant processedAt, int pending) {
        validate(offset, processedAt, pending);

        if (offset < this.lastOffset) {
            throw new IllegalArgumentException(
                    "Checkpoint offset cannot decrease: current=" + this.lastOffset + ", proposed=" + offset
            );
        }

        if (offset == this.lastOffset) {
            // Accepted requirement: Duplicate offset delivery is an idempotent no-op.
            // Preserves existing stored state without modification or failure.
            return false;
        }

        this.lastOffset = offset;
        this.lastProcessedAt = processedAt;
        this.pendingIntents = pending;
        return true;
    }

    @Embeddable
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class CheckpointId implements Serializable {
        @Column(name = "tenant_id", nullable = false)
        private UUID tenantId;

        @Column(name = "topic", nullable = false, length = 120)
        private String topic;

        @Column(name = "partition_num", nullable = false)
        private int partitionNum;

        public CheckpointId(UUID tenantId, String topic, int partitionNum) {
            this.tenantId = tenantId;
            this.topic = topic;
            this.partitionNum = partitionNum;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CheckpointId that = (CheckpointId) o;
            return partitionNum == that.partitionNum && Objects.equals(tenantId, that.tenantId) && Objects.equals(topic, that.topic);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tenantId, topic, partitionNum);
        }
    }
}
