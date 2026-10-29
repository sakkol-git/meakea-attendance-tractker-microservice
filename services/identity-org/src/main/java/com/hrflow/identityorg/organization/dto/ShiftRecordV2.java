package com.hrflow.identityorg.organization.dto;

import java.time.LocalTime;
import java.util.UUID;

public record ShiftRecordV2(UUID id, String name, LocalTime startTime, LocalTime endTime,
        int graceMinutes, int workingDays, UUID siteId, String timezone, long version,
        String lastChangeReason) {}
