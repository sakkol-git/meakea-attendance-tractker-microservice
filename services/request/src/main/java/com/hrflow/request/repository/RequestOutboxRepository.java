package com.hrflow.request.repository;

import com.hrflow.request.entity.RequestOutbox;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestOutboxRepository extends JpaRepository<RequestOutbox, String> {
    List<RequestOutbox> findByStatusOrderByCreatedAtAsc(String status);
}
