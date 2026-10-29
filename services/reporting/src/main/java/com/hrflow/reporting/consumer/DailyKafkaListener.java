package com.hrflow.reporting.consumer;

import com.hrflow.contracts.attendance.AttendanceDailyFact;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hrflow.reporting.daily-consumer.enabled", havingValue = "true")
public class DailyKafkaListener {
    private final DailyProjectionConsumer consumer;
    @KafkaListener(topics = "attendance.daily-status.v1", groupId = "hrflow-reporting-daily-v1")
    public void receive(ConsumerRecord<String, AttendanceDailyFact> record, Acknowledgment acknowledgment) {
        consumer.apply(com.hrflow.events.SourceRecords.original(record));
        acknowledgment.acknowledge();
    }
}
