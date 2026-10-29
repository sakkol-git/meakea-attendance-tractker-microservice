package com.hrflow.reporting.service;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TenantSqlScope {
    private final EntityManager entityManager;

    public TenantSqlScope(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void use(UUID tenantId) {
        entityManager.createNativeQuery("SELECT set_config('app.tenant_id', :tenant, true)")
                .setParameter("tenant", tenantId.toString())
                .getSingleResult();
    }
}
