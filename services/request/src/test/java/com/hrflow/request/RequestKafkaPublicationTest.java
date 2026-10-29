package com.hrflow.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.hrflow.contracts.request.RequestWorkflowFact;
import com.hrflow.request.dto.CreateGeneralRequestDto;
import com.hrflow.request.dto.FulfillRequest;
import com.hrflow.request.service.RequestService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest(properties = {"hrflow.request.employee-consumer.enabled=true", "spring.kafka.listener.auto-startup=true",
        "hrflow.request.relay.enabled=true", "hrflow.request.relay.poll-ms=50"})
class RequestKafkaPublicationTest extends RequestKafkaEnvironment {
    @Autowired RequestService requests;
    @SuppressWarnings("rawtypes") @MockitoSpyBean(name = "kafkaTemplate") org.springframework.kafka.core.KafkaTemplate kafka;

    @Test void realCommandsPublishOrderedPrivateWorkflowFactsOnlyAfterBrokerAcknowledgment() throws Exception {
        var admin = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        try (var attendanceLeader = java.sql.DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var lock = attendanceLeader.createStatement()) {
            var result = lock.executeQuery("SELECT pg_try_advisory_lock(73104221)");
            result.next();
            assertThat(result.getBoolean(1)).as("Attendance daily-worker leadership must coexist with request publication").isTrue();
        var allowSend = new AtomicBoolean(false);
        var attempts = new AtomicInteger();
        org.mockito.Mockito.doAnswer(call -> {
            if (!allowSend.get()) {
                attempts.incrementAndGet();
                return CompletableFuture.failedFuture(new IllegalStateException("Injected broker failure"));
            }
            return call.callRealMethod();
        }).when(kafka).send(org.mockito.ArgumentMatchers.eq("request.workflow.v1"), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        var draft = requests.createDraftRequest(TENANT, EMPLOYEE, "Sensitive name", CreateGeneralRequestDto.builder()
                .title("Sensitive title").type("MATERIAL").reason("Sensitive reason").quantity(1).build());
        var submitted = requests.submitRequest(TENANT, EMPLOYEE, draft.getId(), null, "0");
        var managerApproved = requests.approveRequestStep(TENANT, MANAGER, Set.of("MANAGER"), draft.getId(), null, Long.toString(submitted.getVersion()));
        var hrApproved = requests.approveRequestStep(TENANT, UUID.randomUUID().toString(), Set.of("HR_ADMIN"), draft.getId(), null, Long.toString(managerApproved.getVersion()));
        var fulfilled = requests.fulfillApprovedMaterial(TENANT, UUID.randomUUID().toString(), Set.of("HR_ADMIN"), draft.getId(),
                FulfillRequest.builder().handoverReason("Sensitive serial and private notes").build(), Long.toString(hrApproved.getVersion()));
        await().atMost(Duration.ofSeconds(5)).until(() -> attempts.get() > 0);
        assertThat(admin.queryForObject("SELECT count(*) FROM request.request_outbox WHERE status='PUBLISHED'", Integer.class)).isZero();
        allowSend.set(true);
        List<RequestWorkflowFact> facts = new ArrayList<>();
        try (var consumer = new KafkaConsumer<String, RequestWorkflowFact>(Map.of("bootstrap.servers", KAFKA.getBootstrapServers(),
                "group.id", "request-publication-" + UUID.randomUUID(), "auto.offset.reset", "earliest", "enable.auto.commit", false,
                "key.deserializer", org.apache.kafka.common.serialization.StringDeserializer.class,
                "value.deserializer", io.confluent.kafka.serializers.KafkaAvroDeserializer.class,
                "schema.registry.url", registry(), "specific.avro.reader", true))) {
            consumer.subscribe(List.of("request.workflow.v1"));
            long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
            while (facts.size() < 4 && System.nanoTime() < deadline) {
                for (var record : consumer.poll(Duration.ofMillis(250))) {
                    assertThat(record.key()).isEqualTo(TENANT + ":" + draft.getId());
                    facts.add(record.value());
                }
            }
        }
        assertThat(facts).extracting(RequestWorkflowFact::getState).containsExactly("PENDING_APPROVAL", "PENDING_APPROVAL", "APPROVED", "FULFILLED");
        assertThat(facts).extracting(RequestWorkflowFact::getAggregateVersion).containsExactly(submitted.getVersion() + 1,
                managerApproved.getVersion() + 1, hrApproved.getVersion() + 1, fulfilled.getVersion() + 1);
        assertThat(facts).allSatisfy(fact -> {
            assertThat(fact.toString()).doesNotContain("Sensitive");
            assertThat(fact.getCreatedAt()).isEqualTo(facts.getFirst().getCreatedAt());
        });
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(admin.queryForObject(
                "SELECT count(*) FROM request.request_outbox WHERE status='PUBLISHED'", Integer.class)).isEqualTo(4));
        assertThat(admin.queryForObject("SELECT count(*) FROM request.workflow_history", Integer.class)).isEqualTo(4);
        var job = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), "request_job", "job-test-pass"));
        assertThat(job.queryForList("SELECT tenant_id FROM request_jobs.tenant_registry", String.class)).containsExactly(TENANT);
        assertThatThrownBy(() -> job.queryForList("SELECT payload FROM request.request_outbox")).isInstanceOf(org.springframework.dao.DataAccessException.class);
        }
    }
}
