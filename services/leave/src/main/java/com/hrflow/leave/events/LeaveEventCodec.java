package com.hrflow.leave.events;

import com.hrflow.contracts.leave.LeaveRequestFact;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class LeaveEventCodec {
    private final JsonMapper json;

    public LeaveRequestFact encode(Stored event) {
        var body = json.readValue(event.payload(), LeaveSnapshot.class);
        UUID.fromString(event.id());
        UUID.fromString(event.tenant());
        UUID.fromString(event.aggregate());
        UUID.fromString(body.employeeId());
        if (event.revision() < 1 || event.correlation() == null || event.occurredAt() == null
                || !(event.tenant() + ":" + body.employeeId()).equals(event.key())
                || body.startDate() == null || body.endDate() == null || body.endDate().isBefore(body.startDate())
                || body.leaveType() == null || body.leaveType().isBlank() || body.portion() == null
                || !Set.of("FULL_DAY", "MORNING", "AFTERNOON").contains(body.portion())
                || body.state() == null || !Set.of("PENDING", "APPROVED", "REJECTED", "CANCELLED", "NEEDS_REVIEW").contains(body.state())
                || body.chargedDays() == null || new BigDecimal(body.chargedDays()).signum() <= 0
                || body.workingDates() == null || body.workingDates().isEmpty() || body.workingDates().stream().distinct().count() != body.workingDates().size()
                || body.workingDates().stream().anyMatch(date -> date.isBefore(body.startDate()) || date.isAfter(body.endDate()))) {
            throw new IllegalArgumentException("Invalid leave snapshot envelope or owner facts");
        }
        return LeaveRequestFact.newBuilder().setEventId(event.id()).setType("LeaveRequestSnapshot.v1")
                .setSchemaVersion(1).setTenantId(event.tenant()).setAggregateId(event.aggregate())
                .setAggregateVersion(event.revision()).setOccurredAt(event.occurredAt())
                .setCorrelationId(event.correlation().toString()).setCausationId(null).setEmployeeId(body.employeeId())
                .setLeaveType(body.leaveType()).setStartDate(body.startDate()).setEndDate(body.endDate())
                .setPortion(body.portion()).setChargedDays(body.chargedDays()).setWorkingDates(body.workingDates())
                .setState(body.state()).build();
    }

    public record Stored(String id, String tenant, String aggregate, long revision, String key,
            UUID correlation, Instant occurredAt, String payload) {}
}
