package com.hrflow.reporting.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;

@Service
public class ReportingMetricsService {

    private static final Set<String> ALLOWED_REASONS = Set.of(
            "TIMEOUT",
            "IO_ERROR",
            "GENERATION_FAILED",
            "STORAGE_UNAVAILABLE"
    );

    private static final Set<String> ALLOWED_EVENT_TYPES = Set.of(
            "ATTENDANCE",
            "LEAVE",
            "REQUEST"
    );

    private final MeterRegistry meterRegistry;
    private final AtomicReference<Double> currentLagSeconds = new AtomicReference<>(0.0);

    public ReportingMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.meterRegistry.gauge("hrflow.reporting.projection.lag.seconds", currentLagSeconds, AtomicReference::get);
    }

    public void recordExportFailure(String reason) {
        if (reason == null || !ALLOWED_REASONS.contains(reason)) {
            throw new IllegalArgumentException("Disallowed or unbounded failure reason: " + reason);
        }
        Counter.builder("hrflow.reporting.export.failures")
                .description("Number of export job failures")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    public void recordFactProcessed(String eventType) {
        if (eventType == null || !ALLOWED_EVENT_TYPES.contains(eventType)) {
            throw new IllegalArgumentException("Disallowed or unbounded event type: " + eventType);
        }
        Counter.builder("hrflow.reporting.facts.processed")
                .description("Number of ingested domain facts projected into reporting schema")
                .tag("event_type", eventType)
                .register(meterRegistry)
                .increment();
    }

    public void recordLagSeconds(double lag) {
        currentLagSeconds.set(lag);
    }
}
