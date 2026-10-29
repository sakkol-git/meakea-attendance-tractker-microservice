package com.hrflow.attendance.service;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Binds the attendance tenant to the current database transaction only. */
@Component
@RequiredArgsConstructor
public class TenantSqlScope {
    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.MANDATORY)
    public void use(UUID tenantId) {
        entityManager.createNativeQuery("SELECT set_config('app.tenant_id', :tenant, true)")
                .setParameter("tenant", tenantId.toString()).getSingleResult();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void lockEmployee(UUID tenant, UUID employee) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtextextended(:key,0))")
                .setParameter("key", "attendance.command:" + tenant + ":" + employee).getSingleResult();
    }
}
