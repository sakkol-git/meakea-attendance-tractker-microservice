package com.hrflow.leave.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

/** Employee-effective assignments and holidays are local owner snapshots, never a guessed company calendar. */
@Service
@RequiredArgsConstructor
public class LeaveWorkCalendar {
    private final TenantSqlScope scope;
    private final JdbcTemplate jdbc;
    private final JsonMapper json;
    private final Clock clock;
    private final org.springframework.beans.factory.ObjectProvider<com.hrflow.leave.organization.LeaveEmployeeBootstrap> bootstrap;

    private final org.springframework.beans.factory.ObjectProvider<com.hrflow.leave.organization.LeaveCalendarBootstrap> calendarBootstrap;

    @Transactional(readOnly = true, noRollbackFor = ErrorResponseException.class)
    public Range resolve(String tenant, String employee, LocalDate from, LocalDate to) {
        scope.use(tenant);
        var calendarReplay = calendarBootstrap.getIfAvailable();
        if (calendarReplay == null || !calendarReplay.isReady()) throw unavailable("Calendar reference replay is not ready");
        var replay = bootstrap.getIfAvailable();
        if (replay == null || !replay.isReady()) throw unavailable("Employee reference replay is not ready");
        var employees = jdbc.queryForList("SELECT active,assignments::text AS assignments FROM leave.employee_projection WHERE tenant_id=? AND employee_id=?", tenant, employee);
        if (employees.isEmpty() || employees.getFirst().get("assignments") == null) throw unavailable("Employee assignment reference is unavailable");
        if (!Boolean.TRUE.equals(employees.getFirst().get("active"))) throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee is not active");
        Assignment[] assignments = json.readValue((String) employees.getFirst().get("assignments"), Assignment[].class);
        Set<LocalDate> holidays = Set.copyOf(jdbc.queryForList("SELECT date FROM leave.holiday_projection WHERE tenant_id=? AND date BETWEEN ? AND ? AND deleted=false",
                LocalDate.class, tenant, from, to));
        Map<LocalDate, Window> working = new LinkedHashMap<>();
        ZoneId firstZone = null;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            LocalDate selectedDate = date;
            List<Assignment> effective = java.util.Arrays.stream(assignments).filter(assignment -> assignment.validFrom() != null
                    && !selectedDate.isBefore(assignment.validFrom()) && (assignment.validTo() == null || !selectedDate.isAfter(assignment.validTo()))).toList();
            if (effective.size() != 1) throw unavailable("Exactly one effective assigned shift is required for each requested date");
            var shifts = jdbc.queryForList("SELECT start_time,end_time,timezone,working_days FROM leave.shift_projection WHERE tenant_id=? AND shift_id=? AND deleted=false",
                    tenant, effective.getFirst().shiftId().toString());
            if (shifts.size() != 1) throw unavailable("Assigned shift reference is unavailable");
            var shift = shifts.getFirst();
            ZoneId zone = ZoneId.of((String) shift.get("timezone"));
            if (firstZone == null) firstZone = zone;
            Instant start = instant(date, time(shift.get("start_time")), zone), end = instant(date, time(shift.get("end_time")), zone);
            if (!end.isAfter(start)) throw unavailable("Assigned shift window is invalid");
            boolean works = (((Number) shift.get("working_days")).intValue() & (1 << (date.getDayOfWeek().getValue() - 1))) != 0;
            if (works && !holidays.contains(date)) working.put(date, new Window(start, end));
        }
        return new Range(Map.copyOf(working), clock.instant().atZone(firstZone).toLocalDate());
    }

    private static Instant instant(LocalDate date, LocalTime time, ZoneId zone) {
        var local = date.atTime(time);
        var offsets = zone.getRules().getValidOffsets(local);
        if (offsets.size() != 1) throw unavailable("Unresolved shift daylight-saving offset");
        return local.toInstant(offsets.getFirst());
    }
    private static LocalTime time(Object value) {
        if (value instanceof LocalTime local) return local;
        if (value instanceof java.sql.Time sql) return sql.toLocalTime();
        throw unavailable("Shift time is unavailable");
    }
    private static ErrorResponseException unavailable(String detail) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, detail);
        problem.setProperty("code", "REFERENCE_NOT_READY");
        return new ErrorResponseException(HttpStatus.SERVICE_UNAVAILABLE, problem, null);
    }
    public record Assignment(UUID assignmentId, UUID shiftId, LocalDate validFrom, LocalDate validTo) {}
    public record Window(Instant start, Instant end) {
        public long seconds(String portion) {
            Duration duration = Duration.between(start, end);
            return ("FULL_DAY".equals(portion) ? duration : duration.dividedBy(2)).toSeconds();
        }
    }
    public record Range(Map<LocalDate, Window> workingDays, LocalDate today) {}
}
