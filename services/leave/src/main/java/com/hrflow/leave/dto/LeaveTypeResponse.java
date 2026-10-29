package com.hrflow.leave.dto;

import java.math.BigDecimal;

public record LeaveTypeResponse(
        String id,
        String name,
        String code,
        BigDecimal daysAllowed,
        boolean paid,
        boolean allowHalfDay,
        boolean evidenceRequired,
        int minimumNoticeDays,
        BigDecimal carryOverMax
) {
}
