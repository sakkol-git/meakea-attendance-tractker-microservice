package com.hrflow.events;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.common.header.Headers;

/** Retains received wire bytes in memory so retries never re-encode under a different schema subject. */
public class WireAvroDeserializer extends KafkaAvroDeserializer {
    public static final String ORIGINAL_VALUE = "hrflow-internal-original-value";
    @Override public Object deserialize(String topic, Headers headers, byte[] data) {
        headers.remove(ORIGINAL_VALUE); // Never trust a producer-supplied copy of the wire.
        Object decoded = super.deserialize(topic, headers, data);
        if (data != null) headers.add(ORIGINAL_VALUE, data);
        return decoded;
    }
}
