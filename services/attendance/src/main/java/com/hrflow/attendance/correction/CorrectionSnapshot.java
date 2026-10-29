package com.hrflow.attendance.correction;

import java.time.LocalDate;
import java.util.UUID;

public record CorrectionSnapshot(UUID employeeId, LocalDate workDate, String state) {}
