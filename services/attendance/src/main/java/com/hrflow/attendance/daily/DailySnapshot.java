package com.hrflow.attendance.daily;

import com.hrflow.attendance.dto.DailyStatusResponse;
import java.util.List;
import java.util.UUID;

/** Stable input fingerprint excludes delivery metadata and the evaluation clock. */
public record DailySnapshot(DailyStatusResponse overview, String timezone, UUID shiftId, long shiftRevision,
        java.time.Instant scheduledStart, java.time.Instant scheduledEnd,
        @com.fasterxml.jackson.annotation.JsonIgnore Integer graceMinutes,
        @com.fasterxml.jackson.annotation.JsonIgnore Integer workingDays,
        List<LeaveBasis> leaveReferences, List<SessionBasis> sessionReferences) {
    public record LeaveBasis(UUID leaveId, long revision, String portion, Boolean workRecorded) {
        public LeaveBasis(UUID leaveId, long revision, String portion) { this(leaveId, revision, portion, null); }
    }
    public record SessionBasis(UUID sessionId, long revision) {}
}
