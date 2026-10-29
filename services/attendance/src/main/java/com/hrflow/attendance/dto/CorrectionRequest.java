package com.hrflow.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CorrectionRequest(
    UUID sessionId,
    @NotNull LocalDate date,
    Instant proposedClockIn,
    Instant proposedClockOut,
    @NotBlank String reason
) {}
