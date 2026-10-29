package com.hrflow.request.repository;

import com.hrflow.request.entity.RequestComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestCommentRepository extends JpaRepository<RequestComment, String> {
    List<RequestComment> findByTenantIdAndRequestIdOrderByCreatedAtAsc(String tenantId, String requestId);
}
