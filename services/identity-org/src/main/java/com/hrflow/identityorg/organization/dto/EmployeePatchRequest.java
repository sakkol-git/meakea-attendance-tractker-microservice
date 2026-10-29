package com.hrflow.identityorg.organization.dto;

import java.time.LocalDate;
import java.util.UUID;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** PATCH needs to distinguish an omitted manager from an explicit null. */
@Getter
@Setter
public class EmployeePatchRequest {
    @Size(min = 2, max = 120)
    private String name;
    @Email
    @Size(max = 254)
    private String email;
    @Size(min = 1, max = 64)
    private String employeeNo;
    private UUID departmentId;
    private UUID positionId;
    private UUID managerId;
    private LocalDate hireDate;
    @Setter(lombok.AccessLevel.NONE)
    private boolean managerIdPresent;

    public void setManagerId(UUID managerId) {
        this.managerId = managerId;
        this.managerIdPresent = true;
    }
}
