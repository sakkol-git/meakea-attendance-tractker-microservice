package com.hrflow.contracts.attendance;

import com.hrflow.contracts.attendance.AttendanceDailyFact;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

public final class AttendanceDailyWire {
    private AttendanceDailyWire() {}

    public static Identity validate(String key, AttendanceDailyFact fact) {
        if (fact == null || fact.getSchemaVersion() != 1 || fact.getAggregateVersion() < 1
                || !"AttendanceDailySnapshot.v1".equals(fact.getType()) || fact.getOccurredAt() == null) {
            throw new IllegalArgumentException("Invalid daily fact envelope");
        }
        var id = new Identity(UUID.fromString(fact.getTenantId()), UUID.fromString(fact.getAggregateId()),
                UUID.fromString(fact.getEmployeeId()), UUID.fromString(fact.getEventId()));
        UUID.fromString(fact.getCorrelationId());
        UUID.fromString(fact.getShiftId());
        ZoneId.of(fact.getTimezone());
        if (!(id.tenant() + ":" + id.employee()).equals(key) || fact.getWorkDate() == null || fact.getShiftRevision() < 1
                || fact.getStatus() == null || !Set.of("PENDING", "ABSENT", "DAY_OFF", "HOLIDAY", "ON_LEAVE", "IN_PROGRESS",
                        "LATE", "EARLY_DEPARTURE", "LATE_AND_EARLY_DEPARTURE", "ON_TIME", "REVIEW_REQUIRED").contains(fact.getStatus())
                || fact.getDayType() == null || !Set.of("WORKING_DAY", "DAY_OFF", "HOLIDAY", "ON_LEAVE", "PARTIAL_LEAVE").contains(fact.getDayType())
                || fact.getMinutesLate() < 0 || fact.getOpenSession() && (fact.getMinutesWorked() != null || fact.getFinalized())
                || !fact.getOpenSession() && (fact.getMinutesWorked() == null || fact.getMinutesWorked() < 0)
                || fact.getClockOut() != null && (fact.getClockIn() == null || fact.getClockOut().isBefore(fact.getClockIn()))
                || fact.getLate() && (fact.getClockIn() == null || fact.getExpectedStart() == null)
                || fact.getEarlyDeparture() && (!fact.getFinalized() || fact.getOpenSession())
                || "ABSENT".equals(fact.getStatus()) && (!fact.getFinalized() || fact.getClockIn() != null)
                || fact.getReviewRequired() && (fact.getReviewReason() == null || fact.getReviewReason().isBlank())
                || fact.getLeaveReferences() == null || fact.getSessionReferences() == null) {
            throw new IllegalArgumentException("Invalid daily fact routing or effective snapshot");
        }
        for (var ref : fact.getLeaveReferences()) {
            UUID.fromString(ref.getLeaveId());
            if (Boolean.TRUE.equals(ref.getWorkRecorded()) && (fact.getClockIn() == null || !fact.getReviewRequired() || fact.getSessionReferences().isEmpty())) {
                throw new IllegalArgumentException("Explicit leave conflict requires recorded session evidence");
            }
            if (ref.getRevision() < 1 || ref.getPortion() == null || !Set.of("FULL_DAY", "MORNING", "AFTERNOON").contains(ref.getPortion())) {
                throw new IllegalArgumentException("Invalid daily leave basis");
            }
        }
        for (var ref : fact.getSessionReferences()) {
            UUID.fromString(ref.getSessionId());
            if (ref.getRevision() < 1) throw new IllegalArgumentException("Invalid daily session basis");
        }
        return id;
    }
    public record Identity(UUID tenant, UUID day, UUID employee, UUID event) {}
}
