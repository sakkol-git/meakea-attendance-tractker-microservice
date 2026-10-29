package com.hrflow.events;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.KafkaHeaders;

/** Private local single-record redrive. Dry-run is the default; no payload is printed or source offset reset. */
public final class DeadLetterReplay {
    private DeadLetterReplay() {}
    public static void main(String[] args) throws Exception {
        if (args.length < 4 || args.length > 5 || args.length == 5 && !"--execute".equals(args[4]))
            throw new IllegalArgumentException("Usage: <source-topic> <dlq-partition> <dlq-offset> <expected-consumer-group> [--execute]");
        String brokers = System.getenv("KAFKA_BOOTSTRAP_SERVERS");
        if (brokers == null || brokers.isBlank()) throw new IllegalArgumentException("KAFKA_BOOTSTRAP_SERVERS is required");
        replay(brokers, args[0], Integer.parseInt(args[1]), Long.parseLong(args[2]), args[3], args.length == 5);
    }
    public static void replay(String brokers, String source, int partition, long offset, String group, boolean execute) throws Exception {
        if (!SourceRecords.TOPICS.contains(source) || partition < 0 || offset < 0 || group == null || !group.matches("hrflow-[a-z0-9-]+"))
            throw new IllegalArgumentException("Invalid bounded replay selection");
        var topic = new TopicPartition(source + ".dlq", partition);
        ConsumerRecord<String, byte[]> selected = null;
        try (var consumer = new KafkaConsumer<String, byte[]>(Map.of("bootstrap.servers", brokers, "enable.auto.commit", false,
                "key.deserializer", StringDeserializer.class, "value.deserializer", ByteArrayDeserializer.class, "allow.auto.create.topics", false))) {
            consumer.assign(List.of(topic)); consumer.seek(topic, offset);
            long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
            while (selected == null && System.nanoTime() < deadline) {
                for (var record : consumer.poll(Duration.ofMillis(250))) if (record.offset() == offset) selected = record;
                if (consumer.position(topic) > offset && selected == null) break;
            }
        }
        if (selected == null || !header(selected, KafkaHeaders.ORIGINAL_TOPIC).equals(source)
                || !header(selected, KafkaHeaders.ORIGINAL_CONSUMER_GROUP).equals(group))
            throw new IllegalArgumentException("Dead-letter record or expected owner provenance is unavailable");
        System.out.println((execute ? "Replaying " : "Dry-run ") + source + ".dlq partition=" + partition + " offset=" + offset + " consumer=" + group);
        if (!execute) return;
        try (var producer = new KafkaProducer<String, byte[]>(Map.of("bootstrap.servers", brokers, "acks", "all", "enable.idempotence", true,
                "key.serializer", StringSerializer.class, "value.serializer", ByteArraySerializer.class, "max.block.ms", 5000))) {
            producer.send(new ProducerRecord<>(source, null, selected.key(), selected.value(), new RecordHeaders())).get(5, TimeUnit.SECONDS);
        }
        System.out.println("Replay acknowledged; original dead-letter record retained. Verify consumer convergence before closing the incident.");
    }
    private static String header(ConsumerRecord<?, ?> record, String name) {
        var header = record.headers().lastHeader(name);
        return header == null || header.value() == null ? "" : new String(header.value(), StandardCharsets.UTF_8);
    }
}
