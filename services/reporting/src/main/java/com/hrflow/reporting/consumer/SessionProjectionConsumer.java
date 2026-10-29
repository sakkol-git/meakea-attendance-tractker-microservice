package com.hrflow.reporting.consumer;

import com.hrflow.contracts.attendance.AttendanceSessionFact;
import com.hrflow.reporting.service.TenantSqlScope;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Receipt, monotonic full snapshot and consumed position commit together before acknowledgment. */
@Service
@RequiredArgsConstructor
public class SessionProjectionConsumer {
    public static final String CONSUMER = "reporting-session-v1";
    private final EntityManager entityManager;
    private final TenantSqlScope scope;

    @Transactional
    public void apply(ConsumerRecord<String, AttendanceSessionFact> record) {
        var id = SessionFactValidation.validate(record);
        var fact = record.value();
        scope.use(id.tenant());
        // A missing projection row cannot be protected by SELECT FOR UPDATE. Lock the logical key first.
        lock("reporting.partition:" + id.tenant() + ":" + record.partition());
        lock("reporting.session:" + id.tenant() + ":" + id.session());
        int inserted = entityManager.createNativeQuery("""
                INSERT INTO reporting.reporting_inbox
                  (consumer_name,tenant_id,event_id,topic,aggregate_id,aggregate_revision)
                VALUES (:consumer,:tenant,:event,:topic,:aggregate,:revision) ON CONFLICT DO NOTHING
                """).setParameter("consumer", CONSUMER).setParameter("tenant", id.tenant())
                .setParameter("event", id.event()).setParameter("topic", record.topic())
                .setParameter("aggregate", id.session()).setParameter("revision", fact.getAggregateVersion())
                .executeUpdate();
        if (inserted == 1) {
            Integer worked = fact.getClockOut() == null ? null : Math.toIntExact(
                    Duration.between(fact.getClockIn(), fact.getClockOut()).toMinutes());
            // Raw elapsed minutes are session facts; daily status/leave precedence are not inferred here.
            int late = fact.getClockIn().isAfter(fact.getScheduledStart().plusSeconds(60L * fact.getGraceMinutes()))
                    ? Math.toIntExact(Duration.between(fact.getScheduledStart(), fact.getClockIn()).toMinutes()) : 0;
            entityManager.createNativeQuery("""
                    INSERT INTO reporting.attendance_fact_projection
                      (tenant_id,session_id,employee_id,work_date,status,minutes_late,minutes_worked,
                       aggregate_revision,clock_in,clock_out,timezone,scheduled_start,scheduled_end,grace_minutes,source)
                    VALUES (:tenant,:session,:employee,:date,:state,:late,cast(:worked as integer),:revision,
                            :clockIn,cast(:clockOut as timestamptz),:zone,:start,:end,:grace,:source)
                    ON CONFLICT (tenant_id,session_id) DO UPDATE SET
                      employee_id=EXCLUDED.employee_id,work_date=EXCLUDED.work_date,status=EXCLUDED.status,
                      minutes_late=EXCLUDED.minutes_late,minutes_worked=EXCLUDED.minutes_worked,
                      aggregate_revision=EXCLUDED.aggregate_revision,clock_in=EXCLUDED.clock_in,
                      clock_out=EXCLUDED.clock_out,timezone=EXCLUDED.timezone,scheduled_start=EXCLUDED.scheduled_start,
                      scheduled_end=EXCLUDED.scheduled_end,grace_minutes=EXCLUDED.grace_minutes,
                      source=EXCLUDED.source,updated_at=now()
                    WHERE reporting.attendance_fact_projection.aggregate_revision<EXCLUDED.aggregate_revision
                    """).setParameter("tenant", id.tenant()).setParameter("session", id.session())
                    .setParameter("employee", id.employee()).setParameter("date", fact.getWorkDate())
                    .setParameter("state", fact.getState()).setParameter("late", late).setParameter("worked", worked)
                    .setParameter("revision", fact.getAggregateVersion()).setParameter("clockIn", fact.getClockIn())
                    .setParameter("clockOut", fact.getClockOut()).setParameter("zone", fact.getTimezone())
                    .setParameter("start", fact.getScheduledStart()).setParameter("end", fact.getScheduledEnd())
                    .setParameter("grace", fact.getGraceMinutes()).setParameter("source", fact.getSource()).executeUpdate();
        }
        entityManager.createNativeQuery("""
                INSERT INTO reporting.checkpoint_progress (tenant_id,topic,partition_num,last_offset,last_processed_at,pending_intents)
                VALUES (:tenant,:topic,:partition,:offset,now(),0)
                ON CONFLICT (tenant_id,topic,partition_num) DO UPDATE
                SET last_offset=EXCLUDED.last_offset,last_processed_at=EXCLUDED.last_processed_at
                WHERE reporting.checkpoint_progress.last_offset<EXCLUDED.last_offset
                """).setParameter("tenant", id.tenant()).setParameter("topic", record.topic())
                .setParameter("partition", record.partition()).setParameter("offset", record.offset()).executeUpdate();
    }

    private void lock(String key) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtextextended(:key,0))")
                .setParameter("key", key).getSingleResult();
    }
}
