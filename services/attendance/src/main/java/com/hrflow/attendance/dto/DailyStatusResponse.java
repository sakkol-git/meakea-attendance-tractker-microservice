package com.hrflow.attendance.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record DailyStatusResponse(
    UUID employeeId,
    LocalDate date,
    LocalTime shiftStart,
    LocalTime shiftEnd,
    Integer graceMinutes,
    Instant clockIn,
    Instant clockOut,
    boolean late,
    boolean earlyDeparture,
    boolean reviewRequired,
    String reviewReason,
    String status,
    Integer minutesWorked,
    boolean finalized,
    String dayType,
    boolean openSession,
    Instant expectedStart,
    Instant expectedEnd
) {}
