package com.hrflow.identityorg.auth.repository;

import com.hrflow.identityorg.auth.entity.AuthAttempt;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthAttemptRepository extends JpaRepository<AuthAttempt, String> {
    @Query(value = "SELECT * FROM identity_org.auth_attempt WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<AuthAttempt> findLocked(@Param("id") String id);

    @Modifying
    @Query(value = "INSERT INTO identity_org.auth_attempt (id, failures, window_started) "
            + "VALUES (:id, 0, :now) ON CONFLICT (id) DO NOTHING", nativeQuery = true)
    void ensure(@Param("id") String id, @Param("now") Instant now);
}
