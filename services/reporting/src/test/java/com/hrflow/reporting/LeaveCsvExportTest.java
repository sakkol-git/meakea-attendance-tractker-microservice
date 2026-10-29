package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.contracts.leave.LeaveRequestFact;
import com.hrflow.reporting.consumer.LeaveProjectionConsumer;
import com.hrflow.reporting.service.CsvExportService;
import com.hrflow.reporting.service.ExportQueue;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
class LeaveCsvExportTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("context-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", POSTGRES::getUsername);
        p.add("spring.datasource.password", POSTGRES::getPassword);
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
        p.add("hrflow.reporting.export.row-cap", () -> 1);
    }
    @Autowired LeaveProjectionConsumer consumer;
    @Autowired ExportQueue queue;
    @Autowired CsvExportService exports;
    @Autowired JdbcTemplate jdbc;

    @Test void immutablePrivateLeaveCsvUsesOverlapAndOwnerDaysWithoutChangingReviewDebit() throws Exception {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID(), leave = UUID.randomUUID(), requester = UUID.randomUUID();
        var today = LocalDate.now();
        var review = LeaveRequestFact.newBuilder(LeaveReportingKafkaIT.fact(tenant, employee, leave, 2, "NEEDS_REVIEW"))
                .setStartDate(today.minusDays(1)).setEndDate(today.plusDays(1)).setChargedDays("2.00")
                .setWorkingDates(List.of(today.minusDays(1), today.plusDays(1))).build();
        apply(review);
        apply(LeaveReportingKafkaIT.fact(UUID.randomUUID(), employee, UUID.randomUUID(), 1, "APPROVED"));
        var job = queue.create(tenant, requester, "LEAVE", today, today);
        apply(LeaveRequestFact.newBuilder(review).setEventId(UUID.randomUUID().toString())
                .setAggregateVersion(3).setState("CANCELLED").build());
        assertThat(queue.processNext(tenant)).isTrue();
        var result = exports.getExportJob(tenant, job.getId(), requester).orElseThrow();
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getRowCount()).isEqualTo(1);
        assertThat(exports.getExportJob(tenant, job.getId(), employee)).isEmpty();
        var path = exports.getStorageRoot().resolve(result.getFileName());
        try {
            String csv = Files.readString(path);
            assertThat(csv).contains("chargedDays", employee.toString(), leave.toString(), "NEEDS_REVIEW", "2.00",
                    today.minusDays(1).toString(), today.plusDays(1).toString());
            assertThat(csv).doesNotContain("CANCELLED");
        } finally { Files.deleteIfExists(path); }
    }

    @Test void rowCapAndUnknownLegacyBasisDoNotCreateJobs() {
        UUID tenant = UUID.randomUUID();
        apply(LeaveReportingKafkaIT.fact(tenant, UUID.randomUUID(), UUID.randomUUID(), 1, "APPROVED"));
        apply(LeaveReportingKafkaIT.fact(tenant, UUID.randomUUID(), UUID.randomUUID(), 1, "APPROVED"));
        assertThatThrownBy(() -> queue.create(tenant, UUID.randomUUID(), "LEAVE", null, null))
                .isInstanceOfSatisfying(ResponseStatusException.class, error ->
                        assertThat(error.getStatusCode().value()).isEqualTo(422));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reporting.export_job WHERE tenant_id=?", Integer.class, tenant)).isZero();
        UUID legacy = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO reporting.leave_fact_projection(tenant_id,leave_id,employee_id,leave_type,
                    start_date,end_date,portion_days,status,aggregate_revision)
                VALUES (?,?,?,'ANNUAL',current_date,current_date,1,'APPROVED',1)
                """, legacy, UUID.randomUUID(), UUID.randomUUID());
        assertThatThrownBy(() -> queue.create(legacy, UUID.randomUUID(), "LEAVE", null, null))
                .isInstanceOfSatisfying(ResponseStatusException.class, error ->
                        assertThat(error.getStatusCode().value()).isEqualTo(503));
    }

    private void apply(LeaveRequestFact fact) {
        consumer.apply(new ConsumerRecord<>("leave.request.v1", 0, fact.getAggregateVersion(),
                fact.getTenantId() + ":" + fact.getEmployeeId(), fact));
    }
}
