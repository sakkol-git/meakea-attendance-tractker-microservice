package com.hrflow.request.repository;

import com.hrflow.request.entity.MaterialItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaterialItemRepository extends JpaRepository<MaterialItem, String> {
    Optional<MaterialItem> findByTenantIdAndCode(String tenantId, String code);

    Optional<MaterialItem> findByTenantIdAndId(String tenantId, String id);

    List<MaterialItem> findByTenantIdAndActiveTrue(String tenantId);

    List<MaterialItem> findByTenantId(String tenantId);
}
