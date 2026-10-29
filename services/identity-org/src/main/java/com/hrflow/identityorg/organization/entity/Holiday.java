package com.hrflow.identityorg.organization.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "holiday", schema = "identity_org")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Holiday {
    @Id private UUID id;
    private UUID tenantId;
    private LocalDate date;
    private String name;
    private String kind;
    private Instant deletedAt;
    @Version private long version;

    public Holiday(UUID tenantId, LocalDate date, String name, String kind) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.date = date;
        this.name = name;
        this.kind = kind;
    }

    public void delete(Instant when) { this.deletedAt = when; }

    public void update(String name, LocalDate date, String kind) {
        if (name != null) this.name = name;
        if (date != null) this.date = date;
        if (kind != null) this.kind = kind;
    }
}
