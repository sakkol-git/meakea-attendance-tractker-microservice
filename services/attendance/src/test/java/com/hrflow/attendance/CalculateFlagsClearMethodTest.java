package com.hrflow.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hrflow.attendance.dto.DailyStatusResponse;
import com.hrflow.attendance.service.AttendanceCalculationService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CalculateFlagsClearMethodTest {

    private final AttendanceCalculationService service = new AttendanceCalculationService();
    private final ZoneId zone = ZoneId.of("Asia/Phnom_Penh");
    private final LocalDate date = LocalDate.of(2026, 11, 16);
    private final LocalTime shiftStart = LocalTime.of(9, 0);
    private final LocalTime shiftEnd = LocalTime.of(18, 0);
    private final int graceMinutes = 15;
    private final UUID employeeId = UUID.randomUUID();

    @Test
    void exactlyTenMinutesEarlyIsWithinTheAcceptedUatAllowance() {
        var result = completed(employeeId, date, shiftStart, shiftEnd, graceMinutes, zone,
                date.atTime(9, 0).atZone(zone).toInstant(), date.atTime(17, 50).atZone(zone).toInstant(), false, false);
        assertFalse(result.earlyDeparture());
    }

    @Test
    void statusIsDeterministicAtGraceBoundary() {
        // Clock in exactly at grace boundary 09:15:00 -> not late
        ZonedDateTime onTimeBoundary = date.atTime(9, 15, 0).atZone(zone);
        ZonedDateTime standardClockOut = date.atTime(18, 0, 0).atZone(zone);

        DailyStatusResponse onTimeResult = completed(
                employeeId, date, shiftStart, shiftEnd, graceMinutes, zone,
                onTimeBoundary.toInstant(), standardClockOut.toInstant(), false, false);

        assertFalse(onTimeResult.late(), "Exactly at grace boundary 09:15:00 must NOT be flagged as late");
        assertFalse(onTimeResult.earlyDeparture());
        assertEquals("ON_TIME", onTimeResult.status());

        // Clock in 1 second after grace boundary 09:15:01 -> late
        ZonedDateTime lateInstant = date.atTime(9, 15, 1).atZone(zone);
        DailyStatusResponse lateResult = completed(
                employeeId, date, shiftStart, shiftEnd, graceMinutes, zone,
                lateInstant.toInstant(), standardClockOut.toInstant(), false, false);

        assertTrue(lateResult.late(), "09:15:01 must be flagged as late");
        assertEquals("LATE", lateResult.status());
    }

    @Test
    void earlyDepartureIsFlaggedWhenClockOutPrecedesShiftEnd() {
        ZonedDateTime clockIn = date.atTime(9, 0, 0).atZone(zone);
        ZonedDateTime earlyClockOut = date.atTime(17, 45, 0).atZone(zone); // 15 mins early

        DailyStatusResponse result = completed(
                employeeId, date, shiftStart, shiftEnd, graceMinutes, zone,
                clockIn.toInstant(), earlyClockOut.toInstant(), false, false);

        assertFalse(result.late());
        assertTrue(result.earlyDeparture(), "17:45:00 is earlier than shift end 18:00:00");
        assertEquals("EARLY_DEPARTURE", result.status());
    }

    @Test
    void lateAndEarlyDepartureAreCombinedWhenBothOccur() {
        ZonedDateTime lateClockIn = date.atTime(9, 30, 0).atZone(zone);
        ZonedDateTime earlyClockOut = date.atTime(17, 30, 0).atZone(zone);

        DailyStatusResponse result = completed(
                employeeId, date, shiftStart, shiftEnd, graceMinutes, zone,
                lateClockIn.toInstant(), earlyClockOut.toInstant(), false, false);

        assertTrue(result.late());
        assertTrue(result.earlyDeparture());
        assertEquals("LATE_AND_EARLY_DEPARTURE", result.status());
    }

    @Test
    void holidayAndLeaveConflictsRemainVisibleForReview() {
        ZonedDateTime clockIn = date.atTime(9, 0, 0).atZone(zone);
        ZonedDateTime clockOut = date.atTime(18, 0, 0).atZone(zone);

        // Presence recorded on a public holiday
        DailyStatusResponse holidayConflict = completed(
                employeeId, date, shiftStart, shiftEnd, graceMinutes, zone,
                clockIn.toInstant(), clockOut.toInstant(), true, false);

        assertTrue(holidayConflict.reviewRequired(), "Working on holiday requires review");
        assertEquals("REVIEW_REQUIRED", holidayConflict.status());
        assertEquals("Attendance recorded on official holiday", holidayConflict.reviewReason());

        // Presence recorded during approved leave
        DailyStatusResponse leaveConflict = completed(
                employeeId, date, shiftStart, shiftEnd, graceMinutes, zone,
                clockIn.toInstant(), clockOut.toInstant(), false, true);

        assertTrue(leaveConflict.reviewRequired(), "Working during approved leave requires review");
        assertEquals("REVIEW_REQUIRED", leaveConflict.status());
        assertEquals("Attendance recorded during approved leave", leaveConflict.reviewReason());
    }

    @Test
    void absencesAndApprovedAbsencesYieldCorrectStatusWithoutSession() {
        DailyStatusResponse holidayNoWork = completed(
                employeeId, date, shiftStart, shiftEnd, graceMinutes, zone,
                null, null, true, false);
        assertEquals("HOLIDAY", holidayNoWork.status());
        assertFalse(holidayNoWork.reviewRequired());

        DailyStatusResponse leaveNoWork = completed(
                employeeId, date, shiftStart, shiftEnd, graceMinutes, zone,
                null, null, false, true);
        assertEquals("ON_LEAVE", leaveNoWork.status());
        assertFalse(leaveNoWork.reviewRequired());

        DailyStatusResponse absent = completed(
                employeeId, date, shiftStart, shiftEnd, graceMinutes, zone,
                null, null, false, false);
        assertEquals("ABSENT", absent.status());
        assertFalse(absent.reviewRequired());
    }
    private DailyStatusResponse completed(UUID employee, LocalDate day, LocalTime start, LocalTime end,
            int grace, ZoneId timezone, java.time.Instant in, java.time.Instant out, boolean holiday, boolean leave) {
        var intervals = in == null ? java.util.List.<AttendanceCalculationService.Session>of()
                : java.util.List.of(new AttendanceCalculationService.Session(in, out));
        return service.calculate(new AttendanceCalculationService.Day(employee, day, timezone,
                day.atTime(start).atZone(timezone).toInstant(), day.atTime(end).atZone(timezone).toInstant(),
                grace, true, holiday, new com.hrflow.attendance.leave.LeaveCoverage(leave, leave), intervals,
                day.atTime(end).atZone(timezone).toInstant().plusSeconds(30 * 60L)));
    }

}
