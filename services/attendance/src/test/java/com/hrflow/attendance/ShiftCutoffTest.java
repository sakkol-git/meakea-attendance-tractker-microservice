package com.hrflow.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = {"hrflow.attendance.daily-status.enabled=true", "hrflow.attendance.cutoff.enabled=true",
        "hrflow.attendance.cutoff.poll-ms=50"})
@Testcontainers
class ShiftCutoffTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("attendance-wire-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", () -> "attendance_runtime");
        p.add("spring.datasource.password", () -> "wire-only");
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
        p.add("hrflow.attendance.relay.job-user", () -> "attendance_job");
        p.add("hrflow.attendance.relay.job-password", () -> "wire-only");
    }
    @MockitoBean Clock clock;
    @Autowired com.hrflow.attendance.daily.ShiftCutoffConsumer consumer;
    @Autowired org.springframework.transaction.support.TransactionTemplate transactions;

    @Test void aLaterAssignedShiftEndRetainsAFutureTriggerAfterTheOriginalCutoffWasAlreadyEmitted() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        when(clock.instant()).thenReturn(DailyAttendanceTest.at("17:30"));
        UUID shift = AttendanceFixtures.schedule(jdbc, tenant, employee);
        await().atMost(Duration.ofSeconds(8)).untilAsserted(() -> assertThat(intents(jdbc, tenant)).isEqualTo(1));
        jdbc.update("UPDATE attendance.shift_projection SET end_time='19:00',revision=2 WHERE tenant_id=? AND shift_id=?", tenant, shift);
        var original = cutoffFact(tenant, employee, shift, 1, "17:00", "17:30");
        consumer.apply(new org.apache.kafka.clients.consumer.ConsumerRecord<>("time.shift-cutoff.v1", 0, 0, tenant + ":" + employee, original));
        assertThat(jdbc.queryForObject("SELECT payload->'overview'->>'finalized' FROM attendance.daily_status WHERE tenant_id=?", String.class, tenant)).isEqualTo("false");
        when(clock.instant()).thenReturn(DailyAttendanceTest.at("19:29"));
        await().during(Duration.ofMillis(150)).atMost(Duration.ofSeconds(2)).untilAsserted(() -> assertThat(intents(jdbc, tenant)).isEqualTo(1));
        when(clock.instant()).thenReturn(DailyAttendanceTest.at("19:30"));
        await().atMost(Duration.ofSeconds(8)).untilAsserted(() -> assertThat(intents(jdbc, tenant)).isEqualTo(2));
        var corrected = cutoffFact(tenant, employee, shift, 2, "19:00", "19:30");
        consumer.apply(new org.apache.kafka.clients.consumer.ConsumerRecord<>("time.shift-cutoff.v1", 0, 1, tenant + ":" + employee, corrected));
        assertThat(jdbc.queryForObject("SELECT payload->'overview'->>'status' FROM attendance.daily_status WHERE tenant_id=?", String.class, tenant)).isEqualTo("ABSENT");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM attendance.attendance_evidence WHERE tenant_id=?", Integer.class, tenant)).isZero();
    }

    private com.hrflow.contracts.attendance.ShiftCutoffFact cutoffFact(UUID tenant, UUID employee, UUID shift, long revision, String end, String at) {
        return com.hrflow.contracts.attendance.ShiftCutoffFact.newBuilder().setEventId(UUID.randomUUID().toString())
                .setType("ShiftCutoff.v1").setSchemaVersion(1).setTenantId(tenant.toString()).setAggregateId(UUID.randomUUID().toString())
                .setAggregateVersion(1).setOccurredAt(DailyAttendanceTest.at(at)).setCorrelationId(UUID.randomUUID().toString())
                .setEmployeeId(employee.toString()).setWorkDate(DailyAttendanceTest.DAY).setTimezone(DailyAttendanceTest.ZONE.getId())
                .setShiftId(shift.toString()).setShiftRevision(revision).setScheduledEnd(DailyAttendanceTest.at(end)).setCutoffAt(DailyAttendanceTest.at(at)).build();
    }

    @Test void scanningSeveralTenantsDoesNotStarveTheirSecondEmployee() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        when(clock.instant()).thenReturn(DailyAttendanceTest.at("17:30"));
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        for (UUID tenant : java.util.List.of(first, second)) {
            AttendanceFixtures.schedule(jdbc, tenant, UUID.randomUUID());
            AttendanceFixtures.schedule(jdbc, tenant, UUID.randomUUID());
        }
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(intents(jdbc, first)).isEqualTo(2);
            assertThat(intents(jdbc, second)).isEqualTo(2);
        });
    }

    @Test void cutoffReceiptAndDailyEffectRollbackTogetherAndDuplicateDeliveryDoesNotAddRevision() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        when(clock.instant()).thenReturn(DailyAttendanceTest.at("17:30"));
        UUID shift = AttendanceFixtures.schedule(jdbc, tenant, employee);
        var fact = com.hrflow.contracts.attendance.ShiftCutoffFact.newBuilder().setEventId(UUID.randomUUID().toString())
                .setType("ShiftCutoff.v1").setSchemaVersion(1).setTenantId(tenant.toString()).setAggregateId(UUID.randomUUID().toString())
                .setAggregateVersion(1).setOccurredAt(DailyAttendanceTest.at("17:30")).setCorrelationId(UUID.randomUUID().toString())
                .setEmployeeId(employee.toString()).setWorkDate(DailyAttendanceTest.DAY).setTimezone(DailyAttendanceTest.ZONE.getId())
                .setShiftId(shift.toString()).setShiftRevision(1).setScheduledEnd(DailyAttendanceTest.at("17:00"))
                .setCutoffAt(DailyAttendanceTest.at("17:30")).build();
        var record = new org.apache.kafka.clients.consumer.ConsumerRecord<>("time.shift-cutoff.v1", 0, 0, tenant + ":" + employee, fact);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> consumer.apply(new org.apache.kafka.clients.consumer.ConsumerRecord<>("time.shift-cutoff.v1", 0, 0, "wrong", fact)))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> transactions.executeWithoutResult(tx -> {
            consumer.apply(record);
            throw new IllegalStateException("rollback cutoff");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM attendance.cutoff_inbox WHERE tenant_id=?", Integer.class, tenant)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM attendance.daily_status WHERE tenant_id=?", Integer.class, tenant)).isZero();
        consumer.apply(record);
        consumer.apply(record);
        assertThat(jdbc.queryForObject("SELECT revision FROM attendance.daily_status WHERE tenant_id=?", Long.class, tenant)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM attendance.cutoff_inbox WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(1);
    }

    @Test void noTapEmployeeGetsExactlyOneDurableCutoffAtBoundaryAndCatchupAfterAMissedDay() throws Exception {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        when(clock.instant()).thenReturn(DailyAttendanceTest.at("17:29").plusSeconds(59));
        AttendanceFixtures.schedule(jdbc, tenant, employee);
        Thread.sleep(300);
        assertThat(intents(jdbc, tenant)).isZero();
        when(clock.instant()).thenReturn(DailyAttendanceTest.at("17:30"));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(intents(jdbc, tenant)).isEqualTo(1));
        await().during(Duration.ofMillis(250)).atMost(Duration.ofSeconds(3)).untilAsserted(() -> assertThat(intents(jdbc, tenant)).isEqualTo(1));
        when(clock.instant()).thenReturn(DailyAttendanceTest.at("17:30").plusSeconds(2 * 24 * 3600L));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(intents(jdbc, tenant)).isEqualTo(3));
        assertThat(jdbc.queryForObject("SELECT count(DISTINCT aggregate_id) FROM attendance.attendance_outbox WHERE tenant_id=? AND topic='time.shift-cutoff.v1'", Integer.class, tenant)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM attendance.attendance_evidence WHERE tenant_id=?", Integer.class, tenant)).isZero();
    }

    private int intents(JdbcTemplate jdbc, UUID tenant) {
        return jdbc.queryForObject("SELECT count(*) FROM attendance.attendance_outbox WHERE tenant_id=? AND topic='time.shift-cutoff.v1'", Integer.class, tenant);
    }
}
