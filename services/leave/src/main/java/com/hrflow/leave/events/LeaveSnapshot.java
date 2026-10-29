package com.hrflow.leave.events;

import java.time.LocalDate;
import java.util.List;

/** Minimal owner facts; no employee name, reason, medical note or supporting file data. */
public record LeaveSnapshot(String employeeId, String leaveType, LocalDate startDate, LocalDate endDate,
        String portion, String chargedDays, List<LocalDate> workingDates, String state) {}
