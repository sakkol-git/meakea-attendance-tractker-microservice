package com.hrflow.reporting.dto;

import java.time.LocalDate;
import java.util.UUID;

public record AttendanceFactResponse(
        UUID id,
        UUID sessionId,
        UUID employeeId,
        LocalDate workDate,
        String status,
        int minutesLate,
        Integer minutesWorked,
        long aggregateRevision,
        java.time.Instant clockIn,
        java.time.Instant clockOut,
        String timezone,
        String source
) {}
