package com.hrflow.reporting.dto;

import java.time.LocalDate;
import java.util.UUID;

public record AttendanceFactEvent(
        UUID eventId,
        UUID tenantId,
        UUID sessionId,
        UUID employeeId,
        LocalDate workDate,
        String status,
        int minutesLate,
        int minutesWorked,
        long aggregateRevision
) {}
