package com.hrflow.request.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@lombok.Getter
@lombok.Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentCompleteRequest {
    @com.fasterxml.jackson.annotation.JsonAlias("sha256")
    private String checksumSha256;
    @com.fasterxml.jackson.annotation.JsonAlias("size")
    private Long fileSize;
}
