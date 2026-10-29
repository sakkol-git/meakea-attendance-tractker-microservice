package com.hrflow.reporting.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.contracts.attendance.AttendanceCheckedIn;
import com.hrflow.contracts.attendance.Source;
import com.hrflow.reporting.dto.AttendanceFactEvent;
import com.hrflow.reporting.dto.FreshnessStatus;
import com.hrflow.reporting.dto.LeaveFactEvent;
import com.hrflow.reporting.dto.RequestFactEvent;
import com.hrflow.reporting.entity.AttendanceFactProjection;
import com.hrflow.reporting.entity.CheckpointProgress;
import com.hrflow.reporting.entity.LeaveFactProjection;
import com.hrflow.reporting.entity.RequestFactProjection;
import com.hrflow.reporting.repository.AttendanceFactProjectionRepository;
import com.hrflow.reporting.repository.CheckpointProgressRepository;
import com.hrflow.reporting.repository.LeaveFactProjectionRepository;
import com.hrflow.reporting.repository.ReportingInboxRepository;
import com.hrflow.reporting.repository.RequestFactProjectionRepository;
import com.hrflow.reporting.service.FreshnessService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
                "spring.kafka.listener.auto-startup=false",
                "hrflow.reporting.kafka-consumer.enabled=true"
})
@Testcontainers
class WireCommittedKafkaOffsetsToFreshnessTest {

        @Container
        static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
                        .withDatabaseName("hrflow")
                        .withUsername("hrflow")
                        .withPassword("test-only")
                        .withInitScript("context-schema.sql");

        @DynamicPropertySource
        static void configureProperties(DynamicPropertyRegistry registry) {
                registry.add("spring.datasource.url", postgres::getJdbcUrl);
                registry.add("spring.datasource.username", postgres::getUsername);
                registry.add("spring.datasource.password", postgres::getPassword);
                registry.add("spring.flyway.user", postgres::getUsername);
                registry.add("spring.flyway.password", postgres::getPassword);
        }

        @Autowired
        ReportingKafkaListener kafkaListener;

        @Autowired
        ReportingProjectionConsumer projectionConsumer;

        @Autowired
        FreshnessService freshnessService;

        @Autowired
        AttendanceFactProjectionRepository attendanceRepo;

        @Autowired
        LeaveFactProjectionRepository leaveRepo;

        @Autowired
        RequestFactProjectionRepository requestRepo;

        @Autowired
        ReportingInboxRepository inboxRepo;

        @Autowired
        CheckpointProgressRepository checkpointRepo;

        @Test
        @DisplayName("RPT-19: AttendanceFact committed offset advances freshness checkpoint and acknowledges Kafka offset")
        void attendanceFactCommittedOffsetAdvancesFreshnessAndAcknowledges() {
                UUID tenantId = UUID.randomUUID();
                UUID sessionId = UUID.randomUUID();
                UUID employeeId = UUID.randomUUID();
                UUID eventId = UUID.randomUUID();
                LocalDate workDate = LocalDate.of(2026, 10, 20);
                String topic = "hrflow.attendance.facts";
                int partition = 0;
                long offset = 100L;

                AttendanceFactEvent event = new AttendanceFactEvent(
                                eventId, tenantId, sessionId, employeeId,
                                workDate, "PRESENT", 0, 480, 1L);
                ConsumerRecord<String, AttendanceFactEvent> record = new ConsumerRecord<>(topic, partition, offset,
                                sessionId.toString(), event);

                AtomicBoolean ackCalled = new AtomicBoolean(false);
                Acknowledgment ack = () -> ackCalled.set(true);

                kafkaListener.onAttendanceFact(record, ack);

                // 1. Verify projection committed
                AttendanceFactProjection projection = attendanceRepo.findByTenantIdAndSessionId(tenantId, sessionId)
                                .orElseThrow();
                assertThat(projection.getAggregateRevision()).isEqualTo(1L);
                assertThat(projection.getStatus()).isEqualTo("PRESENT");

                // 2. Verify inbox committed
                assertThat(inboxRepo.existsByConsumerAndEvent(ReportingProjectionConsumer.CONSUMER_NAME, tenantId,
                                eventId)).isTrue();

                // 3. Verify freshness checkpoint recorded with committed offset and 0 pending
                // intents
                CheckpointProgress progress = checkpointRepo.findByTenantTopicPartition(tenantId, topic, partition)
                                .orElseThrow();
                assertThat(progress.getLastOffset()).isEqualTo(offset);
                assertThat(progress.getPendingIntents()).isEqualTo(0);

                FreshnessStatus freshness = freshnessService.getFreshness(tenantId, topic);
                assertThat(freshness.isHealthy()).isTrue();
                assertThat(freshness.lagSeconds()).isEqualTo(0L);
                assertThat(freshness.pendingIntents()).isEqualTo(0);

                // 4. Verify Kafka offset acknowledged
                assertThat(ackCalled.get()).isTrue();
        }

