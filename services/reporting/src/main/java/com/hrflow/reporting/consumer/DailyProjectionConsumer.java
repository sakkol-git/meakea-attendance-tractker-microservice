package com.hrflow.reporting.consumer;

import com.hrflow.contracts.attendance.AttendanceDailyFact;
import com.hrflow.reporting.service.TenantSqlScope;
import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class DailyProjectionConsumer {
    private final EntityManager entityManager;
    private final TenantSqlScope scope;
    private final JsonMapper json;
    private final DailyReportMapper mapper;

    @Transactional
    public void apply(ConsumerRecord<String, AttendanceDailyFact> record) {
        var id = DailyFactValidation.validate(record);
        var fact = record.value();
        scope.use(id.tenant());
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtextextended(:key,0))")
                .setParameter("key", "reporting.day:" + id.tenant() + ":" + id.employee() + ":" + fact.getWorkDate()).getSingleResult();
        List<?> existing = entityManager.createNativeQuery("""
                SELECT daily_id FROM reporting.daily_fact_projection WHERE tenant_id=:tenant AND employee_id=:employee AND work_date=:date
                """).setParameter("tenant", id.tenant()).setParameter("employee", id.employee()).setParameter("date", fact.getWorkDate()).getResultList();
        if (!existing.isEmpty() && !id.day().equals(existing.getFirst())) throw new IllegalArgumentException("Daily aggregate identity changed");
        int receipt = entityManager.createNativeQuery("""
                INSERT INTO reporting.reporting_inbox(consumer_name,tenant_id,event_id,topic,aggregate_id,aggregate_revision)
                VALUES ('reporting-daily-v1',:tenant,:event,:topic,:day,:revision) ON CONFLICT DO NOTHING
                """).setParameter("tenant", id.tenant()).setParameter("event", id.event()).setParameter("topic", record.topic())
                .setParameter("day", id.day()).setParameter("revision", fact.getAggregateVersion()).executeUpdate();
        if (receipt == 1) entityManager.createNativeQuery("""
                INSERT INTO reporting.daily_fact_projection(tenant_id,employee_id,work_date,daily_id,revision,payload)
                VALUES (:tenant,:employee,:date,:day,:revision,cast(:body as jsonb))
                ON CONFLICT(tenant_id,employee_id,work_date) DO UPDATE SET revision=EXCLUDED.revision,
                  payload=EXCLUDED.payload,updated_at=now()
                WHERE reporting.daily_fact_projection.revision<EXCLUDED.revision
                """).setParameter("tenant", id.tenant()).setParameter("employee", id.employee()).setParameter("date", fact.getWorkDate())
                .setParameter("day", id.day()).setParameter("revision", fact.getAggregateVersion())
                .setParameter("body", json.writeValueAsString(mapper.snapshot(fact))).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT INTO reporting.checkpoint_progress(tenant_id,topic,partition_num,last_offset,last_processed_at,pending_intents)
                VALUES (:tenant,:topic,:partition,:offset,now(),0)
                ON CONFLICT(tenant_id,topic,partition_num) DO UPDATE SET
                  last_offset=EXCLUDED.last_offset,last_processed_at=EXCLUDED.last_processed_at
                WHERE reporting.checkpoint_progress.last_offset<EXCLUDED.last_offset
                """).setParameter("tenant", id.tenant()).setParameter("topic", record.topic())
                .setParameter("partition", record.partition()).setParameter("offset", record.offset()).executeUpdate();
    }
}
