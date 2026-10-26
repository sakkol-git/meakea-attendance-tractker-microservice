package com.hrflow.identityorg.auth.repository;

import com.hrflow.identityorg.auth.entity.RefreshSession;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshSession> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshSession s set s.revokedAt = :now "
            + "where s.tenantId = :tenant and s.familyId = :family and s.revokedAt is null")
    void revokeFamily(@Param("tenant") UUID tenant, @Param("family") UUID family, @Param("now") Instant now);

    @Modifying
    @Query("update RefreshSession s set s.revokedAt = :now "
            + "where s.tenantId = :tenant and s.userId = :user and s.revokedAt is null")
    void revokeUser(@Param("tenant") UUID tenant, @Param("user") UUID user, @Param("now") Instant now);
}
