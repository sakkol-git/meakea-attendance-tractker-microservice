package com.hrflow.attendance.organization;

import com.hrflow.contracts.organization.OrganizationFact;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/** Commit the owner snapshot and inbox receipt before acknowledging the Kafka offset. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hrflow.attendance.organization-consumer.enabled", havingValue = "true")
public class OrganizationProjectionListener {
    private final OrganizationProjectionService projections;

    @KafkaListener(topics = {"org.employee.v1", "org.shift.v1", "org.holiday.v1"},
            groupId = "${spring.kafka.consumer.group-id}")
    public void onFact(ConsumerRecord<String, OrganizationFact> record, Acknowledgment acknowledgment) {
        var source = com.hrflow.events.SourceRecords.original(record);
        projections.apply(source.topic(), source.key(), source.value());
        acknowledgment.acknowledge();
    }
}
