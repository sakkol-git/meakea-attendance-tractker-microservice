package com.hrflow.request.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@lombok.Getter
@lombok.Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponse {
    private String id;
    private String fileName;
    private String contentType;
    private long fileSize;
    private String checksumSha256;
    private String status;
    private Instant createdAt;
    private Instant completedAt;
    private long version;
    private String title;
    private java.util.Map<String, Object> details;
    private java.util.List<Object> history;
}
