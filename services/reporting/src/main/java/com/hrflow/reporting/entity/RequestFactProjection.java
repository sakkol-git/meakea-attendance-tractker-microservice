package com.hrflow.reporting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "request_fact_projection", schema = "reporting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RequestFactProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Column(name = "request_type", nullable = false, length = 100)
    private String requestType;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "aggregate_revision", nullable = false)
    private long aggregateRevision;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    public void retainCreationTime(Instant value) {
        if (createdAt != null && !createdAt.equals(value)) throw new IllegalArgumentException("Original request creation time cannot change");
        createdAt = value;
    }

    public RequestFactProjection(
            UUID tenantId,
            UUID requestId,
            UUID requesterId,
            String requestType,
            String status,
            long aggregateRevision
    ) {
        this.tenantId = tenantId;
        this.requestId = requestId;
        this.requesterId = requesterId;
        this.requestType = requestType;
        this.status = status;
        this.aggregateRevision = aggregateRevision;
        this.updatedAt = Instant.now();
    }

    public void updateFact(UUID requesterId, String requestType, String status, long revision) {
        if (revision > this.aggregateRevision) {
            this.requesterId = requesterId;
            this.requestType = requestType;
            this.status = status;
            this.aggregateRevision = revision;
            this.updatedAt = Instant.now();
        }
    }

    public void updateFact(String status, long revision) {
        updateFact(this.requesterId, this.requestType, status, revision);
    }
}
