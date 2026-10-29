package com.hrflow.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notification_preference", schema = "notification")
@Getter
@Setter
@NoArgsConstructor
public class NotificationPreference {

    @EmbeddedId
    private NotificationPreferenceId id;

    @Column(name = "email", nullable = false)
    private boolean email = true;

    @Column(name = "in_app", nullable = false)
    private boolean inApp = true;

    @Column(name = "reminders", nullable = false)
    private boolean reminders = true;

    @Column(name = "digest_frequency", nullable = false, length = 50)
    private String digestFrequency = "DAILY";

    @Version
    @Column(name = "version", nullable = false)
    private long version = 0L;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public NotificationPreference(UUID tenantId, UUID employeeId) {
        this.id = new NotificationPreferenceId(tenantId, employeeId);
        this.email = true;
        this.inApp = true;
        this.reminders = true;
        this.digestFrequency = "DAILY";
        this.version = 0L;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
