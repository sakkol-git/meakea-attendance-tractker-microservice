package com.hrflow.leave.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "leave_day", schema = "leave")
public class LeaveDay {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;

    @Column(name = "employee_id", nullable = false, length = 64)
    private String employeeId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "portion", nullable = false, length = 32)
    private String portion = "FULL_DAY";

    @Column(name = "hours", nullable = false)
    private BigDecimal hours = new BigDecimal("8.00");

    @Column(name = "scheduled_seconds")
    private Long scheduledSeconds;

    public LeaveDay() {
    }

    public LeaveDay(String id, String tenantId, String requestId, String employeeId,
                    LocalDate date, String portion, long scheduledSeconds) {
        this.id = id;
        this.tenantId = tenantId;
        this.requestId = requestId;
        this.employeeId = employeeId;
        this.date = date;
        this.portion = portion;
        this.scheduledSeconds = scheduledSeconds;
        // Legacy display precision only; exact owner duration is retained separately and never bills the day balance.
        this.hours = BigDecimal.valueOf(scheduledSeconds).divide(BigDecimal.valueOf(3600), 2, java.math.RoundingMode.HALF_UP);
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

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getPortion() {
        return portion;
    }

    public void setPortion(String portion) {
        this.portion = portion;
    }

    public BigDecimal getHours() {
        return hours;
    }

    public void setHours(BigDecimal hours) {
        this.hours = hours;
    }
}
