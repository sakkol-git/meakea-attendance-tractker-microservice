package com.hrflow.reporting.repository;

import com.hrflow.reporting.entity.CheckpointProgress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckpointProgressRepository extends JpaRepository<CheckpointProgress, CheckpointProgress.CheckpointId> {

    @Query("SELECT c FROM CheckpointProgress c WHERE c.id.tenantId = :tenantId AND c.id.topic = :topic")
    List<CheckpointProgress> findByTenantAndTopic(@Param("tenantId") UUID tenantId, @Param("topic") String topic);

    @Query("SELECT c FROM CheckpointProgress c WHERE c.id.tenantId = :tenantId AND c.id.topic = :topic AND c.id.partitionNum = :partitionNum")
    Optional<CheckpointProgress> findByTenantTopicPartition(
            @Param("tenantId") UUID tenantId,
            @Param("topic") String topic,
            @Param("partitionNum") int partitionNum
    );
}
