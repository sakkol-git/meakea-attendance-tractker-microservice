package com.hrflow.reporting.dto;

import java.time.LocalDate;
import java.util.List;

public record AttendanceSummaryResponse(
        LocalDate from,
        LocalDate to,
        int totalCount,
        List<AttendanceFactResponse> records
) {}
