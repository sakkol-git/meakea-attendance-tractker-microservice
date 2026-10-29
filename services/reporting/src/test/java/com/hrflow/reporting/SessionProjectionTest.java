package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.contracts.attendance.AttendanceSessionFact;
import com.hrflow.reporting.consumer.SessionProjectionConsumer;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class SessionProjectionTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("context-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", POSTGRES::getUsername);
        p.add("spring.datasource.password", POSTGRES::getPassword);
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @Autowired SessionProjectionConsumer consumer;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transactions;

    @Test void openCompletedDuplicateAndOutOfOrderFactsKeepActualDurationsAndRevision() {
        var open = fact(1, null);
        consumer.apply(record(open, 0));
        var row = row(open);
        assertThat(row.get("minutes_worked")).isNull();
        assertThat(row.get("status")).isEqualTo("OPEN");
        var completed = AttendanceSessionFact.newBuilder(open).setEventId(UUID.randomUUID().toString())
                .setAggregateVersion(2).setClockOut(Instant.parse("2026-10-28T10:02:00Z"))
                .setState("COMPLETED").build();
        consumer.apply(record(completed, 1));
        consumer.apply(record(completed, 1));
        var older = AttendanceSessionFact.newBuilder(open).setEventId(UUID.randomUUID().toString()).build();
        consumer.apply(record(older, 2));
        row = row(open);
        assertThat(row.get("aggregate_revision")).isEqualTo(2L);
        assertThat(row.get("minutes_worked")).isEqualTo(542);
        assertThat(row.get("status")).isEqualTo("COMPLETED");
        assertThat(row.get("source")).isEqualTo("TAP");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reporting.reporting_inbox WHERE tenant_id=?",
                Integer.class, UUID.fromString(open.getTenantId()))).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT last_offset FROM reporting.checkpoint_progress WHERE tenant_id=?",
                Long.class, UUID.fromString(open.getTenantId()))).isEqualTo(2L);
    }

    @Test void wrongRoutingAndRollbackNeverLeaveAReceiptProjectionOrCheckpoint() {
        var event = fact(1, null);
        assertThatThrownBy(() -> consumer.apply(new ConsumerRecord<>("attendance.session.v1", 0, 0,
                UUID.randomUUID().toString(), event))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> transactions.executeWithoutResult(tx -> {
            consumer.apply(record(event, 0));
            throw new IllegalStateException("rollback after effect");
        })).isInstanceOf(IllegalStateException.class);
        for (String table : java.util.List.of("attendance_fact_projection", "reporting_inbox", "checkpoint_progress")) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM reporting." + table + " WHERE tenant_id=?",
                    Integer.class, UUID.fromString(event.getTenantId()))).isZero();
        }
        consumer.apply(record(event, 0));
        assertThat(row(event).get("aggregate_revision")).isEqualTo(1L);
    }

    static AttendanceSessionFact fact(long revision, Instant out) {
        return AttendanceSessionFact.newBuilder().setEventId(UUID.randomUUID().toString())
                .setType("AttendanceSessionSnapshot.v1").setSchemaVersion(1)
                .setTenantId(UUID.randomUUID().toString()).setAggregateId(UUID.randomUUID().toString())
                .setAggregateVersion(revision).setOccurredAt(Instant.now()).setCorrelationId(UUID.randomUUID().toString())
                .setEmployeeId(UUID.randomUUID().toString()).setWorkDate(LocalDate.parse("2026-10-28"))
                .setTimezone("Asia/Phnom_Penh").setClockIn(Instant.parse("2026-10-28T01:00:00Z"))
                .setClockOut(out).setState(out == null ? "OPEN" : "COMPLETED")
                .setScheduledStart(Instant.parse("2026-10-28T01:00:00Z"))
                .setScheduledEnd(Instant.parse("2026-10-28T10:00:00Z")).setGraceMinutes(10).setSource("TAP").build();
    }
    static ConsumerRecord<String, AttendanceSessionFact> record(AttendanceSessionFact event, long offset) {
        return new ConsumerRecord<>("attendance.session.v1", 0, offset,
                event.getTenantId() + ":" + event.getEmployeeId(), event);
    }
    private java.util.Map<String, Object> row(AttendanceSessionFact event) {
        return jdbc.queryForMap("SELECT * FROM reporting.attendance_fact_projection WHERE tenant_id=? AND session_id=?",
                UUID.fromString(event.getTenantId()), UUID.fromString(event.getAggregateId()));
    }
}
