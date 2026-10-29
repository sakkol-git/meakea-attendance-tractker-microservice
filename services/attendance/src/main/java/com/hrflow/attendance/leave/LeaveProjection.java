package com.hrflow.attendance.leave;

import com.hrflow.attendance.service.TenantSqlScope;
import com.hrflow.contracts.leave.LeaveRequestFact;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Attendance reads owner dates locally; it has no authority to change leave or its balance. */
@Service
@RequiredArgsConstructor
public class LeaveProjection {
    private final JdbcTemplate jdbc;
    private final JsonMapper json;
    private final TenantSqlScope scope;

    @Transactional
    public void apply(ConsumerRecord<String, LeaveRequestFact> record) {
        var id = LeaveFactValidation.validate(record);
        var fact = record.value();
        scope.use(id.tenant());
        scope.lockEmployee(id.tenant(), id.employee());
        var previous = jdbc.queryForList("SELECT employee_id,working_dates::text AS dates FROM attendance.leave_projection WHERE tenant_id=? AND leave_id=?",
                id.tenant(), id.leave());
        if (!previous.isEmpty() && !id.employee().equals(previous.getFirst().get("employee_id"))) {
            throw new IllegalArgumentException("Leave owner employee identity changed");
        }
        int receipt = jdbc.update("INSERT INTO attendance.leave_inbox(tenant_id,event_id) VALUES (?,?) ON CONFLICT DO NOTHING",
                id.tenant(), id.event());
        if (receipt == 0) return;
        int applied = jdbc.update("""
                INSERT INTO attendance.leave_projection(tenant_id,leave_id,employee_id,portion,state,working_dates,charged_days,revision)
                VALUES (?,?,?,?,?,?::jsonb,?,?)
                ON CONFLICT(tenant_id,leave_id) DO UPDATE SET employee_id=EXCLUDED.employee_id,
                  portion=EXCLUDED.portion,state=EXCLUDED.state,working_dates=EXCLUDED.working_dates,
                  charged_days=EXCLUDED.charged_days,revision=EXCLUDED.revision
                WHERE attendance.leave_projection.revision<EXCLUDED.revision
                """, id.tenant(), id.leave(), id.employee(), fact.getPortion(), fact.getState(),
                json.writeValueAsString(fact.getWorkingDates()), new BigDecimal(fact.getChargedDays()), fact.getAggregateVersion());
        if (applied == 0) return;
        var dates = new java.util.TreeSet<>(fact.getWorkingDates());
        if (!previous.isEmpty()) dates.addAll(java.util.Arrays.asList(json.readValue((String) previous.getFirst().get("dates"), LocalDate[].class)));
        jdbc.batchUpdate("""
                INSERT INTO attendance.daily_recalculation(tenant_id,employee_id,work_date) VALUES (?,?,?)
                ON CONFLICT(tenant_id,employee_id,work_date) DO UPDATE SET requested_at=now(),available_at=now()
                """, dates, 100, (statement, date) -> {
                    statement.setObject(1, id.tenant());
                    statement.setObject(2, id.employee());
                    statement.setObject(3, date);
                });
    }

    @Transactional(readOnly = true)
    public boolean fullDay(UUID tenant, UUID employee, LocalDate date) {
        return coverage(tenant, employee, date).fullDay();
    }

    @Transactional(readOnly = true)
    public LeaveCoverage coverage(UUID tenant, UUID employee, LocalDate date) {
        scope.use(tenant);
        List<String> portions = jdbc.queryForList("""
                SELECT portion FROM attendance.leave_projection WHERE tenant_id=? AND employee_id=?
                  AND state IN ('APPROVED','NEEDS_REVIEW') AND working_dates @> ?::jsonb
                """, String.class, tenant, employee, json.writeValueAsString(List.of(date)));
        return new LeaveCoverage(portions.contains("FULL_DAY") || portions.contains("MORNING"),
                portions.contains("FULL_DAY") || portions.contains("AFTERNOON"));
    }

    @Transactional(readOnly = true)
    public List<com.hrflow.attendance.daily.DailySnapshot.LeaveBasis> references(UUID tenant, UUID employee, LocalDate date) {
        scope.use(tenant);
        return jdbc.query("""
                SELECT leave_id,revision,portion FROM attendance.leave_projection
                WHERE tenant_id=? AND employee_id=? AND state IN ('APPROVED','NEEDS_REVIEW')
                  AND working_dates @> ?::jsonb ORDER BY leave_id
                """, (row, index) -> new com.hrflow.attendance.daily.DailySnapshot.LeaveBasis(
                        row.getObject(1, UUID.class), row.getLong(2), row.getString(3)),
                tenant, employee, json.writeValueAsString(List.of(date)));
    }
}
