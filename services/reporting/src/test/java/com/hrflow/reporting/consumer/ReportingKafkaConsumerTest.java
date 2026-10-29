package com.hrflow.reporting.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.contracts.attendance.AttendanceCheckedIn;
import com.hrflow.contracts.attendance.Source;
import com.hrflow.reporting.repository.AttendanceFactProjectionRepository;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@Testcontainers
class ReportingKafkaConsumerTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("hrflow")
            .withUsername("hrflow")
            .withPassword("test-only")
            .withInitScript("context-schema.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired
    ReportingProjectionConsumer consumer;

    @Autowired
    AttendanceFactProjectionRepository attendanceRepo;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("Blocked: AttendanceCheckedIn event processing throws UnsupportedOperationException because event lacks aggregate revision")
    void attendanceCheckedInProcessingIsBlocked() {
        UUID tenantId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-10-20T08:30:00Z");

        AttendanceCheckedIn event = AttendanceCheckedIn.newBuilder()
                .setEventId(eventId)
                .setTenantId(tenantId.toString())
                .setEmployeeId(employeeId.toString())
                .setOccurredAt(occurredAt)
                .setSource(Source.MANUAL)
                .setReason("On-site check-in")
                .build();

        AtomicBoolean ackCalled = new AtomicBoolean(false);

        assertThatThrownBy(() -> consumer.processAttendanceCheckedIn(sessionId.toString(), event, () -> ackCalled.set(true)))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("AttendanceCheckedIn processing is BLOCKED");

        assertThat(ackCalled.get()).isFalse();

        // Verify nothing was persisted into reporting_inbox
        jdbc.queryForObject("SELECT set_config('app.tenant_id', ?, true)", String.class, tenantId.toString());
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM reporting.reporting_inbox WHERE tenant_id = ? AND event_id = ?",
                Integer.class,
                tenantId, eventId
        );
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("Blocked: ReportingKafkaListener does not persist receipt with sentinel revision or acknowledge offset")
    void unsupportedListenerDoesNotPersistOrAcknowledgeReceipt() {
        ReportingKafkaListener listener = new ReportingKafkaListener(consumer);

        UUID tenantId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-10-20T08:30:00Z");

        AttendanceCheckedIn event = AttendanceCheckedIn.newBuilder()
                .setEventId(eventId)
                .setTenantId(tenantId.toString())
                .setEmployeeId(employeeId.toString())
                .setOccurredAt(occurredAt)
                .setSource(Source.MANUAL)
                .setReason("On-site check-in")
                .build();

        ConsumerRecord<String, AttendanceCheckedIn> record =
                new ConsumerRecord<>("hrflow.attendance.facts", 0, 100L, sessionId.toString(), event);

        AtomicBoolean ackCalled = new AtomicBoolean(false);
        Acknowledgment ack = () -> ackCalled.set(true);

        assertThatThrownBy(() -> listener.onAttendanceCheckedIn(record, ack))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("AttendanceCheckedIn processing is BLOCKED");

        assertThat(ackCalled.get()).isFalse();

        // Verify no receipt with revision 0 or 1 was persisted
        jdbc.queryForObject("SELECT set_config('app.tenant_id', ?, true)", String.class, tenantId.toString());
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM reporting.reporting_inbox WHERE tenant_id = ? AND event_id = ?",
                Integer.class,
                tenantId, eventId
        );
        assertThat(count).isEqualTo(0);

        // Verify no fact projection was persisted
        assertThat(attendanceRepo.findByTenantIdAndSessionId(tenantId, sessionId)).isEmpty();
    }

    @Test
    @org.junit.jupiter.api.Timeout(5)
    @DisplayName("Invalid envelope throws exception and strictly does not acknowledge Kafka offset")
    void invalidEnvelopeDoesNotAcknowledgeOffset() {
        AtomicBoolean ackCalled = new AtomicBoolean(false);

        assertThatThrownBy(() -> consumer.processAttendanceCheckedIn(null, null, () -> ackCalled.set(true)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(ackCalled.get()).isFalse();
    }
}
