package com.hrflow.request.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentPresignResponse {
    private String attachmentId;
    private String uploadUrl;
    private Map<String, String> requiredHeaders;
}
