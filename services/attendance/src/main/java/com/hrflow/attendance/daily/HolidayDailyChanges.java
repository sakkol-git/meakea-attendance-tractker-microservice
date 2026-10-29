package com.hrflow.attendance.daily;

import com.hrflow.attendance.service.TenantSqlScope;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Durable metadata fanout over observed daily keys; a batch limit never discards remaining employees. */
@Service
@RequiredArgsConstructor
public class HolidayDailyChanges {
    private final JdbcTemplate jdbc;
    private final TenantSqlScope scope;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID tenant, UUID event, LocalDate oldDate, LocalDate newDate) {
        jdbc.update("INSERT INTO attendance.holiday_daily_change(tenant_id,event_id,old_date,new_date) VALUES (?,?,?,?) ON CONFLICT DO NOTHING",
                tenant, event, oldDate, newDate);
    }

    @Transactional
    public void expandNext(UUID tenant) {
        scope.use(tenant);
        var changes = jdbc.query("""
                SELECT event_id,old_date,new_date,employee_cursor,date_cursor FROM attendance.holiday_daily_change
                WHERE tenant_id=? ORDER BY created_at,event_id LIMIT 1 FOR UPDATE SKIP LOCKED
                """, (row, index) -> new Change(row.getObject(1, UUID.class), row.getObject(2, LocalDate.class),
                        row.getObject(3, LocalDate.class), row.getObject(4, UUID.class), row.getObject(5, LocalDate.class)), tenant);
        if (changes.isEmpty()) return;
        Change change = changes.getFirst();
        var days = jdbc.query("""
                SELECT employee_id,work_date FROM attendance.daily_status WHERE tenant_id=? AND (work_date=? OR work_date=?)
                  AND (?::uuid IS NULL OR (employee_id,work_date)>(?::uuid,?::date))
                ORDER BY employee_id,work_date LIMIT 100
                """, (row, index) -> new Day(row.getObject(1, UUID.class), row.getObject(2, LocalDate.class)), tenant,
                change.oldDate(), change.newDate(), change.employee(), change.employee(), change.date());
        for (Day day : days) {
            // Match all daily producers: employee lock before touching the recalculation queue.
            scope.lockEmployee(tenant, day.employee());
            jdbc.update("""
                    INSERT INTO attendance.daily_recalculation(tenant_id,employee_id,work_date) VALUES (?,?,?)
                    ON CONFLICT(tenant_id,employee_id,work_date) DO UPDATE SET requested_at=now(),available_at=now()
                    """, tenant, day.employee(), day.date());
        }
        if (days.isEmpty()) {
            jdbc.update("DELETE FROM attendance.holiday_daily_change WHERE tenant_id=? AND event_id=?", tenant, change.event());
        } else {
            Day last = days.getLast();
            jdbc.update("UPDATE attendance.holiday_daily_change SET employee_cursor=?,date_cursor=? WHERE tenant_id=? AND event_id=?",
                    last.employee(), last.date(), tenant, change.event());
        }
    }

    private record Change(UUID event, LocalDate oldDate, LocalDate newDate, UUID employee, LocalDate date) {}
    private record Day(UUID employee, LocalDate date) {}
}
