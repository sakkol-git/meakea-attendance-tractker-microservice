package com.hrflow.events;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.retrytopic.RetryTopicHeaders;
import org.springframework.kafka.support.KafkaHeaders;

/** Exact original bytes, bounded safe headers and acknowledged recovery publication. */
public class WireDeadLetterRecoverer extends DeadLetterPublishingRecoverer {
    private static final Set<String> SAFE = Set.of(RetryTopicHeaders.DEFAULT_HEADER_ATTEMPTS,
            RetryTopicHeaders.DEFAULT_HEADER_BACKOFF_TIMESTAMP, RetryTopicHeaders.DEFAULT_HEADER_ORIGINAL_TIMESTAMP,
            KafkaHeaders.ORIGINAL_CONSUMER_GROUP, KafkaHeaders.EXCEPTION_FQCN);

    public WireDeadLetterRecoverer(Function<ProducerRecord<?, ?>, ? extends KafkaOperations<?, ?>> templates,
            BiFunction<ConsumerRecord<?, ?>, Exception, TopicPartition> destination) {
        super(templates, destination);
        setExceptionHeadersCreator((headers, exception, key, names) -> {
            headers.remove(KafkaHeaders.EXCEPTION_FQCN);
            headers.add(KafkaHeaders.EXCEPTION_FQCN, exception.getClass().getName().getBytes(StandardCharsets.UTF_8));
        });
        setLogRecoveryRecord(false);
    }

    @Override protected ProducerRecord<Object, Object> createProducerRecord(ConsumerRecord<?, ?> record, TopicPartition target,
            Headers headers, byte[] key, byte[] value) {
        var original = SourceRecords.original(record);
        var wire = record.headers().lastHeader(WireAvroDeserializer.ORIGINAL_VALUE);
        byte[] bytes = value != null ? value : wire == null ? null : wire.value();
        if (bytes == null && record.value() != null) throw new IllegalStateException("Original event wire is unavailable");
        var safe = new RecordHeaders();
        for (String name : SAFE) {
            var header = headers.lastHeader(name);
            if (header != null && header.value() != null && header.value().length <= 256) safe.add(header);
        }
        var trace = headers.lastHeader("traceparent");
        if (trace != null && trace.value() != null && new String(trace.value(), StandardCharsets.US_ASCII)
                .matches("00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}")) safe.add(trace);
        safe.add(KafkaHeaders.ORIGINAL_TOPIC, original.topic().getBytes(StandardCharsets.UTF_8));
        safe.add(KafkaHeaders.ORIGINAL_PARTITION, ByteBuffer.allocate(4).putInt(original.partition()).array());
        safe.add(KafkaHeaders.ORIGINAL_OFFSET, ByteBuffer.allocate(8).putLong(original.offset()).array());
        return new ProducerRecord<>(target.topic(), target.partition() < 0 ? null : target.partition(), key != null ? key : record.key(), bytes, safe);
    }

    @Override protected void publish(ProducerRecord<Object, Object> output, KafkaOperations<Object, Object> kafka, ConsumerRecord<?, ?> input) {
        try { kafka.send(output).get(3, TimeUnit.SECONDS); }
        catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt(); throw new IllegalStateException("Recovery broker acknowledgment unavailable");
        } catch (Exception failure) { throw new IllegalStateException("Recovery broker acknowledgment unavailable"); }
        if (output.topic().endsWith(".dlq")) LoggerFactory.getLogger(WireDeadLetterRecoverer.class)
                .warn("Event stored in private dead-letter topic {}; operator review required", output.topic());
    }
}
