package com.hrflow.attendance.service;

import com.hrflow.attendance.dto.DailyStatusResponse;
import com.hrflow.attendance.leave.LeaveCoverage;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Pure daily policy, using explicit owner facts and the accepted UAT timing decisions. */
@Service
public class AttendanceCalculationService {
    public DailyStatusResponse calculate(Day day) {
        Instant midpoint = day.start().plus(Duration.between(day.start(), day.end()).dividedBy(2));
        Instant expectedStart = day.leave().morning() ? midpoint : day.start();
        Instant expectedEnd = day.leave().afternoon() ? midpoint : day.end();
        boolean cutoffReached = !day.now().isBefore(day.end().plusSeconds(30 * 60L));
        var first = day.sessions().stream().map(Session::in).min(Comparator.naturalOrder()).orElse(null);
        var last = day.sessions().stream().map(Session::out).filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null);
        boolean open = day.sessions().stream().anyMatch(session -> session.out() == null);
        boolean finalized = cutoffReached && !open;
        Integer minutes = open ? null : Math.toIntExact(day.sessions().stream()
                .mapToLong(session -> Duration.between(session.in(), session.out()).toSeconds()).sum() / 60);
        boolean hasExpectedWork = day.workingDay() && !day.holiday() && !day.leave().fullDay();
        boolean late = hasExpectedWork && first != null && first.isAfter(expectedStart.plusSeconds(60L * day.grace()));
        boolean early = hasExpectedWork && finalized && !open && last != null
                && last.isBefore(expectedEnd.minusSeconds(10 * 60L));
        boolean leaveConflict = day.leave().fullDay() && first != null
                || day.leave().morning() && intersects(day.sessions(), day.start(), midpoint, day.now())
                || day.leave().afternoon() && intersects(day.sessions(), midpoint, day.end(), day.now());
        String reason = first != null && day.holiday() ? "Attendance recorded on official holiday"
                : leaveConflict ? "Attendance recorded during approved leave" : null;
        String type = !day.workingDay() ? "DAY_OFF" : day.holiday() ? "HOLIDAY"
                : day.leave().fullDay() ? "ON_LEAVE" : day.leave().any() ? "PARTIAL_LEAVE" : "WORKING_DAY";
        String status = status(type, reason != null, first != null, open, finalized, late, early);
        return new DailyStatusResponse(day.employee(), day.date(), day.start().atZone(day.zone()).toLocalTime(),
                day.end().atZone(day.zone()).toLocalTime(), day.grace(), first, last, late, early,
                reason != null, reason, status, minutes, finalized, type, open,
                hasExpectedWork ? expectedStart : null, hasExpectedWork ? expectedEnd : null);
    }

    public boolean workDuringPortion(String portion, Instant start, Instant end, List<Session> sessions, Instant now) {
        Instant midpoint = start.plus(Duration.between(start, end).dividedBy(2));
        return switch (portion) {
            case "FULL_DAY" -> !sessions.isEmpty();
            case "MORNING" -> intersects(sessions, start, midpoint, now);
            case "AFTERNOON" -> intersects(sessions, midpoint, end, now);
            default -> throw new IllegalArgumentException("Unknown leave portion");
        };
    }

    private static String status(String type, boolean review, boolean worked, boolean open,
            boolean finalized, boolean late, boolean early) {
        if (review) return "REVIEW_REQUIRED";
        if (List.of("DAY_OFF", "HOLIDAY", "ON_LEAVE").contains(type)) return type;
        if (!worked) return "PARTIAL_LEAVE".equals(type) ? "ON_LEAVE" : finalized ? "ABSENT" : "PENDING";
        if (late && early) return "LATE_AND_EARLY_DEPARTURE";
        if (late) return "LATE";
        if (early) return "EARLY_DEPARTURE";
        return open || !finalized ? "IN_PROGRESS" : "ON_TIME";
    }

    private static boolean intersects(List<Session> sessions, Instant start, Instant end, Instant now) {
        return sessions.stream().anyMatch(session -> {
            Instant out = session.out() == null ? now : session.out();
            return !session.in().isBefore(start) && session.in().isBefore(end)
                    || session.in().isBefore(start) && out.isAfter(start);
        });
    }

    public record Session(Instant in, Instant out) {}
    public record Day(UUID employee, LocalDate date, ZoneId zone, Instant start, Instant end, int grace,
            boolean workingDay, boolean holiday, LeaveCoverage leave, List<Session> sessions, Instant now) {}
}
