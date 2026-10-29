package com.hrflow.attendance.events;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Minimal effective session snapshot; deliberately excludes device/location evidence. */
public record SessionPayload(UUID employeeId, LocalDate workDate, String timezone, Instant clockIn,
        Instant clockOut, String state, Instant scheduledStart, Instant scheduledEnd, Integer graceMinutes,
        String source) {}
