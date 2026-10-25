package com.hrflow.identityorg.auth;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthAttemptRepository extends JpaRepository<AuthAttempt, String> {
    @Modifying
    @Query(value = "INSERT INTO identity_org.auth_attempt(id, failures, window_started) "
            + "VALUES (:id, 0, :now) ON CONFLICT (id) DO NOTHING", nativeQuery = true)
    void ensure(@Param("id") String id, @Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select attempt from AuthAttempt attempt where attempt.id = :id")
    Optional<AuthAttempt> findLocked(@Param("id") String id);
}
