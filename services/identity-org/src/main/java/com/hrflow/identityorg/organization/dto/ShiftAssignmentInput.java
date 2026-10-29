package com.hrflow.identityorg.organization.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record ShiftAssignmentInput(@NotNull UUID shiftId, @NotNull LocalDate validFrom,
        LocalDate validTo) {}
