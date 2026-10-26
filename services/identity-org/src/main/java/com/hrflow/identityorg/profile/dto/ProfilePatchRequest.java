package com.hrflow.identityorg.profile.dto;

import jakarta.validation.constraints.Size;

public record ProfilePatchRequest(
        @Size(min = 2, max = 120) String name,
        @Size(min = 1, max = 255) String timeZone
) {}
