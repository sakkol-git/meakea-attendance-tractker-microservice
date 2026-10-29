package com.hrflow.notification.domain;

import com.hrflow.notification.entity.NotificationRecord;
import com.hrflow.notification.metrics.NotificationMetrics;
import com.hrflow.notification.organization.NotificationReferences;
import com.hrflow.notification.repository.NotificationRecordRepository;
import com.hrflow.notification.service.TenantSqlScope;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** One owner fact, monotonic inbox receipt and all private recipients commit atomically. */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class NoticeDelivery {
    private final TenantSqlScope scope;
    private final JdbcTemplate jdbc;
    private final NotificationRecordRepository records;
    private final NotificationMetrics metrics;

    public boolean begin(Context fact) {
        scope.use(fact.tenant());
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(?,0))", Object.class,
                "notification.domain:" + fact.tenant() + ":" + fact.topic() + ":" + fact.aggregate());
        int receipt = jdbc.update("INSERT INTO notification.notification_inbox(consumer_name,tenant_id,event_id,topic,aggregate_id) VALUES ('notification-domain',?,?,?,?) ON CONFLICT DO NOTHING",
                fact.tenant(), fact.event(), fact.topic(), fact.aggregate());
        if (receipt == 0) return false;
        var prior = jdbc.queryForList("SELECT employee_id FROM notification.workflow_revision WHERE tenant_id=? AND topic=? AND aggregate_id=?",
                fact.tenant(), fact.topic(), fact.aggregate());
        if (!prior.isEmpty() && !fact.employee().equals(prior.getFirst().get("employee_id")))
            throw new IllegalArgumentException("Notification owner employee identity changed");
        return jdbc.update("""
                INSERT INTO notification.workflow_revision(tenant_id,topic,aggregate_id,employee_id,revision) VALUES (?,?,?,?,?)
                ON CONFLICT(tenant_id,topic,aggregate_id) DO UPDATE SET revision=EXCLUDED.revision
                WHERE notification.workflow_revision.revision<EXCLUDED.revision
                """, fact.tenant(), fact.topic(), fact.aggregate(), fact.employee(), fact.revision()) != 0;
    }

    public void create(Context fact, NotificationReferences.Employee recipient, String title, String kind, boolean critical) {
        if (!recipient.active()) return;
        var preferences = jdbc.queryForList("SELECT in_app FROM notification.notification_preference WHERE tenant_id=? AND employee_id=?", fact.tenant(), recipient.employee());
        if (!critical && !preferences.isEmpty() && !Boolean.TRUE.equals(preferences.getFirst().get("in_app"))) return;
        records.save(NotificationRecord.domain(fact.tenant(), recipient.employee(), title,
                "Open the record to review its current state.", fact.aggregate().toString(), kind, fact.event()));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { metrics.incrementDelivered(); }
        });
    }

    public record Context(UUID tenant, UUID event, UUID aggregate, UUID employee, String topic, long revision) {}
}
