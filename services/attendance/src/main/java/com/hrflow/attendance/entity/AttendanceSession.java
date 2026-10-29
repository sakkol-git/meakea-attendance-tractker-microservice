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
@Table(schema = "attendance", name = "attendance_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendanceSession {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "clock_in", nullable = false)
    private Instant clockIn;

    @Column(name = "clock_out")
    private Instant clockOut;

    @Column(name = "status", nullable = false)
    private String status;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "in_evidence_id")
    private UUID inEvidenceId;

    @Column(name = "out_evidence_id")
    private UUID outEvidenceId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "work_date") private LocalDate workDate;
    private String timezone;
    @Column(name = "shift_id") private UUID shiftId;
    @Column(name = "shift_revision") private Long shiftRevision;
    @Column(name = "scheduled_start") private Instant scheduledStart;
    @Column(name = "scheduled_end") private Instant scheduledEnd;
    @Column(name = "grace_minutes") private Integer graceMinutes;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "original_schedule", updatable = false)
    private java.util.Map<String, Object> originalSchedule;
    private String effectiveSource;
    @Column(name = "working_days") private Integer workingDays;

    public AttendanceSession(UUID tenantId, UUID employeeId, Instant clockIn, UUID inEvidenceId) {
        this.id = UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.employeeId = Objects.requireNonNull(employeeId, "employeeId must not be null");
        this.clockIn = Objects.requireNonNull(clockIn, "clockIn must not be null");
        this.clockOut = null;
        this.status = "OPEN";
        this.inEvidenceId = inEvidenceId;
        this.outEvidenceId = null;
        this.updatedAt = clockIn;
        this.effectiveSource = "TAP";
    }

    public void close(Instant clockOut, UUID outEvidenceId) {
        if (this.clockOut != null) {
            throw new IllegalStateException("Session is already closed");
        }
        if (clockOut == null || clockOut.isBefore(this.clockIn)) {
            throw new IllegalArgumentException("Clock out time must not be before clock in time");
        }
        this.clockOut = clockOut;
        this.status = "COMPLETED";
        this.outEvidenceId = outEvidenceId;
        this.updatedAt = clockOut;
    }

    public void captureSchedule(LocalDate date, String zone, UUID shiftId, long shiftRevision,
            Instant start, Instant end, int grace, int workingDays) {
        if (this.workDate != null || date == null || zone == null || shiftId == null || shiftRevision < 1
                || start == null || end == null || !end.isAfter(start) || grace < 0 || grace > 120
                || workingDays < 1 || workingDays > 127) {
            throw new IllegalArgumentException("A session requires one valid immutable schedule basis");
        }
        this.workDate = date;
        this.timezone = zone;
        this.shiftId = shiftId;
        this.shiftRevision = shiftRevision;
        this.scheduledStart = start;
        this.scheduledEnd = end;
        this.graceMinutes = grace;
        this.workingDays = workingDays;
        this.originalSchedule = java.util.Map.of("workDate", date.toString(), "timezone", zone,
                "shiftId", shiftId.toString(), "shiftRevision", shiftRevision,
                "scheduledStart", start.toString(), "scheduledEnd", end.toString(), "graceMinutes", grace,
                "workingDays", workingDays);
    }

    /** Human-approved effective context; original captured context and raw evidence remain unchanged. */
    public void correctSchedule(LocalDate date, String zone, UUID shiftId, long revision,
            Instant start, Instant end, int grace, int workingDays) {
        if (date == null || zone == null || shiftId == null || revision < 1 || start == null || end == null
                || !end.isAfter(start) || grace < 0 || grace > 120
                || workingDays < 1 || workingDays > 127
                || !date.equals(clockIn.atZone(java.time.ZoneId.of(zone)).toLocalDate())) {
            throw new IllegalArgumentException("Invalid effective correction schedule");
        }
        this.workDate = date;
        this.workingDays = workingDays;
        this.timezone = zone;
        this.shiftId = shiftId;
        this.shiftRevision = revision;
        this.scheduledStart = start;
        this.scheduledEnd = end;
        this.graceMinutes = grace;
    }

    public void applyCorrection(Instant clockIn, Instant clockOut, Instant updatedAt) {
        if (clockIn == null) {
            throw new IllegalArgumentException("Clock in time cannot be null");
        }
        if (clockOut != null && clockOut.isBefore(clockIn)) {
            throw new IllegalArgumentException("Clock out time must not be before clock in time");
        }
        this.clockIn = clockIn;
        this.clockOut = clockOut;
        this.status = clockOut == null ? "OPEN" : "COMPLETED";
        this.effectiveSource = "MANUAL";
        this.updatedAt = updatedAt != null ? updatedAt : clockIn;
    }
}
