package com.hrflow.notification.repository;

import com.hrflow.notification.entity.NotificationRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRecordRepository extends JpaRepository<NotificationRecord, UUID> {

    Page<NotificationRecord> findByTenantIdAndRecipientIdOrderByCreatedAtDesc(UUID tenantId, UUID recipientId, Pageable pageable);

    List<NotificationRecord> findByTenantIdAndRecipientIdOrderByCreatedAtDesc(UUID tenantId, UUID recipientId);

    long countByTenantIdAndRecipientIdAndReadFalse(UUID tenantId, UUID recipientId);

    Optional<NotificationRecord> findByIdAndTenantIdAndRecipientId(UUID id, UUID tenantId, UUID recipientId);

    @Modifying
    @Query("UPDATE NotificationRecord n SET n.read = true WHERE n.tenantId = :tenantId AND n.recipientId = :recipientId AND n.read = false")
    int markAllReadForRecipient(UUID tenantId, UUID recipientId);
}
