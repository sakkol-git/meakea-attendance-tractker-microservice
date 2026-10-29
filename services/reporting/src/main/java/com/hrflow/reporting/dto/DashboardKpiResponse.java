package com.hrflow.reporting.dto;

import java.util.Map;

public record DashboardKpiResponse(
        long totalPresentToday,
        long totalLateToday,
        long totalAbsentToday,
        long activeLeavesToday,
        long pendingRequestsCount,
        Map<String, Object> summary
) {}
