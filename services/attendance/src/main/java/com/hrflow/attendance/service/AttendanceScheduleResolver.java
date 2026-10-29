package com.hrflow.attendance.service;

import jakarta.persistence.EntityManager;
import java.sql.Time;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

/** Resolve a token-linked employee's effective local snapshot; no transactional-owner HTTP/SQL joins. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, noRollbackFor = ErrorResponseException.class)
public class AttendanceScheduleResolver {
    private final EntityManager entityManager;
    private final TenantSqlScope scope;
    private final JsonMapper json;

    public Resolved at(UUID tenant, UUID employee, Instant instant) {
        return resolve(tenant, employee, zone -> instant.atZone(zone).toLocalDate());
    }

    public Resolved forDate(UUID tenant, UUID employee, LocalDate date) {
        return resolve(tenant, employee, ignored -> date);
    }

    private Resolved resolve(UUID tenant, UUID employee, Function<ZoneId, LocalDate> date) {
        scope.use(tenant);
        @SuppressWarnings("unchecked")
        List<Object[]> employees = entityManager.createNativeQuery("""
                SELECT status, assignments::text FROM attendance.employee_projection
                WHERE tenant_id = :tenant AND employee_id = :employee
                """).setParameter("tenant", tenant).setParameter("employee", employee).getResultList();
        if (employees.size() != 1) throw unavailable("Employee snapshot is not available");
        if (!"ACTIVE".equals(employees.getFirst()[0])) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee is not active");
        }
        Assignment[] assignments;
        try {
            assignments = json.readValue((String) employees.getFirst()[1], Assignment[].class);
        } catch (RuntimeException invalid) {
            throw unavailable("Employee assignment snapshot is invalid");
        }
        var candidates = new ArrayList<Resolved>();
        for (Assignment assignment : assignments) {
            @SuppressWarnings("unchecked")
            List<Object[]> shifts = entityManager.createNativeQuery("""
                    SELECT start_time,end_time,timezone,grace_minutes,revision,working_days
                    FROM attendance.shift_projection
                    WHERE tenant_id = :tenant AND shift_id = :shift AND deleted_at IS NULL
                    """).setParameter("tenant", tenant).setParameter("shift", assignment.shiftId()).getResultList();
            if (shifts.size() != 1) throw unavailable("Assigned shift snapshot is not available");
            Object[] row = shifts.getFirst();
            ZoneId zone = ZoneId.of((String) row[2]);
            LocalDate workDate = date.apply(zone);
            if (assignment.validFrom() == null) throw unavailable("Assignment effective date is missing");
            if (workDate.isBefore(assignment.validFrom())
                    || assignment.validTo() != null && workDate.isAfter(assignment.validTo())) continue;
            Instant start = resolvedInstant(workDate, time(row[0]), zone);
            Instant end = resolvedInstant(workDate, time(row[1]), zone);
            if (!end.isAfter(start)) throw unavailable("Assigned shift window is invalid");
            candidates.add(new Resolved(assignment.shiftId(), ((Number) row[4]).longValue(),
                    workDate, zone, start, end, ((Number) row[3]).intValue(), ((Number) row[5]).intValue()));
        }
        if (candidates.size() != 1) throw unavailable("Exactly one effective shift assignment is required");
        return candidates.getFirst();
    }

    private static Instant resolvedInstant(LocalDate date, LocalTime time, ZoneId zone) {
        var local = date.atTime(time);
        var offsets = zone.getRules().getValidOffsets(local);
        if (offsets.size() != 1) throw unavailable("Shift has an unresolved daylight-saving offset");
        return local.toInstant(offsets.getFirst());
    }

    private static LocalTime time(Object value) {
        if (value instanceof LocalTime local) return local;
        if (value instanceof Time sql) return sql.toLocalTime();
        throw unavailable("Assigned shift time is invalid");
    }

    private static ErrorResponseException unavailable(String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, detail);
        problem.setProperty("code", "REFERENCE_NOT_READY");
        return new ErrorResponseException(HttpStatus.SERVICE_UNAVAILABLE, problem, null);
    }

    record Assignment(UUID assignmentId, UUID shiftId, LocalDate validFrom, LocalDate validTo) {}

    public record Resolved(UUID shiftId, long revision, LocalDate workDate, ZoneId zone,
            Instant startsAt, Instant endsAt, int graceMinutes, int workingDays) {}
}
