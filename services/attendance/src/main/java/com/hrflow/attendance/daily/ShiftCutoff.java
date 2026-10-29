package com.hrflow.attendance.daily;

import com.hrflow.attendance.service.AttendanceScheduleResolver;
import com.hrflow.attendance.service.DailyAttendanceService;
import com.hrflow.attendance.service.TenantSqlScope;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Durable triggers per observed day/schedule basis, including no-tap employees and restart catch-up. */
@Service
@RequiredArgsConstructor
public class ShiftCutoff {
    private final JdbcTemplate jdbc;
    private final TenantSqlScope scope;
    private final AttendanceScheduleResolver schedule;
    private final DailyAttendanceService daily;
    private final JsonMapper json;
    private final Clock clock;
    private final DailyScheduleHistory history;

    @Transactional(noRollbackFor = org.springframework.web.ErrorResponseException.class)
    public void inspect(UUID tenant, UUID employee) {
        scope.use(tenant);
        scope.lockEmployee(tenant, employee);
        var rechecks = jdbc.queryForList("""
                SELECT work_date FROM attendance.cutoff_recheck WHERE tenant_id=? AND employee_id=? AND available_at<=?
                ORDER BY available_at,work_date LIMIT 1
                """, LocalDate.class, tenant, employee, java.sql.Timestamp.from(clock.instant()));
        if (!rechecks.isEmpty()) {
            LocalDate retry = rechecks.getFirst();
            if (emit(tenant, employee, retry)) {
                jdbc.update("DELETE FROM attendance.cutoff_recheck WHERE tenant_id=? AND employee_id=? AND work_date=?", tenant, employee, retry);
            }
            return;
        }
        var dates = jdbc.queryForList("SELECT next_date FROM attendance.cutoff_cursor WHERE tenant_id=? AND employee_id=?", LocalDate.class, tenant, employee);
        LocalDate date;
        if (dates.isEmpty()) {
            // Start only at the verified local day first observed; no invented historical absence.
            date = schedule.at(tenant, employee, clock.instant()).workDate();
            jdbc.update("INSERT INTO attendance.cutoff_cursor(tenant_id,employee_id,next_date) VALUES (?,?,?)", tenant, employee, date);
        } else date = dates.getFirst();
        if (emit(tenant, employee, date)) {
            jdbc.update("UPDATE attendance.cutoff_cursor SET next_date=? WHERE tenant_id=? AND employee_id=?", date.plusDays(1), tenant, employee);
        }
    }

    private boolean emit(UUID tenant, UUID employee, LocalDate date) {
        var snapshot = daily.snapshot(tenant, employee, date);
        history.remember(tenant, employee, snapshot);
        Instant cutoffAt = snapshot.scheduledEnd().plusSeconds(30 * 60L);
        if (clock.instant().isBefore(cutoffAt)) {
            jdbc.update("UPDATE attendance.cutoff_recheck SET available_at=? WHERE tenant_id=? AND employee_id=? AND work_date=?",
                    java.sql.Timestamp.from(cutoffAt), tenant, employee, date);
            return false;
        }
        UUID aggregate = UUID.nameUUIDFromBytes(("shift-cutoff:" + tenant + ":" + employee + ":" + date
                + ":" + snapshot.shiftId() + ":" + snapshot.shiftRevision() + ":" + snapshot.scheduledEnd())
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var payload = new Payload(employee, date, snapshot.timezone(), snapshot.shiftId(), snapshot.shiftRevision(), snapshot.scheduledEnd(), cutoffAt);
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(?,0))", Object.class, "attendance.stream:" + tenant + ":" + employee);
        jdbc.update("""
                INSERT INTO attendance.attendance_outbox(id,tenant_id,topic,event_id,aggregate_id,aggregate_revision,payload,
                    schema_version,event_type,correlation_id,occurred_at,event_key)
                VALUES (?,?,'time.shift-cutoff.v1',?,?,1,?::jsonb,1,'ShiftCutoff.v1',?,?,?)
                ON CONFLICT(tenant_id,topic,aggregate_id,aggregate_revision) DO NOTHING
                """, UUID.randomUUID(), tenant, UUID.randomUUID(), aggregate, json.writeValueAsString(payload), UUID.randomUUID(),
                java.sql.Timestamp.from(clock.instant()), tenant + ":" + employee);
        return true;
    }

    public record Payload(UUID employeeId, LocalDate workDate, String timezone, UUID shiftId,
            long shiftRevision, Instant scheduledEnd, Instant cutoffAt) {}
}
