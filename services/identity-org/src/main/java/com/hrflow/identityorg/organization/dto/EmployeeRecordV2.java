package com.hrflow.identityorg.organization.dto;

import java.util.UUID;
import java.time.LocalDate;

public record EmployeeRecordV2(
        UUID id, String name, String email, String employeeNo,
        UUID departmentId, UUID positionId, UUID managerId,
        String department, String position, String status, LocalDate hireDate, long version) {}
