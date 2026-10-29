package com.hrflow.reporting.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record CreateExportRequest(
        @NotNull(message = "report is required")
        @Pattern(regexp = "^(ATTENDANCE|LEAVE|REQUESTS)$", message = "report must be ATTENDANCE, LEAVE, or REQUESTS")
        String report,

        @NotNull(message = "format is required")
        @Pattern(regexp = "^CSV$", message = "format must be CSV")
        String format,

        LocalDate from,
        LocalDate to
) {}
