package com.hrflow.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(schema = "attendance", name = "attendance_evidence")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendanceEvidence {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "client_observation_id")
    private UUID clientObservationId;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "accuracy_meters")
    private Double accuracyMeters;

    @Column(name = "device_model")
    private String deviceModel;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    public AttendanceEvidence(UUID tenantId, UUID employeeId, UUID clientObservationId,
            Instant recordedAt, Double latitude, Double longitude, Double accuracyMeters, String deviceModel) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.employeeId = employeeId;
        this.clientObservationId = clientObservationId;
        this.recordedAt = recordedAt;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyMeters = accuracyMeters;
        this.deviceModel = deviceModel;
    }
}
