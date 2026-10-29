package com.hrflow.identityorg.organization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "employee", schema = "identity_org")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Employee {
    @Id
    private UUID id;
    @Column(nullable = false)
    private UUID tenantId;
    @Column(nullable = false)
    private String employeeCode;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private UUID departmentId;
    @Column(nullable = false)
    private UUID positionId;
    private UUID managerId;
    private LocalDate hireDate;
    @Column(nullable = false)
    private String status;
    @Version
    private long version;

    public Employee(UUID tenantId, String employeeCode, String name, String email,
            UUID departmentId, UUID positionId, UUID managerId, LocalDate hireDate) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.employeeCode = employeeCode;
        this.name = name;
        this.email = email;
        this.departmentId = departmentId;
        this.positionId = positionId;
        this.managerId = managerId;
        this.hireDate = hireDate;
        this.status = "ACTIVE";
    }

    public void update(String name, String email, String employeeCode,
            UUID departmentId, UUID positionId, UUID managerId, boolean managerIdPresent, LocalDate hireDate) {
        if (name != null) this.name = name;
        if (email != null) this.email = email;
        if (employeeCode != null) this.employeeCode = employeeCode;
        if (departmentId != null) this.departmentId = departmentId;
        if (positionId != null) this.positionId = positionId;
        if (managerIdPresent) this.managerId = managerId;
        if (hireDate != null) this.hireDate = hireDate;
    }
}
