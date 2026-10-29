package com.hrflow.leave.dto;

import java.math.BigDecimal;

public record LeaveBalanceResponse(
        String id,
        String name,
        String type,
        BigDecimal allocated,
        BigDecimal entitled,
        BigDecimal used,
        BigDecimal pending,
        BigDecimal available,
        String unit,
        int year
) {
}
