package com.hrflow.attendance.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Actual effective facts captured at correction submission, never reconstructed at review time. */
public record OriginalAttendanceSession(UUID id, Instant clockIn, Instant clockOut, String status, long version,
        LocalDate workDate, String timezone, UUID shiftId, Long shiftRevision, String source) {}
