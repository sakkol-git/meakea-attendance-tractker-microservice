package com.hrflow.attendance.dto;

import java.util.List;

public record TodayAttendanceResponse(
    String date,
    String status,
    List<AttendanceSessionResponse> sessions
) {}
