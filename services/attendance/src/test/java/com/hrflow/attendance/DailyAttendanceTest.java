package com.hrflow.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.hrflow.attendance.entity.AttendanceSession;
import com.hrflow.attendance.leave.LeaveProjection;
import com.hrflow.attendance.repository.AttendanceSessionRepository;
import com.hrflow.attendance.service.AttendanceService;
import com.hrflow.contracts.leave.LeaveRequestFact;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class DailyAttendanceTest {
    static final LocalDate DAY = LocalDate.parse("2026-11-02");
    static final ZoneId ZONE = ZoneId.of("Asia/Phnom_Penh");
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("context-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", POSTGRES::getUsername);
        p.add("spring.datasource.password", POSTGRES::getPassword);
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @Autowired AttendanceService attendance;
    @Autowired com.hrflow.attendance.service.DailyAttendanceService daily;
    @Autowired AttendanceSessionRepository sessions;
    @Autowired LeaveProjection leave;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean Clock clock;

    @Test void allSessionsDetermineFirstInLastOutAndRawWorkedTotal() {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        UUID shift = AttendanceFixtures.schedule(jdbc, tenant, employee);
        session(tenant, employee, shift, "08:00", "12:00");
        session(tenant, employee, shift, "13:00", "17:00");
        when(clock.instant()).thenReturn(at("17:30"));
        var day = attendance.dailyStatusMe(tenant, employee, DAY);
        assertThat(day.clockIn()).isEqualTo(at("08:00"));
        assertThat(day.clockOut()).isEqualTo(at("17:00"));
        assertThat(day.late()).isFalse();
        assertThat(day.minutesWorked()).isEqualTo(480);
        assertThat(day.finalized()).isTrue();
    }

    @Test void todayUsesAssignedTimezoneAndExcludesHistoricalAndFutureSessions() {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        UUID shift = AttendanceFixtures.schedule(jdbc, tenant, employee);
        session(tenant, employee, shift, "08:00", "17:00");
        var previous = new AttendanceSession(tenant, employee, at("08:00").minusSeconds(24 * 3600L), null);
        previous.captureSchedule(DAY.minusDays(1), ZONE.getId(), shift, 1, at("08:00").minusSeconds(24 * 3600L),
                at("17:00").minusSeconds(24 * 3600L), 10, 127);
        previous.close(at("17:00").minusSeconds(24 * 3600L), null);
        sessions.saveAndFlush(previous);
        when(clock.instant()).thenReturn(at("08:01"));
        var today = attendance.today(tenant, employee);
        assertThat(today.date()).isEqualTo(DAY.toString());
        assertThat(today.sessions()).hasSize(1);
        assertThat(today.sessions().getFirst().clockIn()).isEqualTo(at("08:00"));
        when(clock.instant()).thenReturn(at("00:01")); // The UTC date is still yesterday.
        assertThat(attendance.today(tenant, employee).date()).isEqualTo(DAY.toString());
    }

    @Test void noSessionIsPendingUntilTheAcceptedCutoffThenAbsentAtTheBoundary() {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        AttendanceFixtures.schedule(jdbc, tenant, employee);
        when(clock.instant()).thenReturn(at("17:29").plusSeconds(59));
        assertThat(attendance.dailyStatusMe(tenant, employee, DAY).status()).isEqualTo("PENDING");
        when(clock.instant()).thenReturn(at("17:30"));
        assertThat(attendance.dailyStatusMe(tenant, employee, DAY).status()).isEqualTo("ABSENT");
    }

    @Test void morningLeaveShortensExpectedWindowWithoutFlaggingWorkInTheUncoveredHalf() {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        UUID shift = AttendanceFixtures.schedule(jdbc, tenant, employee);
        var morning = LeaveRequestFact.newBuilder(LeaveOverlayKafkaTest.fact(tenant, employee, UUID.randomUUID(), DAY, 1, "APPROVED"))
                .setPortion("MORNING").setChargedDays("0.50").build();
        leave.apply(new ConsumerRecord<>("leave.request.v1", 0, 0, tenant + ":" + employee, morning));
        session(tenant, employee, shift, "12:30", "17:00");
        when(clock.instant()).thenReturn(at("17:30"));
        var day = attendance.dailyStatusMe(tenant, employee, DAY);
        assertThat(day.reviewRequired()).isFalse();
        assertThat(day.late()).isFalse();
        assertThat(day.earlyDeparture()).isFalse();
    }

    @Test void complementaryLeaveFactsFlagOnlyThePortionWithRecordedWorkUsingTheCapturedShift() {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID(), morningId = UUID.randomUUID(), afternoonId = UUID.randomUUID();
        UUID shift = AttendanceFixtures.schedule(jdbc, tenant, employee);
        for (var entry : java.util.Map.of(morningId, "MORNING", afternoonId, "AFTERNOON").entrySet()) {
            var fact = LeaveRequestFact.newBuilder(LeaveOverlayKafkaTest.fact(tenant, employee, entry.getKey(), DAY, 1, "APPROVED"))
                    .setPortion(entry.getValue()).setChargedDays("0.50").build();
            leave.apply(new ConsumerRecord<>("leave.request.v1", 0, 0, tenant + ":" + employee, fact));
        }
        session(tenant, employee, shift, "08:00", "12:30");
        jdbc.update("UPDATE attendance.shift_projection SET start_time='14:00',end_time='22:00',revision=2 WHERE tenant_id=?", tenant);
        when(clock.instant()).thenReturn(at("17:30"));
        var snapshot = daily.snapshot(tenant, employee, DAY);
        assertThat(snapshot.leaveReferences().stream().filter(ref -> ref.leaveId().equals(morningId)).findFirst().orElseThrow().workRecorded()).isTrue();
        assertThat(snapshot.leaveReferences().stream().filter(ref -> ref.leaveId().equals(afternoonId)).findFirst().orElseThrow().workRecorded()).isFalse();
    }

    @Test void assignedNonWorkingDayIsNeverAbsent() {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        UUID shift = AttendanceFixtures.schedule(jdbc, tenant, employee);
        jdbc.update("UPDATE attendance.shift_projection SET working_days=64 WHERE tenant_id=? AND shift_id=?", tenant, shift);
        when(clock.instant()).thenReturn(at("17:30"));
        assertThat(attendance.dailyStatusMe(tenant, employee, DAY).status()).isEqualTo("DAY_OFF");
    }

    @Test void capturedCalendarSurvivesShiftEditsAndUnknownLegacyBasisFailsExplicitly() {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        UUID shift = AttendanceFixtures.schedule(jdbc, tenant, employee);
        session(tenant, employee, shift, "08:00", "17:00");
        when(clock.instant()).thenReturn(at("17:30"));
        jdbc.update("UPDATE attendance.shift_projection SET start_time='11:00',working_days=64 WHERE tenant_id=? AND shift_id=?", tenant, shift);
        var original = attendance.dailyStatusMe(tenant, employee, DAY);
        assertThat(original.dayType()).isEqualTo("WORKING_DAY");
        assertThat(original.shiftStart()).isEqualTo(java.time.LocalTime.of(8, 0));
        jdbc.update("UPDATE attendance.attendance_session SET working_days=null WHERE tenant_id=?", tenant);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> attendance.dailyStatusMe(tenant, employee, DAY))
                .isInstanceOfSatisfying(org.springframework.web.server.ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode().value()).isEqualTo(503));
    }

    private void session(UUID tenant, UUID employee, UUID shift, String in, String out) {
        var session = new AttendanceSession(tenant, employee, at(in), null);
        session.captureSchedule(DAY, ZONE.getId(), shift, 1, at("08:00"), at("17:00"), 10, 127);
        session.close(at(out), null);
        sessions.saveAndFlush(session);
    }
    static Instant at(String time) { return DAY.atTime(java.time.LocalTime.parse(time)).atZone(ZONE).toInstant(); }
}
