package com.hrflow.leave.service;

import jakarta.persistence.EntityManager;
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
    public void use(String tenantId) {
        entityManager.createNativeQuery("SELECT set_config('app.tenant_id', :tenant, true)")
                .setParameter("tenant", tenantId).getSingleResult();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void lockEmployee(String tenant, String employee) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtextextended(:key,0))")
                .setParameter("key", "leave.command:" + tenant + ":" + employee).getSingleResult();
    }
}
