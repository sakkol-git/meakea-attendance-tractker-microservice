package com.hrflow.leave.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;

@Entity
@Table(name = "leave_balance", schema = "leave")
public class LeaveBalance {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "employee_id", nullable = false, length = 64)
    private String employeeId;

    @Column(name = "leave_type_code", nullable = false, length = 64)
    private String leaveTypeCode;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "allocated", nullable = false)
    private BigDecimal allocated = BigDecimal.ZERO;

    @Column(name = "used", nullable = false)
    private BigDecimal used = BigDecimal.ZERO;

    @Column(name = "pending", nullable = false)
    private BigDecimal pending = BigDecimal.ZERO;

    @Column(name = "available", nullable = false)
    private BigDecimal available = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private long version = 0L;

    public LeaveBalance() {
    }

    public LeaveBalance(String id, String tenantId, String employeeId, String leaveTypeCode, int year,
                        BigDecimal allocated, BigDecimal used, BigDecimal pending, BigDecimal available) {
        this.id = id;
        this.tenantId = tenantId;
        this.employeeId = employeeId;
        this.leaveTypeCode = leaveTypeCode;
        this.year = year;
        this.allocated = allocated;
        this.used = used;
        this.pending = pending;
        this.available = available;
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

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getLeaveTypeCode() {
        return leaveTypeCode;
    }

    public void setLeaveTypeCode(String leaveTypeCode) {
        this.leaveTypeCode = leaveTypeCode;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public BigDecimal getAllocated() {
        return allocated;
    }

    public void setAllocated(BigDecimal allocated) {
        this.allocated = allocated;
    }

    public BigDecimal getUsed() {
        return used;
    }

    public void setUsed(BigDecimal used) {
        this.used = used;
    }

    public BigDecimal getPending() {
        return pending;
    }

    public void setPending(BigDecimal pending) {
        this.pending = pending;
    }

    public BigDecimal getAvailable() {
        return available;
    }

    public void setAvailable(BigDecimal available) {
        this.available = available;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
