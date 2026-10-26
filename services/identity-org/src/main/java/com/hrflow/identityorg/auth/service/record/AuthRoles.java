package com.hrflow.identityorg.auth.service.record;

import java.util.Arrays;
import java.util.List;

public final class AuthRoles {
    private static final java.util.Set<String> ALLOWED = java.util.Set.of(
            "EMPLOYEE", "MANAGER", "HR_ADMIN", "OWNER");

    private AuthRoles() {}

    public static List<String> parse(String storedRoles) {
        if (storedRoles == null || storedRoles.isBlank()) return List.of();
        List<String> roles = Arrays.stream(storedRoles.split(","))
                .map(String::strip).filter(value -> !value.isEmpty()).distinct().toList();
        if (roles.isEmpty() || !ALLOWED.containsAll(roles)) {
            throw new IllegalStateException("Auth user contains an unsupported role");
        }
        return roles;
    }
}
