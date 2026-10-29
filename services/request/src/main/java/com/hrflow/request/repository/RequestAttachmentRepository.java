package com.hrflow.request.repository;

import com.hrflow.request.entity.RequestAttachment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestAttachmentRepository extends JpaRepository<RequestAttachment, String> {
    List<RequestAttachment> findByTenantIdAndRequestId(String tenantId, String requestId);

    Optional<RequestAttachment> findByTenantIdAndRequestIdAndId(String tenantId, String requestId, String id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select attachment from RequestAttachment attachment where attachment.tenantId=:tenant and attachment.requestId=:request and attachment.id=:id")
    Optional<RequestAttachment> lock(@org.springframework.data.repository.query.Param("tenant") String tenant,
            @org.springframework.data.repository.query.Param("request") String request,
            @org.springframework.data.repository.query.Param("id") String id);

    List<RequestAttachment> findByTenantIdAndStatusAndCreatedAtBefore(String tenantId, String status, Instant cutoff);
}
