package com.hrflow.leave.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "leave_type", schema = "leave")
public class LeaveType {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "days_allowed", nullable = false)
    private BigDecimal daysAllowed = BigDecimal.ZERO;

    @Column(name = "paid", nullable = false)
    private boolean paid = true;

    @Column(name = "allow_half_day", nullable = false)
    private boolean allowHalfDay = true;

    @Column(name = "evidence_required", nullable = false)
    private boolean evidenceRequired = false;

    @Column(name = "minimum_notice_days", nullable = false)
    private int minimumNoticeDays = 0;

    @Column(name = "carry_over_max", nullable = false)
    private BigDecimal carryOverMax = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public LeaveType() {
    }

    public LeaveType(String id, String tenantId, String code, String name, BigDecimal daysAllowed,
                     boolean paid, boolean allowHalfDay, boolean evidenceRequired,
                     int minimumNoticeDays, BigDecimal carryOverMax) {
        this.id = id;
        this.tenantId = tenantId;
        this.code = code;
        this.name = name;
        this.daysAllowed = daysAllowed;
        this.paid = paid;
        this.allowHalfDay = allowHalfDay;
        this.evidenceRequired = evidenceRequired;
        this.minimumNoticeDays = minimumNoticeDays;
        this.carryOverMax = carryOverMax;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getDaysAllowed() {
        return daysAllowed;
    }

    public void setDaysAllowed(BigDecimal daysAllowed) {
        this.daysAllowed = daysAllowed;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public boolean isAllowHalfDay() {
        return allowHalfDay;
    }

    public void setAllowHalfDay(boolean allowHalfDay) {
        this.allowHalfDay = allowHalfDay;
    }

    public boolean isEvidenceRequired() {
        return evidenceRequired;
    }

    public void setEvidenceRequired(boolean evidenceRequired) {
        this.evidenceRequired = evidenceRequired;
    }

    public int getMinimumNoticeDays() {
        return minimumNoticeDays;
    }

    public void setMinimumNoticeDays(int minimumNoticeDays) {
        this.minimumNoticeDays = minimumNoticeDays;
    }

    public BigDecimal getCarryOverMax() {
        return carryOverMax;
    }

    public void setCarryOverMax(BigDecimal carryOverMax) {
        this.carryOverMax = carryOverMax;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
