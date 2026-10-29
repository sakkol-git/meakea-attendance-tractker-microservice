package com.hrflow.contracts.attendance;

import java.util.Set;
import java.util.UUID;

public final class AttendanceCorrectionWire {
    private AttendanceCorrectionWire() {}
    public static Identity validate(String key, AttendanceCorrectionFact fact) {
        if (fact == null || fact.getSchemaVersion() != 1 || fact.getAggregateVersion() < 1
                || !"AttendanceCorrectionSnapshot.v1".equals(fact.getType()) || fact.getOccurredAt() == null
                || fact.getWorkDate() == null || !Set.of("PENDING", "APPROVED", "REJECTED").contains(fact.getState()))
            throw new IllegalArgumentException("Invalid correction snapshot");
        var id = new Identity(UUID.fromString(fact.getTenantId()), UUID.fromString(fact.getAggregateId()),
                UUID.fromString(fact.getEmployeeId()), UUID.fromString(fact.getEventId()));
        UUID.fromString(fact.getCorrelationId());
        if (!(id.tenant() + ":" + id.employee()).equals(key)) throw new IllegalArgumentException("Correction snapshot routing mismatch");
        return id;
    }
    public record Identity(UUID tenant, UUID correction, UUID employee, UUID event) {}
}
