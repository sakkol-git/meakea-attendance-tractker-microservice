package com.hrflow.reporting.service;

import com.hrflow.reporting.dto.AuditLogEntry;
import com.hrflow.reporting.entity.AuditLog;
import com.hrflow.reporting.repository.AuditLogRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service managing audit log records with append-only semantics.
 * Only insert (record) and tenant-scoped queries (search) are supported.
 * Modification and deletion are forbidden and rejected at the database level.
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    public static final int MIN_PAGE = 0;
    public static final int MIN_SIZE = 1;
    public static final int MAX_SIZE = 100;

    private final AuditLogRepository auditRepo;
    private final TenantSqlScope tenantScope;

    @Transactional
    public AuditLog record(
            UUID tenantId,
            UUID actorId,
            String actorEmail,
            String action,
            String resourceType,
            String resourceId,
            String details,
            String ipAddress
    ) {
        tenantScope.use(tenantId);
        AuditLog log = new AuditLog(tenantId, actorId, actorEmail, action, resourceType, resourceId, details, ipAddress);
        return auditRepo.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLogEntry> search(
            UUID tenantId,
            String resourceType,
            String action,
            int page,
            int size
    ) {
        if (page < MIN_PAGE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page index must not be less than zero: " + page);
        }
        if (size < MIN_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page size must be at least one: " + size);
        }
        if (size > MAX_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page size must not exceed " + MAX_SIZE + ": " + size);
        }

        tenantScope.use(tenantId);
        List<AuditLog> logs = auditRepo.searchLogs(tenantId, resourceType, action, PageRequest.of(page, size));
        return logs.stream()
                .map(log -> new AuditLogEntry(
                        log.getId(),
                        log.getTenantId(),
                        log.getActorId(),
                        log.getActorEmail(),
                        log.getAction(),
                        log.getResourceType(),
                        log.getResourceId(),
                        log.getDetails(),
                        log.getIpAddress(),
                        log.getCreatedAt()
                ))
                .toList();
    }
}
