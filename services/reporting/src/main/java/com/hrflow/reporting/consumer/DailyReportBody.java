package com.hrflow.reporting.consumer;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DailyReportBody(UUID employeeId, LocalDate workDate, String timezone, String status, String dayType,
        Instant clockIn, Instant clockOut, Integer minutesWorked, int minutesLate, boolean late,
        boolean earlyDeparture, boolean reviewRequired, boolean finalized, boolean openSession,
        Instant expectedStart, Instant expectedEnd, UUID shiftId, long shiftRevision,
        List<LeaveBasis> leaveReferences, List<SessionBasis> sessionReferences) {
    public record LeaveBasis(UUID leaveId, long revision, String portion) {}
    public record SessionBasis(UUID sessionId, long revision) {}
}
