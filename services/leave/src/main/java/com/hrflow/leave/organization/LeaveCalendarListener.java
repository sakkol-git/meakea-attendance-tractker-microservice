package com.hrflow.leave.organization;

import com.hrflow.contracts.organization.OrganizationFact;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hrflow.leave.calendar-consumer.enabled", havingValue = "true")
public class LeaveCalendarListener {
    private final LeaveCalendarConsumer consumer;
    @KafkaListener(topics = {"org.shift.v1", "org.holiday.v1"}, groupId = "hrflow-leave-calendar-v1")
    public void receive(ConsumerRecord<String, OrganizationFact> record, Acknowledgment acknowledgment) {
        consumer.apply(record.topic(), record.key(), record.value());
        acknowledgment.acknowledge();
    }
}
