package com.hrflow.leave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.leave.dto.LeaveCalculationResult;
import com.hrflow.leave.service.LeaveCalculationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CalculateEligibleLeaveDaysTest {
    @org.junit.jupiter.api.Test
    void separateRequestsAreRequiredForEachCalendarYear() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new com.hrflow.leave.service.LeaveCalculationService().calculate(
                java.time.LocalDate.parse("2026-12-31"), java.time.LocalDate.parse("2027-01-01"), "FULL_DAY",
                new java.math.BigDecimal("20.00"), 0, true, false, java.util.Set.of(), java.time.LocalDate.parse("2026-10-28")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("calendar year");
    }

    private LeaveCalculationService calculationService;

    @BeforeEach
    void setUp() {
        calculationService = new LeaveCalculationService();
    }

    @Test
    @DisplayName("Charge calculation uses the supplied owner working dates deterministically")
    void usesOwnerWorkingDates() {
        // Range: Friday 2026-06-05 to Tuesday 2026-06-09
        // 2026-06-05: Friday (Workday)
        // 2026-06-06: Saturday (Weekend)
        // 2026-06-07: Sunday (Weekend)
        // 2026-06-08: Monday (Public Holiday)
        // 2026-06-09: Tuesday (Workday)
        LocalDate friday = LocalDate.of(2026, 6, 5);
        LocalDate tuesday = LocalDate.of(2026, 6, 9);
        LocalDate holidayMonday = LocalDate.of(2026, 6, 8);

        LeaveCalculationResult result = calculationService.calculate(
                friday,
                tuesday,
                "FULL_DAY",
                new BigDecimal("10.0"),
                0,
                true,
                false,
                Set.of(friday, tuesday),
                friday
        );

        // Expect: 2 working days (Friday and Tuesday)
        assertThat(result.chargeableDays()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(result.workingDates()).containsExactly(friday, tuesday);
        assertThat(result.balanceBefore()).isEqualByComparingTo(new BigDecimal("10.0"));
        assertThat(result.balanceAfter()).isEqualByComparingTo(new BigDecimal("8.0"));
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    @DisplayName("Half-day leave portion is strictly restricted to a single calendar date")
    void halfDayOnlyValidForSingleDate() {
        LocalDate start = LocalDate.of(2026, 6, 5);
        LocalDate end = LocalDate.of(2026, 6, 8);

        assertThatThrownBy(() -> calculationService.calculate(
                start,
                end,
                "MORNING",
                new BigDecimal("5.0"),
                0,
                true,
                false,
                Set.of(),
                start
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Half-day leave portion is only allowed for a single date");
    }

    @Test
    @DisplayName("Single-day half-day request calculates exactly 0.5 chargeable days")
    void halfDayCalculatesHalfDay() {
        LocalDate date = LocalDate.of(2026, 6, 5); // Friday

        LeaveCalculationResult result = calculationService.calculate(
                date,
                date,
                "AFTERNOON",
                new BigDecimal("5.0"),
                0,
                true,
                false,
                Set.of(date),
                date
        );

        assertThat(result.chargeableDays()).isEqualByComparingTo(new BigDecimal("0.5"));
        assertThat(result.balanceAfter()).isEqualByComparingTo(new BigDecimal("4.5"));
    }

    @Test
    @DisplayName("Inverted date range throws IllegalArgumentException")
    void invertedDatesThrowsIllegalArgumentException() {
        LocalDate start = LocalDate.of(2026, 6, 10);
        LocalDate end = LocalDate.of(2026, 6, 5);

        assertThatThrownBy(() -> calculationService.calculate(
                start,
                end,
                "FULL_DAY",
                new BigDecimal("5.0"),
                0,
                true,
                false,
                Set.of(),
                start
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End date cannot be before start date");
    }

    @Test
    @DisplayName("Insufficient balance and short notice produce authoritative policy warnings")
    void insufficientBalanceAndNoticeWarnings() {
        LocalDate today = LocalDate.of(2026, 6, 1);
        LocalDate start = LocalDate.of(2026, 6, 2); // 1 day notice
        LocalDate end = LocalDate.of(2026, 6, 4);   // 3 days: June 2, 3, 4 (all workdays)

        LeaveCalculationResult result = calculationService.calculate(
                start,
                end,
                "FULL_DAY",
                new BigDecimal("1.0"), // Only 1 day available, but requesting 3
                5,                     // Requires 5 days notice
                true,
                false,
                Set.of(start, start.plusDays(1), end),
                today
        );

        assertThat(result.chargeableDays()).isEqualByComparingTo(new BigDecimal("3"));
        assertThat(result.balanceAfter()).isEqualByComparingTo(new BigDecimal("-2.0"));
        assertThat(result.warnings()).containsExactlyInAnyOrder("INSUFFICIENT_BALANCE", "SHORT_NOTICE");
    }
}
