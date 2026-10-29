package com.hrflow.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LeaveFactEvent(
        UUID eventId,
        UUID tenantId,
        UUID leaveId,
        UUID employeeId,
        String leaveType,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal portionDays,
        String status,
        long aggregateRevision
) {}
