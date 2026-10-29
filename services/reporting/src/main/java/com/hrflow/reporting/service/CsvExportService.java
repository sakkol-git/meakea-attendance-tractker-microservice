package com.hrflow.reporting.service;

import com.hrflow.reporting.entity.ExportJob;
import com.hrflow.reporting.repository.ExportJobRepository;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CsvExportService {

    private final ExportJobRepository exportRepo;
    private final TenantSqlScope tenantScope;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;
    private final Path exportStorageRoot;

    @Transactional
    public ExportJob createExportJob(UUID tenantId, UUID requesterId, String exportType, java.time.LocalDate fromDate, java.time.LocalDate toDate) {
        tenantScope.use(tenantId);
        ExportJob job = new ExportJob(tenantId, requesterId, exportType, fromDate, toDate);
        return exportRepo.save(job);
    }

    @Transactional
    public ExportJob createExportJob(UUID tenantId, UUID requesterId, String exportType) {
        return createExportJob(tenantId, requesterId, exportType, null, null);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<ExportJob> getExportJob(UUID tenantId, UUID jobId, UUID requesterId) {
        tenantScope.use(tenantId);
        return exportRepo.findByIdAndTenantIdAndRequesterId(jobId, tenantId, requesterId);
    }

    @Transactional(readOnly = true)
    public List<ExportJob> listExportJobs(UUID tenantId, UUID requesterId) {
        tenantScope.use(tenantId);
        return exportRepo.findByTenantIdAndRequesterId(tenantId, requesterId);
    }

    public void executeExport(UUID tenantId, UUID jobId, List<String> csvLines, boolean simulateFailureBeforeRename, Path baseDirectory) throws IOException {
        // Step 1: Commit RUNNING before filesystem I/O
        transactionTemplate.executeWithoutResult(status -> {
            tenantScope.use(tenantId);
            ExportJob job = exportRepo.findByIdAndTenantId(jobId, tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("Export job not found: " + jobId));
            if (!"QUEUED".equals(job.getStatus())) throw new IllegalStateException("Export is already claimed or terminal");
            job.markRunning();
            exportRepo.save(job);
        });

        publish(tenantId, jobId, csvLines, simulateFailureBeforeRename, baseDirectory, csvLines.size());
    }

    public void completeClaimedExport(UUID tenantId, UUID jobId, List<List<String>> rows) throws IOException {
        List<String> lines = rows.stream().map(com.hrflow.reporting.util.CsvSanitizer::formatRow).toList();
        publish(tenantId, jobId, lines, false, null, Math.max(0, rows.size() - 1));
    }

    private void publish(UUID tenantId, UUID jobId, List<String> csvLines, boolean simulateFailureBeforeRename,
            Path baseDirectory, int rowCount) throws IOException {
        Path effectiveRoot = (baseDirectory != null) ? baseDirectory.toAbsolutePath().normalize() : exportStorageRoot;
        Path tempFile = effectiveRoot.resolve(jobId + ".csv.tmp");
        Path finalFile = effectiveRoot.resolve(jobId + ".csv");

        try {
            Files.createDirectories(effectiveRoot);
            try (BufferedWriter writer = Files.newBufferedWriter(tempFile, java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE_NEW, java.nio.file.StandardOpenOption.WRITE,
                    java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                for (String line : csvLines) {
                    writer.write(com.hrflow.reporting.util.CsvSanitizer.sanitizeLine(line));
                    writer.newLine();
                }
            }

            if (simulateFailureBeforeRename) {
                // Worker crash or error before atomic rename:
                throw new IOException("Simulated worker failure before atomic file rename");
            }

            // Atomic move from tmp to final
            try {
                Files.move(tempFile, finalFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                throw new IOException("Export storage must support atomic publication", e);
            }

            // Step 2: Commit COMPLETED only after atomic publication
            transactionTemplate.executeWithoutResult(status -> {
                tenantScope.use(tenantId);
                ExportJob job = exportRepo.findByIdAndTenantId(jobId, tenantId)
                        .orElseThrow(() -> new IllegalArgumentException("Export job not found: " + jobId));
                if (!"RUNNING".equals(job.getStatus())) throw new IllegalStateException("Export claim is no longer active");
                job.markCompleted(finalFile.getFileName().toString(),
                        "/api/v1/reports/exports/" + jobId + "/download", rowCount);
                exportRepo.save(job);
            });
        } catch (Exception e) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
            }

            // Step 3: Commit FAILED independently so throwing cannot roll back the failure state
            transactionTemplate.executeWithoutResult(status -> {
                tenantScope.use(tenantId);
                exportRepo.findByIdAndTenantId(jobId, tenantId).ifPresent(job -> {
                    if ("RUNNING".equals(job.getStatus())) {
                        job.markFailed();
                        exportRepo.save(job);
                    }
                });
            });

            if (e instanceof IOException ioException) {
                throw ioException;
            }
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(e);
        }
    }

    public void executeExportRows(UUID tenantId, UUID jobId, List<List<String>> rows, boolean simulateFailureBeforeRename, Path baseDirectory) throws IOException {
        List<String> formattedLines = (rows == null) ? List.of() : rows.stream()
                .map(com.hrflow.reporting.util.CsvSanitizer::formatRow)
                .toList();
        executeExport(tenantId, jobId, formattedLines, simulateFailureBeforeRename, baseDirectory);
    }

    public String escapeAndBuildRow(List<String> values) {
        return com.hrflow.reporting.util.CsvSanitizer.formatRow(values);
    }

    @Transactional(readOnly = true)
    public ExportJob authorizeDownload(UUID tenantId, UUID jobId, UUID actorId, boolean isActorActive) {
        tenantScope.use(tenantId);
        if (!isActorActive) {
            throw new SecurityException("Revoked or deactivated actor cannot download export");
        }
        ExportJob job = exportRepo.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Export job not found: " + jobId));

        if (!"COMPLETED".equals(job.getStatus())) {
            throw new IllegalStateException("Export job is not ready for download. Current status: " + job.getStatus());
        }
        if (!job.getRequesterId().equals(actorId)) {
            throw new SecurityException("Access denied: user did not request this export");
        }
        return job;
    }

    @Transactional
    public ExportJob retryFailedJob(UUID tenantId, UUID oldJobId, UUID requesterId) {
        tenantScope.use(tenantId);
        ExportJob oldJob = exportRepo.findByIdAndTenantId(oldJobId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Old export job not found: " + oldJobId));

        if (!"FAILED".equals(oldJob.getStatus()) && !"CANCELLED".equals(oldJob.getStatus())) {
            throw new IllegalStateException("Only terminal failed or cancelled jobs can be retried as a new command. Status: " + oldJob.getStatus());
        }

        // Key domain invariant: Old failed job is terminal and unchanged; retry uses a new explicit command
        ExportJob newJob = oldJob.createRetry(requesterId);
        return exportRepo.save(newJob);
    }

    public Path getStorageRoot() {
        return exportStorageRoot;
    }

    public Path resolveStoragePath(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("Storage key must not be null or blank");
        }
        if (storageKey.startsWith("/") || storageKey.startsWith("\\") || java.nio.file.Paths.get(storageKey).isAbsolute()) {
            throw new SecurityException("Absolute path escapes are rejected: " + storageKey);
        }
        if (storageKey.contains("..")) {
            throw new SecurityException("Path traversal escapes are rejected: " + storageKey);
        }
        Path resolved = exportStorageRoot.resolve(storageKey).normalize();
        if (!resolved.startsWith(exportStorageRoot) || resolved.equals(exportStorageRoot)) {
            throw new SecurityException("Resolved path escaped storage root: " + storageKey);
        }
        return resolved;
    }

    @Transactional
    public int reconcileInterruptedJobs(UUID tenantId) {
        tenantScope.use(tenantId);
        List<ExportJob> activeJobs = exportRepo.findByTenantId(tenantId);
        int reconciled = 0;
        for (ExportJob job : activeJobs) {
            if ("RUNNING".equals(job.getStatus()) || "PROCESSING".equals(job.getStatus())) {
                // Stored fileName may be damaged. Clean only this job's generated names, never another job's file.
                try {
                    Path defaultTmp = exportStorageRoot.resolve(job.getId() + ".csv.tmp").normalize();
                    if (defaultTmp.startsWith(exportStorageRoot)) {
                        Files.deleteIfExists(defaultTmp);
                        Files.deleteIfExists(exportStorageRoot.resolve(job.getId() + ".csv"));
                    }
                } catch (Exception ignored) {
                }

                job.markFailed();
                exportRepo.save(job);
                reconciled++;
            }
        }
        return reconciled;
    }
}
