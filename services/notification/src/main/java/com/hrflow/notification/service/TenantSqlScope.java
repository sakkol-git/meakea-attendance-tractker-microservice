package com.hrflow.notification.service;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TenantSqlScope {
    private final EntityManager entities;

    @Transactional(propagation = Propagation.MANDATORY)
    public void use(UUID tenant) {
        entities.createNativeQuery("SELECT set_config('app.tenant_id',:tenant,true)")
                .setParameter("tenant", tenant.toString()).getSingleResult();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void lockPreferences(UUID tenant, UUID employee) {
        entities.createNativeQuery("SELECT pg_advisory_xact_lock(hashtextextended(:key,0))")
                .setParameter("key", "notification.preferences:" + tenant + ":" + employee).getSingleResult();
    }
}
