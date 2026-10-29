package com.hrflow.request.repository;

import com.hrflow.request.entity.RequestLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestLineRepository extends JpaRepository<RequestLine, String> {
    List<RequestLine> findByTenantIdAndRequestId(String tenantId, String requestId);

    void deleteByTenantIdAndRequestId(String tenantId, String requestId);
}
