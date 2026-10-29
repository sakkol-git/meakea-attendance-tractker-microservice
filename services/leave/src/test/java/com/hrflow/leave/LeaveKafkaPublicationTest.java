package com.hrflow.leave;

import static org.assertj.core.api.Assertions.assertThat;

import com.hrflow.contracts.leave.LeaveRequestFact;
import com.hrflow.leave.dto.CreateLeaveRequestDto;
import com.hrflow.leave.dto.DecisionRequest;
import com.hrflow.leave.service.LeaveSubmissionService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = {"hrflow.leave.relay.enabled=true", "hrflow.leave.relay.poll-ms=50"})
@Testcontainers
class LeaveKafkaPublicationTest extends LeaveKafkaEnvironment {
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.hrflow.leave.organization.LeaveCalendarBootstrap calendarBootstrap;
    @org.junit.jupiter.api.BeforeEach void calendarReferenceReady() {
        org.mockito.Mockito.when(calendarBootstrap.isReady()).thenReturn(true);
    }

    @org.springframework.test.context.bean.override.mockito.MockitoBean com.hrflow.leave.organization.LeaveEmployeeBootstrap bootstrap;
    @org.junit.jupiter.api.BeforeEach void referenceReady() { org.mockito.Mockito.when(bootstrap.isReady()).thenReturn(true); }

    @Autowired com.hrflow.leave.service.LeaveDecisions decisions;
    @SuppressWarnings("rawtypes")
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean(name = "kafkaTemplate")
    org.springframework.kafka.core.KafkaTemplate kafka;
    @Autowired LeaveSubmissionService leave;

    @Test void actualLeaveCommandsAutomaticallyPublishRegisteredPrivateSnapshotsInOrder() throws Exception {
        var admin = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        String tenant = UUID.randomUUID().toString(), employee = UUID.randomUUID().toString();
        var day = LocalDate.now().plusDays(7).with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY));
        admin.update("INSERT INTO leave.leave_type(id,tenant_id,code,name,days_allowed) VALUES (?,?,'ANNUAL','Annual',20)",
                UUID.randomUUID().toString(), tenant);
        admin.update("""
                INSERT INTO leave.leave_balance(id,tenant_id,employee_id,leave_type_code,year,allocated,used,pending,available)
                VALUES (?,?,?,'ANNUAL',?,20,0,0,20)
                """, UUID.randomUUID().toString(), tenant, employee, day.getYear());
        LeaveReferenceFixtures.calendar(admin, tenant, employee, 31);
        var allowSend = new java.util.concurrent.atomic.AtomicBoolean(false);
        var failedAttempts = new java.util.concurrent.atomic.AtomicInteger();
        org.mockito.Mockito.doAnswer(call -> {
            if (!allowSend.get()) {
                failedAttempts.incrementAndGet();
                return java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException("Injected broker failure"));
            }
            return call.callRealMethod();
        }).when(kafka).send(org.mockito.ArgumentMatchers.eq("leave.request.v1"),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        var pending = leave.submitLeaveRequest(tenant, employee, "Sensitive name",
                new CreateLeaveRequestDto("ANNUAL", day, day, "FULL_DAY", "Sensitive reason", "Sensitive title"));
        var approved = decisions.approveLeaveRequest(tenant, UUID.randomUUID().toString(), List.of("HR_ADMIN"), pending.id(),
                "\"" + pending.version() + "\"", new DecisionRequest("Verified", "Approved"));
        decisions.cancelLeaveRequest(tenant, employee, pending.id(), "\"" + approved.version() + "\"", "Changed plans");
        var next = leave.submitLeaveRequest(tenant, employee, "Sensitive name",
                new CreateLeaveRequestDto("ANNUAL", day, day, "FULL_DAY", "Sensitive reason", "Sensitive title"));
        decisions.rejectLeaveRequest(tenant, UUID.randomUUID().toString(), List.of("HR_ADMIN"), next.id(),
                "\"" + next.version() + "\"", new DecisionRequest("Declined", "Reviewed"));
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> failedAttempts.get() > 0);
        assertThat(admin.queryForObject("SELECT count(*) FROM leave.leave_outbox WHERE tenant_id=? AND status='PUBLISHED'",
                Integer.class, tenant)).isZero();
        allowSend.set(true);
        var properties = Map.<String, Object>of("bootstrap.servers", KAFKA.getBootstrapServers(), "group.id", "leave-publication-" + UUID.randomUUID(),
                "auto.offset.reset", "earliest", "enable.auto.commit", false,
                "key.deserializer", org.apache.kafka.common.serialization.StringDeserializer.class,
                "value.deserializer", io.confluent.kafka.serializers.KafkaAvroDeserializer.class,
                "schema.registry.url", registryUrl(), "specific.avro.reader", true);
        List<LeaveRequestFact> received = new ArrayList<>();
        try (var consumer = new KafkaConsumer<String, LeaveRequestFact>(properties)) {
            consumer.subscribe(List.of("leave.request.v1"));
            long deadline = System.nanoTime() + Duration.ofSeconds(8).toNanos();
            while (received.size() < 5 && System.nanoTime() < deadline) {
                for (var record : consumer.poll(Duration.ofMillis(250))) {
                    assertThat(record.key()).isEqualTo(tenant + ":" + employee);
                    received.add(record.value());
                }
            }
        }
        assertThat(received).hasSize(5);
        assertThat(received).extracting(LeaveRequestFact::getState).containsExactly("PENDING", "APPROVED", "CANCELLED", "PENDING", "REJECTED");
        assertThat(received.get(1).getAggregateVersion()).isGreaterThan(received.get(0).getAggregateVersion());
        assertThat(received.get(1).getWorkingDates()).containsExactly(day);
        assertThat(received.get(1).toString()).doesNotContain("Sensitive");
        assertThat(received.get(2).getWorkingDates()).containsExactly(day);
        assertThat(received.get(4).getWorkingDates()).containsExactly(day);
        assertThat(admin.queryForObject("SELECT count(*) FROM leave.leave_day WHERE tenant_id=?", Integer.class, tenant)).isZero();
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(admin.queryForObject("SELECT count(*) FROM leave.leave_outbox WHERE tenant_id=? AND status='PUBLISHED'",
                        Integer.class, tenant)).isEqualTo(5));
    }
}
