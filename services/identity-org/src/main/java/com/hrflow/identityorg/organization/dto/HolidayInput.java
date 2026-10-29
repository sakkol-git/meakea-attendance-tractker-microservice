package com.hrflow.identityorg.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record HolidayInput(@NotBlank @Size(min = 2, max = 120) String name,
        @NotNull LocalDate date, @NotNull Kind kind) {
    public enum Kind { PUBLIC, COMPANY }
}
