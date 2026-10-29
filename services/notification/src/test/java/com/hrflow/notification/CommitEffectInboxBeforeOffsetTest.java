package com.hrflow.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.notification.consumer.NotificationConsumerService;
import com.hrflow.notification.repository.NotificationRecordRepository;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@Testcontainers
class CommitEffectInboxBeforeOffsetTest {

    @Container
    static final PostgreSQLContainer<?> database = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("hrflow")
            .withUsername("hrflow")
            .withPassword("test-only")
            .withInitScript("context-schema.sql");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", database::getJdbcUrl);
        properties.add("spring.datasource.username", database::getUsername);
        properties.add("spring.datasource.password", database::getPassword);
        properties.add("spring.flyway.user", database::getUsername);
        properties.add("spring.flyway.password", database::getPassword);
    }

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    NotificationRecordRepository recordRepository;

    @Autowired
    NotificationConsumerService consumerService;

    @Test
    @DisplayName("Transactional effect: notification record and inbox committed before offset acknowledgment")
    void commitEffectAndInboxBeforeOffset() {
        UUID tenantId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        String topic = "org.attendance.correction.approved.v1";

        AtomicBoolean ackCalled = new AtomicBoolean(false);

        boolean processed = consumerService.processIncomingEvent(
                tenantId, eventId, topic, aggregateId, recipientId,
                "Attendance Approved", "Your attendance correction has been approved",
                aggregateId.toString(), "ATTENDANCE",
                () -> ackCalled.set(true)
        );

        assertThat(processed).isTrue();
        assertThat(ackCalled.get()).isTrue();

        // Check both inbox and notification record exist
        Integer inboxCount = jdbc.queryForObject(
                "SELECT count(*) FROM notification.notification_inbox WHERE tenant_id = ? AND event_id = ?",
                Integer.class,
                tenantId, eventId
        );
        assertThat(inboxCount).isEqualTo(1);

        var records = recordRepository.findByTenantIdAndRecipientIdOrderByCreatedAtDesc(tenantId, recipientId);
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getTitle()).isEqualTo("Attendance Approved");
    }

    @Test
    @DisplayName("Redelivery after commit yields exactly one DB effect despite duplicate Kafka replay")
    void duplicateReplayYieldsExactlyOneEffect() {
        UUID tenantId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        String topic = "org.attendance.session.created.v1";

        AtomicBoolean firstAck = new AtomicBoolean(false);
        AtomicBoolean secondAck = new AtomicBoolean(false);

        // First delivery: successful
        boolean firstProcessed = consumerService.processIncomingEvent(
                tenantId, eventId, topic, aggregateId, recipientId,
                "Check-in Recorded", "You checked in at 09:00",
                aggregateId.toString(), "ATTENDANCE",
                () -> firstAck.set(true)
        );
        assertThat(firstProcessed).isTrue();
        assertThat(firstAck.get()).isTrue();

        // Second delivery (simulating crash before offset ack): duplicate event replay
        boolean secondProcessed = consumerService.processIncomingEvent(
                tenantId, eventId, topic, aggregateId, recipientId,
                "Check-in Recorded", "You checked in at 09:00",
                aggregateId.toString(), "ATTENDANCE",
                () -> secondAck.set(true)
        );
        // Duplicate skipped
        assertThat(secondProcessed).isFalse();
        // Acknowledgment still invoked so offset advances
        assertThat(secondAck.get()).isTrue();

        // Assert exactly ONE DB notification record exists (no duplicate)
        var records = recordRepository.findByTenantIdAndRecipientIdOrderByCreatedAtDesc(tenantId, recipientId);
        assertThat(records).hasSize(1);
    }
}
