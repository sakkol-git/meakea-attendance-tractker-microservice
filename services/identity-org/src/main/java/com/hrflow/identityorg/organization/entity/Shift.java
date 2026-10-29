package com.hrflow.identityorg.organization.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "shift", schema = "identity_org")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shift {
    @Id private UUID id;
    private UUID tenantId;
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
    private int graceMinutes;
    private int workingDays;
    private UUID siteId;
    private String timezone;
    private Instant deletedAt;
    private String lastChangeReason;
    @Version private long version;

    public Shift(UUID tenantId, String name, LocalTime startTime, LocalTime endTime,
            int graceMinutes, int workingDays, UUID siteId, String timezone, String reason) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        update(name, startTime, endTime, graceMinutes, workingDays, siteId, timezone);
        this.lastChangeReason = reason;
    }

    public void update(String name, LocalTime startTime, LocalTime endTime,
            Integer graceMinutes, Integer workingDays, UUID siteId, String timezone) {
        if (name != null) this.name = name;
        if (startTime != null) this.startTime = startTime;
        if (endTime != null) this.endTime = endTime;
        if (graceMinutes != null) this.graceMinutes = graceMinutes;
        if (workingDays != null) this.workingDays = workingDays;
        if (siteId != null) this.siteId = siteId;
        if (timezone != null) this.timezone = timezone;
    }

    public void recordReason(String reason) {
        if (reason != null) this.lastChangeReason = reason;
    }
}
