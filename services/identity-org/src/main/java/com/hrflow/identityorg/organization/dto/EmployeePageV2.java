package com.hrflow.identityorg.organization.dto;

import java.util.List;

public record EmployeePageV2(List<EmployeeRecordV2> items, int page, int size, long total) {}
