package com.hrflow.identityorg.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "tenant is required")
        @Pattern(regexp = "[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?", message = "tenant must be alphanumeric")
        String tenant,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        @Size(max = 254)
        String email,

        @NotBlank(message = "password is required")
        @Size(max = 1024)
        String password
) {}
