package com.hrflow.notification.outbox;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OutboxPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OutboxStore outboxStore;

    public OutboxPublisher(KafkaTemplate<String, Object> kafkaTemplate, OutboxStore outboxStore) {
        this.kafkaTemplate = kafkaTemplate;
        this.outboxStore = outboxStore;
    }

    public PublicationTrace publish(OutboxRecord record) {
        try {
            // Await broker acknowledgment before marking sent in DB
            var future = kafkaTemplate.send(record.topic(), record.partitionKey(), record.payload());
            future.get(5, TimeUnit.SECONDS);

            Instant publishedAt = Instant.now();
            outboxStore.markPublished(record.eventId(), publishedAt);

            return new PublicationTrace(
                    record.eventId(),
                    record.tenantId(),
                    record.aggregateRevision(),
                    record.schemaVersion(),
                    record.partitionKey(),
                    publishedAt
            );
        } catch (Exception error) {
            throw new PublicationException("Kafka broker delivery failed: " + error.getMessage(), error);
        }
    }
}
