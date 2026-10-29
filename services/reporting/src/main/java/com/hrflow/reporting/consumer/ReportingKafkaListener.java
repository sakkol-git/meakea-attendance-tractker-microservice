package com.hrflow.reporting.consumer;

import com.hrflow.contracts.attendance.AttendanceCheckedIn;
import com.hrflow.reporting.dto.AttendanceFactEvent;
import com.hrflow.reporting.dto.LeaveFactEvent;
import com.hrflow.reporting.dto.RequestFactEvent;
import com.hrflow.reporting.service.FreshnessService;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka listener for reporting facts and events.
 * Wires committed Kafka offsets to FreshnessService checkpoint progress.
 *
 * Invariant: Records topic/partition/offset to freshness ONLY after projection
 * and inbox have committed. Never advances checkpoint progress or acknowledges
 * Kafka offset on failure or rollback.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "hrflow.reporting.kafka-consumer.enabled", havingValue = "true")
public class ReportingKafkaListener {

    private final ReportingProjectionConsumer consumer;
    private final FreshnessService freshnessService;

    public ReportingKafkaListener(ReportingProjectionConsumer consumer) {
        this(consumer, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ReportingKafkaListener(ReportingProjectionConsumer consumer, FreshnessService freshnessService) {
        this.consumer = consumer;
        this.freshnessService = freshnessService;
    }

    @KafkaListener(topics = "${hrflow.reporting.topics.attendance:hrflow.attendance.facts}", groupId = "${spring.kafka.consumer.group-id:hrflow-reporting-v1}")
    public void onAttendanceCheckedIn(ConsumerRecord<String, AttendanceCheckedIn> record,
            Acknowledgment acknowledgment) {
        log.info("Received AttendanceCheckedIn key={}, partition={}, offset={}",
                record.key(), record.partition(), record.offset());
        consumer.processAttendanceCheckedIn(record.key(), record.value(), () -> {
            recordCommittedOffsetAndAcknowledge(
                    UUID.fromString(record.value().getTenantId()),
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.value().getOccurredAt(),
                    acknowledgment);
        });
    }

    @KafkaListener(topics = "${hrflow.reporting.topics.attendance-facts:hrflow.attendance.facts}", groupId = "${spring.kafka.consumer.group-id:hrflow-reporting-v1}")
    public void onAttendanceFact(ConsumerRecord<String, AttendanceFactEvent> record, Acknowledgment acknowledgment) {
        log.info("Received AttendanceFactEvent key={}, partition={}, offset={}",
                record.key(), record.partition(), record.offset());
        AttendanceFactEvent event = record.value();
        if (event == null) {
            throw new IllegalArgumentException("AttendanceFactEvent record value cannot be null");
        }
        consumer.processAttendanceFact(event);
        recordCommittedOffsetAndAcknowledge(
                event.tenantId(),
                record.topic(),
                record.partition(),
                record.offset(),
                extractTimestamp(record),
                acknowledgment);
    }

    @KafkaListener(topics = "${hrflow.reporting.topics.leave-facts:hrflow.leave.facts}", groupId = "${spring.kafka.consumer.group-id:hrflow-reporting-v1}")
    public void onLeaveFact(ConsumerRecord<String, LeaveFactEvent> record, Acknowledgment acknowledgment) {
        log.info("Received LeaveFactEvent key={}, partition={}, offset={}",
                record.key(), record.partition(), record.offset());
        LeaveFactEvent event = record.value();
        if (event == null) {
            throw new IllegalArgumentException("LeaveFactEvent record value cannot be null");
        }
        consumer.processLeaveFact(event);
        recordCommittedOffsetAndAcknowledge(
                event.tenantId(),
                record.topic(),
                record.partition(),
                record.offset(),
                extractTimestamp(record),
                acknowledgment);
    }

    @KafkaListener(topics = "${hrflow.reporting.topics.request-facts:hrflow.request.facts}", groupId = "${spring.kafka.consumer.group-id:hrflow-reporting-v1}")
    public void onRequestFact(ConsumerRecord<String, RequestFactEvent> record, Acknowledgment acknowledgment) {
        log.info("Received RequestFactEvent key={}, partition={}, offset={}",
                record.key(), record.partition(), record.offset());
        RequestFactEvent event = record.value();
        if (event == null) {
            throw new IllegalArgumentException("RequestFactEvent record value cannot be null");
        }
        consumer.processRequestFact(event);
        recordCommittedOffsetAndAcknowledge(
                event.tenantId(),
                record.topic(),
                record.partition(),
                record.offset(),
                extractTimestamp(record),
                acknowledgment);
    }

    private void recordCommittedOffsetAndAcknowledge(
            UUID tenantId,
            String topic,
            int partition,
            long offset,
            Instant processedAt,
            Acknowledgment acknowledgment) {
        if (freshnessService != null) {
            freshnessService.recordCommittedOffset(
                    tenantId,
                    topic,
                    partition,
                    offset,
                    processedAt);
        }
        if (acknowledgment != null) {
            acknowledgment.acknowledge();
        }
    }

    private Instant extractTimestamp(ConsumerRecord<?, ?> record) {
        if (record != null && record.timestamp() > 0) {
            return Instant.ofEpochMilli(record.timestamp());
        }
        return freshnessService != null && freshnessService.getClock() != null
                ? freshnessService.getClock().instant()
                : Instant.now();
    }
}
