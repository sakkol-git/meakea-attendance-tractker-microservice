package com.hrflow.request.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalStepDto {
    private String id;
    private int stepOrder;
    private String approverId;
    private String approverName;
    private String approverRole;
    private String status;
    private String decisionReason;
    private String decidedAt;
    private String decidedByAccount;
}
