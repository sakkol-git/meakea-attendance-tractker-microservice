package com.hrflow.leave.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LeaveRecordResponse(
        String id,
        String employeeId,
        String employeeName,
        String approverId,
        String type,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String portion,
        BigDecimal days,
        String reason,
        String status,
        long version,
        String decisionReason,
        String decisionNote,
        String createdAt
) {
}
