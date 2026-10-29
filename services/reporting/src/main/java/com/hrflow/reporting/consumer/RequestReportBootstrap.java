package com.hrflow.reporting.consumer;

import com.hrflow.contracts.request.RequestWorkflowFact;
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

/** Replays retained request snapshots to captured broker positions before exposing the request export source. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hrflow.reporting.request-consumer.enabled", havingValue = "true")
public class RequestReportBootstrap implements ApplicationRunner {
    private final RequestProjectionConsumer consumer;
    @Value("${spring.kafka.bootstrap-servers}") private String brokers;
    @Value("${spring.kafka.properties.schema.registry.url}") private String registry;
    @Value("${hrflow.reporting.request-consumer.bootstrap-timeout-ms:60000}") private long timeoutMillis;
    @Getter private volatile boolean ready;

    @Override public void run(ApplicationArguments ignored) {
        ready = false;
        if (timeoutMillis < 1000 || timeoutMillis > 600000) throw new IllegalStateException("Invalid employee bootstrap timeout");
        Map<String, Object> properties = Map.of(
                "bootstrap.servers", brokers, "group.id", "report-request-bootstrap-" + UUID.randomUUID(),
                "key.deserializer", org.apache.kafka.common.serialization.StringDeserializer.class,
                "value.deserializer", io.confluent.kafka.serializers.KafkaAvroDeserializer.class,
                "schema.registry.url", registry, "specific.avro.reader", true,
                "enable.auto.commit", false, "isolation.level", "read_committed", "allow.auto.create.topics", false);
        long deadline = System.nanoTime() + Duration.ofMillis(timeoutMillis).toNanos();
        try (var replay = new KafkaConsumer<String, RequestWorkflowFact>(properties)) {
            var metadata = replay.partitionsFor("request.workflow.v1", Duration.ofSeconds(5));
            if (metadata == null || metadata.isEmpty()) throw new IllegalStateException("Request topic is unavailable");
            var partitions = metadata.stream().map(partition -> new TopicPartition(partition.topic(), partition.partition())).toList();
            replay.assign(partitions);
            var target = replay.endOffsets(partitions, Duration.ofSeconds(5));
            replay.seekToBeginning(partitions);
            while (target.entrySet().stream().anyMatch(partition -> replay.position(partition.getKey()) < partition.getValue())) {
                if (Thread.currentThread().isInterrupted() || System.nanoTime() >= deadline) {
                    throw new IllegalStateException("Request bootstrap did not complete in time");
                }
                for (var record : replay.poll(Duration.ofMillis(250))) consumer.apply(record);
            }
        }
        ready = true;
    }
}
