package com.hrflow.contracts.leave;

import com.hrflow.contracts.leave.LeaveRequestFact;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public final class LeaveRequestWire {
    private LeaveRequestWire() {}

    public static Identity validate(String key, LeaveRequestFact fact) {
        if (fact == null
                || fact.getSchemaVersion() != 1 || fact.getAggregateVersion() < 1
                || !"LeaveRequestSnapshot.v1".equals(fact.getType()) || fact.getOccurredAt() == null) {
            throw new IllegalArgumentException("Invalid leave fact envelope");
        }
        var id = new Identity(UUID.fromString(fact.getTenantId()), UUID.fromString(fact.getAggregateId()),
                UUID.fromString(fact.getEmployeeId()), UUID.fromString(fact.getEventId()));
        UUID.fromString(fact.getCorrelationId());
        if (!(id.tenant() + ":" + id.employee()).equals(key)
                || fact.getStartDate() == null || fact.getEndDate() == null
                || fact.getEndDate().isBefore(fact.getStartDate())
                || fact.getLeaveType() == null || fact.getLeaveType().isBlank()
                || fact.getPortion() == null || !Set.of("FULL_DAY", "MORNING", "AFTERNOON").contains(fact.getPortion())
                || fact.getState() == null || !Set.of("PENDING", "APPROVED", "REJECTED", "CANCELLED", "NEEDS_REVIEW").contains(fact.getState())
                || fact.getChargedDays() == null || new BigDecimal(fact.getChargedDays()).signum() <= 0
                || fact.getWorkingDates() == null || fact.getWorkingDates().isEmpty()
                || fact.getWorkingDates().stream().distinct().count() != fact.getWorkingDates().size()
                || fact.getWorkingDates().stream().anyMatch(date -> date == null
                        || date.isBefore(fact.getStartDate()) || date.isAfter(fact.getEndDate()))) {
            throw new IllegalArgumentException("Invalid leave fact routing or owner basis");
        }
        return id;
    }

    public record Identity(UUID tenant, UUID leave, UUID employee, UUID event) {}
}
