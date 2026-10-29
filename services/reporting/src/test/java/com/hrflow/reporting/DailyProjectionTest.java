package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.contracts.attendance.AttendanceDailyFact;
import com.hrflow.reporting.consumer.DailyProjectionConsumer;
import com.hrflow.reporting.repository.DailyReportRepository;
import com.hrflow.reporting.service.TenantSqlScope;
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
class DailyProjectionTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("context-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", POSTGRES::getUsername);
        p.add("spring.datasource.password", POSTGRES::getPassword);
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @Autowired DailyProjectionConsumer consumer;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transactions;
    @Autowired DailyReportRepository reports;
    @Autowired TenantSqlScope scope;

    @Test void duplicateOrOldSnapshotCannotUndoFinalDailyRevisionAndCheckpoint() {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID(), day = UUID.randomUUID();
        var pending = DailyReportingKafkaIT.fact(tenant, employee, day, 1, false);
        var finalized = DailyReportingKafkaIT.fact(tenant, employee, day, 2, true);
        consumer.apply(record(pending, 0));
        consumer.apply(record(finalized, 1));
        consumer.apply(record(finalized, 1));
        consumer.apply(record(DailyReportingKafkaIT.fact(tenant, employee, day, 1, false), 2));
        assertThat(jdbc.queryForObject("SELECT revision FROM reporting.daily_fact_projection WHERE tenant_id=?", Long.class, tenant)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT payload->>'status' FROM reporting.daily_fact_projection WHERE tenant_id=?", String.class, tenant)).isEqualTo("ABSENT");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reporting.reporting_inbox WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT last_offset FROM reporting.checkpoint_progress WHERE tenant_id=?", Long.class, tenant)).isEqualTo(2);
    }

    @Test void invalidRoutingIdentityChangeAndRollbackCannotPublishAReceiptOrEffect() {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        var fact = DailyReportingKafkaIT.fact(tenant, employee, UUID.randomUUID(), 1, true);
        assertThatThrownBy(() -> consumer.apply(new ConsumerRecord<>("attendance.daily-status.v1", 0, 0, "wrong", fact)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> transactions.executeWithoutResult(tx -> {
            consumer.apply(record(fact, 0));
            throw new IllegalStateException("rollback daily effect");
        })).isInstanceOf(IllegalStateException.class);
        for (String table : java.util.List.of("daily_fact_projection", "reporting_inbox", "checkpoint_progress")) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM reporting." + table + " WHERE tenant_id=?", Integer.class, tenant)).isZero();
        }
        consumer.apply(record(fact, 0));
        assertThatThrownBy(() -> consumer.apply(record(DailyReportingKafkaIT.fact(tenant, employee, UUID.randomUUID(), 2, true), 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reporting.reporting_inbox WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(1);
    }

    @Test void ownerTimezoneChoosesTheCurrentDayWithoutCountingYesterdayAgain() {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        for (String date : java.util.List.of("2026-10-28", "2026-10-29")) {
            var fact = AttendanceDailyFact.newBuilder(DailyReportingKafkaIT.fact(tenant, employee, UUID.randomUUID(), 1, true))
                    .setWorkDate(LocalDate.parse(date)).build();
            consumer.apply(record(fact, 0));
        }
        transactions.executeWithoutResult(tx -> {
            scope.use(tenant);
            assertThat(reports.today(tenant, null, Instant.parse("2026-10-28T18:00:00Z")).absent()).isEqualTo(1);
            assertThat(reports.today(UUID.randomUUID(), null, Instant.parse("2026-10-28T18:00:00Z")).absent()).isZero();
        });
    }

    private ConsumerRecord<String, AttendanceDailyFact> record(AttendanceDailyFact fact, long offset) {
        return new ConsumerRecord<>("attendance.daily-status.v1", 0, offset, fact.getTenantId() + ":" + fact.getEmployeeId(), fact);
    }
}
