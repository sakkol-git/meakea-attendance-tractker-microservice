package com.hrflow.reporting.consumer;

import com.hrflow.contracts.attendance.AttendanceSessionFact;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hrflow.reporting.session-consumer.enabled", havingValue = "true")
public class SessionKafkaListener {
    private final SessionProjectionConsumer consumer;

    @KafkaListener(topics = "attendance.session.v1", groupId = "hrflow-reporting-sessions-v1")
    public void receive(ConsumerRecord<String, AttendanceSessionFact> record, Acknowledgment acknowledgment) {
        consumer.apply(com.hrflow.events.SourceRecords.original(record));
        acknowledgment.acknowledge();
    }
}
