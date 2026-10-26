package com.hrflow.identityorg.profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auth_tenant", schema = "identity_org")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tenant {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    public Tenant(String slug) {
        this.id = UUID.randomUUID();
        this.slug = slug != null ? slug.toLowerCase() : null;
    }
}
