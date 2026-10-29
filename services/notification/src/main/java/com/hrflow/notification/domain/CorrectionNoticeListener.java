package com.hrflow.notification.domain;

import com.hrflow.contracts.attendance.AttendanceCorrectionFact;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hrflow.notification.domain-consumer.enabled", havingValue = "true")
public class CorrectionNoticeListener {
    private final CorrectionNotices notices;
    @KafkaListener(topics = "attendance.correction.v1", groupId = "hrflow-notification-correction-v1")
    public void receive(ConsumerRecord<String, AttendanceCorrectionFact> record, Acknowledgment ack) {
        notices.apply(record.key(), record.value()); CommitAcknowledgment.afterCommit(ack);
    }
}
