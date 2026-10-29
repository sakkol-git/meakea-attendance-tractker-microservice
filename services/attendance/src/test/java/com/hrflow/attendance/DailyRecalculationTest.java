package com.hrflow.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.hrflow.attendance.dto.CheckInRequest;
import com.hrflow.attendance.dto.CorrectionRequest;
import com.hrflow.attendance.leave.LeaveProjection;
import com.hrflow.attendance.service.AttendanceCorrectionService;
import com.hrflow.attendance.service.AttendanceService;
import com.hrflow.attendance.service.CorrectionAccess;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = {"hrflow.attendance.daily-status.enabled=true", "hrflow.attendance.daily-worker.enabled=true",
        "hrflow.attendance.daily-worker.poll-ms=50", "hrflow.attendance.holiday-fanout.enabled=true",
        "hrflow.attendance.holiday-fanout.poll-ms=50"})
@Testcontainers
class DailyRecalculationTest {
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
    @Autowired AttendanceService attendance;
    @Autowired AttendanceCorrectionService corrections;
    @Autowired LeaveProjection leave;
    @Autowired com.hrflow.attendance.organization.OrganizationProjectionService organization;
    @Autowired com.hrflow.attendance.daily.DailyStatusWriter daily;
    @Autowired org.springframework.transaction.support.TransactionTemplate transactions;

    @Test void committedHolidayAddMoveAndRemovalRecalculateBothObservedDatesWithoutChangingEvidence() {
        var jdbc = admin();
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID(), holiday = UUID.randomUUID();
        AttendanceFixtures.schedule(jdbc, tenant, employee);
        attendance.checkIn(tenant, employee, employee, UUID.randomUUID().toString(), new CheckInRequest(null, null, null, null));
        var today = LocalDate.now(ZoneId.of("Asia/Phnom_Penh"));
        transactions.executeWithoutResult(tx -> daily.refresh(tenant, employee, today.plusDays(1)));
        holiday(tenant, holiday, today, 1, false);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(dayType(jdbc, tenant, today)).isEqualTo("HOLIDAY"));
        holiday(tenant, holiday, today.plusDays(1), 2, false);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(dayType(jdbc, tenant, today)).isEqualTo("WORKING_DAY");
            assertThat(dayType(jdbc, tenant, today.plusDays(1))).isEqualTo("HOLIDAY");
        });
        holiday(tenant, holiday, today.plusDays(1), 3, true);
        holiday(tenant, holiday, today, 1, false);
        await().during(Duration.ofMillis(200)).atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(dayType(jdbc, tenant, today.plusDays(1))).isEqualTo("WORKING_DAY"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM attendance.attendance_evidence WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(1);
    }

    private void holiday(UUID tenant, UUID id, LocalDate date, long revision, boolean removed) {
        var fact = new com.hrflow.contracts.organization.OrganizationFact(UUID.randomUUID().toString(),
                removed ? "org.holiday.removed.v1" : "org.holiday.updated.v1", 1, tenant.toString(), id.toString(), revision,
                java.time.Instant.now().toString(), UUID.randomUUID().toString(), null,
                new com.hrflow.contracts.organization.HolidaySnapshot(id.toString(), date.toString(), "Synthetic", "COMPANY",
                        removed ? java.time.Instant.now().toString() : null));
        organization.apply("org.holiday.v1", tenant + ":" + id, fact);
    }

    private String dayType(JdbcTemplate jdbc, UUID tenant, LocalDate date) {
        return jdbc.queryForObject("SELECT payload->'overview'->>'dayType' FROM attendance.daily_status WHERE tenant_id=? AND work_date=?", String.class, tenant, date);
    }

    @Test void committedLeaveChangesRefreshWorkReviewThroughThePrivateWorkerAndNeverDeleteEvidence() {
        var jdbc = admin();
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID(), leaveId = UUID.randomUUID();
        AttendanceFixtures.schedule(jdbc, tenant, employee);
        attendance.checkIn(tenant, employee, employee, UUID.randomUUID().toString(), new CheckInRequest(null, null, null, null));
        var date = LocalDate.now(ZoneId.of("Asia/Phnom_Penh"));
        var approved = LeaveOverlayKafkaTest.fact(tenant, employee, leaveId, date, 1, "APPROVED");
        leave.apply(new ConsumerRecord<>("leave.request.v1", 0, 0, tenant + ":" + employee, approved));
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(review(jdbc, tenant)).isEqualTo("true"));
        var cancelled = LeaveOverlayKafkaTest.fact(tenant, employee, leaveId, date, 2, "CANCELLED");
        leave.apply(new ConsumerRecord<>("leave.request.v1", 0, 1, tenant + ":" + employee, cancelled));
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(review(jdbc, tenant)).isEqualTo("false"));
        assertThat(jdbc.queryForObject("SELECT revision FROM attendance.daily_status WHERE tenant_id=?", Long.class, tenant)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM attendance.attendance_evidence WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(1);
    }

    @Test void approvedDateMoveUpdatesBothOldAndNewDailyFactsInTheSameCommand() {
        var jdbc = admin();
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID(), reviewer = UUID.randomUUID();
        AttendanceFixtures.schedule(jdbc, tenant, employee);
        var opened = attendance.checkIn(tenant, employee, employee, UUID.randomUUID().toString(), new CheckInRequest(null, null, null, null));
        var moved = opened.clockIn().plusSeconds(24 * 3600L);
        var date = moved.atZone(ZoneId.of("Asia/Phnom_Penh")).toLocalDate();
        var request = corrections.requestCorrection(tenant, employee, employee, UUID.randomUUID().toString(),
                new CorrectionRequest(opened.id(), date, moved, null, "Correct date"));
        corrections.approveCorrection(tenant, new CorrectionAccess.Caller(reviewer, reviewer, true, false), request.id(), "\"0\"");
        assertThat(jdbc.queryForObject("""
                SELECT payload->'overview'->>'clockIn' FROM attendance.daily_status WHERE tenant_id=? AND work_date=?
                """, String.class, tenant, date.minusDays(1))).isNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM attendance.daily_status WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM attendance.attendance_evidence WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(1);
    }

    private JdbcTemplate admin() {
        return new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
    }
    private String review(JdbcTemplate jdbc, UUID tenant) {
        return jdbc.queryForObject("SELECT payload->'overview'->>'reviewRequired' FROM attendance.daily_status WHERE tenant_id=?", String.class, tenant);
    }
}
