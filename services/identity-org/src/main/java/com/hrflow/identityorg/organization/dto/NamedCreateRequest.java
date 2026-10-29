package com.hrflow.identityorg.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NamedCreateRequest(@NotBlank @Size(max = 120) String name,
                                 @Size(max = 64) String code) {}
