package com.hrflow.request.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "approval_step", schema = "request")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalStep {
    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "approver_id", length = 64)
    private String approverId;

    @Column(name = "approver_name", length = 128)
    private String approverName;

    @Column(name = "approver_role", nullable = false, length = 64)
    private String approverRole;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "decision_reason", columnDefinition = "text")
    private String decisionReason;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Setter(lombok.AccessLevel.NONE)
    @Column(name = "decided_by_account", length = 64)
    private String decidedByAccount;

    @Setter(lombok.AccessLevel.NONE)
    @Column(name = "requester_revision", updatable = false)
    private Long requesterRevision;

    @Setter(lombok.AccessLevel.NONE)
    @Column(name = "approver_revision", updatable = false)
    private Long approverRevision;

    public void recordDecision(String decision, String reason, String account, Instant at) {
        if (!"PENDING".equals(status) || !java.util.Set.of("APPROVED", "REJECTED").contains(decision)
                || account == null || account.isBlank() || at == null) {
            throw new IllegalStateException("An independent pending-step decision and account are required");
        }
        status = decision;
        decisionReason = reason;
        decidedAt = at;
        decidedByAccount = account;
    }

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
