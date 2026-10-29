package com.hrflow.identityorg.audit;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;

/** Audit writes are append-only; a database trigger also rejects bulk modification APIs. */
public interface AuditRepository extends Repository<IdentityAudit, UUID>, JpaSpecificationExecutor<IdentityAudit> {
    IdentityAudit save(IdentityAudit entry);
}
