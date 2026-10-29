package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.reporting.consumer.SessionProjectionConsumer;
import com.hrflow.reporting.service.CsvExportService;
import com.hrflow.reporting.service.ExportQueue;
import java.nio.file.Files;
import java.time.Instant;
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
class AutomaticExportTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("context-schema.sql");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", POSTGRES::getUsername);
        p.add("spring.datasource.password", POSTGRES::getPassword);
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
        p.add("hrflow.reporting.export.row-cap", () -> 1);
    }
    @Autowired ExportQueue queue;
    @Autowired CsvExportService exports;
    @Autowired SessionProjectionConsumer consumer;
    @Autowired JdbcTemplate jdbc;

    @Test void queuedSnapshotIsAutomaticallyCompletedWithoutIncludingLaterChangesOrForeignRows() throws Exception {
        var event = SessionProjectionTest.fact(1, Instant.parse("2026-10-28T10:00:00Z"));
        consumer.apply(SessionProjectionTest.record(event, 0));
        UUID tenant = UUID.fromString(event.getTenantId()), requester = UUID.randomUUID();
        var queued = queue.create(tenant, requester, "ATTENDANCE", event.getWorkDate(), event.getWorkDate());
        var later = com.hrflow.contracts.attendance.AttendanceSessionFact.newBuilder(event)
                .setEventId(UUID.randomUUID().toString()).setAggregateVersion(2)
                .setClockOut(Instant.parse("2026-10-28T11:00:00Z")).build();
        consumer.apply(SessionProjectionTest.record(later, 1));
        consumer.apply(SessionProjectionTest.record(SessionProjectionTest.fact(1, null), 2));
        assertThat(queue.processNext(tenant)).isTrue();
        var completed = exports.getExportJob(tenant, queued.getId(), requester).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo("COMPLETED");
        assertThat(completed.getRowCount()).isEqualTo(1);
        assertThat(completed.getDownloadUrl()).isEqualTo("/api/v1/reports/exports/" + queued.getId() + "/download");
        String csv = Files.readString(exports.getStorageRoot().resolve(completed.getFileName()));
        assertThat(csv).contains(event.getEmployeeId(), "2026-10-28T10:00:00Z", ",540,1");
        assertThat(csv).doesNotContain("2026-10-28T11:00:00Z");
        assertThat(queue.processNext(tenant)).isFalse();
        assertThatThrownBy(() -> exports.executeExport(tenant, queued.getId(), java.util.List.of("overwrite"), false, null))
                .isInstanceOf(IllegalStateException.class);
        assertThat(exports.getExportJob(tenant, queued.getId(), requester).orElseThrow().getStatus()).isEqualTo("COMPLETED");
        Files.deleteIfExists(exports.getStorageRoot().resolve(completed.getFileName()));
    }

    @Test void unsupportedSourcesAndRowCapDoNotCreateMisleadingJobs() {
        UUID tenant = UUID.randomUUID();
        assertThatThrownBy(() -> queue.create(tenant, UUID.randomUUID(), "REQUESTS", null, null))
                .isInstanceOf(ResponseStatusException.class);
        var first = SessionProjectionTest.fact(1, null);
        var second = com.hrflow.contracts.attendance.AttendanceSessionFact.newBuilder(first)
                .setEventId(UUID.randomUUID().toString()).setAggregateId(UUID.randomUUID().toString()).build();
        consumer.apply(SessionProjectionTest.record(first, 0));
        consumer.apply(SessionProjectionTest.record(second, 1));
        UUID populated = UUID.fromString(first.getTenantId());
        assertThatThrownBy(() -> queue.create(populated, UUID.randomUUID(), "ATTENDANCE", null, null))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reporting.export_job WHERE tenant_id=?",
                Integer.class, populated)).isZero();
    }

    @Test void anUnknownLegacyScheduleCannotDisappearSilentlyFromAnExport() {
        UUID tenant = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO reporting.attendance_fact_projection(tenant_id,session_id,employee_id,work_date,
                  status,minutes_late,minutes_worked,aggregate_revision)
                VALUES (?,?,?,'2026-10-28','PRESENT',0,480,1)
                """, tenant, UUID.randomUUID(), UUID.randomUUID());
        assertThatThrownBy(() -> queue.create(tenant, UUID.randomUUID(), "ATTENDANCE", null, null))
                .isInstanceOf(ResponseStatusException.class);
    }
}
