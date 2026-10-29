package com.hrflow.attendance.daily;

import com.hrflow.contracts.attendance.ShiftCutoffFact;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hrflow.attendance.cutoff-consumer.enabled", havingValue = "true")
public class ShiftCutoffListener {
    private final ShiftCutoffConsumer consumer;
    @KafkaListener(topics = "time.shift-cutoff.v1", groupId = "hrflow-attendance-cutoff-v1")
    public void receive(ConsumerRecord<String, ShiftCutoffFact> record, Acknowledgment acknowledgment) {
        consumer.apply(com.hrflow.events.SourceRecords.original(record));
        acknowledgment.acknowledge();
    }
}
