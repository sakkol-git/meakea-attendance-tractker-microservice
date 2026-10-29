package com.hrflow.attendance.daily;

import com.hrflow.attendance.service.DailyAttendanceService;
import com.hrflow.attendance.service.TenantSqlScope;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Day revision, effective snapshot and immutable publication intent commit with the owner command. */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hrflow.attendance.daily-status.enabled", havingValue = "true")
public class DailyStatusWriter {
    private final JdbcTemplate jdbc;
    private final TenantSqlScope scope;
    private final DailyAttendanceService daily;
    private final JsonMapper json;
    private final Clock clock;
    private final DailyScheduleHistory history;

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean refresh(UUID tenant, UUID employee, LocalDate date) {
        scope.use(tenant);
        scope.lockEmployee(tenant, employee);
        DailySnapshot snapshot;
        try { snapshot = daily.snapshot(tenant, employee, date); }
        catch (org.springframework.web.ErrorResponseException unavailable) {
            if (unavailable.getStatusCode().value() != 503) throw unavailable;
            // A missing old calendar must not reject a genuine tap or manufacture a daily fact.
            jdbc.update("INSERT INTO attendance.daily_recalculation(tenant_id,employee_id,work_date) VALUES (?,?,?) ON CONFLICT DO NOTHING",
                    tenant, employee, date);
            return false;
        }
        history.remember(tenant, employee, snapshot);
        retainCutoff(tenant, employee, date, snapshot);
        String payload = json.writeValueAsString(snapshot);
        var prior = jdbc.queryForList("""
                SELECT id,revision,(payload=?::jsonb) AS unchanged FROM attendance.daily_status
                WHERE tenant_id=? AND employee_id=? AND work_date=? FOR UPDATE
                """, payload, tenant, employee, date);
        if (!prior.isEmpty() && Boolean.TRUE.equals(prior.getFirst().get("unchanged"))) {
            clear(tenant, employee, date);
            return true;
        }
        UUID aggregate = prior.isEmpty() ? UUID.randomUUID() : (UUID) prior.getFirst().get("id");
        long revision = prior.isEmpty() ? 1 : Math.addExact(((Number) prior.getFirst().get("revision")).longValue(), 1L);
        jdbc.update("""
                INSERT INTO attendance.daily_status(tenant_id,employee_id,work_date,id,revision,payload)
                VALUES (?,?,?,?,?,?::jsonb) ON CONFLICT(tenant_id,employee_id,work_date) DO UPDATE
                SET revision=EXCLUDED.revision,payload=EXCLUDED.payload,updated_at=now()
                """, tenant, employee, date, aggregate, revision, payload);
        // Same employee stream lock/order as session publication; envelope metadata is contextual.
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(?,0))", Object.class,
                "attendance.stream:" + tenant + ":" + employee);
        jdbc.update("""
                INSERT INTO attendance.attendance_outbox(id,tenant_id,topic,event_id,aggregate_id,aggregate_revision,
                    payload,schema_version,event_type,correlation_id,occurred_at,event_key)
                VALUES (?,?,'attendance.daily-status.v1',?,?,?,?::jsonb,1,'AttendanceDailySnapshot.v1',?,?,?)
                """, UUID.randomUUID(), tenant, UUID.randomUUID(), aggregate, revision, payload,
                UUID.randomUUID(), java.sql.Timestamp.from(clock.instant()), tenant + ":" + employee);
        clear(tenant, employee, date);
        return true;
    }

    private void clear(UUID tenant, UUID employee, LocalDate date) {
        jdbc.update("DELETE FROM attendance.daily_recalculation WHERE tenant_id=? AND employee_id=? AND work_date=?", tenant, employee, date);
    }

    private void retainCutoff(UUID tenant, UUID employee, LocalDate date, DailySnapshot snapshot) {
        var cutoff = snapshot.scheduledEnd().plusSeconds(30 * 60L);
        if (clock.instant().isBefore(cutoff)) {
            jdbc.update("""
                    INSERT INTO attendance.cutoff_recheck(tenant_id,employee_id,work_date,available_at) VALUES (?,?,?,?)
                    ON CONFLICT(tenant_id,employee_id,work_date) DO UPDATE SET available_at=EXCLUDED.available_at
                    """, tenant, employee, date, java.sql.Timestamp.from(cutoff));
        } else jdbc.update("DELETE FROM attendance.cutoff_recheck WHERE tenant_id=? AND employee_id=? AND work_date=?", tenant, employee, date);
    }
}
