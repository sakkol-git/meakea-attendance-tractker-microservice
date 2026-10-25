package com.hrflow.identityorg.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthTenantRepository extends JpaRepository<AuthTenant, UUID> {
    Optional<AuthTenant> findBySlug(String slug);
}
