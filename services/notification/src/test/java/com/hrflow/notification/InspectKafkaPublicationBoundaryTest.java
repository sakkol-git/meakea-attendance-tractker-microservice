package com.hrflow.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.notification.outbox.OutboxPublisher;
import com.hrflow.notification.outbox.OutboxRecord;
import com.hrflow.notification.outbox.OutboxStore;
import com.hrflow.notification.outbox.PublicationException;
import com.hrflow.notification.outbox.PublicationTrace;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;

class InspectKafkaPublicationBoundaryTest {

    @Test
    @DisplayName("Publication boundary traces event metadata and requires Kafka acknowledgment before marking sent")
    void publicationBoundaryRequiresKafkaAcknowledgment() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        long aggregateRevision = 1L;
        String topic = "org.attendance.session.created.v1";
        String partitionKey = tenantId + ":" + aggregateId;
        String payload = "{\"sessionId\":\"" + aggregateId + "\"}";

        OutboxRecord outboxRecord = new OutboxRecord(
                eventId, tenantId, topic, aggregateId, aggregateRevision, 1, partitionKey, payload, Instant.now()
        );

        RecordingKafkaTemplate kafkaTemplate = new RecordingKafkaTemplate(false);
        InMemoryOutboxStore outboxStore = new InMemoryOutboxStore();
        outboxStore.save(outboxRecord);

        OutboxPublisher publisher = new OutboxPublisher(kafkaTemplate, outboxStore);

        // When publishing successfully
        PublicationTrace trace = publisher.publish(outboxRecord);

        assertThat(trace).isNotNull();
        assertThat(trace.eventId()).isEqualTo(eventId);
        assertThat(trace.tenantId()).isEqualTo(tenantId);
        assertThat(trace.aggregateRevision()).isEqualTo(aggregateRevision);
        assertThat(trace.schemaVersion()).isEqualTo(1);
        assertThat(trace.partitionKey()).isEqualTo(partitionKey);

        // Acknowledged before mark sent
        assertThat(outboxStore.isPublished(eventId)).isTrue();
    }

    @Test
    @DisplayName("Broker outage or error leaves outbox intent pending without marking sent")
    void brokerOutageLeavesIntentPending() {
        UUID eventId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        OutboxRecord outboxRecord = new OutboxRecord(
                eventId, tenantId, "org.attendance.session.created.v1", aggregateId, 1L, 1,
                tenantId + ":" + aggregateId, "{}", Instant.now()
        );

        RecordingKafkaTemplate failingKafka = new RecordingKafkaTemplate(true);
        InMemoryOutboxStore outboxStore = new InMemoryOutboxStore();
        outboxStore.save(outboxRecord);

        OutboxPublisher publisher = new OutboxPublisher(failingKafka, outboxStore);

        assertThatThrownBy(() -> publisher.publish(outboxRecord))
                .isInstanceOf(PublicationException.class)
                .hasMessageContaining("Kafka broker delivery failed");

        // Outbox must remain UNPUBLISHED
        assertThat(outboxStore.isPublished(eventId)).isFalse();
    }

    static class InMemoryOutboxStore implements OutboxStore {
        private final Map<UUID, OutboxRecord> records = new HashMap<>();
        private final Map<UUID, Instant> published = new HashMap<>();

        @Override
        public void save(OutboxRecord record) {
            records.put(record.eventId(), record);
        }

        @Override
        public boolean isPublished(UUID eventId) {
            return published.containsKey(eventId);
        }

        @Override
        public void markPublished(UUID eventId, Instant publishedAt) {
            published.put(eventId, publishedAt);
        }

        @Override
        public Optional<OutboxRecord> findById(UUID eventId) {
            return Optional.ofNullable(records.get(eventId));
        }
    }

    @SuppressWarnings("unchecked")
    static class RecordingKafkaTemplate extends KafkaTemplate<String, Object> {
        private final boolean shouldFail;

        public RecordingKafkaTemplate(boolean shouldFail) {
            super(org.mockito.Mockito.mock(ProducerFactory.class));
            this.shouldFail = shouldFail;
        }

        @Override
        public CompletableFuture<SendResult<String, Object>> send(String topic, String key, Object data) {
            CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
            if (shouldFail) {
                future.completeExceptionally(new RuntimeException("Connection refused to Kafka broker"));
            } else {
                ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(topic, key, data);
                RecordMetadata metadata = new RecordMetadata(new TopicPartition(topic, 0), 0, 0, System.currentTimeMillis(), 0, 0);
                future.complete(new SendResult<>(producerRecord, metadata));
            }
            return future;
        }
    }
}
