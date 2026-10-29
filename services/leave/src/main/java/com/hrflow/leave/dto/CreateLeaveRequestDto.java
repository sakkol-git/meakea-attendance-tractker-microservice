package com.hrflow.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateLeaveRequestDto(
        @NotBlank(message = "Leave type is required")
        String type,
        @NotNull(message = "Start date is required")
        LocalDate startDate,
        @NotNull(message = "End date is required")
        LocalDate endDate,
        String portion,
        String reason,
        String title
) {
}
