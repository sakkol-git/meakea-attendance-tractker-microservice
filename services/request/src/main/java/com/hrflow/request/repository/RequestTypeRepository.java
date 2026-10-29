package com.hrflow.request.repository;

import com.hrflow.request.entity.RequestType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestTypeRepository extends JpaRepository<RequestType, String> {
    Optional<RequestType> findByTenantIdAndCode(String tenantId, String code);

    List<RequestType> findByTenantIdAndActiveTrue(String tenantId);

    List<RequestType> findByTenantId(String tenantId);
}
