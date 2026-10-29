package com.hrflow.reporting.repository;

import com.hrflow.reporting.entity.AuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId "
            + "AND (:resourceType IS NULL OR a.resourceType = :resourceType) "
            + "AND (:action IS NULL OR a.action = :action) "
            + "ORDER BY a.createdAt DESC")
    List<AuditLog> searchLogs(
            @Param("tenantId") UUID tenantId,
            @Param("resourceType") String resourceType,
            @Param("action") String action,
            Pageable pageable
    );

    @Override
    default void delete(AuditLog entity) {
        throw new UnsupportedOperationException("Audit log is append-only: delete is not permitted");
    }

    @Override
    default void deleteById(UUID id) {
        throw new UnsupportedOperationException("Audit log is append-only: deleteById is not permitted");
    }

    @Override
    default void deleteAll() {
        throw new UnsupportedOperationException("Audit log is append-only: deleteAll is not permitted");
    }

    @Override
    default void deleteAll(Iterable<? extends AuditLog> entities) {
        throw new UnsupportedOperationException("Audit log is append-only: deleteAll is not permitted");
    }

    @Override
    default void deleteAllById(Iterable<? extends UUID> ids) {
        throw new UnsupportedOperationException("Audit log is append-only: deleteAllById is not permitted");
    }
}
