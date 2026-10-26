package com.hrflow.identityorg.profile.dto;

import java.util.List;

public record UserProfileResponse(
        String userId,
        String employeeId,
        String tenantId,
        List<String> roles,
        String name,
        String email,
        String timeZone,
        String locale
) {}
