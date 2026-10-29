package com.hrflow.reporting.consumer;

import com.hrflow.contracts.request.RequestWorkflowFact;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hrflow.reporting.request-consumer.enabled", havingValue = "true")
public class RequestKafkaListener {
    private final RequestProjectionConsumer consumer;
    @KafkaListener(topics = "request.workflow.v1", groupId = "hrflow-reporting-request-v1")
    public void receive(ConsumerRecord<String, RequestWorkflowFact> record, Acknowledgment acknowledgment) {
        consumer.apply(com.hrflow.events.SourceRecords.original(record));
        acknowledgment.acknowledge();
    }
}
