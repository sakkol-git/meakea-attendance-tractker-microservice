package com.hrflow.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notification_record", schema = "notification")
@Getter
@NoArgsConstructor
public class NotificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "detail")
    private String detail;

    @Column(name = "body")
    private String body;

    @Column(name = "read", nullable = false)
    private boolean read = false;

    @Column(name = "record_id", length = 100)
    private String recordId;

    @Column(name = "kind", length = 50)
    private String kind;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "origin_event_id", updatable = false)
    private UUID originEventId;
    @Column(nullable = false, updatable = false)
    private String channel = "IN_APP";

    public void markRead() { read = true; }

    public static NotificationRecord domain(UUID tenant, UUID recipient, String title, String body, String record, String kind, UUID event) {
        var notice = new NotificationRecord(tenant, recipient, title, body, record, kind);
        notice.originEventId = event;
        return notice;
    }

    public NotificationRecord(UUID tenantId, UUID recipientId, String title, String body, String recordId, String kind) {
        this.tenantId = tenantId;
        this.recipientId = recipientId;
        this.title = title;
        this.detail = body;
        this.body = body;
        this.recordId = recordId;
        this.kind = kind;
        this.read = false;
        this.createdAt = Instant.now();
    }
}