        @Test
        @DisplayName("RPT-19: Leave and Request facts commit offsets, update freshness checkpoints and acknowledge")
        void leaveAndRequestFactsCommitOffsetsAndAdvanceFreshness() {
                UUID tenantId = UUID.randomUUID();
                UUID leaveId = UUID.randomUUID();
                UUID requestId = UUID.randomUUID();
                UUID employeeId = UUID.randomUUID();

                // 1. Leave Fact
                String leaveTopic = "hrflow.leave.facts";
                UUID leaveEventId = UUID.randomUUID();
                LeaveFactEvent leaveEvent = new LeaveFactEvent(
                                leaveEventId, tenantId, leaveId, employeeId, "ANNUAL",
                                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5),
                                new BigDecimal("5.00"), "APPROVED", 1L);
                ConsumerRecord<String, LeaveFactEvent> leaveRecord = new ConsumerRecord<>(leaveTopic, 1, 200L,
                                leaveId.toString(), leaveEvent);
                AtomicBoolean leaveAckCalled = new AtomicBoolean(false);

                kafkaListener.onLeaveFact(leaveRecord, () -> leaveAckCalled.set(true));

                LeaveFactProjection lp = leaveRepo.findByTenantIdAndLeaveId(tenantId, leaveId).orElseThrow();
                assertThat(lp.getStatus()).isEqualTo("APPROVED");
                assertThat(inboxRepo.existsByConsumerAndEvent(ReportingProjectionConsumer.CONSUMER_NAME, tenantId,
                                leaveEventId)).isTrue();
                CheckpointProgress lpProgress = checkpointRepo.findByTenantTopicPartition(tenantId, leaveTopic, 1)
                                .orElseThrow();
                assertThat(lpProgress.getLastOffset()).isEqualTo(200L);
                assertThat(lpProgress.getPendingIntents()).isEqualTo(0);
                assertThat(leaveAckCalled.get()).isTrue();

                // 2. Request Fact
                String reqTopic = "hrflow.request.facts";
                UUID reqEventId = UUID.randomUUID();
                RequestFactEvent reqEvent = new RequestFactEvent(
                                reqEventId, tenantId, requestId, employeeId, "EQUIPMENT", "SUBMITTED", 1L);
                ConsumerRecord<String, RequestFactEvent> reqRecord = new ConsumerRecord<>(reqTopic, 2, 300L,
                                requestId.toString(), reqEvent);
                AtomicBoolean reqAckCalled = new AtomicBoolean(false);

                kafkaListener.onRequestFact(reqRecord, () -> reqAckCalled.set(true));

