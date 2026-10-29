package com.hrflow.identityorg.organization.dto;

import java.time.LocalDate;
import java.util.UUID;

public record HolidayRecordV2(UUID id, String name, LocalDate date, String kind, long version) {}
