package com.hrflow.reporting.dto;

import java.util.List;

public record ExportJobListResponse(
        List<ExportJobResponse> items,
        int totalCount
) {}
