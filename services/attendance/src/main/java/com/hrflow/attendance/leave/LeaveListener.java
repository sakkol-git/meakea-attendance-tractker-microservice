package com.hrflow.attendance.leave;

import com.hrflow.contracts.leave.LeaveRequestFact;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hrflow.attendance.leave-consumer.enabled", havingValue = "true")
public class LeaveListener {
    private final LeaveProjection projection;

    @KafkaListener(topics = "leave.request.v1", groupId = "hrflow-attendance-leave-v1")
    public void receive(ConsumerRecord<String, LeaveRequestFact> record, Acknowledgment acknowledgment) {
        projection.apply(com.hrflow.events.SourceRecords.original(record));
        acknowledgment.acknowledge();
    }
}
