package com.hrflow.attendance;

import static com.hrflow.attendance.DailyAttendanceTest.at;
import static org.assertj.core.api.Assertions.assertThat;

import com.hrflow.attendance.leave.LeaveCoverage;
import com.hrflow.attendance.service.AttendanceCalculationService;
import com.hrflow.attendance.service.AttendanceCalculationService.Session;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DailyCalculationTest {
    private final AttendanceCalculationService calculation = new AttendanceCalculationService();

    @Test void afternoonLeaveAllowsTenMinutesEarlyAtTheShortenedWindowBoundary() {
        var allowed = day(new LeaveCoverage(false, true), List.of(new Session(at("08:00"), at("12:20"))));
        assertThat(allowed.expectedEnd()).isEqualTo(at("12:30"));
        assertThat(allowed.earlyDeparture()).isFalse();
        assertThat(allowed.reviewRequired()).isFalse();
        var early = day(new LeaveCoverage(false, true), List.of(new Session(at("08:00"), at("12:20").minusSeconds(1))));
        assertThat(early.earlyDeparture()).isTrue();
    }

    @Test void halfDayOverlapRaisesReviewAndPreservesAllWorkedMinutes() {
        var result = day(new LeaveCoverage(true, false),
                List.of(new Session(at("08:00"), at("12:00")), new Session(at("13:00"), at("17:00"))));
        assertThat(result.reviewRequired()).isTrue();
        assertThat(result.dayType()).isEqualTo("PARTIAL_LEAVE");
        assertThat(result.minutesWorked()).isEqualTo(480);
    }

    @Test void openSessionKeepsTotalUnknownAndCannotProduceAFinalEarlyDeparture() {
        var result = day(new LeaveCoverage(false, false),
                List.of(new Session(at("08:00"), at("12:00")), new Session(at("13:00"), null)));
        assertThat(result.minutesWorked()).isNull();
        assertThat(result.clockOut()).isEqualTo(at("12:00"));
        assertThat(result.openSession()).isTrue();
        assertThat(result.finalized()).isFalse();
        assertThat(result.earlyDeparture()).isFalse();
    }

    @Test void fullLeaveKeepsCalendarTypeSeparateFromWorkAndReview() {
        var result = day(new LeaveCoverage(true, true), List.of(new Session(at("08:00"), at("17:00"))));
        assertThat(result.dayType()).isEqualTo("ON_LEAVE");
        assertThat(result.reviewRequired()).isTrue();
        assertThat(result.clockIn()).isEqualTo(at("08:00"));
        assertThat(result.minutesWorked()).isEqualTo(540);
        assertThat(result.expectedStart()).isNull();
        assertThat(result.expectedEnd()).isNull();
    }

    private com.hrflow.attendance.dto.DailyStatusResponse day(LeaveCoverage leave, List<Session> intervals) {
        return calculation.calculate(new AttendanceCalculationService.Day(UUID.randomUUID(), DailyAttendanceTest.DAY,
                DailyAttendanceTest.ZONE, at("08:00"), at("17:00"), 10, true, false, leave, intervals, at("17:30")));
    }
}
