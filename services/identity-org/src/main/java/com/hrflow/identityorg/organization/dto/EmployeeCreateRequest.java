package com.hrflow.identityorg.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import java.time.LocalDate;

public record EmployeeCreateRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 64) String employeeNo,
        @NotNull UUID departmentId,
        @NotNull UUID positionId,
        UUID managerId,
        LocalDate hireDate) {}
