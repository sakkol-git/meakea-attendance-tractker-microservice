package com.hrflow.attendance.daily;

import com.hrflow.attendance.service.TenantSqlScope;
import com.hrflow.contracts.attendance.ShiftCutoffFact;
import java.time.Clock;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "hrflow.attendance.daily-status.enabled", havingValue = "true")
public class ShiftCutoffConsumer {
    private final TenantSqlScope scope;
    private final JdbcTemplate jdbc;
    private final DailyStatusWriter writer;
    private final Clock clock;

    @Transactional
    public void apply(ConsumerRecord<String, ShiftCutoffFact> record) {
        var fact = record.value();
        if (fact == null || !"time.shift-cutoff.v1".equals(record.topic()) || record.partition() < 0 || record.offset() < 0
                || fact.getSchemaVersion() != 1 || fact.getAggregateVersion() != 1 || !"ShiftCutoff.v1".equals(fact.getType())
                || fact.getOccurredAt() == null || fact.getScheduledEnd() == null || fact.getCutoffAt() == null
                || fact.getWorkDate() == null || fact.getShiftRevision() < 1
                || !fact.getCutoffAt().equals(fact.getScheduledEnd().plusSeconds(30 * 60L))
                || fact.getOccurredAt().isBefore(fact.getCutoffAt()) || clock.instant().isBefore(fact.getCutoffAt())
                || !fact.getWorkDate().equals(fact.getScheduledEnd().atZone(ZoneId.of(fact.getTimezone())).toLocalDate())) {
            throw new IllegalArgumentException("Invalid cutoff fact");
        }
        UUID tenant = UUID.fromString(fact.getTenantId()), employee = UUID.fromString(fact.getEmployeeId());
        UUID event = UUID.fromString(fact.getEventId());
        UUID.fromString(fact.getAggregateId());
        UUID.fromString(fact.getCorrelationId());
        UUID.fromString(fact.getShiftId());
        if (!(tenant + ":" + employee).equals(record.key())) throw new IllegalArgumentException("Cutoff key mismatch");
        scope.use(tenant);
        scope.lockEmployee(tenant, employee);
        int receipt = jdbc.update("INSERT INTO attendance.cutoff_inbox(tenant_id,event_id) VALUES (?,?) ON CONFLICT DO NOTHING", tenant, event);
        if (receipt == 0) return;
        writer.refresh(tenant, employee, fact.getWorkDate());
    }
}
