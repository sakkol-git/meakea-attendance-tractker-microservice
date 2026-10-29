package com.hrflow.reporting;

import com.hrflow.reporting.entity.ExportJob;
import com.hrflow.reporting.repository.ExportJobRepository;
import com.hrflow.reporting.service.CsvExportService;
import com.hrflow.reporting.service.TenantSqlScope;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class RecoverInterruptedExportJobTest {

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
    private ExportJobRepository exportJobRepo;

    @Autowired
    private CsvExportService exportService;

    @Autowired
    private TenantSqlScope tenantScope;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("W09-S35: Reconciles interrupted jobs stuck in PROCESSING to FAILED; partial file is never READY")
    void reconcilesInterruptedProcessingJobsToFailed(@TempDir File tempDir) throws IOException {
        UUID tenantId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        // 1. Create export job through service
        ExportJob job = exportService.createExportJob(tenantId, requesterId, "ATTENDANCE_CSV");
        UUID jobId = job.getId();

        // 2. Simulate crash: job in RUNNING status with orphaned tmp file
        File orphanedTmpFile = new File(tempDir, "export_" + jobId + ".csv.tmp");
        assertThat(orphanedTmpFile.createNewFile()).isTrue();

        transactionTemplate.executeWithoutResult(status -> {
            tenantScope.use(tenantId);
            ExportJob runningJob = exportJobRepo.findById(jobId).orElseThrow();
            runningJob.markRunning();
            exportJobRepo.save(runningJob);
        });

        // 3. Reconcile on restart / recovery
        int recoveredCount = exportService.reconcileInterruptedJobs(tenantId);
        assertThat(recoveredCount).isGreaterThanOrEqualTo(1);

        transactionTemplate.executeWithoutResult(status -> {
            tenantScope.use(tenantId);
            ExportJob updatedJob = exportJobRepo.findById(jobId).orElseThrow();
            assertThat(updatedJob.getStatus()).isEqualTo("FAILED");
            assertThat(updatedJob.getStatus()).isNotEqualTo("READY");
        });
    }

    @Test
    @DisplayName("RPT-12: Confine cleanup to storage root; reject absolute and ../ escapes; never delete outside files")
    void rejectsEscapesAndNeverDeletesOutsideFiles(@TempDir java.nio.file.Path outsideDir) throws IOException {
        UUID tenantId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        // 1. Create an outside file that must NOT be deleted
        java.nio.file.Path outsideFile = outsideDir.resolve("outside-victim.txt");
        java.nio.file.Files.writeString(outsideFile, "do-not-delete-critical-data");
        assertThat(java.nio.file.Files.exists(outsideFile)).isTrue();

        // 2. Create an interrupted job whose fileName maliciously points to the outside file (absolute path)
        ExportJob jobAbsolute = exportService.createExportJob(tenantId, requesterId, "ATTENDANCE_CSV");
        UUID jobIdAbsolute = jobAbsolute.getId();

        transactionTemplate.executeWithoutResult(status -> {
            tenantScope.use(tenantId);
            ExportJob runningJob = exportJobRepo.findById(jobIdAbsolute).orElseThrow();
            // Infiltrate absolute path into database fileName
            runningJob.markCompleted(outsideFile.toAbsolutePath().toString(), null, 0);
            runningJob.markRunning(); // put back into RUNNING
            exportJobRepo.save(runningJob);
        });

        // 3. Create another interrupted job whose fileName contains ../ traversal
        ExportJob jobTraversal = exportService.createExportJob(tenantId, requesterId, "LEAVE_CSV");
        UUID jobIdTraversal = jobTraversal.getId();

        transactionTemplate.executeWithoutResult(status -> {
            tenantScope.use(tenantId);
            ExportJob runningJob = exportJobRepo.findById(jobIdTraversal).orElseThrow();
            runningJob.markCompleted("../../outside-victim.txt", null, 0);
            runningJob.markRunning();
            exportJobRepo.save(runningJob);
        });

        // 4. Run reconciliation
        int reconciled = exportService.reconcileInterruptedJobs(tenantId);
        assertThat(reconciled).isGreaterThanOrEqualTo(2);

        // 5. PROVE: Outside file MUST NOT be deleted!
        assertThat(java.nio.file.Files.exists(outsideFile))
                .as("Outside file must never be deleted by cleanup reconciliation")
                .isTrue();

        // 6. PROVE: Absolute and ../ escapes are rejected by resolveStoragePath
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> exportService.resolveStoragePath(outsideFile.toAbsolutePath().toString()))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Absolute path escapes are rejected");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> exportService.resolveStoragePath("../../outside-victim.txt"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Path traversal escapes are rejected");

        // 7. PROVE: Server-owned storage key resolves safely beneath injected root
        java.nio.file.Path safePath = exportService.resolveStoragePath("valid-export.csv");
        assertThat(safePath.startsWith(exportService.getStorageRoot())).isTrue();
        assertThat(safePath.getFileName().toString()).isEqualTo("valid-export.csv");
    }

    @Test
    void interruptedJobCannotDeleteAnotherJobsCompletedCsv() throws IOException {
        UUID tenant = UUID.randomUUID(), requester = UUID.randomUUID();
        var complete = exportService.createExportJob(tenant, requester, "ATTENDANCE");
        exportService.executeExport(tenant, complete.getId(), java.util.List.of("safe"), false, null);
        var interrupted = exportService.createExportJob(tenant, requester, "ATTENDANCE");
        transactionTemplate.executeWithoutResult(tx -> {
            tenantScope.use(tenant);
            // Simulate a damaged database pointer in a crashed job; cleanup may use only its own generated ID.
            var row = exportJobRepo.findByIdAndTenantId(interrupted.getId(), tenant).orElseThrow();
            row.markCompleted(complete.getId() + ".csv", null, 0);
            row.markRunning();
        });
        try {
            exportService.reconcileInterruptedJobs(tenant);
            assertThat(java.nio.file.Files.readString(exportService.getStorageRoot().resolve(complete.getId() + ".csv")))
                    .isEqualTo("safe\n");
        } finally { java.nio.file.Files.deleteIfExists(exportService.getStorageRoot().resolve(complete.getId() + ".csv")); }
    }
}
