package com.hrflow.request.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@lombok.Getter
@lombok.Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGeneralRequestDto implements RequestFormInput {
    @NotBlank
    private String title;

    @NotBlank
    private String type;

    @NotBlank
    private String reason;

    @Min(1)
    @Builder.Default
    private Integer quantity = 1;

    public int getQuantity() { return quantity == null ? 1 : quantity; }

    private String formData;
    private String startDate;
    private String endDate;
    private String fromTime;
    private String toTime;

    @com.fasterxml.jackson.annotation.JsonAlias("items")
    private List<@jakarta.validation.Valid RequestLineDto> lines;

    private List<ApprovalStepDto> approvers;
}
