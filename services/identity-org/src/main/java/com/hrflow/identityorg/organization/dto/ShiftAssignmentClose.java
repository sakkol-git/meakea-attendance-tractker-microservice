package com.hrflow.identityorg.organization.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ShiftAssignmentClose(@NotNull LocalDate validTo) {}
