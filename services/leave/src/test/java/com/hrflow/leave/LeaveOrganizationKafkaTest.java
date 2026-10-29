package com.hrflow.leave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"hrflow.leave.employee-consumer.enabled=true", "hrflow.leave.calendar-consumer.enabled=true", "spring.kafka.listener.auto-startup=true", "hrflow.leave.attendance-consumer.enabled=true", "hrflow.leave.relay.enabled=true", "hrflow.leave.relay.poll-ms=50"})
@AutoConfigureMockMvc
class LeaveOrganizationKafkaTest extends LeaveKafkaEnvironment {
    @Autowired MockMvc mvc;
    @Autowired KafkaTemplate<Object, Object> kafka;

    @Test void actualOwnerShiftAndHolidayReplayDrivePreviewThenLiveHolidayRemovalRecomputesCharge() throws Exception {
        var admin = new org.springframework.jdbc.core.JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        admin.update("INSERT INTO leave.leave_type(id,tenant_id,code,name,days_allowed) VALUES (?,?,'ANNUAL','Annual',20)", UUID.randomUUID().toString(), TEAM_TENANT.toString());
        admin.update("""
                INSERT INTO leave.leave_balance(id,tenant_id,employee_id,leave_type_code,year,allocated,used,pending,available)
                VALUES (?,?,?,'ANNUAL',2026,20,0,0,20)
                """, UUID.randomUUID().toString(), TEAM_TENANT.toString(), TEAM_EMPLOYEE.toString());
        assertThat(previewDays()).isEqualTo(1.0);
        kafka.send("org.holiday.v1", TEAM_TENANT + ":" + TEAM_HOLIDAY, holiday(2, true)).get();
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(previewDays()).isEqualTo(2.0));
        kafka.send("org.holiday.v1", TEAM_TENANT + ":" + TEAM_HOLIDAY, holiday(1, false)).get();
        await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(previewDays()).isEqualTo(2.0));
    }

    @Autowired com.hrflow.leave.service.LeaveSubmissionService submissions;
    @Autowired com.hrflow.leave.service.LeaveDecisions decisions;

    @Test void explicitAttendanceConflictCommitsOwnerReviewAndPublishesItWithoutCompensatingTheDebit() throws Exception {
        var admin = new org.springframework.jdbc.core.JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        admin.update("INSERT INTO leave.leave_type(id,tenant_id,code,name,days_allowed,paid) VALUES (?,?,'UNPAID','Unpaid',0,false)", UUID.randomUUID().toString(), TEAM_TENANT.toString());
        var day = java.time.LocalDate.parse("2026-11-02");
        var pending = submissions.submitLeaveRequest(TEAM_TENANT.toString(), TEAM_EMPLOYEE.toString(), "Private employee",
                new com.hrflow.leave.dto.CreateLeaveRequestDto("UNPAID", day, day, "FULL_DAY", "Private reason", "Private title"));
        var approved = decisions.approveLeaveRequest(TEAM_TENANT.toString(), UUID.randomUUID().toString(), java.util.List.of("HR_ADMIN"),
                pending.id(), Long.toString(pending.version()), null);
        var fact = com.hrflow.contracts.attendance.AttendanceDailyFact.newBuilder()
                .setEventId(UUID.randomUUID().toString()).setType("AttendanceDailySnapshot.v1").setSchemaVersion(1)
                .setTenantId(TEAM_TENANT.toString()).setAggregateId(UUID.randomUUID().toString()).setAggregateVersion(1L)
                .setOccurredAt(java.time.Instant.now()).setCorrelationId(UUID.randomUUID().toString()).setEmployeeId(TEAM_EMPLOYEE.toString())
                .setWorkDate(day).setTimezone("Asia/Phnom_Penh").setStatus("REVIEW_REQUIRED").setDayType("ON_LEAVE")
                .setClockIn(java.time.Instant.parse("2026-11-02T03:00:00Z")).setClockOut(java.time.Instant.parse("2026-11-02T04:00:00Z"))
                .setLate(false).setEarlyDeparture(false).setReviewRequired(true).setReviewReason("Attendance recorded during approved leave")
                .setFinalized(true).setOpenSession(false).setMinutesWorked(60).setMinutesLate(0).setShiftId(TEAM_SHIFT.toString()).setShiftRevision(1L)
                .setLeaveReferences(java.util.List.of(com.hrflow.contracts.attendance.DailyLeaveReference.newBuilder().setLeaveId(pending.id())
                        .setPortion("FULL_DAY").setRevision(approved.version() + 1).setWorkRecorded(true).build()))
                .setSessionReferences(java.util.List.of(new com.hrflow.contracts.attendance.DailySessionReference(UUID.randomUUID().toString(), 1L))).build();
        kafka.send("attendance.daily-status.v1", TEAM_TENANT + ":" + TEAM_EMPLOYEE, fact).get();
        long end = kafka.send("attendance.daily-status.v1", TEAM_TENANT + ":" + TEAM_EMPLOYEE, fact).get().getRecordMetadata().offset() + 1;
        try (var broker = org.apache.kafka.clients.admin.AdminClient.create(java.util.Map.of("bootstrap.servers", KAFKA.getBootstrapServers()))) {
            await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
                var offsets = broker.listConsumerGroupOffsets("hrflow-leave-attendance-v1").partitionsToOffsetAndMetadata().get();
                assertThat(offsets.get(new org.apache.kafka.common.TopicPartition("attendance.daily-status.v1", 0)).offset()).isGreaterThanOrEqualTo(end);
                assertThat(admin.queryForObject("SELECT status FROM leave.leave_request WHERE id=?", String.class, pending.id())).isEqualTo("NEEDS_REVIEW");
                assertThat(admin.queryForObject("SELECT count(*) FROM leave.leave_outbox WHERE aggregate_id=? AND payload->>'state'='NEEDS_REVIEW' AND status='PUBLISHED'", Integer.class, pending.id())).isEqualTo(1);
            });
        }
        assertThat(admin.queryForList("SELECT entry_type FROM leave.leave_ledger WHERE reference_id=?", String.class, pending.id()))
                .containsExactlyInAnyOrder("UNPAID_RESERVATION", "UNPAID_USED");
        assertThat(admin.queryForObject("SELECT count(*) FROM leave.leave_day WHERE request_id=?", Integer.class, pending.id())).isEqualTo(1);
    }

    private double previewDays() throws Exception {
        var response = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/leave/requests/preview")
                .with(LeaveScopeTest.caller(TEAM_TENANT, TEAM_EMPLOYEE, TEAM_EMPLOYEE, "EMPLOYEE"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"type\":\"ANNUAL\",\"startDate\":\"2026-11-08\",\"endDate\":\"2026-11-09\",\"portion\":\"FULL_DAY\"}"))
                .andExpect(status().isOk()).andReturn().getResponse();
        return tools.jackson.databind.json.JsonMapper.builder().build().readTree(response.getContentAsString()).path("chargeableDays").asDouble();
    }

    @Test void startupReplaysOwnerManagerAndLiveTerminationRevokesScope() throws Exception {
        var caller = LeaveScopeTest.caller(TEAM_TENANT, UUID.randomUUID(), TEAM_MANAGER, "MANAGER");
        mvc.perform(get("/leave/requests/pending").with(caller)).andExpect(status().isOk());
        var terminated = LeaveReferenceFixtures.fact(TEAM_TENANT, TEAM_MANAGER, null, 2);
        ((com.hrflow.contracts.organization.EmployeeSnapshot) terminated.getPayload()).setStatus("TERMINATED");
        kafka.send("org.employee.v1", TEAM_TENANT + ":" + TEAM_MANAGER, terminated).get();
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(mvc.perform(get("/leave/requests/pending").with(caller)).andReturn().getResponse().getStatus()).isEqualTo(403));
        kafka.send("org.employee.v1", TEAM_TENANT + ":" + TEAM_MANAGER,
                LeaveReferenceFixtures.fact(TEAM_TENANT, TEAM_MANAGER, null, 1)).get();
        await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(mvc.perform(get("/leave/requests/pending").with(caller)).andReturn().getResponse().getStatus()).isEqualTo(403));
    }
}
