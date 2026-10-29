package com.hrflow.request.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "request_line", schema = "request")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestLine {
    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;

    @Column(name = "material_item_id", nullable = false, length = 64)
    private String materialItemId;

    @Column(nullable = false)
    @Builder.Default
    private int quantity = 1;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
