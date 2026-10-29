package com.hrflow.request.dto;

import jakarta.validation.constraints.Max;
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
public class AttachmentPresignRequest {
    @NotBlank
    private String fileName;

    @NotBlank
    private String contentType;

    @Min(1)
    @Max(10485760) // 10 MiB limit
    @com.fasterxml.jackson.annotation.JsonAlias("size")
    private long fileSize;

    @NotBlank
    @jakarta.validation.constraints.Pattern(regexp = "[0-9a-fA-F]{64}")
    @com.fasterxml.jackson.annotation.JsonAlias("sha256")
    private String checksumSha256;
}
