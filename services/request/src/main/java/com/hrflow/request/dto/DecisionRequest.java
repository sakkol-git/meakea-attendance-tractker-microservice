package com.hrflow.request.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionRequest {
    private String reason;
    private String decisionReason;
    private String note;

    public String effectiveReason() {
        if (reason != null && !reason.isBlank()) {
            return reason;
        }
        return decisionReason;
    }
}
