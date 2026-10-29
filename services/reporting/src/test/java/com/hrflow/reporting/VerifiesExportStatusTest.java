package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.reporting.entity.ExportJob;
import com.hrflow.reporting.repository.ExportJobRepository;
import com.hrflow.reporting.service.CsvExportService;
import com.hrflow.reporting.service.TenantSqlScope;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.AbstractList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class VerifiesExportStatusTest {

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
    CsvExportService exportService;

    @Autowired
    ExportJobRepository exportRepo;

    @Autowired
    TenantSqlScope tenantScope;

    @Autowired
    TransactionTemplate transactionTemplate;

    @TempDir
    Path tempStorageDir;

    @Test
    @DisplayName("W09-S16 to S18: Expired or failed job is terminal and retry creates a distinct new command")
    void failedJobIsTerminalAndRetryCreatesNewJob() {
        UUID tenantId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        ExportJob initialJob = exportService.createExportJob(tenantId, requesterId, "LEAVE_EXPORT");

        // Execute with simulated failure
        assertThatThrownBy(() -> {
            exportService.executeExport(tenantId, initialJob.getId(), List.of("Col"), true, tempStorageDir);
        }).isInstanceOf(IOException.class);

        ExportJob failedJob = exportRepo.findByIdAndTenantId(initialJob.getId(), tenantId).orElseThrow();
        assertThat(failedJob.getStatus()).isEqualTo("FAILED");

        // Retry failed job: creates a new explicit job
        ExportJob retriedJob = exportService.retryFailedJob(tenantId, failedJob.getId(), requesterId);
        assertThat(retriedJob.getId()).isNotEqualTo(failedJob.getId());
        assertThat(retriedJob.getStatus()).isEqualTo("QUEUED");

        // Original failed job remains unchanged in status FAILED (terminal invariant)
        ExportJob unchangedOldJob = exportRepo.findByIdAndTenantId(failedJob.getId(), tenantId).orElseThrow();
        assertThat(unchangedOldJob.getStatus()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("RPT-14: A retried export copies report type, fromDate, and toDate into new job while leaving old terminal job unchanged")
    void retryPreservesReportTypeAndDateFiltersWhileLeavingOldJobUnchanged() throws IOException {
        UUID tenantId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        LocalDate fromDate = LocalDate.of(2026, 3, 1);
        LocalDate toDate = LocalDate.of(2026, 3, 31);

        ExportJob initialJob = exportService.createExportJob(tenantId, requesterId, "ATTENDANCE_MONTHLY", fromDate, toDate);

        // Fail the export
        assertThatThrownBy(() -> {
            exportService.executeExport(tenantId, initialJob.getId(), List.of("Col1"), true, tempStorageDir);
        }).isInstanceOf(IOException.class);

        ExportJob failedJob = exportRepo.findByIdAndTenantId(initialJob.getId(), tenantId).orElseThrow();
        assertThat(failedJob.getStatus()).isEqualTo("FAILED");
        assertThat(failedJob.getFromDate()).isEqualTo(fromDate);
        assertThat(failedJob.getToDate()).isEqualTo(toDate);
        assertThat(failedJob.getExportType()).isEqualTo("ATTENDANCE_MONTHLY");

        // Retry the failed job
        ExportJob retriedJob = exportService.retryFailedJob(tenantId, failedJob.getId(), requesterId);

        // Verify new job copied report type, fromDate, and toDate
        assertThat(retriedJob.getId()).isNotEqualTo(failedJob.getId());
        assertThat(retriedJob.getStatus()).isEqualTo("QUEUED");
        assertThat(retriedJob.getExportType()).isEqualTo("ATTENDANCE_MONTHLY");
        assertThat(retriedJob.getFromDate()).isEqualTo(fromDate);
        assertThat(retriedJob.getToDate()).isEqualTo(toDate);

        // Verify old terminal job remains completely unchanged
        ExportJob reloadedOldJob = exportRepo.findByIdAndTenantId(failedJob.getId(), tenantId).orElseThrow();
        assertThat(reloadedOldJob.getStatus()).isEqualTo("FAILED");
        assertThat(reloadedOldJob.getExportType()).isEqualTo("ATTENDANCE_MONTHLY");
        assertThat(reloadedOldJob.getFromDate()).isEqualTo(fromDate);
        assertThat(reloadedOldJob.getToDate()).isEqualTo(toDate);
    }

    @Test
    @DisplayName("W09-S16 to S18: Attempting to retry a non-terminal job is rejected")
    void retryingNonTerminalJobIsRejected() {
        UUID tenantId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        ExportJob activeJob = exportService.createExportJob(tenantId, requesterId, "ACTIVE_EXPORT");

        assertThatThrownBy(() -> {
            exportService.retryFailedJob(tenantId, activeJob.getId(), requesterId);
        }).isInstanceOf(IllegalStateException.class).hasMessageContaining("Only terminal failed or cancelled jobs can be retried");
    }

    @Test
    @DisplayName("RPT-11: RUNNING status is committed and visible to concurrent queries during filesystem I/O")
    void runningStatusIsCommittedAndVisibleDuringIo() throws IOException {
        UUID tenantId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        ExportJob job = exportService.createExportJob(tenantId, requesterId, "ATTENDANCE_EXPORT");
        UUID jobId = job.getId();

        AtomicReference<String> observedStatusDuringIo = new AtomicReference<>();

        List<String> dynamicLines = new AbstractList<>() {
            @Override
            public String get(int index) {
                try {
                    // Observe committed status from an independent thread/transaction during file writing
                    String status = CompletableFuture.supplyAsync(() -> transactionTemplate.execute(tx -> {
                        tenantScope.use(tenantId);
                        return exportRepo.findByIdAndTenantId(jobId, tenantId).orElseThrow().getStatus();
                    })).get();
                    observedStatusDuringIo.set(status);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return "line" + index;
            }

            @Override
            public int size() {
                return 3;
            }
        };

        exportService.executeExport(tenantId, jobId, dynamicLines, false, tempStorageDir);

        assertThat(observedStatusDuringIo.get())
                .as("Status in database during filesystem I/O must be committed RUNNING")
                .isEqualTo("RUNNING");

        ExportJob completedJob = exportRepo.findByIdAndTenantId(jobId, tenantId).orElseThrow();
        assertThat(completedJob.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("RPT-11: FAILED status is committed independently and not rolled back when an exception occurs during I/O")
    void failedStatusIsCommittedIndependentlyOnException() {
        UUID tenantId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        ExportJob job = exportService.createExportJob(tenantId, requesterId, "LEAVE_EXPORT");
        UUID jobId = job.getId();

        List<String> failingLines = new AbstractList<>() {
            @Override
            public String get(int index) {
                if (index == 1) {
                    throw new RuntimeException("Simulated disk error during CSV write");
                }
                return "header";
            }

            @Override
            public int size() {
                return 3;
            }
        };

        assertThatThrownBy(() -> {
            exportService.executeExport(tenantId, jobId, failingLines, false, tempStorageDir);
        }).isInstanceOf(RuntimeException.class).hasMessageContaining("Simulated disk error");

        // The failure state must be committed independently in database and NOT rolled back
        ExportJob failedJob = exportRepo.findByIdAndTenantId(jobId, tenantId).orElseThrow();
        assertThat(failedJob.getStatus())
                .as("Status in database must be FAILED and not rolled back to QUEUED")
                .isEqualTo("FAILED");
    }
}
