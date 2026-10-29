package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.contracts.leave.LeaveRequestFact;
import com.hrflow.reporting.consumer.LeaveProjectionConsumer;
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
class LeaveProjectionTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("context-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", POSTGRES::getUsername);
        p.add("spring.datasource.password", POSTGRES::getPassword);
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @Autowired LeaveProjectionConsumer consumer;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transactions;

    @Test void newerReviewPreservesDebitAndOldOrDuplicateSnapshotCannotUndoIt() {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID(), leave = UUID.randomUUID();
        var approved = LeaveReportingKafkaIT.fact(tenant, employee, leave, 1, "APPROVED");
        var review = LeaveReportingKafkaIT.fact(tenant, employee, leave, 2, "NEEDS_REVIEW");
        consumer.apply(record(approved, 0));
        consumer.apply(record(review, 1));
        consumer.apply(record(review, 1));
        consumer.apply(record(LeaveReportingKafkaIT.fact(tenant, employee, leave, 1, "CANCELLED"), 2));
        var row = jdbc.queryForMap("SELECT * FROM reporting.leave_fact_projection WHERE tenant_id=? AND leave_id=?", tenant, leave);
        assertThat(row.get("status")).isEqualTo("NEEDS_REVIEW");
        assertThat(row.get("portion_days").toString()).isEqualTo("1.00");
        assertThat(row.get("aggregate_revision")).isEqualTo(2L);
        assertThat(row.get("working_dates").toString()).contains(approved.getStartDate().toString());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reporting.reporting_inbox WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT last_offset FROM reporting.checkpoint_progress WHERE tenant_id=?", Long.class, tenant)).isEqualTo(2L);
    }

    @Test void invalidRoutingOrOwnerDatesAndTransactionRollbackLeaveNoEffectOrReceipt() {
        var fact = LeaveReportingKafkaIT.fact(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, "APPROVED");
        assertThatThrownBy(() -> consumer.apply(new ConsumerRecord<>("leave.request.v1", 0, 0, "wrong", fact)))
                .isInstanceOf(IllegalArgumentException.class);
        var invalid = LeaveRequestFact.newBuilder(fact).setWorkingDates(java.util.List.of(fact.getEndDate().plusDays(1))).build();
        assertThatThrownBy(() -> consumer.apply(record(invalid, 0))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> transactions.executeWithoutResult(tx -> {
            consumer.apply(record(fact, 0));
            throw new IllegalStateException("rollback after owner fact");
        })).isInstanceOf(IllegalStateException.class);
        for (String table : java.util.List.of("leave_fact_projection", "reporting_inbox", "checkpoint_progress")) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM reporting." + table + " WHERE tenant_id=?",
                    Integer.class, UUID.fromString(fact.getTenantId()))).isZero();
        }
        consumer.apply(record(fact, 0));
    }

    private static ConsumerRecord<String, LeaveRequestFact> record(LeaveRequestFact event, long offset) {
        return new ConsumerRecord<>("leave.request.v1", 0, offset, event.getTenantId() + ":" + event.getEmployeeId(), event);
    }
}
