package com.hrflow.reporting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "export_job", schema = "reporting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Column(name = "export_type", nullable = false, length = 100)
    private String exportType;

    @Column(name = "from_date")
    private java.time.LocalDate fromDate;

    @Column(name = "to_date")
    private java.time.LocalDate toDate;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "download_url", length = 500)
    private String downloadUrl;

    @Column(name = "row_count", nullable = false)
    private int rowCount;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public ExportJob(UUID tenantId, UUID requesterId, String exportType, java.time.LocalDate fromDate, java.time.LocalDate toDate) {
        this.tenantId = tenantId;
        this.requesterId = requesterId;
        this.exportType = exportType;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.status = "QUEUED";
        this.rowCount = 0;
        this.createdAt = Instant.now();
    }

    public ExportJob(UUID tenantId, UUID requesterId, String exportType) {
        this(tenantId, requesterId, exportType, null, null);
    }

    public ExportJob(UUID id, UUID tenantId, UUID requesterId, String exportType, String fileName) {
        this.id = id;
        this.tenantId = tenantId;
        this.requesterId = requesterId;
        this.exportType = exportType;
        this.status = "PROCESSING";
        this.fileName = fileName;
        this.rowCount = 0;
        this.createdAt = Instant.now();
    }

    public void markRunning() {
        this.status = "RUNNING";
    }

    public void markCompleted(String fileName, String downloadUrl, int rowCount) {
        this.status = "COMPLETED";
        this.fileName = fileName;
        this.downloadUrl = downloadUrl;
        this.rowCount = rowCount;
        this.completedAt = Instant.now();
    }

    public void markFailed() {
        this.status = "FAILED";
        this.completedAt = Instant.now();
    }

    public ExportJob createRetry(UUID requesterId) {
        return new ExportJob(this.tenantId, requesterId, this.exportType, this.fromDate, this.toDate);
    }
}
