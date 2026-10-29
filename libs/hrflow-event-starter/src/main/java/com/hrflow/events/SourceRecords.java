package com.hrflow.events;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.support.KafkaHeaders;

/** Domain projections receive the source coordinates; retry offsets remain owned by their transport container. */
public final class SourceRecords {
    public static final List<String> TOPICS = List.of("org.employee.v1", "org.shift.v1", "org.holiday.v1",
            "attendance.session.v1", "attendance.daily-status.v1", "attendance.correction.v1", "leave.request.v1",
            "request.workflow.v1", "time.shift-cutoff.v1");
    private SourceRecords() {}

    public static <K, V> ConsumerRecord<K, V> original(ConsumerRecord<K, V> record) {
        String source = TOPICS.stream().filter(topic -> record.topic().matches(java.util.regex.Pattern.quote(topic) + "\\.retry-[0-2]"))
                .findFirst().orElse(null);
        if (source == null) return record;
        var topic = record.headers().lastHeader(KafkaHeaders.ORIGINAL_TOPIC);
        if (topic == null || !source.equals(new String(topic.value(), StandardCharsets.UTF_8))) throw invalid();
        int partition = ByteBuffer.wrap(bytes(record.headers(), KafkaHeaders.ORIGINAL_PARTITION, 4)).getInt();
        long offset = ByteBuffer.wrap(bytes(record.headers(), KafkaHeaders.ORIGINAL_OFFSET, 8)).getLong();
        if (partition < 0 || offset < 0) throw invalid();
        return new ConsumerRecord<>(source, partition, offset, record.timestamp(), record.timestampType(), record.serializedKeySize(),
                record.serializedValueSize(), record.key(), record.value(), record.headers(), record.leaderEpoch(), record.deliveryCount());
    }
    private static byte[] bytes(Headers headers, String key, int size) {
        var header = headers.lastHeader(key);
        if (header == null || header.value() == null || header.value().length != size) throw invalid();
        return header.value();
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("Invalid retry source provenance"); }
}
