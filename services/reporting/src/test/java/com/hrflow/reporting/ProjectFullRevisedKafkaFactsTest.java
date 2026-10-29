package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.hrflow.reporting.consumer.ReportingProjectionConsumer;
import com.hrflow.reporting.dto.AttendanceFactEvent;
import com.hrflow.reporting.dto.LeaveFactEvent;
import com.hrflow.reporting.dto.RequestFactEvent;
import com.hrflow.reporting.entity.AttendanceFactProjection;
import com.hrflow.reporting.entity.LeaveFactProjection;
import com.hrflow.reporting.entity.RequestFactProjection;
import com.hrflow.reporting.repository.AttendanceFactProjectionRepository;
import com.hrflow.reporting.repository.LeaveFactProjectionRepository;
import com.hrflow.reporting.repository.RequestFactProjectionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class ProjectFullRevisedKafkaFactsTest {

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
    ReportingProjectionConsumer projectionConsumer;

    @Autowired
    AttendanceFactProjectionRepository attendanceRepo;

    @Autowired
    LeaveFactProjectionRepository leaveRepo;

    @Autowired
    RequestFactProjectionRepository requestRepo;

    @Test
    @DisplayName("W09-S01 to S03: Revision N+1 replaces every mutable snapshot field; revision N and older change nothing")
    void higherRevisionsUpdateAndOlderRevisionsIgnored() {
        UUID tenantId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID employeeId1 = UUID.randomUUID();
        UUID employeeId2 = UUID.randomUUID();
        UUID staleEmployeeId = UUID.randomUUID();
        LocalDate workDate1 = LocalDate.of(2026, 10, 20);
        LocalDate workDate2 = LocalDate.of(2026, 10, 21);
        LocalDate staleWorkDate = LocalDate.of(2026, 10, 22);
        UUID eventId1 = UUID.randomUUID();
        UUID eventId2 = UUID.randomUUID();
        UUID eventId3 = UUID.randomUUID();
        UUID eventId4 = UUID.randomUUID();

        // 1. Initial snapshot revision 1 (PRESENT, on time)
        AttendanceFactEvent eventRev1 = new AttendanceFactEvent(
                eventId1, tenantId, sessionId, employeeId1,
                workDate1, "PRESENT", 0, 480, 1L);
        projectionConsumer.processAttendanceFact(eventRev1);

        AttendanceFactProjection p1 = attendanceRepo.findByTenantIdAndSessionId(tenantId, sessionId).orElseThrow();
        assertThat(p1.getAggregateRevision()).isEqualTo(1L);
        assertThat(p1.getEmployeeId()).isEqualTo(employeeId1);
        assertThat(p1.getWorkDate()).isEqualTo(workDate1);
        assertThat(p1.getStatus()).isEqualTo("PRESENT");
        assertThat(p1.getMinutesLate()).isEqualTo(0);
        assertThat(p1.getMinutesWorked()).isEqualTo(480);

        // 2. Updated snapshot revision 2 (N+1 replaces EVERY mutable accepted snapshot field: employee, date, status, minutes)
        AttendanceFactEvent eventRev2 = new AttendanceFactEvent(
                eventId2, tenantId, sessionId, employeeId2,
                workDate2, "LATE", 25, 455, 2L);
        projectionConsumer.processAttendanceFact(eventRev2);

        AttendanceFactProjection p2 = attendanceRepo.findByTenantIdAndSessionId(tenantId, sessionId).orElseThrow();
        assertThat(p2.getAggregateRevision()).isEqualTo(2L);
        assertThat(p2.getEmployeeId()).isEqualTo(employeeId2);
        assertThat(p2.getWorkDate()).isEqualTo(workDate2);
        assertThat(p2.getStatus()).isEqualTo("LATE");
        assertThat(p2.getMinutesLate()).isEqualTo(25);
        assertThat(p2.getMinutesWorked()).isEqualTo(455);

        // 3. Replay with revision N (revision 2) with different fields -> must change nothing
        AttendanceFactEvent eventRev2Replay = new AttendanceFactEvent(
                eventId3, tenantId, sessionId, staleEmployeeId,
                staleWorkDate, "ABSENT", 60, 0, 2L);
        projectionConsumer.processAttendanceFact(eventRev2Replay);

        AttendanceFactProjection p2Unchanged = attendanceRepo.findByTenantIdAndSessionId(tenantId, sessionId).orElseThrow();
        assertThat(p2Unchanged.getAggregateRevision()).isEqualTo(2L);
        assertThat(p2Unchanged.getEmployeeId()).isEqualTo(employeeId2);
        assertThat(p2Unchanged.getWorkDate()).isEqualTo(workDate2);
        assertThat(p2Unchanged.getStatus()).isEqualTo("LATE");
        assertThat(p2Unchanged.getMinutesLate()).isEqualTo(25);
        assertThat(p2Unchanged.getMinutesWorked()).isEqualTo(455);

        // 4. Replayed older event with revision 1 -> must change nothing
        AttendanceFactEvent staleEvent = new AttendanceFactEvent(
                eventId4, tenantId, sessionId, staleEmployeeId,
                staleWorkDate, "PRESENT", 0, 480, 1L);
        projectionConsumer.processAttendanceFact(staleEvent);

        AttendanceFactProjection p3 = attendanceRepo.findByTenantIdAndSessionId(tenantId, sessionId).orElseThrow();
        assertThat(p3.getAggregateRevision()).isEqualTo(2L);
        assertThat(p3.getEmployeeId()).isEqualTo(employeeId2);
        assertThat(p3.getWorkDate()).isEqualTo(workDate2);
        assertThat(p3.getStatus()).isEqualTo("LATE");
        assertThat(p3.getMinutesLate()).isEqualTo(25);
        assertThat(p3.getMinutesWorked()).isEqualTo(455);
    }

    @Test
    @DisplayName("W09-S01 to S03: Leave and Request facts project aggregate revisions idempotently")
    void leaveAndRequestFactsProjectIdempotently() {
        UUID tenantId = UUID.randomUUID();
        UUID leaveId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        // Leave fact projection: Prove revision N+1 replaces every mutable snapshot field; revision N and older change nothing
        UUID employeeId1 = UUID.randomUUID();
        UUID employeeId2 = UUID.randomUUID();
        UUID staleEmployeeId = UUID.randomUUID();
        LocalDate startDate1 = LocalDate.of(2026, 11, 1);
        LocalDate endDate1 = LocalDate.of(2026, 11, 3);
        LocalDate startDate2 = LocalDate.of(2026, 11, 4);
        LocalDate endDate2 = LocalDate.of(2026, 11, 5);

        // 1. Initial snapshot revision 1 (PENDING, ANNUAL, 3.00 days)
        LeaveFactEvent leaveRev1 = new LeaveFactEvent(
                UUID.randomUUID(), tenantId, leaveId, employeeId1, "ANNUAL",
                startDate1, endDate1, new BigDecimal("3.00"), "PENDING", 1L);
        projectionConsumer.processLeaveFact(leaveRev1);

        LeaveFactProjection lp1 = leaveRepo.findByTenantIdAndLeaveId(tenantId, leaveId).orElseThrow();
        assertThat(lp1.getAggregateRevision()).isEqualTo(1L);
        assertThat(lp1.getEmployeeId()).isEqualTo(employeeId1);
        assertThat(lp1.getLeaveType()).isEqualTo("ANNUAL");
        assertThat(lp1.getStartDate()).isEqualTo(startDate1);
        assertThat(lp1.getEndDate()).isEqualTo(endDate1);
        assertThat(lp1.getPortionDays()).isEqualByComparingTo("3.00");
        assertThat(lp1.getStatus()).isEqualTo("PENDING");

        // 2. Updated snapshot revision 2 (N+1 replaces employee, leaveType, dates, portion, status, revision)
        LeaveFactEvent leaveRev2 = new LeaveFactEvent(
                UUID.randomUUID(), tenantId, leaveId, employeeId2, "SICK",
                startDate2, endDate2, new BigDecimal("2.00"), "APPROVED", 2L);
        projectionConsumer.processLeaveFact(leaveRev2);

        LeaveFactProjection lp2 = leaveRepo.findByTenantIdAndLeaveId(tenantId, leaveId).orElseThrow();
        assertThat(lp2.getAggregateRevision()).isEqualTo(2L);
        assertThat(lp2.getEmployeeId()).isEqualTo(employeeId2);
        assertThat(lp2.getLeaveType()).isEqualTo("SICK");
        assertThat(lp2.getStartDate()).isEqualTo(startDate2);
        assertThat(lp2.getEndDate()).isEqualTo(endDate2);
        assertThat(lp2.getPortionDays()).isEqualByComparingTo("2.00");
        assertThat(lp2.getStatus()).isEqualTo("APPROVED");

        // 3. Replay with revision N (revision 2) with different fields -> changes nothing
        LeaveFactEvent leaveRev2Equal = new LeaveFactEvent(
                UUID.randomUUID(), tenantId, leaveId, staleEmployeeId, "MATERNITY",
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 10), new BigDecimal("10.00"), "REJECTED", 2L);
        projectionConsumer.processLeaveFact(leaveRev2Equal);

        LeaveFactProjection lpEqual = leaveRepo.findByTenantIdAndLeaveId(tenantId, leaveId).orElseThrow();
        assertThat(lpEqual.getAggregateRevision()).isEqualTo(2L);
        assertThat(lpEqual.getEmployeeId()).isEqualTo(employeeId2);
        assertThat(lpEqual.getLeaveType()).isEqualTo("SICK");
        assertThat(lpEqual.getStartDate()).isEqualTo(startDate2);
        assertThat(lpEqual.getEndDate()).isEqualTo(endDate2);
        assertThat(lpEqual.getPortionDays()).isEqualByComparingTo("2.00");
        assertThat(lpEqual.getStatus()).isEqualTo("APPROVED");

        // 4. Replay with older revision 1 -> changes nothing
        LeaveFactEvent leaveRev1Older = new LeaveFactEvent(
                UUID.randomUUID(), tenantId, leaveId, staleEmployeeId, "UNPAID",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), new BigDecimal("5.00"), "CANCELLED", 1L);
        projectionConsumer.processLeaveFact(leaveRev1Older);

        LeaveFactProjection lpOlder = leaveRepo.findByTenantIdAndLeaveId(tenantId, leaveId).orElseThrow();
        assertThat(lpOlder.getAggregateRevision()).isEqualTo(2L);
        assertThat(lpOlder.getEmployeeId()).isEqualTo(employeeId2);
        assertThat(lpOlder.getLeaveType()).isEqualTo("SICK");
        assertThat(lpOlder.getStartDate()).isEqualTo(startDate2);
        assertThat(lpOlder.getEndDate()).isEqualTo(endDate2);
        assertThat(lpOlder.getPortionDays()).isEqualByComparingTo("2.00");
        assertThat(lpOlder.getStatus()).isEqualTo("APPROVED");

        // Request fact projection: Prove revision N+1 replaces requester, requestType, status, revision; equal/older change nothing
        UUID requesterId1 = UUID.randomUUID();
        UUID requesterId2 = UUID.randomUUID();
        UUID staleRequesterId = UUID.randomUUID();

        // 1. Initial snapshot revision 1 (EQUIPMENT, SUBMITTED)
        RequestFactEvent reqRev1 = new RequestFactEvent(
                UUID.randomUUID(), tenantId, requestId, requesterId1, "EQUIPMENT", "SUBMITTED", 1L);
        projectionConsumer.processRequestFact(reqRev1);

        RequestFactProjection rp1 = requestRepo.findByTenantIdAndRequestId(tenantId, requestId).orElseThrow();
        assertThat(rp1.getAggregateRevision()).isEqualTo(1L);
        assertThat(rp1.getRequesterId()).isEqualTo(requesterId1);
        assertThat(rp1.getRequestType()).isEqualTo("EQUIPMENT");
        assertThat(rp1.getStatus()).isEqualTo("SUBMITTED");

        // 2. Updated snapshot revision 2 (N+1 replaces requesterId, requestType, status, revision)
        RequestFactEvent reqRev2 = new RequestFactEvent(
                UUID.randomUUID(), tenantId, requestId, requesterId2, "TRAINING", "APPROVED", 2L);
        projectionConsumer.processRequestFact(reqRev2);

        RequestFactProjection rp2 = requestRepo.findByTenantIdAndRequestId(tenantId, requestId).orElseThrow();
        assertThat(rp2.getAggregateRevision()).isEqualTo(2L);
        assertThat(rp2.getRequesterId()).isEqualTo(requesterId2);
        assertThat(rp2.getRequestType()).isEqualTo("TRAINING");
        assertThat(rp2.getStatus()).isEqualTo("APPROVED");

        // 3. Replay with revision N (revision 2) with different fields -> changes nothing
        RequestFactEvent reqRev2Equal = new RequestFactEvent(
                UUID.randomUUID(), tenantId, requestId, staleRequesterId, "HARDWARE", "REJECTED", 2L);
        projectionConsumer.processRequestFact(reqRev2Equal);

        RequestFactProjection rpEqual = requestRepo.findByTenantIdAndRequestId(tenantId, requestId).orElseThrow();
        assertThat(rpEqual.getAggregateRevision()).isEqualTo(2L);
        assertThat(rpEqual.getRequesterId()).isEqualTo(requesterId2);
        assertThat(rpEqual.getRequestType()).isEqualTo("TRAINING");
        assertThat(rpEqual.getStatus()).isEqualTo("APPROVED");

        // 4. Replay with older revision 1 -> changes nothing
        RequestFactEvent reqRev1Older = new RequestFactEvent(
                UUID.randomUUID(), tenantId, requestId, staleRequesterId, "SOFTWARE", "CANCELLED", 1L);
        projectionConsumer.processRequestFact(reqRev1Older);

        RequestFactProjection rpOlder = requestRepo.findByTenantIdAndRequestId(tenantId, requestId).orElseThrow();
        assertThat(rpOlder.getAggregateRevision()).isEqualTo(2L);
        assertThat(rpOlder.getRequesterId()).isEqualTo(requesterId2);
        assertThat(rpOlder.getRequestType()).isEqualTo("TRAINING");
        assertThat(rpOlder.getStatus()).isEqualTo("APPROVED");
    }
}
