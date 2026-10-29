package com.hrflow.identityorg.organization.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record HolidayPatch(@Size(min = 2, max = 120) String name,
        LocalDate date, HolidayInput.Kind kind) {}
