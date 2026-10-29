package com.hrflow.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.notification.outbox.JdbcOutboxStore;
import com.hrflow.notification.outbox.OrderedOutboxRelay;
import com.hrflow.notification.outbox.OutboxPublisher;
import com.hrflow.notification.outbox.OutboxRecord;
import com.hrflow.notification.outbox.PublicationException;
import com.hrflow.notification.outbox.PublicationTrace;
import java.sql.Connection;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@Testcontainers
class PublishBoundedOrderedKafkaWorkTest {

    @Container
    static final PostgreSQLContainer<?> database = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("hrflow")
            .withUsername("hrflow")
            .withPassword("test-only")
            .withInitScript("context-schema.sql");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", database::getJdbcUrl);
        properties.add("spring.datasource.username", database::getUsername);
        properties.add("spring.datasource.password", database::getPassword);
        properties.add("spring.flyway.user", database::getUsername);
        properties.add("spring.flyway.password", database::getPassword);
    }

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    JdbcOutboxStore outboxStore;

    private ControlledKafkaTemplate kafkaTemplate;
    private OrderedOutboxRelay relay;

    @BeforeEach
    void setUp() {
        kafkaTemplate = new ControlledKafkaTemplate();
        OutboxPublisher publisher = new OutboxPublisher(kafkaTemplate, outboxStore);
        relay = new OrderedOutboxRelay(jdbc, publisher);
    }

    @Test
    @DisplayName("Ordered publication: earlier revision publishes first and later revision cannot bypass earlier")
    void orderedPublicationPreventsBypass() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        UUID event1 = UUID.randomUUID();
        UUID event2 = UUID.randomUUID();
        String topic = "org.attendance.session.v1";

        try (Connection conn = jdbc.getDataSource().getConnection()) {
            conn.createStatement().execute("SET ROLE notification_app");
            conn.createStatement().execute("SET app.tenant_id = '" + tenantId + "'");

            // Save revision 1 and revision 2 for same aggregate
            outboxStore.save(new OutboxRecord(
                    event1, tenantId, topic, aggregateId, 1L, 1, tenantId + ":" + aggregateId, "{\"rev\":1}", Instant.now()
            ));
            outboxStore.save(new OutboxRecord(
                    event2, tenantId, topic, aggregateId, 2L, 1, tenantId + ":" + aggregateId, "{\"rev\":2}", Instant.now()
            ));

            // First relay poll must publish revision 1
            Optional<PublicationTrace> first = relay.publishNextPending(tenantId);
            assertThat(first).isPresent();
            assertThat(first.get().eventId()).isEqualTo(event1);
            assertThat(first.get().aggregateRevision()).isEqualTo(1L);
            assertThat(outboxStore.isPublished(event1)).isTrue();
            assertThat(outboxStore.isPublished(event2)).isFalse();

            // Second relay poll must publish revision 2
            Optional<PublicationTrace> second = relay.publishNextPending(tenantId);
            assertThat(second).isPresent();
            assertThat(second.get().eventId()).isEqualTo(event2);
            assertThat(second.get().aggregateRevision()).isEqualTo(2L);
            assertThat(outboxStore.isPublished(event2)).isTrue();

            // Third relay poll has no pending
            Optional<PublicationTrace> third = relay.publishNextPending(tenantId);
            assertThat(third).isEmpty();
        }
    }

    @Test
    @DisplayName("Broker outage leaves earlier revision pending, blocking later revision from overtaking")
    void brokerOutageBlocksLaterRevision() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        UUID event1 = UUID.randomUUID();
        UUID event2 = UUID.randomUUID();
        String topic = "org.attendance.session.v1";

        try (Connection conn = jdbc.getDataSource().getConnection()) {
            conn.createStatement().execute("SET ROLE notification_app");
            conn.createStatement().execute("SET app.tenant_id = '" + tenantId + "'");

            outboxStore.save(new OutboxRecord(
                    event1, tenantId, topic, aggregateId, 1L, 1, tenantId + ":" + aggregateId, "{\"rev\":1}", Instant.now()
            ));
            outboxStore.save(new OutboxRecord(
                    event2, tenantId, topic, aggregateId, 2L, 1, tenantId + ":" + aggregateId, "{\"rev\":2}", Instant.now()
            ));

            // Simulate broker failure
            kafkaTemplate.failNextSend(true);

            assertThatThrownBy(() -> relay.publishNextPending(tenantId))
                    .isInstanceOf(PublicationException.class);

            // Revision 1 is still pending
            assertThat(outboxStore.isPublished(event1)).isFalse();
            assertThat(outboxStore.isPublished(event2)).isFalse();

            // Recover Kafka
            kafkaTemplate.failNextSend(false);

            // Next attempt must still pick revision 1, NOT revision 2!
            Optional<PublicationTrace> trace = relay.publishNextPending(tenantId);
            assertThat(trace).isPresent();
            assertThat(trace.get().eventId()).isEqualTo(event1);
            assertThat(trace.get().aggregateRevision()).isEqualTo(1L);
            assertThat(outboxStore.isPublished(event1)).isTrue();
        }
    }

    @SuppressWarnings("unchecked")
    static class ControlledKafkaTemplate extends KafkaTemplate<String, Object> {
        private boolean fail = false;

        public ControlledKafkaTemplate() {
            super(org.mockito.Mockito.mock(ProducerFactory.class));
        }

        public void failNextSend(boolean fail) {
            this.fail = fail;
        }

        @Override
        public CompletableFuture<SendResult<String, Object>> send(String topic, String key, Object data) {
            CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
            if (fail) {
                future.completeExceptionally(new RuntimeException("Kafka broker unavailable"));
            } else {
                ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, data);
                RecordMetadata metadata = new RecordMetadata(new TopicPartition(topic, 0), 0, 0, System.currentTimeMillis(), 0, 0);
                future.complete(new SendResult<>(record, metadata));
            }
            return future;
        }
    }
}
