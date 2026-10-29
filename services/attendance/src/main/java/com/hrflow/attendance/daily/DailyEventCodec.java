package com.hrflow.attendance.daily;

import com.hrflow.attendance.events.StoredAttendanceEvent;
import com.hrflow.contracts.attendance.AttendanceDailyFact;
import com.hrflow.contracts.attendance.DailyLeaveReference;
import com.hrflow.contracts.attendance.DailySessionReference;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Contextual envelope creation validates the daily owner snapshot; no business rule is guessed. */
@Component
@RequiredArgsConstructor
public class DailyEventCodec {
    private final JsonMapper json;

    public AttendanceDailyFact encode(StoredAttendanceEvent event) {
        var body = json.readValue(event.payload(), DailySnapshot.class);
        var day = body.overview();
        if (!"attendance.daily-status.v1".equals(event.topic()) || !"AttendanceDailySnapshot.v1".equals(event.type())
                || event.revision() < 1 || event.eventId() == null || event.tenant() == null || event.aggregate() == null
                || event.correlation() == null || event.occurredAt() == null || day == null || day.employeeId() == null
                || !(event.tenant() + ":" + day.employeeId()).equals(event.key()) || day.date() == null
                || body.timezone() == null || body.shiftId() == null || body.shiftRevision() < 1
                || body.leaveReferences() == null || body.sessionReferences() == null
                || day.dayType() == null || !Set.of("WORKING_DAY", "DAY_OFF", "HOLIDAY", "ON_LEAVE", "PARTIAL_LEAVE").contains(day.dayType())
                || day.status() == null || !Set.of("PENDING", "ABSENT", "DAY_OFF", "HOLIDAY", "ON_LEAVE", "IN_PROGRESS",
                        "LATE", "EARLY_DEPARTURE", "LATE_AND_EARLY_DEPARTURE", "ON_TIME", "REVIEW_REQUIRED").contains(day.status())
                || day.openSession() && (day.minutesWorked() != null || day.finalized())
                || !day.openSession() && (day.minutesWorked() == null || day.minutesWorked() < 0)
                || day.reviewRequired() && (day.reviewReason() == null || day.reviewReason().isBlank())
                || day.clockOut() != null && (day.clockIn() == null || day.clockOut().isBefore(day.clockIn()))
                || day.late() && (day.clockIn() == null || day.expectedStart() == null)
                || day.earlyDeparture() && (!day.finalized() || day.openSession())
                || body.leaveReferences().stream().anyMatch(ref -> ref.leaveId() == null || ref.revision() < 1
                        || ref.portion() == null || !Set.of("FULL_DAY", "MORNING", "AFTERNOON").contains(ref.portion()))
                || body.sessionReferences().stream().anyMatch(ref -> ref.sessionId() == null || ref.revision() < 1)) {
            throw new IllegalArgumentException("Invalid daily event envelope or effective snapshot");
        }
        ZoneId.of(body.timezone());
        int lateMinutes = day.late() ? Math.toIntExact(Duration.between(day.expectedStart(), day.clockIn()).toMinutes()) : 0;
        return AttendanceDailyFact.newBuilder().setEventId(event.eventId().toString()).setType(event.type()).setSchemaVersion(1)
                .setTenantId(event.tenant().toString()).setAggregateId(event.aggregate().toString()).setAggregateVersion(event.revision())
                .setOccurredAt(event.occurredAt()).setCorrelationId(event.correlation().toString()).setCausationId(null)
                .setEmployeeId(day.employeeId().toString()).setWorkDate(day.date()).setTimezone(body.timezone())
                .setClockIn(day.clockIn()).setClockOut(day.clockOut()).setExpectedStart(day.expectedStart()).setExpectedEnd(day.expectedEnd())
                .setStatus(day.status()).setDayType(day.dayType()).setLate(day.late()).setEarlyDeparture(day.earlyDeparture())
                .setReviewRequired(day.reviewRequired()).setReviewReason(day.reviewReason()).setFinalized(day.finalized())
                .setOpenSession(day.openSession()).setMinutesWorked(day.minutesWorked()).setMinutesLate(lateMinutes)
                .setShiftId(body.shiftId().toString()).setShiftRevision(body.shiftRevision())
                .setLeaveReferences(body.leaveReferences().stream().map(ref -> DailyLeaveReference.newBuilder()
                        .setLeaveId(ref.leaveId().toString()).setRevision(ref.revision()).setPortion(ref.portion()).setWorkRecorded(ref.workRecorded()).build()).toList())
                .setSessionReferences(body.sessionReferences().stream().map(ref -> DailySessionReference.newBuilder()
                        .setSessionId(ref.sessionId().toString()).setRevision(ref.revision()).build()).toList()).build();
    }
}
