package com.hrflow.identityorg.organization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "work_site", schema = "identity_org")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkSite {
    @Id private UUID id;
    private UUID tenantId;
    private String name;
    private BigDecimal latitude;
    private BigDecimal longitude;
    @Column(name = "radius_m") private int radiusM;
    private String timezone;
    private Instant deletedAt;
    @Version private long version;

    public WorkSite(UUID tenantId, String name, BigDecimal latitude, BigDecimal longitude,
            int radiusM, String timezone) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        update(name, latitude, longitude, radiusM, timezone);
    }

    public void update(String name, BigDecimal latitude, BigDecimal longitude, Integer radiusM, String timezone) {
        if (name != null) this.name = name;
        if (latitude != null) this.latitude = latitude;
        if (longitude != null) this.longitude = longitude;
        if (radiusM != null) this.radiusM = radiusM;
        if (timezone != null) this.timezone = timezone;
    }

    public void delete(Instant when) { this.deletedAt = when; }
}
