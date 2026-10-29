package com.hrflow.attendance.daily;

import com.hrflow.attendance.service.TenantSqlScope;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Resumable current/future recalculation; the captured calendar defines whether a day is historical. */
@Service
@RequiredArgsConstructor
public class ScheduleDailyChanges {
    private final JdbcTemplate jdbc;
    private final TenantSqlScope scope;
    private final Clock clock;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID tenant, UUID event, String kind, UUID source) {
        jdbc.update("INSERT INTO attendance.schedule_daily_change(tenant_id,event_id,source_kind,source_id,observed_at) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING",
                tenant, event, kind, source, Timestamp.from(clock.instant()));
    }

    @Transactional
    public void expandNext(UUID tenant) {
        scope.use(tenant);
        var changes = jdbc.query("""
                SELECT event_id,source_kind,source_id,observed_at,employee_cursor,date_cursor FROM attendance.schedule_daily_change
                WHERE tenant_id=? ORDER BY created_at,event_id LIMIT 1 FOR UPDATE SKIP LOCKED
                """, (row, index) -> new Change(row.getObject(1, UUID.class), row.getString(2), row.getObject(3, UUID.class),
                        row.getTimestamp(4), row.getObject(5, UUID.class), row.getObject(6, LocalDate.class)), tenant);
        if (changes.isEmpty()) return;
        var change = changes.getFirst();
        var days = jdbc.query("""
                SELECT employee_id,work_date FROM attendance.daily_status WHERE tenant_id=?
                  AND ((?='EMPLOYEE' AND employee_id=?) OR (?='SHIFT' AND payload->>'shiftId'=?))
                  AND (payload->>'timezone' IS NULL OR work_date>=(?::timestamptz AT TIME ZONE (payload->>'timezone'))::date)
                  AND (?::uuid IS NULL OR (employee_id,work_date)>(?::uuid,?::date))
                ORDER BY employee_id,work_date LIMIT 100
                """, (row, index) -> new Day(row.getObject(1, UUID.class), row.getObject(2, LocalDate.class)), tenant,
                change.kind(), change.source(), change.kind(), change.source().toString(), change.observed(),
                change.employee(), change.employee(), change.date());
        for (var day : days) {
            scope.lockEmployee(tenant, day.employee());
            jdbc.update("""
                    INSERT INTO attendance.daily_recalculation(tenant_id,employee_id,work_date) VALUES (?,?,?)
                    ON CONFLICT(tenant_id,employee_id,work_date) DO UPDATE SET requested_at=now(),available_at=now()
                    """, tenant, day.employee(), day.date());
        }
        if (days.isEmpty()) jdbc.update("DELETE FROM attendance.schedule_daily_change WHERE tenant_id=? AND event_id=?", tenant, change.event());
        else {
            var last = days.getLast();
            jdbc.update("UPDATE attendance.schedule_daily_change SET employee_cursor=?,date_cursor=? WHERE tenant_id=? AND event_id=?",
                    last.employee(), last.date(), tenant, change.event());
        }
    }
    private record Change(UUID event, String kind, UUID source, Timestamp observed, UUID employee, LocalDate date) {}
    private record Day(UUID employee, LocalDate date) {}
}
