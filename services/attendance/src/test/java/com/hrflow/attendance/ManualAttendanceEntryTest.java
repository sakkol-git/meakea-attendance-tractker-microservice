package com.hrflow.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.attendance.dto.CorrectionRequest;
import com.hrflow.attendance.service.AttendanceCorrectionService;
import com.hrflow.attendance.service.CorrectionAccess;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class ManualAttendanceEntryTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("context-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", POSTGRES::getUsername);
        p.add("spring.datasource.password", POSTGRES::getPassword);
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @Autowired AttendanceCorrectionService corrections;
    @Autowired JdbcTemplate jdbc;
    static final Instant IN = Instant.parse("2026-10-28T01:00:00Z");
    static final Instant OUT = Instant.parse("2026-10-28T10:00:00Z");
    static final LocalDate DATE = LocalDate.parse("2026-10-28");

    @Test void approvalCreatesOneAuditableManualSessionAndOneFullIntentWithoutInventedTapEvidence() {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID(), reviewer = UUID.randomUUID();
        AttendanceFixtures.schedule(jdbc, tenant, employee);
        var pending = corrections.requestCorrection(tenant, employee, employee, UUID.randomUUID().toString(),
                new CorrectionRequest(null, DATE, IN, OUT, "Forgot to tap; verified by supervisor"));
        var approved = corrections.approveCorrection(tenant, hr(reviewer), pending.id(), "\"0\"");
        assertThat(approved.sessionId()).isNotNull();
        assertThat(approved.reviewedBy()).isEqualTo(reviewer);
        assertThat(approved.reason()).isEqualTo(pending.reason());
        var session = jdbc.queryForMap("SELECT * FROM attendance.attendance_session WHERE id=?", approved.sessionId());
        assertThat(session.get("effective_source")).isEqualTo("MANUAL");
        assertThat(session.get("status")).isEqualTo("COMPLETED");
        assertThat(session.get("in_evidence_id")).isNull();
        assertThat(session.get("out_evidence_id")).isNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM attendance.session_revision WHERE session_id=?",
                Integer.class, approved.sessionId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM attendance.attendance_outbox WHERE aggregate_id=? AND schema_version=1
                  AND aggregate_revision=1 AND payload->>'source'='MANUAL'
                """, Integer.class, approved.sessionId())).isEqualTo(1);
        assertThatThrownBy(() -> corrections.approveCorrection(tenant, hr(reviewer), pending.id(), "\"1\""))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM attendance.attendance_session WHERE tenant_id=?",
                Integer.class, tenant)).isEqualTo(1);
    }

    @Test void missingTimesWrongWorkDateAndOverlapCannotApproveOrLeavePartialEffects() {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID(), reviewer = UUID.randomUUID();
        AttendanceFixtures.schedule(jdbc, tenant, employee);
        for (CorrectionRequest invalid : java.util.List.of(
                new CorrectionRequest(null, DATE, null, OUT, "Missing entry"),
                new CorrectionRequest(null, DATE.plusDays(1), IN, OUT, "Wrong local work date"))) {
            var request = corrections.requestCorrection(tenant, employee, employee, UUID.randomUUID().toString(), invalid);
            assertThatThrownBy(() -> corrections.approveCorrection(tenant, hr(reviewer), request.id(), "\"0\""))
                    .isInstanceOf(ResponseStatusException.class).satisfies(error ->
                            assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(422));
            assertThat(jdbc.queryForObject("SELECT status FROM attendance.attendance_correction WHERE id=?",
                    String.class, request.id())).isEqualTo("PENDING");
        }
        var first = corrections.requestCorrection(tenant, employee, employee, UUID.randomUUID().toString(),
                new CorrectionRequest(null, DATE, IN, OUT, "Verified work"));
        corrections.approveCorrection(tenant, hr(reviewer), first.id(), "\"0\"");
        var overlap = corrections.requestCorrection(tenant, employee, employee, UUID.randomUUID().toString(),
                new CorrectionRequest(null, DATE, IN.plusSeconds(60), OUT, "Duplicate interval"));
        assertThatThrownBy(() -> corrections.approveCorrection(tenant, hr(reviewer), overlap.id(), "\"0\""))
                .isInstanceOf(ResponseStatusException.class).satisfies(error ->
                        assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(409));
    }
    private static CorrectionAccess.Caller hr(UUID reviewer) {
        return new CorrectionAccess.Caller(reviewer, reviewer, true, false);
    }

    @Test void competingApprovalsForTheSameIntervalProduceOnlyOneManualSession() throws Exception {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID(), reviewer = UUID.randomUUID();
        AttendanceFixtures.schedule(jdbc, tenant, employee);
        var first = corrections.requestCorrection(tenant, employee, employee, UUID.randomUUID().toString(),
                new CorrectionRequest(null, DATE, IN, OUT, "First verified request"));
        var second = corrections.requestCorrection(tenant, employee, employee, UUID.randomUUID().toString(),
                new CorrectionRequest(null, DATE, IN, OUT, "Competing request"));
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var results = java.util.stream.Stream.of(first, second).map(request -> executor.submit(() -> {
                start.await();
                try {
                    corrections.approveCorrection(tenant, hr(reviewer), request.id(), "\"0\"");
                    return 200;
                } catch (ResponseStatusException denied) { return denied.getStatusCode().value(); }
            })).toList();
            start.countDown();
            assertThat(java.util.List.of(results.get(0).get(10, java.util.concurrent.TimeUnit.SECONDS),
                    results.get(1).get(10, java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM attendance.attendance_session WHERE tenant_id=?",
                Integer.class, tenant)).isEqualTo(1);
    }
}
