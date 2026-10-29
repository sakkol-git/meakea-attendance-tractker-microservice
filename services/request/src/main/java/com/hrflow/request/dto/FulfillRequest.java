package com.hrflow.request.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FulfillRequest {
    private String handoverReason;
    private String handoverNotes;
    private String notes;

    public String effectiveReason() {
        if (handoverReason != null && !handoverReason.isBlank()) {
            return handoverReason;
        }
        return handoverNotes;
    }
}
