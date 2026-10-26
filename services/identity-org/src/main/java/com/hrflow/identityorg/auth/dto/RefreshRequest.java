package com.hrflow.identityorg.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshRequest(
        @NotBlank(message = "refreshToken is required")
        @Size(min = 32, max = 512, message = "refreshToken must be between 32 and 512 characters")
        String refreshToken
) {}
