package com.hrflow.request.repository;

import com.hrflow.request.entity.GeneralRequest;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GeneralRequestRepository extends JpaRepository<GeneralRequest, String> {
    Optional<GeneralRequest> findByTenantIdAndId(String tenantId, String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM GeneralRequest r WHERE r.id = :id AND r.tenantId = :tenantId")
    Optional<GeneralRequest> findForUpdate(@Param("tenantId") String tenantId, @Param("id") String id);

    List<GeneralRequest> findByTenantIdAndEmployeeIdOrderByCreatedAtDesc(String tenantId, String employeeId);

    List<GeneralRequest> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, String status);

    List<GeneralRequest> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}
