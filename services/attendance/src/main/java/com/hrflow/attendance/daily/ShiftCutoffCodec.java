package com.hrflow.attendance.daily;

import com.hrflow.attendance.events.StoredAttendanceEvent;
import com.hrflow.contracts.attendance.ShiftCutoffFact;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class ShiftCutoffCodec {
    private final JsonMapper json;

    public ShiftCutoffFact encode(StoredAttendanceEvent event) {
        var body = json.readValue(event.payload(), ShiftCutoff.Payload.class);
        if (!"time.shift-cutoff.v1".equals(event.topic()) || !"ShiftCutoff.v1".equals(event.type()) || event.revision() != 1
                || event.eventId() == null || event.tenant() == null || event.aggregate() == null
                || event.correlation() == null || event.occurredAt() == null || body.employeeId() == null
                || !(event.tenant() + ":" + body.employeeId()).equals(event.key()) || body.workDate() == null
                || body.shiftId() == null || body.shiftRevision() < 1 || body.scheduledEnd() == null || body.cutoffAt() == null
                || !body.cutoffAt().equals(body.scheduledEnd().plusSeconds(30 * 60L))
                || event.occurredAt().isBefore(body.cutoffAt())
                || !body.workDate().equals(body.scheduledEnd().atZone(ZoneId.of(body.timezone())).toLocalDate())) {
            throw new IllegalArgumentException("Invalid shift cutoff envelope or captured basis");
        }
        return ShiftCutoffFact.newBuilder().setEventId(event.eventId().toString()).setType(event.type()).setSchemaVersion(1)
                .setTenantId(event.tenant().toString()).setAggregateId(event.aggregate().toString()).setAggregateVersion(1)
                .setOccurredAt(event.occurredAt()).setCorrelationId(event.correlation().toString()).setCausationId(null)
                .setEmployeeId(body.employeeId().toString()).setWorkDate(body.workDate()).setTimezone(body.timezone())
                .setShiftId(body.shiftId().toString()).setShiftRevision(body.shiftRevision())
                .setScheduledEnd(body.scheduledEnd()).setCutoffAt(body.cutoffAt()).build();
    }
}
