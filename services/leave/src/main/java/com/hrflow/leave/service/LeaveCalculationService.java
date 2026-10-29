package com.hrflow.leave.service;

import com.hrflow.leave.dto.LeaveCalculationResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class LeaveCalculationService {

    public LeaveCalculationResult calculate(
            LocalDate startDate,
            LocalDate endDate,
            String portion,
            BigDecimal currentAvailableBalance,
            int minimumNoticeDays,
            boolean allowHalfDay,
            boolean evidenceRequired,
            Set<LocalDate> ownerWorkingDates,
            LocalDate today) {

        validateRange(startDate, endDate);

        String normalizedPortion = (portion == null || portion.isBlank()) ? "FULL_DAY" : portion.toUpperCase();
        if (!List.of("FULL_DAY", "MORNING", "AFTERNOON").contains(normalizedPortion)) {
            throw new IllegalArgumentException("Invalid portion: " + portion);
        }

        boolean isHalfDay = "MORNING".equals(normalizedPortion) || "AFTERNOON".equals(normalizedPortion);
        if (isHalfDay) {
            if (!allowHalfDay) {
                throw new IllegalArgumentException("Half-day leave is not permitted for this leave type");
            }
            if (!startDate.isEqual(endDate)) {
                throw new IllegalArgumentException("Half-day leave portion is only allowed for a single date");
            }
        }

        if (ownerWorkingDates == null || ownerWorkingDates.stream().anyMatch(date -> date.isBefore(startDate) || date.isAfter(endDate))) {
            throw new IllegalArgumentException("Owner working dates must lie within the request");
        }
        List<LocalDate> workingDates = ownerWorkingDates.stream().sorted().toList();

        BigDecimal chargeableDays;
        if (workingDates.isEmpty()) {
            chargeableDays = BigDecimal.ZERO;
        } else if (isHalfDay) {
            chargeableDays = new BigDecimal("0.5");
        } else {
            chargeableDays = BigDecimal.valueOf(workingDates.size());
        }

        BigDecimal balanceBefore = currentAvailableBalance;
        BigDecimal balanceAfter = balanceBefore == null ? null : balanceBefore.subtract(chargeableDays);

        List<String> warnings = new ArrayList<>();
        if (chargeableDays.compareTo(BigDecimal.ZERO) == 0) {
            warnings.add("NO_WORKING_DAYS");
        }
        if (balanceAfter != null && balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            warnings.add("INSUFFICIENT_BALANCE");
        }
        if (today != null && minimumNoticeDays > 0) {
            long noticeDays = ChronoUnit.DAYS.between(today, startDate);
            if (noticeDays < minimumNoticeDays) {
                warnings.add("SHORT_NOTICE");
            }
        }

        return new LeaveCalculationResult(
                chargeableDays,
                balanceBefore,
                balanceAfter,
                allowHalfDay,
                evidenceRequired,
                minimumNoticeDays,
                workingDates,
                warnings,
                balanceBefore != null
        );
    }

    public static void validateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) throw new IllegalArgumentException("Start date and end date must not be null");
        if (end.isBefore(start)) throw new IllegalArgumentException("End date cannot be before start date");
        if (start.getYear() != end.getYear()) throw new IllegalArgumentException("Submit a separate request for each calendar year");
    }
}
