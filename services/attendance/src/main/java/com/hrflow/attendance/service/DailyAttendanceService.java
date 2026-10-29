package com.hrflow.attendance.service;

import com.hrflow.attendance.dto.DailyStatusResponse;
import com.hrflow.attendance.entity.AttendanceSession;
import com.hrflow.attendance.leave.LeaveProjection;
import com.hrflow.attendance.repository.AttendanceSessionRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Loads only attendance-owned facts, then passes explicit inputs to the pure day calculation. */
@Service
@RequiredArgsConstructor
public class DailyAttendanceService {
    private final AttendanceSessionRepository sessions;
    private final AttendanceScheduleResolver schedule;
    private final LeaveProjection leave;
    private final AttendanceCalculationService calculation;
    private final TenantSqlScope scope;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final com.hrflow.attendance.daily.DailySnapshotMapper mapper;
    private final com.hrflow.attendance.daily.DailyScheduleHistory history;

    @Transactional(readOnly = true)
    public DailyStatusResponse forEmployee(UUID tenant, UUID employee, LocalDate requested) {
        return snapshot(tenant, employee, requested).overview();
    }

    @Transactional(readOnly = true, noRollbackFor = org.springframework.web.ErrorResponseException.class)
    public com.hrflow.attendance.daily.DailySnapshot snapshot(UUID tenant, UUID employee, LocalDate requested) {
        scope.use(tenant);
        var current = requested == null ? schedule.at(tenant, employee, clock.instant()) : null;
        var date = current == null ? requested : current.workDate();
        var all = sessions.findByTenantIdAndEmployeeIdOrderByClockInDesc(tenant, employee);
        List<AttendanceSession> day = all.stream().filter(session -> date.equals(session.getWorkDate()))
                .sorted(Comparator.comparing(AttendanceSession::getClockIn)).toList();
        var basis = day.isEmpty() ? (current == null ? history.resolve(tenant, employee, date) : current) : captured(day.getFirst());
        boolean legacy = all.stream().anyMatch(session -> session.getWorkDate() == null
                && session.getClockIn().atZone(basis.zone()).toLocalDate().equals(date));
        if (legacy || day.stream().anyMatch(session -> !basis.equals(captured(session)))) {
            throw unavailable("Daily session schedules are unknown or disagree; explicit correction is required");
        }
        boolean holiday = !jdbc.queryForList("""
                SELECT 1 FROM attendance.holiday_projection WHERE tenant_id=? AND date=? AND deleted_at IS NULL
                """, tenant, date).isEmpty();
        boolean workingDay = (basis.workingDays() & (1 << (date.getDayOfWeek().getValue() - 1))) != 0;
        var intervals = day.stream().map(session -> new AttendanceCalculationService.Session(
                session.getClockIn(), session.getClockOut())).toList();
        var overview = calculation.calculate(new AttendanceCalculationService.Day(employee, date, basis.zone(),
                basis.startsAt(), basis.endsAt(), basis.graceMinutes(), workingDay, holiday,
                leave.coverage(tenant, employee, date), intervals, clock.instant()));
        var references = day.stream().map(session -> new com.hrflow.attendance.daily.DailySnapshot.SessionBasis(
                session.getId(), Math.addExact(session.getVersion(), 1L))).toList();
        var leaveReferences = leave.references(tenant, employee, date).stream()
                .map(ref -> new com.hrflow.attendance.daily.DailySnapshot.LeaveBasis(ref.leaveId(), ref.revision(), ref.portion(),
                        calculation.workDuringPortion(ref.portion(), basis.startsAt(), basis.endsAt(), intervals, clock.instant())))
                .toList();
        return mapper.snapshot(overview, basis, leaveReferences, references);
    }

    private AttendanceScheduleResolver.Resolved captured(AttendanceSession session) {
        if (session.getWorkDate() == null || session.getTimezone() == null || session.getShiftId() == null
                || session.getShiftRevision() == null || session.getScheduledStart() == null
                || session.getScheduledEnd() == null || session.getGraceMinutes() == null || session.getWorkingDays() == null) {
            throw unavailable("Daily status requires a verified captured schedule and weekday calendar");
        }
        return new AttendanceScheduleResolver.Resolved(session.getShiftId(), session.getShiftRevision(), session.getWorkDate(),
                ZoneId.of(session.getTimezone()), session.getScheduledStart(), session.getScheduledEnd(),
                session.getGraceMinutes(), session.getWorkingDays());
    }

    private ResponseStatusException unavailable(String message) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
