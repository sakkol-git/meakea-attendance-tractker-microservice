package com.hrflow.request.dto;

import java.time.Instant;
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
public class GeneralRequestResponse {
    private String id;
    private String tenantId;
    private String employeeId;
    private String employeeName;
    private String type;
    private String title;
    private String reason;
    private String status;
    private int quantity;
    private String formData;
    @com.fasterxml.jackson.annotation.JsonUnwrapped
    private RequestForm form;
    private List<RequestLineDto> items;
    private String handoverNotes;
    private long version;
    private Instant createdAt;
    private Instant updatedAt;

    private List<RequestLineDto> lines;
    private List<ApprovalStepDto> steps;
    private List<CommentResponse> comments;
    private List<AttachmentResponse> attachments;
}
