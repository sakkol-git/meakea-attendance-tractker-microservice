package com.hrflow.notification.consumer;

import com.hrflow.notification.entity.NotificationRecord;
import com.hrflow.notification.repository.NotificationRecordRepository;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
public class NotificationConsumerService {

    private static final String CONSUMER_NAME = "notification-consumer";

    private final JdbcTemplate jdbc;
    private final NotificationRecordRepository recordRepository;
    private final TransactionTemplate transactionTemplate;
    private final com.hrflow.notification.metrics.NotificationMetrics metrics;
    private final com.hrflow.notification.service.TenantSqlScope scope;

    public NotificationConsumerService(JdbcTemplate jdbc,
                                     NotificationRecordRepository recordRepository,
                                     TransactionTemplate transactionTemplate,
                                     com.hrflow.notification.metrics.NotificationMetrics metrics,
                                     com.hrflow.notification.service.TenantSqlScope scope) {
        this.jdbc = jdbc;
        this.recordRepository = recordRepository;
        this.transactionTemplate = transactionTemplate;
        this.metrics = metrics;
        this.scope = scope;
    }

    public boolean processIncomingEvent(UUID tenantId,
                                        UUID eventId,
                                        String topic,
                                        UUID aggregateId,
                                        UUID recipientId,
                                        String title,
                                        String body,
                                        String recordId,
                                        String kind,
                                        Runnable onCommitAck) {
        Boolean committed = transactionTemplate.execute(status -> {
            scope.use(tenantId);
            // Serialize receipt contenders before reading absence; the unique key remains a final guard.
            jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",
                    Object.class, "notification.event:" + tenantId + ":" + eventId);
            // 1. Check inbox deduplication
            Integer existing = jdbc.queryForObject(
                    "SELECT count(*) FROM notification.notification_inbox WHERE consumer_name = ? AND tenant_id = ? AND event_id = ?",
                    Integer.class,
                    CONSUMER_NAME, tenantId, eventId
            );

            if (existing != null && existing > 0) {
                log.info("Duplicate event receipt detected for eventId={}, topic={}, skipping effect", eventId, topic);
                afterCommit(onCommitAck);
                return false;
            }

            // 2. Persist consumer inbox receipt
            jdbc.update(
                    """
                    INSERT INTO notification.notification_inbox (consumer_name, tenant_id, event_id, topic, aggregate_id)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    CONSUMER_NAME, tenantId, eventId, topic, aggregateId
            );

            // 3. Persist notification record
            NotificationRecord record = new NotificationRecord(tenantId, recipientId, title, body, recordId, kind);
            recordRepository.save(record);
            afterCommit(() -> {
                metrics.incrementDelivered();
                if (onCommitAck != null) onCommitAck.run();
            });

            return true;
        });

        return Boolean.TRUE.equals(committed);
    }

    private void afterCommit(Runnable action) {
        if (action == null) return;
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override public void afterCommit() { action.run(); }
                });
    }
}
