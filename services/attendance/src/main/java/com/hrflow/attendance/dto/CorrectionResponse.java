package com.hrflow.attendance.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CorrectionResponse(
    UUID id,
    UUID tenantId,
    UUID employeeId,
    UUID sessionId,
    LocalDate date,
    Instant proposedClockIn,
    Instant proposedClockOut,
    String reason,
    String status,
    UUID reviewedBy,
    Instant reviewedAt,
    long version,
    Instant createdAt,
    java.util.List<com.hrflow.attendance.entity.OriginalAttendanceSession> previousSessions
) {}
