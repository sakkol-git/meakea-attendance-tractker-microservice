package com.hrflow.notification.domain;

import com.hrflow.contracts.request.RequestWorkflowFact;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hrflow.notification.domain-consumer.enabled", havingValue = "true")
public class RequestNoticeListener {
    private final RequestNotices notices;
    @KafkaListener(topics = "request.workflow.v1", groupId = "hrflow-notification-request-v1")
    public void receive(ConsumerRecord<String, RequestWorkflowFact> record, Acknowledgment ack) {
        notices.apply(record.key(), record.value());
        CommitAcknowledgment.afterCommit(ack);
    }
}
