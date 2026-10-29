package com.hrflow.attendance.organization;

import com.hrflow.contracts.organization.OrganizationFact;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Rebuild from compacted owner snapshots before Spring marks the service ready. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hrflow.attendance.organization-consumer.enabled", havingValue = "true")
public class OrganizationBootstrap implements ApplicationRunner {
    private static final List<String> TOPICS = List.of(
            "org.employee.v1", "org.shift.v1", "org.holiday.v1");
    private final OrganizationProjectionService projections;

    @Value("${spring.kafka.bootstrap-servers}")
    private String brokers;
    @Value("${spring.kafka.properties.schema.registry.url}")
    private String registry;
    @Value("${hrflow.attendance.organization-consumer.bootstrap-timeout-ms:60000}")
    private long timeoutMillis;

    @Override
    public void run(ApplicationArguments ignored) {
        if (timeoutMillis < 1000 || timeoutMillis > 600000) {
            throw new IllegalStateException("Organization bootstrap timeout is outside supported bounds");
        }
        Properties config = new Properties();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "attendance-bootstrap-" + UUID.randomUUID());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        config.put("schema.registry.url", registry);
        config.put("specific.avro.reader", true);
        config.put("allow.auto.create.topics", false);
        long deadline = System.nanoTime() + Duration.ofMillis(timeoutMillis).toNanos();
        try (KafkaConsumer<String, OrganizationFact> consumer = new KafkaConsumer<>(config)) {
            List<TopicPartition> partitions = new ArrayList<>();
            for (String topic : TOPICS) {
                var available = consumer.partitionsFor(topic, Duration.ofSeconds(5));
                if (available == null || available.isEmpty()) {
                    throw new IllegalStateException("Organization bootstrap topic unavailable: " + topic);
                }
                available.forEach(partition -> partitions.add(new TopicPartition(topic, partition.partition())));
            }
            consumer.assign(partitions);
            Map<TopicPartition, Long> target = consumer.endOffsets(partitions);
            consumer.seekToBeginning(partitions);
            while (!caughtUp(consumer, target)) {
                if (Thread.currentThread().isInterrupted() || System.nanoTime() >= deadline) {
                    throw new IllegalStateException("Organization snapshot bootstrap did not complete in time");
                }
                for (var record : consumer.poll(Duration.ofMillis(500))) {
                    projections.apply(record.topic(), record.key(), record.value());
                }
            }
        }
    }

    private boolean caughtUp(KafkaConsumer<String, OrganizationFact> consumer,
            Map<TopicPartition, Long> target) {
        for (var partition : target.entrySet()) {
            if (consumer.position(partition.getKey()) < partition.getValue()) return false;
        }
        return true;
    }
}
