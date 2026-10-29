package com.hrflow.attendance.correction;

import com.hrflow.attendance.events.StoredAttendanceEvent;
import com.hrflow.contracts.attendance.AttendanceCorrectionFact;
import com.hrflow.contracts.attendance.AttendanceCorrectionWire;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class CorrectionEventCodec {
    private final JsonMapper json;
    public AttendanceCorrectionFact encode(StoredAttendanceEvent event) {
        if (!"attendance.correction.v1".equals(event.topic())) throw new IllegalArgumentException("Invalid correction topic");
        var body = json.readValue(event.payload(), CorrectionSnapshot.class);
        var fact = AttendanceCorrectionFact.newBuilder().setEventId(event.eventId().toString()).setType(event.type()).setSchemaVersion(1)
                .setTenantId(event.tenant().toString()).setAggregateId(event.aggregate().toString()).setAggregateVersion(event.revision())
                .setOccurredAt(event.occurredAt()).setCorrelationId(event.correlation().toString()).setEmployeeId(body.employeeId().toString())
                .setWorkDate(body.workDate()).setState(body.state()).build();
        AttendanceCorrectionWire.validate(event.key(), fact);
        return fact;
    }
}
