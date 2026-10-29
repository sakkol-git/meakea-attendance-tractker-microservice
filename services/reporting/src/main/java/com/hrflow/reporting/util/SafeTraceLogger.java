package com.hrflow.reporting.util;

import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SafeTraceLogger {

    private static final Pattern SENSITIVE_KEY_VALUE = Pattern.compile(
            "(?i)(password|secret|token|ssn|authorization)\\s*=\\s*[^,\\s&]+");

    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");

    public String formatSafeLog(UUID tenantId, String correlationId, String message) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID must not be null");
        }
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException("Correlation ID must not be null or blank");
        }

        String sanitized = message != null ? message : "";
        sanitized = SENSITIVE_KEY_VALUE.matcher(sanitized).replaceAll("$1=[REDACTED]");
        sanitized = SSN_PATTERN.matcher(sanitized).replaceAll("[REDACTED]");

        return "[tenant=" + tenantId + ", correlationId=" + correlationId + "] " + sanitized;
    }
}
