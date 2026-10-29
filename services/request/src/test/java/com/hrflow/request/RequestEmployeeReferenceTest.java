package com.hrflow.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.contracts.organization.EmployeeSnapshot;
import com.hrflow.contracts.organization.OrganizationFact;
import com.hrflow.request.organization.RequestEmployeeConsumer;
import com.hrflow.request.organization.RequestRelationships;
import com.hrflow.request.service.TenantSqlScope;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.ErrorResponseException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class RequestEmployeeReferenceTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine").withInitScript("context-schema.sql");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", () -> "request_app");
        p.add("spring.datasource.password", () -> "test-pass");
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @MockitoBean com.hrflow.request.organization.RequestEmployeeBootstrap replay;
    @Autowired RequestEmployeeConsumer consumer;
    @Autowired RequestRelationships relationships;
    @Autowired TenantSqlScope scope;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transactions;

    @Test void receiptAndRelationshipRollbackTogetherAndStaleActiveCannotRestoreTermination() {
        String tenant = UUID.randomUUID().toString(), employee = UUID.randomUUID().toString(), manager = UUID.randomUUID().toString();
        var original = fact(tenant, employee, manager, 1, "ACTIVE");
        assertThatThrownBy(() -> consumer.apply("wrong", original)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> transactions.executeWithoutResult(tx -> {
            consumer.apply(tenant + ":" + employee, original);
            throw new IllegalStateException("interrupt");
        })).isInstanceOf(IllegalStateException.class);
        transactions.executeWithoutResult(tx -> {
            scope.use(tenant);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM request.organization_inbox", Integer.class)).isZero();
        });
        consumer.apply(tenant + ":" + employee, original);
        var terminated = fact(tenant, employee, manager, 2, "TERMINATED");
        consumer.apply(tenant + ":" + employee, terminated);
        consumer.apply(tenant + ":" + employee, terminated);
        consumer.apply(tenant + ":" + employee, fact(tenant, employee, manager, 1, "ACTIVE"));
        org.mockito.Mockito.when(replay.isReady()).thenReturn(true);
        transactions.executeWithoutResult(tx -> {
            scope.use(tenant);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM request.organization_inbox", Integer.class)).isEqualTo(3);
            assertThat(jdbc.queryForObject("SELECT active FROM request.employee_projection", Boolean.class)).isFalse();
            assertThatThrownBy(() -> relationships.current(tenant, employee)).isInstanceOfSatisfying(ErrorResponseException.class,
                    error -> assertThat(error.getStatusCode().value()).isEqualTo(403));
        });
        assertThat(jdbc.queryForObject("SELECT count(*) FROM request.employee_projection", Integer.class)).isZero();
    }

    @Test void knownMissingManagerIsDistinctFromUnknownOrUnreplayedEmployee() {
        String tenant = UUID.randomUUID().toString(), employee = UUID.randomUUID().toString();
        consumer.apply(tenant + ":" + employee, fact(tenant, employee, null, 1, "ACTIVE"));
        transactions.executeWithoutResult(tx -> {
            scope.use(tenant);
            assertThatThrownBy(() -> relationships.current(tenant, employee)).isInstanceOfSatisfying(ErrorResponseException.class,
                    error -> assertThat(error.getStatusCode().value()).isEqualTo(503));
            org.mockito.Mockito.when(replay.isReady()).thenReturn(true);
            assertThat(relationships.current(tenant, employee).manager()).isNull();
            assertThatThrownBy(() -> relationships.current(tenant, UUID.randomUUID().toString()))
                    .isInstanceOfSatisfying(ErrorResponseException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(503));
        });
    }

    static OrganizationFact fact(String tenant, String employee, String manager, long revision, String status) {
        var body = EmployeeSnapshot.newBuilder().setEmployeeId(employee).setEmployeeNo("Synthetic")
                .setFullName("Synthetic").setStatus(status).setDepartmentId(UUID.randomUUID().toString())
                .setPositionId(UUID.randomUUID().toString()).setManagerId(manager).setAssignments(java.util.List.of()).build();
        return OrganizationFact.newBuilder().setEventId(UUID.randomUUID().toString()).setType("org.employee.updated.v1")
                .setSchemaVersion(1).setTenantId(tenant).setAggregateId(employee).setAggregateVersion(revision)
                .setOccurredAt(java.time.Instant.now().toString()).setCorrelationId(UUID.randomUUID().toString()).setPayload(body).build();
    }
}
