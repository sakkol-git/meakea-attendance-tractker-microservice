package com.hrflow.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;

class WireRecoveryTest {
    @SuppressWarnings("unchecked")
    @Test void recoveryKeepsExactWireBytesAndSafeProvenanceWithoutCopyingPrivateHeadersOrExceptionText() {
        KafkaOperations<Object, Object> kafka = mock(KafkaOperations.class);
        when(kafka.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
        byte[] wire = {0, 0, 0, 0, 19, 3, 2, 1};
        var input = new ConsumerRecord<String, Object>("leave.request.v1", 2, 17, "tenant:employee", "decoded fixture");
        input.headers().add(WireAvroDeserializer.ORIGINAL_VALUE, wire).add("Authorization", "Private credential".getBytes(StandardCharsets.UTF_8));
        var recoverer = new WireDeadLetterRecoverer(record -> kafka, (record, error) -> new TopicPartition(record.topic() + ".dlq", -1));
        recoverer.accept(input, new IllegalArgumentException("Private medical reason"));
        ArgumentCaptor<ProducerRecord<Object, Object>> sent = ArgumentCaptor.forClass(ProducerRecord.class);
        org.mockito.Mockito.verify(kafka).send(sent.capture());
        assertThat((byte[]) sent.getValue().value()).isEqualTo(wire);
        assertThat(sent.getValue().headers().lastHeader("Authorization")).isNull();
        assertThat(sent.getValue().headers().lastHeader(WireAvroDeserializer.ORIGINAL_VALUE)).isNull();
        for (var header : sent.getValue().headers()) assertThat(new String(header.value(), StandardCharsets.UTF_8)).doesNotContain("Private");
        assertThat(new String(sent.getValue().headers().lastHeader(KafkaHeaders.ORIGINAL_TOPIC).value(), StandardCharsets.UTF_8)).isEqualTo("leave.request.v1");
        assertThat(ByteBuffer.wrap(sent.getValue().headers().lastHeader(KafkaHeaders.ORIGINAL_OFFSET).value()).getLong()).isEqualTo(17);
    }

    @SuppressWarnings("unchecked")
    @Test void failedBrokerAcknowledgmentCannotBecomeSuccessfulRecovery() {
        KafkaOperations<Object, Object> kafka = mock(KafkaOperations.class);
        when(kafka.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Private upstream detail")));
        var input = new ConsumerRecord<String, Object>("leave.request.v1", 0, 4, "key", null);
        input.headers().add(WireAvroDeserializer.ORIGINAL_VALUE, new byte[] {1,2,3});
        var recoverer = new WireDeadLetterRecoverer(record -> kafka, (record, error) -> new TopicPartition("leave.request.v1.dlq", -1));
        assertThatThrownBy(() -> recoverer.accept(input, new IllegalArgumentException("bad wire")))
                .isInstanceOf(IllegalStateException.class).hasMessage("Recovery broker acknowledgment unavailable").hasNoCause();
    }

    @Test void aRetryKeepsTheOriginalSourceCoordinatesAndRejectsForgedRoutingMetadata() {
        var headers = new RecordHeaders().add(KafkaHeaders.ORIGINAL_TOPIC, "leave.request.v1".getBytes(StandardCharsets.UTF_8))
                .add(KafkaHeaders.ORIGINAL_PARTITION, ByteBuffer.allocate(4).putInt(2).array())
                .add(KafkaHeaders.ORIGINAL_OFFSET, ByteBuffer.allocate(8).putLong(17).array());
        var retry = new ConsumerRecord<String, String>("leave.request.v1.retry-0", 0, 1, 1000,
                org.apache.kafka.common.record.TimestampType.CREATE_TIME, 3, 3, "key", "value", headers, java.util.Optional.empty());
        var source = SourceRecords.original(retry);
        assertThat(source.topic()).isEqualTo("leave.request.v1"); assertThat(source.offset()).isEqualTo(17); assertThat(source.partition()).isEqualTo(2);
        headers.remove(KafkaHeaders.ORIGINAL_TOPIC); headers.add(KafkaHeaders.ORIGINAL_TOPIC, "org.employee.v1".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> SourceRecords.original(retry)).isInstanceOf(IllegalArgumentException.class);
    }
}
