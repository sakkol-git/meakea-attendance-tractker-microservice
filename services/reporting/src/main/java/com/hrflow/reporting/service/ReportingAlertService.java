package com.hrflow.reporting.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ReportingAlertService {

    public enum AlertStatus {
        OK,
        FIRING,
        CLEARED
    }

    public record AlertState(
            AlertStatus status,
            String reason,
            Instant firingTimestamp,
            Instant clearedTimestamp
    ) {}

    public record StreamKey(UUID tenantId, String topic) {
        public StreamKey {
            if (tenantId == null) {
                throw new IllegalArgumentException("Tenant ID cannot be null");
            }
            if (topic == null || topic.isBlank()) {
                throw new IllegalArgumentException("Topic cannot be null or blank");
            }
        }
    }

    private static class StreamState {
        boolean isCurrentlyFiring = false;
        Instant lastFiringTimestamp = null;
        Instant lastClearedTimestamp = null;
    }

    private static final UUID DEFAULT_TENANT_ID = new UUID(0L, 0L);
    private static final String DEFAULT_TOPIC = "default";

    private final double maxLagSeconds;
    private final int maxExportFailures;
    private final Clock clock;
    private final Map<StreamKey, StreamState> streamStates = new ConcurrentHashMap<>();

    public ReportingAlertService(
            @Value("${hrflow.reporting.alerts.max-lag-seconds:60.0}") double maxLagSeconds,
            @Value("${hrflow.reporting.alerts.max-export-failures:5}") int maxExportFailures
    ) {
        this(maxLagSeconds, maxExportFailures, Clock.systemUTC());
    }

    public ReportingAlertService(
            double maxLagSeconds,
            int maxExportFailures,
            Clock clock
    ) {
        this.maxLagSeconds = maxLagSeconds;
        this.maxExportFailures = maxExportFailures;
        this.clock = (clock != null) ? clock : Clock.systemUTC();
    }

    @Autowired
    public ReportingAlertService(
            @Value("${hrflow.reporting.alerts.max-lag-seconds:60.0}") double maxLagSeconds,
            @Value("${hrflow.reporting.alerts.max-export-failures:5}") int maxExportFailures,
            Optional<Clock> clock
    ) {
        this(maxLagSeconds, maxExportFailures, clock != null ? clock.orElse(Clock.systemUTC()) : Clock.systemUTC());
    }

    public Clock getClock() {
        return this.clock;
    }

    public AlertState evaluate(double lagSeconds, int exportFailures) {
        return evaluate(DEFAULT_TENANT_ID, DEFAULT_TOPIC, lagSeconds, exportFailures);
    }

    public synchronized AlertState evaluate(UUID tenantId, String topic, double lagSeconds, int exportFailures) {
        StreamKey key = new StreamKey(tenantId, topic);

        if (lagSeconds < 0) {
            throw new IllegalArgumentException("Lag seconds cannot be negative");
        }
        if (exportFailures < 0) {
            throw new IllegalArgumentException("Export failures count cannot be negative");
        }

        StreamState state = streamStates.computeIfAbsent(key, k -> new StreamState());

        boolean exceedsLag = lagSeconds > maxLagSeconds;
        boolean exceedsFailures = exportFailures > maxExportFailures;

        if (exceedsLag || exceedsFailures) {
            if (!state.isCurrentlyFiring) {
                state.isCurrentlyFiring = true;
                state.lastFiringTimestamp = Instant.now(this.clock);
            }
            StringBuilder reason = new StringBuilder();
            if (exceedsLag) {
                reason.append("Lag threshold exceeded: ").append(lagSeconds).append("s > ").append(maxLagSeconds).append("s");
            }
            if (exceedsFailures) {
                if (!reason.isEmpty()) reason.append("; ");
                reason.append("Export failures exceeded: ").append(exportFailures).append(" > ").append(maxExportFailures);
            }
            return new AlertState(AlertStatus.FIRING, reason.toString(), state.lastFiringTimestamp, null);
        }

        if (state.isCurrentlyFiring) {
            state.isCurrentlyFiring = false;
            state.lastClearedTimestamp = Instant.now(this.clock);
            return new AlertState(AlertStatus.CLEARED, "Metrics normalized below alerting thresholds", state.lastFiringTimestamp, state.lastClearedTimestamp);
        }

        return new AlertState(AlertStatus.OK, "All reporting systems healthy", null, null);
    }
}
