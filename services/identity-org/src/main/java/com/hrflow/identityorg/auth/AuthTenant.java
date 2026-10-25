package com.hrflow.identityorg.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "auth_tenant", schema = "identity_org")
public class AuthTenant {
    @Id public UUID id;
    public String slug;

    protected AuthTenant() {}

    public AuthTenant(String slug) {
        id = UUID.randomUUID();
        this.slug = slug;
    }
}
