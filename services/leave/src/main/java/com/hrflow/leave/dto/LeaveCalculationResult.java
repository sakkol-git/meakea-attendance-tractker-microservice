package com.hrflow.leave.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record LeaveCalculationResult(
        BigDecimal chargeableDays,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        boolean allowHalfDay,
        boolean evidenceRequired,
        int minimumNoticeDays,
        List<LocalDate> workingDates,
        List<String> warnings,
        boolean balanceTracked
) {
}
