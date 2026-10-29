package com.hrflow.reporting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "leave_fact_projection", schema = "reporting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeaveFactProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "leave_id", nullable = false)
    private UUID leaveId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "leave_type", nullable = false, length = 100)
    private String leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "portion_days", nullable = false, precision = 5, scale = 2)
    private BigDecimal portionDays;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "aggregate_revision", nullable = false)
    private long aggregateRevision;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public LeaveFactProjection(
            UUID tenantId,
            UUID leaveId,
            UUID employeeId,
            String leaveType,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal portionDays,
            String status,
            long aggregateRevision
    ) {
        this.tenantId = tenantId;
        this.leaveId = leaveId;
        this.employeeId = employeeId;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.portionDays = portionDays;
        this.status = status;
        this.aggregateRevision = aggregateRevision;
        this.updatedAt = Instant.now();
    }

    public void updateFact(UUID employeeId, String leaveType, LocalDate startDate, LocalDate endDate, BigDecimal portionDays, String status, long revision) {
        if (revision > this.aggregateRevision) {
            this.employeeId = employeeId;
            this.leaveType = leaveType;
            this.startDate = startDate;
            this.endDate = endDate;
            this.portionDays = portionDays;
            this.status = status;
            this.aggregateRevision = revision;
            this.updatedAt = Instant.now();
        }
    }

    public void updateFact(String status, long revision) {
        updateFact(this.employeeId, this.leaveType, this.startDate, this.endDate, this.portionDays, status, revision);
    }
}
