package com.hrflow.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.attendance.leave.LeaveProjection;
import com.hrflow.contracts.leave.LeaveRequestFact;
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
class LeaveReferenceTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("context-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", POSTGRES::getUsername);
        p.add("spring.datasource.password", POSTGRES::getPassword);
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @Autowired LeaveProjection projection;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transactions;

    @Test void actualDatesAndNewestRevisionDetermineOverlayNotOuterDateRangeOrAnotherEmployeesLeave() {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID(), leave = UUID.randomUUID();
        var day = LocalDate.parse("2026-11-02");
        var approved = LeaveOverlayKafkaTest.fact(tenant, employee, leave, day, 1, "APPROVED");
        projection.apply(record(approved));
        projection.apply(record(LeaveOverlayKafkaTest.fact(tenant, employee, leave, day, 2, "NEEDS_REVIEW")));
        projection.apply(record(approved));
        projection.apply(record(LeaveOverlayKafkaTest.fact(tenant, employee, leave, day, 1, "CANCELLED")));
        assertThat(projection.fullDay(tenant, employee, day)).isTrue();
        assertThat(projection.fullDay(tenant, employee, day.minusDays(1))).isFalse();
        assertThat(projection.fullDay(tenant, UUID.randomUUID(), day)).isFalse();
        assertThat(projection.fullDay(UUID.randomUUID(), employee, day)).isFalse();
        assertThat(jdbc.queryForObject("SELECT charged_days FROM attendance.leave_projection WHERE tenant_id=?", java.math.BigDecimal.class, tenant))
                .isEqualByComparingTo("1.00");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM attendance.leave_inbox WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(3);
    }

    @Test void rollbackAndInvalidRoutingWriteNeitherReceiptNorReference() {
        var fact = LeaveOverlayKafkaTest.fact(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), LocalDate.now(), 1, "APPROVED");
        assertThatThrownBy(() -> projection.apply(new ConsumerRecord<>("leave.request.v1", 0, 0, "wrong", fact)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> transactions.executeWithoutResult(tx -> {
            projection.apply(record(fact));
            throw new IllegalStateException("rollback reference");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM attendance.leave_inbox WHERE tenant_id=?", Integer.class, UUID.fromString(fact.getTenantId()))).isZero();
        assertThat(projection.fullDay(UUID.fromString(fact.getTenantId()), UUID.fromString(fact.getEmployeeId()), fact.getWorkingDates().getFirst())).isFalse();
    }

    private static ConsumerRecord<String, LeaveRequestFact> record(LeaveRequestFact fact) {
        return new ConsumerRecord<>("leave.request.v1", 0, fact.getAggregateVersion(), fact.getTenantId() + ":" + fact.getEmployeeId(), fact);
    }
}
