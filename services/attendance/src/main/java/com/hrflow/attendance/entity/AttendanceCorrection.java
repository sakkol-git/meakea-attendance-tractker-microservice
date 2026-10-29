package com.hrflow.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "attendance_correction", schema = "attendance")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendanceCorrection {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "proposed_clock_in")
    private Instant proposedClockIn;

    @Column(name = "proposed_clock_out")
    private Instant proposedClockOut;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "previous_sessions", updatable = false, columnDefinition = "jsonb")
    private String previousSessions;

    public void captureOriginalSessions(String sessions) {
        if (previousSessions != null || version != 0 || !"PENDING".equals(status))
            throw new IllegalStateException("Original sessions can only be captured on initial submission");
        previousSessions = sessions;
    }

    public AttendanceCorrection(UUID tenantId, UUID employeeId, UUID sessionId,
            LocalDate date, Instant proposedClockIn, Instant proposedClockOut, String reason) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.employeeId = employeeId;
        this.sessionId = sessionId;
        this.date = date;
        this.proposedClockIn = proposedClockIn;
        this.proposedClockOut = proposedClockOut;
        this.reason = reason;
        this.status = "PENDING";
        this.createdAt = Instant.now();
    }

    public void approve(UUID reviewerId, Instant reviewedAt) {
        if (!"PENDING".equals(this.status)) {
            throw new IllegalStateException("Correction is not pending: " + this.status);
        }
        this.status = "APPROVED";
        this.reviewedBy = Objects.requireNonNull(reviewerId, "reviewerId must not be null");
        this.reviewedAt = Objects.requireNonNull(reviewedAt, "reviewedAt must not be null");
    }

    public void reject(UUID reviewerId, Instant reviewedAt) {
        if (!"PENDING".equals(this.status)) {
            throw new IllegalStateException("Correction is not pending: " + this.status);
        }
        this.status = "REJECTED";
        this.reviewedBy = Objects.requireNonNull(reviewerId, "reviewerId must not be null");
        this.reviewedAt = Objects.requireNonNull(reviewedAt, "reviewedAt must not be null");
    }

    public void linkApprovedManualSession(UUID sessionId) {
        if (!"APPROVED".equals(status) || this.sessionId != null) {
            throw new IllegalStateException("Only a newly approved manual entry can establish its session link");
        }
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
    }
}
