package com.hrflow.identityorg.organization.service;

import com.hrflow.contracts.organization.OrganizationFact;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Only allowlisted owner snapshots cross the outbox-to-Avro boundary. */
@Component
@RequiredArgsConstructor
public class OrganizationFactMapper {
    private static final Map<String, Set<String>> TYPES = Map.of(
            "org.employee.v1", Set.of("org.employee.created.v1", "org.employee.updated.v1",
                    "org.employee.terminated.v1"),
            "org.shift.v1", Set.of("org.shift.defined.v1", "org.shift.updated.v1",
                    "org.shift.removed.v1"),
            "org.holiday.v1", Set.of("org.holiday.added.v1", "org.holiday.updated.v1",
                    "org.holiday.removed.v1"));

    private final JsonMapper json;

    public OrganizationFact map(OutboxEntry row) {
        if (!TYPES.getOrDefault(row.topic(), Set.of()).contains(row.type())
                || row.revision() < 1) {
            throw new IllegalArgumentException("Unknown organization topic/type or revision");
        }
        Object snapshot = switch (row.topic()) {
            case "org.employee.v1" -> employee(row.payload(), row.aggregateId());
            case "org.shift.v1" -> shift(row.payload(), row.aggregateId());
            case "org.holiday.v1" -> holiday(row.payload(), row.aggregateId());
            default -> throw new IllegalArgumentException("Unknown organization topic");
        };
        return new OrganizationFact(row.id().toString(), row.type(), 1,
                row.tenantId().toString(), row.aggregateId().toString(), row.revision(),
                row.occurredAt().toString(), row.correlationId().toString(),
                row.causationId() == null ? null : row.causationId().toString(), snapshot);
    }

    private Object employee(String payload, UUID aggregateId) {
        var value = json.readValue(payload, OrganizationSnapshots.Employee.class);
        matchId(value.employeeId(), aggregateId);
        return value.avro();
    }

    private Object shift(String payload, UUID aggregateId) {
        var value = json.readValue(payload, OrganizationSnapshots.Shift.class);
        matchId(value.shiftId(), aggregateId);
        return value.avro();
    }

    private Object holiday(String payload, UUID aggregateId) {
        var value = json.readValue(payload, OrganizationSnapshots.Holiday.class);
        matchId(value.holidayId(), aggregateId);
        return value.avro();
    }

    private void matchId(String value, UUID aggregateId) {
        if (value == null || !aggregateId.equals(UUID.fromString(value))) {
            throw new IllegalArgumentException("Payload aggregate does not match outbox key");
        }
    }

    public record OutboxEntry(UUID id, UUID tenantId, String topic, UUID aggregateId,
            long revision, String type, UUID correlationId, UUID causationId,
            String payload, Instant occurredAt) {}
}
