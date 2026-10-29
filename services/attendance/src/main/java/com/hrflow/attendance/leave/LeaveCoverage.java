package com.hrflow.attendance.leave;

/** Calendar portions supplied by the leave owner, never inferred from a date interval. */
public record LeaveCoverage(boolean morning, boolean afternoon) {
    public boolean fullDay() { return morning && afternoon; }
    public boolean any() { return morning || afternoon; }
}
