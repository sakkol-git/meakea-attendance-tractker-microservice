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
@Table(name = "reporting_inbox", schema = "reporting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportingInbox {

    @EmbeddedId
    private InboxId id;

    @Column(name = "topic", nullable = false, length = 120)
    private String topic;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "aggregate_revision", nullable = false)
    private long aggregateRevision;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    public ReportingInbox(String consumerName, UUID tenantId, UUID eventId, String topic, UUID aggregateId, long aggregateRevision) {
        this.id = new InboxId(consumerName, tenantId, eventId);
        this.topic = topic;
        this.aggregateId = aggregateId;
        this.aggregateRevision = aggregateRevision;
        this.appliedAt = Instant.now();
    }

    @Embeddable
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class InboxId implements Serializable {
        @Column(name = "consumer_name", nullable = false, length = 100)
        private String consumerName;

        @Column(name = "tenant_id", nullable = false)
        private UUID tenantId;

        @Column(name = "event_id", nullable = false)
        private UUID eventId;

        public InboxId(String consumerName, UUID tenantId, UUID eventId) {
            this.consumerName = consumerName;
            this.tenantId = tenantId;
            this.eventId = eventId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InboxId inboxId = (InboxId) o;
            return Objects.equals(consumerName, inboxId.consumerName) && Objects.equals(tenantId, inboxId.tenantId) && Objects.equals(eventId, inboxId.eventId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(consumerName, tenantId, eventId);
        }
    }
}
