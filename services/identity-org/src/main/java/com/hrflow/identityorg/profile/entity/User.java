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
@Table(name = "auth_user", schema = "identity_org")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "tenant_slug", nullable = false)
    private String tenantSlug;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "roles", nullable = false)
    private String roles;

    @Column(name = "time_zone", nullable = false)
    private String timeZone;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    public User(Tenant tenant, String email, String hash, String name) {
        this.id = UUID.randomUUID();
        this.tenantId = tenant.getId();
        this.employeeId = UUID.randomUUID();
        this.tenantSlug = tenant.getSlug();
        this.email = email;
        this.passwordHash = hash;
        this.displayName = name;
        this.roles = "EMPLOYEE,OWNER";
        this.timeZone = "Asia/Phnom_Penh";
        this.enabled = true;
    }

    public void updateProfile(String name, String newTimeZone) {
        if (name != null) this.displayName = name.strip();
        if (newTimeZone != null) this.timeZone = newTimeZone;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }
}
