package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.reporting.entity.ExportJob;
import com.hrflow.reporting.service.CsvExportService;
import com.hrflow.reporting.util.CsvSanitizer;
import java.io.IOException;
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
class AuthorizeDownloadsPreventFormulasTest {

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

    @TempDir
    Path tempStorageDir;

    @Test
    @DisplayName("W09-S13 to S15: Formula-leading cells (=, +, -, @) are escaped to prevent CSV injection")
    void formulaLeadingCellsAreEscaped() {
        assertThat(CsvSanitizer.escapeCell("=SUM(A1:A10)")).isEqualTo("'=SUM(A1:A10)");
        assertThat(CsvSanitizer.escapeCell("+123456789")).isEqualTo("'+123456789");
        assertThat(CsvSanitizer.escapeCell("-calc()")).isEqualTo("'-calc()");
        assertThat(CsvSanitizer.escapeCell("@cmd|' /C calc'!A0")).isEqualTo("'@cmd|' /C calc'!A0");
        assertThat(CsvSanitizer.escapeCell("Normal Name")).isEqualTo("Normal Name");

        String row = exportService.escapeAndBuildRow(List.of("Alice", "=1+1", "@calc"));
        assertThat(row).isEqualTo("Alice,'=1+1,'@calc");
    }

    @Test
    @DisplayName("W09-S13 to S15: Revoked authority cannot download even with previously valid job token")
    void revokedAuthorityCannotDownload() throws IOException {
        UUID tenantId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        ExportJob job = exportService.createExportJob(tenantId, requesterId, "ATTENDANCE_SUMMARY");

        exportService.executeExport(tenantId, job.getId(), List.of("Col1,Col2", "A,B"), false, tempStorageDir);

        // 1. Authorized download for active requester succeeds
        ExportJob authorized = exportService.authorizeDownload(tenantId, job.getId(), requesterId, true);
        assertThat(authorized.getStatus()).isEqualTo("COMPLETED");

        // 2. Revoked/deactivated actor is denied
        assertThatThrownBy(() -> {
            exportService.authorizeDownload(tenantId, job.getId(), requesterId, false);
        }).isInstanceOf(SecurityException.class).hasMessageContaining("Revoked or deactivated actor");

        // 3. Different user is denied
        assertThatThrownBy(() -> {
            exportService.authorizeDownload(tenantId, job.getId(), otherUserId, true);
        }).isInstanceOf(SecurityException.class).hasMessageContaining("Access denied");
    }

    @Test
    @DisplayName("RPT-13: executeExport sanitizes cells at write boundary; generated file escapes =, +, -, @, tabs, newlines, quotes, commas")
    void executeExportEnforcesSanitizationAtWriteBoundary() throws IOException {
        UUID tenantId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        ExportJob job = exportService.createExportJob(tenantId, requesterId, "SECURITY_AUDIT_CSV");

        // Pass raw, unsanitized lines directly into executeExport without manual escaping
        List<String> rawDangerousLines = List.of(
                "Header1,Header2,Header3,Header4,Header5",
                "=SUM(A1:A10),+12345,-cmd|' /C calc'!A0,@SUM(B1:B2)",
                "\t=tabFormula,\tplainTab,\"quoted, comma\",\"quote \"\"inside\"\"\"",
                "multi\nline,plain"
        );

        exportService.executeExport(tenantId, job.getId(), rawDangerousLines, false, tempStorageDir);

        Path generatedFile = tempStorageDir.resolve(job.getId() + ".csv");
        assertThat(java.nio.file.Files.exists(generatedFile)).isTrue();

        String fileContent = java.nio.file.Files.readString(generatedFile);

        // Prove that formula cells in the actual generated file are single-quote prefixed:
        assertThat(fileContent).contains("'=SUM(A1:A10)");
        assertThat(fileContent).contains("'+12345");
        assertThat(fileContent).contains("'-cmd|' /C calc'!A0");
        assertThat(fileContent).contains("'@SUM(B1:B2)");
        assertThat(fileContent).contains("'\t=tabFormula");

        // Prove quotes and commas are escaped properly in RFC 4180 format:
        assertThat(fileContent).contains("\"quoted, comma\"");
        assertThat(fileContent).contains("\"quote \"\"inside\"\"\"");

        // Prove newlines and tabs are preserved within quoted cells:
        assertThat(fileContent).contains("\"multi\nline\"");

        // Prove no raw formula injection triggers exist at the start of any cell
        assertThat(fileContent).doesNotContain("\n=SUM");
        assertThat(fileContent).doesNotContain(",=SUM");
        assertThat(fileContent).doesNotContain("\n+12345");
        assertThat(fileContent).doesNotContain(",+12345");
        assertThat(fileContent).doesNotContain("\n-cmd");
        assertThat(fileContent).doesNotContain(",-cmd");
        assertThat(fileContent).doesNotContain("\n@SUM");
        assertThat(fileContent).doesNotContain(",@SUM");
    }
}
