package com.hrflow.identityorg.organization.dto;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record ShiftAssignmentRecordV2(UUID id, UUID employeeId, UUID shiftId,
        LocalDate validFrom, LocalDate validTo, Map<String, Object> shiftSnapshot, long version) {}
