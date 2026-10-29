package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.reporting.entity.ExportJob;
import com.hrflow.reporting.repository.ExportJobRepository;
import com.hrflow.reporting.service.CsvExportService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class CreateObservableCsvJobTest {

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

    @TempDir
    Path tempStorageDir;

    @Test
    @DisplayName("W09-S10 to S12: Successful export writes atomically and marks status COMPLETED")
    void successfulExportMarksCompleted() throws IOException {
        UUID tenantId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        ExportJob job = exportService.createExportJob(tenantId, requesterId, "ATTENDANCE_SUMMARY");
        assertThat(job.getStatus()).isEqualTo("QUEUED");

        List<String> lines = List.of(
                "EmployeeId,WorkDate,Status,MinutesLate",
                "emp-1,2026-10-20,PRESENT,0",
                "emp-2,2026-10-20,LATE,15"
        );

        exportService.executeExport(tenantId, job.getId(), lines, false, tempStorageDir);

        ExportJob completed = exportRepo.findByIdAndTenantId(job.getId(), tenantId).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo("COMPLETED");
        assertThat(completed.getRowCount()).isEqualTo(3);
        assertThat(completed.getDownloadUrl()).isEqualTo("/api/v1/reports/exports/" + job.getId() + "/download");

        Path finalFile = tempStorageDir.resolve(job.getId() + ".csv");
        assertThat(Files.exists(finalFile)).isTrue();
        Path tmpFile = tempStorageDir.resolve(job.getId() + ".csv.tmp");
        assertThat(Files.exists(tmpFile)).isFalse();
    }

    @Test
    @DisplayName("W09-S10 to S12: Worker failure before atomic rename never marks CSV READY or leaks partial file")
    void workerFailureBeforeAtomicRenameNeverMarksCompleted() {
        UUID tenantId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        ExportJob job = exportService.createExportJob(tenantId, requesterId, "LEAVE_SUMMARY");

        List<String> lines = List.of(
                "EmployeeId,LeaveType,StartDate,EndDate",
                "emp-1,ANNUAL,2026-11-01,2026-11-03"
        );

        assertThatThrownBy(() -> {
            exportService.executeExport(tenantId, job.getId(), lines, true, tempStorageDir);
        }).isInstanceOf(IOException.class).hasMessageContaining("Simulated worker failure");

        ExportJob failedJob = exportRepo.findByIdAndTenantId(job.getId(), tenantId).orElseThrow();
        assertThat(failedJob.getStatus()).isEqualTo("FAILED");
        assertThat(failedJob.getStatus()).isNotEqualTo("COMPLETED");

        Path finalFile = tempStorageDir.resolve(job.getId() + ".csv");
        assertThat(Files.exists(finalFile)).isFalse();
        Path tmpFile = tempStorageDir.resolve(job.getId() + ".csv.tmp");
        assertThat(Files.exists(tmpFile)).isFalse();
    }
}
