package com.hrflow.request.dto;

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
public class UpdateGeneralRequestDto implements RequestFormInput {
    private String title;
    private String reason;
    private Integer quantity;
    private String formData;
    private String startDate;
    private String endDate;
    private String fromTime;
    private String toTime;
    @com.fasterxml.jackson.annotation.JsonAlias("items")
    private List<@jakarta.validation.Valid RequestLineDto> lines;
    private List<ApprovalStepDto> approvers;
}
