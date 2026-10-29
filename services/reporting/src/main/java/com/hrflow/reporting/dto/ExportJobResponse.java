package com.hrflow.reporting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExportJobResponse(
        String id,
        String status,
        String downloadUrl,
        String fileName,
        int rowCount,
        long version
) {}