                RequestFactProjection rp = requestRepo.findByTenantIdAndRequestId(tenantId, requestId).orElseThrow();
                assertThat(rp.getStatus()).isEqualTo("SUBMITTED");
                assertThat(inboxRepo.existsByConsumerAndEvent(ReportingProjectionConsumer.CONSUMER_NAME, tenantId,
                                reqEventId)).isTrue();
                CheckpointProgress rpProgress = checkpointRepo.findByTenantTopicPartition(tenantId, reqTopic, 2)
                                .orElseThrow();
                assertThat(rpProgress.getLastOffset()).isEqualTo(300L);
                assertThat(rpProgress.getPendingIntents()).isEqualTo(0);
                assertThat(reqAckCalled.get()).isTrue();
        }

        @Test
        @DisplayName("RPT-19: Never advance freshness or acknowledge offset on projection rollback / failure")
        void neverAdvanceFreshnessOrAcknowledgeOnRollback() {
                UUID tenantId = UUID.randomUUID();
                String topic = "hrflow.attendance.facts";
                int partition = 0;

                // Establish an initial committed checkpoint at offset 50
                freshnessService.recordCheckpoint(tenantId, topic, partition, 50L, Instant.now(), 0);

                // Create an invalid record with null mandatory fields that causes
                // projection/validation failure
                ConsumerRecord<String, AttendanceFactEvent> invalidRecord = new ConsumerRecord<>(topic, partition, 51L,
                                "invalid-key", null);

                AtomicBoolean ackCalled = new AtomicBoolean(false);

                assertThatThrownBy(() -> kafkaListener.onAttendanceFact(invalidRecord, () -> ackCalled.set(true)))
                                .isInstanceOf(IllegalArgumentException.class);

                // Prove offset was NOT acknowledged
                assertThat(ackCalled.get()).isFalse();

                // Prove checkpoint progress was NOT advanced and remains at 50
                CheckpointProgress progress = checkpointRepo.findByTenantTopicPartition(tenantId, topic, partition)
                                .orElseThrow();
                assertThat(progress.getLastOffset()).isEqualTo(50L);
        }

        @Test
        @DisplayName("RPT-19: Blocked AttendanceCheckedIn strictly does not advance freshness or acknowledge offset")
        void blockedAttendanceCheckedInDoesNotAdvanceFreshnessOrAcknowledge() {
                UUID tenantId = UUID.randomUUID();
                UUID sessionId = UUID.randomUUID();
                String topic = "hrflow.attendance.facts";
                int partition = 0;

                AttendanceCheckedIn event = AttendanceCheckedIn.newBuilder()
                                .setEventId(UUID.randomUUID())
                                .setTenantId(tenantId.toString())
                                .setEmployeeId(UUID.randomUUID().toString())
                                .setOccurredAt(Instant.now())
                                .setSource(Source.MANUAL)
                                .setReason("On-site check-in")
                                .build();

                ConsumerRecord<String, AttendanceCheckedIn> record = new ConsumerRecord<>(topic, partition, 999L,
                                sessionId.toString(), event);

                AtomicBoolean ackCalled = new AtomicBoolean(false);

                assertThatThrownBy(() -> kafkaListener.onAttendanceCheckedIn(record, () -> ackCalled.set(true)))
                                .isInstanceOf(UnsupportedOperationException.class)
                                .hasMessageContaining("AttendanceCheckedIn processing is BLOCKED");

                // Prove acknowledgment was NOT called
                assertThat(ackCalled.get()).isFalse();

                // Prove no checkpoint was recorded
                assertThat(checkpointRepo.findByTenantTopicPartition(tenantId, topic, partition)).isEmpty();
        }

        @Test
        @DisplayName("RPT-19: Regressive offset throws and strictly prevents acknowledgment")
        void regressiveOffsetThrowsAndRefusesToAcknowledge() {
                UUID tenantId = UUID.randomUUID();
                UUID sessionId1 = UUID.randomUUID();
                UUID sessionId2 = UUID.randomUUID();
                String topic = "hrflow.attendance.facts";
                int partition = 0;

                // 1. Process offset 100 successfully
                AttendanceFactEvent event1 = new AttendanceFactEvent(
                                UUID.randomUUID(), tenantId, sessionId1, UUID.randomUUID(),
                                LocalDate.of(2026, 10, 20), "PRESENT", 0, 480, 1L);
                kafkaListener.onAttendanceFact(
                                new ConsumerRecord<>(topic, partition, 100L, sessionId1.toString(), event1), () -> {
                                });

                CheckpointProgress p1 = checkpointRepo.findByTenantTopicPartition(tenantId, topic, partition)
                                .orElseThrow();
                assertThat(p1.getLastOffset()).isEqualTo(100L);

                // 2. Attempt to process offset 99 (regressive offset)
                AttendanceFactEvent event2 = new AttendanceFactEvent(
                                UUID.randomUUID(), tenantId, sessionId2, UUID.randomUUID(),
                                LocalDate.of(2026, 10, 20), "PRESENT", 0, 480, 1L);
                AtomicBoolean ackCalled = new AtomicBoolean(false);
                ConsumerRecord<String, AttendanceFactEvent> regressiveRecord = new ConsumerRecord<>(topic, partition,
                                99L, sessionId2.toString(), event2);

                assertThatThrownBy(() -> kafkaListener.onAttendanceFact(regressiveRecord, () -> ackCalled.set(true)))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Checkpoint offset cannot decrease");

                // Acknowledgment must NOT be called
                assertThat(ackCalled.get()).isFalse();

                // Checkpoint must remain at 100
                CheckpointProgress p2 = checkpointRepo.findByTenantTopicPartition(tenantId, topic, partition)
                                .orElseThrow();
                assertThat(p2.getLastOffset()).isEqualTo(100L);
        }
}
