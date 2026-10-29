package com.hrflow.identityorg.organization.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.UUID;

public record ShiftPatch(
        @Size(min = 2, max = 120) String name,
        LocalTime startTime,
        LocalTime endTime,
        @Min(0) @Max(120) Integer graceMinutes,
        @Min(1) @Max(127) Integer workingDays,
        UUID siteId,
        String timezone,
        @Size(max = 2000) String reason) {}
