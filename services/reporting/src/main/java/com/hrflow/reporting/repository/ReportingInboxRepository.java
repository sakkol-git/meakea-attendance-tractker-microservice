package com.hrflow.reporting.repository;

import com.hrflow.reporting.entity.ReportingInbox;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportingInboxRepository extends JpaRepository<ReportingInbox, ReportingInbox.InboxId> {

    @Query("SELECT COUNT(i) > 0 FROM ReportingInbox i WHERE i.id.consumerName = :consumerName AND i.id.tenantId = :tenantId AND i.id.eventId = :eventId")
    boolean existsByConsumerAndEvent(
            @Param("consumerName") String consumerName,
            @Param("tenantId") UUID tenantId,
            @Param("eventId") UUID eventId
    );
}
