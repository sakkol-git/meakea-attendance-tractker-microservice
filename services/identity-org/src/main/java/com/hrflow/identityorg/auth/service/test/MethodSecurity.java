package com.hrflow.identityorg.auth.service.test;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Demonstrates role-based method security rules enforced at Gate 2 (ADR 010/011).
 * Authority is derived purely from the authenticated JWT token.
 */
@Service
public class MethodSecurity {

    @PreAuthorize("hasRole('HR_ADMIN') or hasRole('OWNER')")
    public String executeHrAdminOperation() {
        return "hr-admin-authorized";
    }

    @PreAuthorize("hasRole('MANAGER') or hasRole('OWNER')")
    public String executeManagerOperation() {
        return "manager-authorized";
    }
}
