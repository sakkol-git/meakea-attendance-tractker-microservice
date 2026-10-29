package com.hrflow.reporting.consumer;

import com.hrflow.contracts.leave.LeaveRequestFact;
import com.hrflow.reporting.service.TenantSqlScope;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Owner working dates and debit are facts; review never changes a balance in this service. */
@Service
@RequiredArgsConstructor
public class LeaveProjectionConsumer {
    private final EntityManager entityManager;
    private final TenantSqlScope scope;
    private final JsonMapper json;

    @Transactional
    public void apply(ConsumerRecord<String, LeaveRequestFact> record) {
        var id = LeaveFactValidation.validate(record);
        var fact = record.value();
        scope.use(id.tenant());
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtextextended(:key,0))")
                .setParameter("key", "reporting.leave:" + id.tenant() + ":" + id.leave()).getSingleResult();
        int inserted = entityManager.createNativeQuery("""
                INSERT INTO reporting.reporting_inbox
                  (consumer_name,tenant_id,event_id,topic,aggregate_id,aggregate_revision)
                VALUES ('reporting-leave-v1',:tenant,:event,:topic,:aggregate,:revision) ON CONFLICT DO NOTHING
                """).setParameter("tenant", id.tenant()).setParameter("event", id.event())
                .setParameter("topic", record.topic()).setParameter("aggregate", id.leave())
                .setParameter("revision", fact.getAggregateVersion()).executeUpdate();
        if (inserted == 1) {
            entityManager.createNativeQuery("""
                    INSERT INTO reporting.leave_fact_projection
                      (tenant_id,leave_id,employee_id,leave_type,start_date,end_date,portion_days,status,
                       aggregate_revision,portion,working_dates)
                    VALUES (:tenant,:leave,:employee,:type,:start,:end,:days,:state,:revision,:portion,cast(:dates as jsonb))
                    ON CONFLICT (tenant_id,leave_id) DO UPDATE SET employee_id=EXCLUDED.employee_id,
                      leave_type=EXCLUDED.leave_type,start_date=EXCLUDED.start_date,end_date=EXCLUDED.end_date,
                      portion_days=EXCLUDED.portion_days,status=EXCLUDED.status,aggregate_revision=EXCLUDED.aggregate_revision,
                      portion=EXCLUDED.portion,working_dates=EXCLUDED.working_dates,updated_at=now()
                    WHERE reporting.leave_fact_projection.aggregate_revision<EXCLUDED.aggregate_revision
                    """).setParameter("tenant", id.tenant()).setParameter("leave", id.leave())
                    .setParameter("employee", id.employee()).setParameter("type", fact.getLeaveType())
                    .setParameter("start", fact.getStartDate()).setParameter("end", fact.getEndDate())
                    .setParameter("days", new BigDecimal(fact.getChargedDays())).setParameter("state", fact.getState())
                    .setParameter("revision", fact.getAggregateVersion()).setParameter("portion", fact.getPortion())
                    .setParameter("dates", json.writeValueAsString(fact.getWorkingDates())).executeUpdate();
        }
        entityManager.createNativeQuery("""
                INSERT INTO reporting.checkpoint_progress(tenant_id,topic,partition_num,last_offset,last_processed_at,pending_intents)
                VALUES (:tenant,:topic,:partition,:offset,now(),0)
                ON CONFLICT (tenant_id,topic,partition_num) DO UPDATE
                SET last_offset=EXCLUDED.last_offset,last_processed_at=EXCLUDED.last_processed_at
                WHERE reporting.checkpoint_progress.last_offset<EXCLUDED.last_offset
                """).setParameter("tenant", id.tenant()).setParameter("topic", record.topic())
                .setParameter("partition", record.partition()).setParameter("offset", record.offset()).executeUpdate();
    }
}
