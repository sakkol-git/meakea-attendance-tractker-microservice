package com.hrflow.reporting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "attendance_fact_projection", schema = "reporting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendanceFactProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "minutes_late", nullable = false)
    private int minutesLate;

    @Column(name = "minutes_worked")
    private Integer minutesWorked;

    private Instant clockIn;
    private Instant clockOut;
    private String timezone;
    private Instant scheduledStart;
    private Instant scheduledEnd;
    private Integer graceMinutes;
    private String source;

    @Column(name = "aggregate_revision", nullable = false)
    private long aggregateRevision;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AttendanceFactProjection(
            UUID tenantId,
            UUID sessionId,
            UUID employeeId,
            LocalDate workDate,
            String status,
            int minutesLate,
            int minutesWorked,
            long aggregateRevision
    ) {
        this.tenantId = tenantId;
        this.sessionId = sessionId;
        this.employeeId = employeeId;
        this.workDate = workDate;
        this.status = status;
        this.minutesLate = minutesLate;
        this.minutesWorked = minutesWorked;
        this.aggregateRevision = aggregateRevision;
        this.updatedAt = Instant.now();
    }

    public void updateFact(UUID employeeId, LocalDate workDate, String status, int minutesLate, int minutesWorked, long revision) {
        if (revision > this.aggregateRevision) {
            this.employeeId = employeeId;
            this.workDate = workDate;
            this.status = status;
            this.minutesLate = minutesLate;
            this.minutesWorked = minutesWorked;
            this.aggregateRevision = revision;
            this.updatedAt = Instant.now();
        }
    }

    public void updateFact(String status, int minutesLate, int minutesWorked, long revision) {
        updateFact(this.employeeId, this.workDate, status, minutesLate, minutesWorked, revision);
    }
}
