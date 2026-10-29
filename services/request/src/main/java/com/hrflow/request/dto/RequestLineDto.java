package com.hrflow.request.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@lombok.Getter
@lombok.Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestLineDto {
    private String id;

    @NotBlank
    @com.fasterxml.jackson.annotation.JsonAlias("materialId")
    private String materialItemId;

    public String getMaterialId() { return materialItemId; }

    @Min(1)
    @Builder.Default
    private int quantity = 1;

    private String notes;
}
