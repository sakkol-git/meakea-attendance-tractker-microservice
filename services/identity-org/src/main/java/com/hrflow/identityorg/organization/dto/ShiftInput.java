package com.hrflow.identityorg.organization.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.UUID;

public record ShiftInput(
        @NotBlank @Size(min = 2, max = 120) String name,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @Min(0) @Max(120) Integer graceMinutes,
        @NotNull @Min(1) @Max(127) Integer workingDays,
        @NotNull UUID siteId,
        @NotBlank String timezone,
        @Size(max = 2000) String reason) {}
