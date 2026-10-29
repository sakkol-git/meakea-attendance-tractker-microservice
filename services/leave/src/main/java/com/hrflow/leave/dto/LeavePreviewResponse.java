package com.hrflow.leave.dto;

import java.math.BigDecimal;
import java.util.List;

public record LeavePreviewResponse(
        BigDecimal chargeableDays,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        boolean allowHalfDay,
        boolean evidenceRequired,
        int minimumNoticeDays,
        List<String> warnings,
        boolean balanceTracked
) {
}
