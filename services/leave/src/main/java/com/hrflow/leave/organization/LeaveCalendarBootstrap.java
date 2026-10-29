package com.hrflow.leave.organization;

import com.hrflow.contracts.organization.OrganizationFact;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Replay the compacted shift/holiday topics to a captured broker position before authorizing manager leave decisions. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hrflow.leave.calendar-consumer.enabled", havingValue = "true")
public class LeaveCalendarBootstrap implements ApplicationRunner {
    private static final java.util.List<String> TOPICS = java.util.List.of("org.shift.v1", "org.holiday.v1");
    private final LeaveCalendarConsumer consumer;
    @Value("${spring.kafka.bootstrap-servers}") private String brokers;
    @Value("${spring.kafka.properties.schema.registry.url}") private String registry;
    @Value("${hrflow.leave.calendar-consumer.bootstrap-timeout-ms:60000}") private long timeoutMillis;
    @Getter private volatile boolean ready;

    @Override public void run(ApplicationArguments ignored) {
        ready = false;
        if (timeoutMillis < 1000 || timeoutMillis > 600000) throw new IllegalStateException("Invalid calendar bootstrap timeout");
        Map<String, Object> properties = Map.of(
                "bootstrap.servers", brokers, "group.id", "leave-calendar-bootstrap-" + UUID.randomUUID(),
                "key.deserializer", org.apache.kafka.common.serialization.StringDeserializer.class,
                "value.deserializer", io.confluent.kafka.serializers.KafkaAvroDeserializer.class,
                "schema.registry.url", registry, "specific.avro.reader", true,
                "enable.auto.commit", false, "isolation.level", "read_committed", "allow.auto.create.topics", false);
        long deadline = System.nanoTime() + Duration.ofMillis(timeoutMillis).toNanos();
        try (var replay = new KafkaConsumer<String, OrganizationFact>(properties)) {
            var partitions = new java.util.ArrayList<TopicPartition>();
            for (String topic : TOPICS) {
                var metadata = replay.partitionsFor(topic, Duration.ofSeconds(5));
                if (metadata == null || metadata.isEmpty()) throw new IllegalStateException("Leave calendar topic is unavailable");
                metadata.forEach(partition -> partitions.add(new TopicPartition(partition.topic(), partition.partition())));
            }
            replay.assign(partitions);
            var target = replay.endOffsets(partitions, Duration.ofSeconds(5));
            replay.seekToBeginning(partitions);
            while (target.entrySet().stream().anyMatch(partition -> replay.position(partition.getKey()) < partition.getValue())) {
                if (Thread.currentThread().isInterrupted() || System.nanoTime() >= deadline) {
                    throw new IllegalStateException("Calendar bootstrap did not complete in time");
                }
                for (var record : replay.poll(Duration.ofMillis(250))) consumer.apply(record.topic(), record.key(), record.value());
            }
        }
        ready = true;
    }
}
