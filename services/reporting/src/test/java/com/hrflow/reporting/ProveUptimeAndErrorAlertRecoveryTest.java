package com.hrflow.reporting;

import com.hrflow.reporting.service.ReportingAlertService;
import com.hrflow.reporting.service.ReportingAlertService.AlertState;
import com.hrflow.reporting.service.ReportingAlertService.AlertStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProveUptimeAndErrorAlertRecoveryTest {

    private ReportingAlertService alertService;

    @BeforeEach
    void setUp() {
        alertService = new ReportingAlertService(60.0, 5); // 60s max lag, 5 max export errors
    }

    @Test
    @DisplayName("W09-S28/S29: Alert fires when lag exceeds threshold and clears when lag recovers")
    void alertFiresAndRecoversOnLagThreshold() {
        // Normal baseline
        AlertState s1 = alertService.evaluate(10.0, 0);
        assertThat(s1.status()).isEqualTo(AlertStatus.OK);
        assertThat(s1.firingTimestamp()).isNull();

        // High lag -> triggers alert
        AlertState s2 = alertService.evaluate(95.0, 0);
        assertThat(s2.status()).isEqualTo(AlertStatus.FIRING);
        assertThat(s2.reason()).contains("Lag threshold exceeded");
        assertThat(s2.firingTimestamp()).isNotNull();

        // Recovery -> clears alert
        AlertState s3 = alertService.evaluate(20.0, 0);
        assertThat(s3.status()).isEqualTo(AlertStatus.CLEARED);
        assertThat(s3.clearedTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("W09-S30: Negative lag values or unbounded metrics are rejected")
    void rejectsInvalidMetricsForAlert() {
        assertThatThrownBy(() -> alertService.evaluate(-5.0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Lag seconds cannot be negative");

        assertThatThrownBy(() -> alertService.evaluate(10.0, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Export failures count cannot be negative");
    }

    @Test
    @DisplayName("RPT-25: Stream alert state is keyed by tenant and topic, and uses an injected clock")
    void streamStateIsIsolatedByTenantAndTopic() {
        Instant t0 = Instant.parse("2026-10-28T10:00:00Z");
        Instant t1 = t0.plusSeconds(30);
        AtomicReference<Instant> clockRef = new AtomicReference<>(t0);
        Clock clock = new Clock() {
            @Override
            public ZoneId getZone() { return ZoneOffset.UTC; }
            @Override
            public Clock withZone(ZoneId zone) { return this; }
            @Override
            public Instant instant() { return clockRef.get(); }
        };

        ReportingAlertService service = new ReportingAlertService(60.0, 5, clock);

        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();
        String topicAttendance = "hrflow.attendance.facts";
        String topicLeave = "hrflow.leave.facts";

        // Stream 1 (tenant1, attendance) fires at t0
        AlertState s1 = service.evaluate(tenant1, topicAttendance, 95.0, 0);
        assertThat(s1.status()).isEqualTo(AlertStatus.FIRING);
        assertThat(s1.firingTimestamp()).isEqualTo(t0);

        // Stream 2 (tenant2, attendance) is healthy at t0 -> should be OK, must NOT clear Stream 1
        AlertState s2 = service.evaluate(tenant2, topicAttendance, 10.0, 0);
        assertThat(s2.status()).isEqualTo(AlertStatus.OK);

        // Stream 3 (tenant1, leave) is also healthy -> must NOT clear Stream 1
        AlertState s3 = service.evaluate(tenant1, topicLeave, 10.0, 0);
        assertThat(s3.status()).isEqualTo(AlertStatus.OK);

        // Advance clock to t1
        clockRef.set(t1);

        // Stream 1 evaluated again with high lag -> still FIRING, firing timestamp preserved at t0
        AlertState s1StillFiring = service.evaluate(tenant1, topicAttendance, 90.0, 0);
        assertThat(s1StillFiring.status()).isEqualTo(AlertStatus.FIRING);
        assertThat(s1StillFiring.firingTimestamp()).isEqualTo(t0);

        // Stream 1 recovers at t1 -> CLEARED with firingTimestamp=t0 and clearedTimestamp=t1
        AlertState s1Recovered = service.evaluate(tenant1, topicAttendance, 10.0, 0);
        assertThat(s1Recovered.status()).isEqualTo(AlertStatus.CLEARED);
        assertThat(s1Recovered.firingTimestamp()).isEqualTo(t0);
        assertThat(s1Recovered.clearedTimestamp()).isEqualTo(t1);

        // Stream 2 and Stream 3 remain unaffected
        AlertState s2Check = service.evaluate(tenant2, topicAttendance, 10.0, 0);
        assertThat(s2Check.status()).isEqualTo(AlertStatus.OK);
    }

    @Test
    @DisplayName("RPT-25: Rejects null tenant ID or null/blank topic")
    void rejectsNullOrBlankTenantTopicIdentity() {
        assertThatThrownBy(() -> alertService.evaluate(null, "hrflow.attendance.facts", 10.0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tenant ID cannot be null");

        assertThatThrownBy(() -> alertService.evaluate(UUID.randomUUID(), null, 10.0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Topic cannot be null or blank");

        assertThatThrownBy(() -> alertService.evaluate(UUID.randomUUID(), "   ", 10.0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Topic cannot be null or blank");
    }
}
