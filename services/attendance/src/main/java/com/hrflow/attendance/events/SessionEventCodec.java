package com.hrflow.attendance.events;

import com.hrflow.contracts.attendance.AttendanceSessionFact;
import java.time.ZoneId;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Contextual event construction validates stored envelope and snapshot together. */
@Component
@RequiredArgsConstructor
public class SessionEventCodec {
    private final JsonMapper json;

    public AttendanceSessionFact encode(StoredAttendanceEvent event) {
        SessionPayload body = json.readValue(event.payload(), SessionPayload.class);
        if (event.revision() < 1 || !SessionEventWriter.TYPE.equals(event.type())
                || !SessionEventWriter.TOPIC.equals(event.topic()) || event.eventId() == null
                || event.tenant() == null || event.aggregate() == null || event.correlation() == null
                || event.occurredAt() == null || body.employeeId() == null
                || body.source() == null || !Set.of("TAP", "MANUAL").contains(body.source())
                || !(event.tenant() + ":" + body.employeeId()).equals(event.key())) {
            throw new IllegalArgumentException("Invalid stored session event envelope");
        }
        if (body.clockIn() == null || body.workDate() == null || body.timezone() == null
                || body.scheduledStart() == null || body.scheduledEnd() == null
                || !body.scheduledEnd().isAfter(body.scheduledStart()) || body.graceMinutes() == null
                || body.graceMinutes() < 0 || body.graceMinutes() > 120
                || !body.workDate().equals(body.clockIn().atZone(ZoneId.of(body.timezone())).toLocalDate())
                || body.clockOut() != null && body.clockOut().isBefore(body.clockIn())
                || !(body.clockOut() == null ? "OPEN" : "COMPLETED").equals(body.state())) {
            throw new IllegalArgumentException("Invalid stored effective session snapshot");
        }
        return AttendanceSessionFact.newBuilder().setEventId(event.eventId().toString())
                .setType(event.type()).setSchemaVersion(1).setTenantId(event.tenant().toString())
                .setAggregateId(event.aggregate().toString()).setAggregateVersion(event.revision())
                .setOccurredAt(event.occurredAt()).setCorrelationId(event.correlation().toString())
                .setCausationId(null).setEmployeeId(body.employeeId().toString()).setWorkDate(body.workDate())
                .setTimezone(body.timezone()).setClockIn(body.clockIn()).setClockOut(body.clockOut())
                .setState(body.state()).setScheduledStart(body.scheduledStart()).setScheduledEnd(body.scheduledEnd())
                .setGraceMinutes(body.graceMinutes()).setSource(body.source()).build();
    }

}
