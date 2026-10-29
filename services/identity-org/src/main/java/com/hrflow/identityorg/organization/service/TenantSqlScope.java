package com.hrflow.identityorg.organization.service;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Binds the organization tenant to the current database transaction only. */
@Component
@RequiredArgsConstructor
public class TenantSqlScope {
    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.MANDATORY)
    public void use(UUID tenantId) {
        entityManager.createNativeQuery("SELECT set_config('app.tenant_id', :tenant, true)")
                .setParameter("tenant", tenantId.toString()).getSingleResult();
    }
}
