package com.hrflow.reporting.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "employee_projection", schema = "reporting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportingEmployee {
    @EmbeddedId private Key id;
    private UUID managerId;
    private boolean active;
    private long revision;

    @Embeddable
    public record Key(UUID tenantId, UUID employeeId) implements Serializable {}
}
