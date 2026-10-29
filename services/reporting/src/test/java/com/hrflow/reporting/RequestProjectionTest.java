package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.contracts.request.RequestApprovalStepFact;
import com.hrflow.contracts.request.RequestWorkflowFact;
import com.hrflow.reporting.consumer.RequestProjectionConsumer;
import com.hrflow.reporting.service.TenantSqlScope;
import java.time.Instant;
import java.util.List;
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
class RequestProjectionTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine").withInitScript("context-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", () -> "reporting_app");
        p.add("spring.datasource.password", () -> "test-pass");
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @Autowired RequestProjectionConsumer consumer;
    @Autowired JdbcTemplate jdbc;
    @Autowired TenantSqlScope scope;
    @Autowired TransactionTemplate transactions;

    @Test void duplicateAndStaleSnapshotsCannotUndoLatestOwnerFactsAndCheckpointIsMonotonic() {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID(), request = UUID.randomUUID();
        var first = fact(tenant, employee, request, 1, "PENDING_APPROVAL");
        var latest = fact(tenant, employee, request, 3, "APPROVED");
        consumer.apply(record(first, 0)); consumer.apply(record(latest, 2));
        consumer.apply(record(latest, 2)); consumer.apply(record(fact(tenant, employee, request, 2, "CANCELLED"), 1));
        transactions.executeWithoutResult(tx -> {
            scope.use(tenant);
            var row = jdbc.queryForMap("SELECT status,aggregate_revision,created_at FROM reporting.request_fact_projection WHERE tenant_id=?", tenant);
            assertThat(row.get("status")).isEqualTo("APPROVED");
            assertThat(row.get("aggregate_revision")).isEqualTo(3L);
            assertThat(jdbc.queryForObject("SELECT created_at FROM reporting.request_fact_projection", java.sql.Timestamp.class).toInstant()).isEqualTo(first.getCreatedAt());
            assertThat(jdbc.queryForObject("SELECT count(*) FROM reporting.reporting_inbox", Integer.class)).isEqualTo(3);
            assertThat(jdbc.queryForObject("SELECT last_offset FROM reporting.checkpoint_progress", Long.class)).isEqualTo(2L);
        });
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reporting.request_fact_projection", Integer.class)).isZero();
        assertThatThrownBy(() -> consumer.apply(record(RequestWorkflowFact.newBuilder(latest).setEventId(UUID.randomUUID().toString())
                .setAggregateVersion(4).setEmployeeId(UUID.randomUUID().toString()).build(), 3))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void invalidRoutingAndCommandInterruptionCannotLeaveAReceiptFactOrCheckpoint() {
        UUID tenant = UUID.randomUUID();
        var event = fact(tenant, UUID.randomUUID(), UUID.randomUUID(), 1, "PENDING_APPROVAL");
        assertThatThrownBy(() -> consumer.apply(new ConsumerRecord<>("request.workflow.v1", 0, 0, "wrong", event))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> transactions.executeWithoutResult(tx -> {
            consumer.apply(record(event, 0)); throw new IllegalStateException("interrupt");
        })).isInstanceOf(IllegalStateException.class);
        transactions.executeWithoutResult(tx -> {
            scope.use(tenant);
            for (String table : List.of("request_fact_projection", "reporting_inbox", "checkpoint_progress")) {
                assertThat(jdbc.queryForObject("SELECT count(*) FROM reporting." + table, Integer.class)).isZero();
            }
        });
        consumer.apply(record(event, 0));
    }

    static ConsumerRecord<String, RequestWorkflowFact> record(RequestWorkflowFact fact, long offset) {
        return new ConsumerRecord<>("request.workflow.v1", 0, offset, fact.getTenantId() + ":" + fact.getAggregateId(), fact);
    }
    static RequestWorkflowFact fact(UUID tenant, UUID employee, UUID request, long revision, String state) {
        boolean approved = "APPROVED".equals(state) || "FULFILLED".equals(state);
        return RequestWorkflowFact.newBuilder().setEventId(UUID.randomUUID().toString()).setType("RequestWorkflowSnapshot.v1").setSchemaVersion(1)
                .setTenantId(tenant.toString()).setAggregateId(request.toString()).setAggregateVersion(revision).setOccurredAt(Instant.now())
                .setCorrelationId(UUID.randomUUID().toString()).setEmployeeId(employee.toString()).setRequestType("MATERIAL").setState(state)
                .setCreatedAt(Instant.parse("2026-01-01T01:00:00Z")).setSteps(List.of(
                        new RequestApprovalStepFact(1, "MANAGER", UUID.nameUUIDFromBytes((request + ":manager").getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString(), approved ? "APPROVED" : "PENDING"),
                        new RequestApprovalStepFact(2, "HR_ADMIN", null, approved ? "APPROVED" : "PENDING"))).build();
    }
}
