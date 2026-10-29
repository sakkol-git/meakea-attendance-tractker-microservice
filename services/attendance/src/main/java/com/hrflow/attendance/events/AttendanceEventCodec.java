package com.hrflow.attendance.events;

import com.hrflow.attendance.daily.DailyEventCodec;
import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AttendanceEventCodec {
    private final SessionEventCodec sessions;
    private final com.hrflow.attendance.correction.CorrectionEventCodec corrections;
    private final DailyEventCodec days;
    private final com.hrflow.attendance.daily.ShiftCutoffCodec cutoffs;

    public SpecificRecord encode(StoredAttendanceEvent event) {
        return switch (event.topic()) {
            case "attendance.correction.v1" -> corrections.encode(event);
            case "attendance.session.v1" -> sessions.encode(event);
            case "attendance.daily-status.v1" -> days.encode(event);
            case "time.shift-cutoff.v1" -> cutoffs.encode(event);
            default -> throw new IllegalArgumentException("Unsupported attendance event topic");
        };
    }
}
