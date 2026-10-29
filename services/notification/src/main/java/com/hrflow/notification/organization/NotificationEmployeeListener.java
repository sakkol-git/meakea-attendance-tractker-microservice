package com.hrflow.notification.organization;

import com.hrflow.contracts.organization.OrganizationFact;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hrflow.notification.domain-consumer.enabled", havingValue = "true")
public class NotificationEmployeeListener {
    private final NotificationEmployeeConsumer consumer;
    @KafkaListener(topics = "org.employee.v1", groupId = "hrflow-notification-employee-v1")
    public void receive(ConsumerRecord<String, OrganizationFact> record, Acknowledgment acknowledgment) {
        consumer.apply(record.key(), record.value());
        acknowledgment.acknowledge();
    }
}
