package com.hrflow.attendance.dto;

import java.time.Instant;
import java.util.UUID;

public record AttendanceSessionResponse(
        UUID id,
        UUID employeeId,
        Instant clockIn,
        Instant clockOut,
        String status,
        Long version
) {}
