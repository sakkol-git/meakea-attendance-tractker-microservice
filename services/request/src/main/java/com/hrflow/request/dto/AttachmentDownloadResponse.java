package com.hrflow.request.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDownloadResponse {
    private String downloadUrl;
    private String fileName;
    private String contentType;
    private long fileSize;
    private String checksumSha256;
}
