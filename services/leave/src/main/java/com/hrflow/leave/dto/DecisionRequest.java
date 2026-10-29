package com.hrflow.leave.dto;

public record DecisionRequest(
        String reason,
        String note
) {
}
