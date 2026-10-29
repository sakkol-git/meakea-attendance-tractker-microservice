package com.hrflow.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.hrflow.contracts.request.RequestWorkflowFact;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.KafkaHeaders;

/** Real poison bytes and temporarily missing authoritative recipients; no handler mocks. */
final class NotificationRecoveryFixture {
    static UUID verify() throws Exception {
        String brokers = RequestNotificationKafkaTest.KAFKA.getBootstrapServers();
        byte[] poison = "Private invalid Avro fixture".getBytes(StandardCharsets.UTF_8);
        long offset;
        try (var producer = new KafkaProducer<String, byte[]>(Map.of("bootstrap.servers", brokers,
                "key.serializer", StringSerializer.class, "value.serializer", ByteArraySerializer.class, "acks", "all"))) {
            var input = new ProducerRecord<String, byte[]>("request.workflow.v1", "poison-fixture", poison);
            input.headers().add("Authorization", "Private token fixture".getBytes(StandardCharsets.UTF_8));
            offset = producer.send(input).get().offset();
        }
        var recovered = readDeadLetters(brokers, "request.workflow.v1", offset, 1).getFirst();
        assertThat(recovered.value()).isEqualTo(poison);
        assertThat(recovered.headers().lastHeader("Authorization")).isNull();
        for (var header : recovered.headers()) assertThat(new String(header.value(), StandardCharsets.UTF_8)).doesNotContain("Private");
        UUID request = UUID.randomUUID(), unknownManager = UUID.randomUUID();
        var delayed = RequestNotificationTest.fact(RequestNotificationKafkaTest.TENANT, RequestNotificationKafkaTest.EMPLOYEE,
                request, unknownManager, 1, "PENDING_APPROVAL", "PENDING");
        // Wrong current manager is a temporary unavailable reference, so exhaust the bounded delayed retries.
        try (var producer = new KafkaProducer<String, Object>(RequestNotificationKafkaTest.producer())) {
            offset = producer.send(new ProducerRecord<>("request.workflow.v1", delayed.getTenantId() + ":" + request, delayed)).get().offset();
        }
        var terminal = readDeadLetters(brokers, "request.workflow.v1", offset, 1).getFirst();
        assertThat(terminal.value()).isNotEmpty();
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", brokers))) {
            final long expected = offset + 1;
            await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                    assertThat(admin.listConsumerGroupOffsets("hrflow-notification-request-v1").partitionsToOffsetAndMetadata().get()
                            .get(new TopicPartition("request.workflow.v1", 0)).offset()).isGreaterThanOrEqualTo(expected));
        }
        // Repair the authoritative references, then use the real single-record operator redrive with identical bytes/event ID.
        try (var producer = new KafkaProducer<String, Object>(RequestNotificationKafkaTest.producer())) {
            var employee = RequestNotificationKafkaTest.employee(RequestNotificationKafkaTest.EMPLOYEE, unknownManager, List.of("EMPLOYEE"));
            employee.setAggregateVersion(2L);
            for (var reference : List.of(employee, RequestNotificationKafkaTest.employee(unknownManager, null, List.of("EMPLOYEE", "MANAGER"))))
                producer.send(new ProducerRecord<>("org.employee.v1", reference.getTenantId() + ":" + reference.getAggregateId(), reference)).get();
        }
        com.hrflow.events.DeadLetterReplay.replay(brokers, "request.workflow.v1", terminal.partition(), terminal.offset(), "hrflow-notification-request-v1", false);
        com.hrflow.events.DeadLetterReplay.replay(brokers, "request.workflow.v1", terminal.partition(), terminal.offset(), "hrflow-notification-request-v1", true);
        return unknownManager;
    }

    private static List<org.apache.kafka.clients.consumer.ConsumerRecord<String, byte[]>> readDeadLetters(String brokers, String source, long offset, int expected) {
        var matches = new ArrayList<org.apache.kafka.clients.consumer.ConsumerRecord<String, byte[]>>();
        try (var consumer = new KafkaConsumer<String, byte[]>(Map.of("bootstrap.servers", brokers, "group.id", "dead-letter-proof-" + UUID.randomUUID(),
                "auto.offset.reset", "earliest", "enable.auto.commit", false, "key.deserializer", StringDeserializer.class, "value.deserializer", ByteArrayDeserializer.class))) {
            consumer.subscribe(List.of(source + ".dlq"));
            long deadline = System.nanoTime() + Duration.ofSeconds(35).toNanos();
            while (matches.size() < expected && System.nanoTime() < deadline) for (var record : consumer.poll(Duration.ofMillis(250))) {
                var origin = record.headers().lastHeader(KafkaHeaders.ORIGINAL_OFFSET);
                if (origin != null && java.nio.ByteBuffer.wrap(origin.value()).getLong() == offset) matches.add(record);
            }
        }
        assertThat(matches).hasSize(expected);
        return matches;
    }
}
