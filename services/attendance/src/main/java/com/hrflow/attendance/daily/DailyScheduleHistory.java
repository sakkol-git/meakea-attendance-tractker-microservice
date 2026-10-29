package com.hrflow.attendance.daily;

import com.hrflow.attendance.service.AttendanceScheduleResolver;
import com.hrflow.attendance.service.TenantSqlScope;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.server.ResponseStatusException;

/** Past results keep their observed schedule; calendar/leave facts can still refresh their outcome. */
@Service
@RequiredArgsConstructor
public class DailyScheduleHistory {
    private final JdbcTemplate jdbc;
    private final TenantSqlScope scope;
    private final AttendanceScheduleResolver schedules;
    private final Clock clock;

    @Transactional(readOnly = true, propagation = Propagation.MANDATORY, noRollbackFor = ErrorResponseException.class)
    public AttendanceScheduleResolver.Resolved resolve(UUID tenant, UUID employee, LocalDate date) {
        var prior = retained(tenant, employee, date);
        if (prior != null && past(date, prior.zone())) return prior;
        var current = schedules.forDate(tenant, employee, date);
        if (prior == null && past(date, current.zone()) && !verifiedEarlier(tenant, employee, date, current))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Original daily schedule is unknown; explicit historical repair is required");
        return current;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void remember(UUID tenant, UUID employee, DailySnapshot snapshot) {
        LocalDate date = snapshot.overview().date();
        var prior = retained(tenant, employee, date);
        if (prior != null && past(date, prior.zone())) return;
        jdbc.update("""
                INSERT INTO attendance.daily_schedule(tenant_id,employee_id,work_date,shift_id,shift_revision,timezone,
                  scheduled_start,scheduled_end,grace_minutes,working_days,employee_reference_revision) VALUES (?,?,?,?,?,?,?,?,?,?,
                    (SELECT revision FROM attendance.employee_projection WHERE tenant_id=? AND employee_id=?))
                ON CONFLICT(tenant_id,employee_id,work_date) DO UPDATE SET shift_id=EXCLUDED.shift_id,
                  shift_revision=EXCLUDED.shift_revision,timezone=EXCLUDED.timezone,scheduled_start=EXCLUDED.scheduled_start,
                  scheduled_end=EXCLUDED.scheduled_end,grace_minutes=EXCLUDED.grace_minutes,working_days=EXCLUDED.working_days,
                  employee_reference_revision=EXCLUDED.employee_reference_revision
                """, tenant, employee, date, snapshot.shiftId(), snapshot.shiftRevision(), snapshot.timezone(),
                Timestamp.from(snapshot.scheduledStart()), Timestamp.from(snapshot.scheduledEnd()), snapshot.graceMinutes(), snapshot.workingDays(), tenant, employee);
    }

    private boolean verifiedEarlier(UUID tenant, UUID employee, LocalDate date, AttendanceScheduleResolver.Resolved current) {
        // A previously materialized day with missing original basis remains an explicit repair, never a backfill guess.
        if (!jdbc.queryForList("SELECT 1 FROM attendance.daily_status WHERE tenant_id=? AND employee_id=? AND work_date=?", tenant, employee, date).isEmpty()) return false;
        return !jdbc.queryForList("""
                SELECT 1 FROM attendance.daily_schedule s JOIN attendance.employee_projection e
                  ON e.tenant_id=s.tenant_id AND e.employee_id=s.employee_id
                WHERE s.tenant_id=? AND s.employee_id=? AND s.work_date<? AND s.shift_id=? AND s.shift_revision=?
                  AND s.employee_reference_revision=e.revision ORDER BY s.work_date DESC LIMIT 1
                """, tenant, employee, date, current.shiftId(), current.revision()).isEmpty();
    }

    private AttendanceScheduleResolver.Resolved retained(UUID tenant, UUID employee, LocalDate date) {
        scope.use(tenant);
        var rows = jdbc.query("""
                SELECT shift_id,shift_revision,timezone,scheduled_start,scheduled_end,grace_minutes,working_days
                FROM attendance.daily_schedule WHERE tenant_id=? AND employee_id=? AND work_date=?
                """, (row, index) -> new AttendanceScheduleResolver.Resolved(row.getObject(1, UUID.class), row.getLong(2), date,
                ZoneId.of(row.getString(3)), row.getTimestamp(4).toInstant(), row.getTimestamp(5).toInstant(), row.getInt(6), row.getInt(7)), tenant, employee, date);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    private boolean past(LocalDate date, ZoneId zone) { return date.isBefore(clock.instant().atZone(zone).toLocalDate()); }
}
