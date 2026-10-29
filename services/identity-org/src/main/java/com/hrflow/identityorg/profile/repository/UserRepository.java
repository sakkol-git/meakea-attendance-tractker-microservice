package com.hrflow.identityorg.profile.repository;

import com.hrflow.identityorg.profile.entity.User;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByTenantSlugAndEmailAndEnabledTrue(String tenantSlug, String email);
    Optional<User> findByIdAndTenantIdAndEnabledTrue(UUID id, UUID tenantId);
    Optional<User> findByTenantIdAndEmployeeIdAndEnabledTrue(UUID tenantId, UUID employeeId);
    boolean existsByTenantIdAndEmployeeIdAndEnabledTrue(UUID tenantId, UUID employeeId);
    boolean existsByTenantIdAndEmail(UUID tenantId, String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id and u.tenantId = :tenantId and u.enabled = true")
    Optional<User> findForUpdateByIdAndTenantIdAndEnabledTrue(
            @Param("id") UUID id, @Param("tenantId") UUID tenantId);
}
