package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.reporting.consumer.ReportingProjectionConsumer;
import com.hrflow.reporting.dto.RequestFactEvent;
import com.hrflow.reporting.repository.RequestFactProjectionRepository;
import com.hrflow.reporting.service.CsvExportService;
import com.hrflow.reporting.service.ExportQueue;
import java.nio.file.Files;
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
class RequestCsvExportTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine").withInitScript("context-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", POSTGRES::getUsername);
        p.add("spring.datasource.password", POSTGRES::getPassword);
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @Autowired ReportingProjectionConsumer consumer;
    @Autowired RequestFactProjectionRepository facts;
    @Autowired ExportQueue queue;
    @Autowired CsvExportService exports;
    @Autowired JdbcTemplate jdbc;
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.hrflow.reporting.consumer.RequestReportBootstrap replay;
    @org.junit.jupiter.api.BeforeEach void referenceReady() {
        org.mockito.Mockito.when(replay.isReady()).thenReturn(true);
    }

    @Test void incompleteRequestReplayCannotProduceASeeminglyEmptySuccessfulExport() {
        UUID tenant = UUID.randomUUID();
        org.mockito.Mockito.when(replay.isReady()).thenReturn(false);
        assertThatThrownBy(() -> queue.create(tenant, UUID.randomUUID(), "REQUESTS", null, null))
                .isInstanceOfSatisfying(ResponseStatusException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(503));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reporting.export_job WHERE tenant_id=?", Integer.class, tenant)).isZero();
    }

    @Test void requestCsvUsesOriginalOwnerCreationDateAndTheQueuedRevisionWithoutPrivateText() throws Exception {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID(), request = UUID.randomUUID(), requester = UUID.randomUUID();
        var created = Instant.parse("2026-01-01T01:00:00Z");
        consumer.processRequestFact(new RequestFactEvent(UUID.randomUUID(), tenant, request, employee, "MATERIAL", "PENDING_APPROVAL", 1, created));
        var job = queue.create(tenant, requester, "REQUESTS", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-01"));
        consumer.processRequestFact(new RequestFactEvent(UUID.randomUUID(), tenant, request, employee, "MATERIAL", "APPROVED", 2, created));
        assertThat(queue.processNext(tenant)).isTrue();
        var result = exports.getExportJob(tenant, job.getId(), requester).orElseThrow();
        assertThat(result.getRowCount()).isEqualTo(1);
        var path = exports.getStorageRoot().resolve(result.getFileName());
        try {
            assertThat(Files.readString(path)).contains("createdAt", created.toString(), request.toString(), "PENDING_APPROVAL")
                    .doesNotContain("APPROVED", "reason", "handover");
        } finally { Files.deleteIfExists(path); }
        assertThat(exports.getExportJob(tenant, job.getId(), employee)).isEmpty();
    }

    @Test void actualOwnerPendingStateContributesToRequestDashboardCounts() {
        UUID tenant = UUID.randomUUID();
        consumer.processRequestFact(new RequestFactEvent(UUID.randomUUID(), tenant, UUID.randomUUID(), UUID.randomUUID(), "OTHER", "PENDING_APPROVAL", 1,
                Instant.parse("2026-01-01T01:00:00Z")));
        assertThat(facts.countPendingRequests(tenant)).isEqualTo(1);
    }

    @Test void unknownHistoricalCreationTimeMustNotBeGuessedFromProjectionUpdateTime() {
        UUID tenant = UUID.randomUUID();
        consumer.processRequestFact(new RequestFactEvent(UUID.randomUUID(), tenant, UUID.randomUUID(), UUID.randomUUID(), "OTHER", "PENDING", 1));
        assertThatThrownBy(() -> queue.create(tenant, UUID.randomUUID(), "REQUESTS", null, null))
                .isInstanceOfSatisfying(ResponseStatusException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(503));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reporting.export_job WHERE tenant_id=?", Integer.class, tenant)).isZero();
    }
}
