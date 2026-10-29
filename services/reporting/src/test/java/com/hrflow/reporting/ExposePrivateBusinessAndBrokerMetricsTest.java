package com.hrflow.reporting;

import com.hrflow.reporting.service.ReportingMetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExposePrivateBusinessAndBrokerMetricsTest {

    private MeterRegistry meterRegistry;
    private ReportingMetricsService metricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new ReportingMetricsService(meterRegistry);
    }

    @Test
    @DisplayName("W09-S22/S23: Record export failure and event processed with low-cardinality bounded labels")
    void recordMetricsWithBoundedLabels() {
        metricsService.recordExportFailure("TIMEOUT");
        metricsService.recordExportFailure("IO_ERROR");
        metricsService.recordFactProcessed("ATTENDANCE");
        metricsService.recordLagSeconds(15.5);

        double timeoutFailures = meterRegistry.get("hrflow.reporting.export.failures")
                .tag("reason", "TIMEOUT")
                .counter()
                .count();
        assertThat(timeoutFailures).isEqualTo(1.0);

        double factsProcessed = meterRegistry.get("hrflow.reporting.facts.processed")
                .tag("event_type", "ATTENDANCE")
                .counter()
                .count();
        assertThat(factsProcessed).isEqualTo(1.0);

        double lag = meterRegistry.get("hrflow.reporting.projection.lag.seconds")
                .gauge()
                .value();
        assertThat(lag).isEqualTo(15.5);
    }

    @Test
    @DisplayName("W09-S24: Rejects unbounded or high-cardinality sensitive labels to prevent metrics leakage")
    void rejectsUnboundedOrSensitiveLabels() {
        assertThatThrownBy(() -> metricsService.recordExportFailure("user_12345_sensitive_session_id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Disallowed or unbounded failure reason");
    }
}
