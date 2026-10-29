package com.hrflow.attendance.repository;

import com.hrflow.attendance.entity.AttendanceEvidence;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceEvidenceRepository extends JpaRepository<AttendanceEvidence, UUID> {
    Optional<AttendanceEvidence> findByTenantIdAndClientObservationId(UUID tenantId, UUID clientObservationId);
}
