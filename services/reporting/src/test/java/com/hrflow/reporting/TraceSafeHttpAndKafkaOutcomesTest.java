package com.hrflow.reporting;

import com.hrflow.reporting.util.SafeTraceLogger;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TraceSafeHttpAndKafkaOutcomesTest {

    @Test
    @DisplayName("W09-S25/S26: Correlation ID and technical tenant are captured; sentinel secrets are strictly scrubbed")
    void correlationTrackedAndSecretsScrubbed() {
        SafeTraceLogger logger = new SafeTraceLogger();
        UUID tenantId = UUID.randomUUID();
        String correlationId = "corr-" + UUID.randomUUID();

        String rawMessage = "Processing export job for tenant=" + tenantId + " with password=SUPER_SECRET_PWD and ssn=123-45-6789";
        String sanitized = logger.formatSafeLog(tenantId, correlationId, rawMessage);

        assertThat(sanitized).contains("tenant=" + tenantId);
        assertThat(sanitized).contains("correlationId=" + correlationId);
        assertThat(sanitized).doesNotContain("SUPER_SECRET_PWD");
        assertThat(sanitized).doesNotContain("123-45-6789");
        assertThat(sanitized).contains("[REDACTED]");
    }

    @Test
    @DisplayName("W09-S27: Null correlation ID or missing tenant is rejected to prevent un-traceable operations")
    void missingTracingContextRejected() {
        SafeTraceLogger logger = new SafeTraceLogger();
        assertThatThrownBy(() -> logger.formatSafeLog(null, "corr-1", "message"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tenant ID must not be null");

        assertThatThrownBy(() -> logger.formatSafeLog(UUID.randomUUID(), null, "message"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Correlation ID must not be null or blank");
    }
}
