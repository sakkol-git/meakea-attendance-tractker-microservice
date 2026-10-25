package com.hrflow.identityorg.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "auth_user", schema = "identity_org")
public class AuthUser {
    @Id public UUID id;
    public UUID tenantId;
    public String tenantSlug;
    public UUID employeeId;
    public String email;
    public String passwordHash;
    public String displayName;
    public String roles;
    public String timeZone;
    public boolean enabled;

    protected AuthUser() {}

    public AuthUser(AuthTenant tenant, String email, String hash, String name) {
        id = UUID.randomUUID();
        tenantId = tenant.id;
        employeeId = UUID.randomUUID();
        tenantSlug = tenant.slug;
        this.email = email;
        passwordHash = hash;
        displayName = name;
        roles = "EMPLOYEE,OWNER";
        timeZone = "Asia/Phnom_Penh";
        enabled = true;
    }
}
