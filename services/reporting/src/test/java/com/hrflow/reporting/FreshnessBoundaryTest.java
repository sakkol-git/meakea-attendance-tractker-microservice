package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hrflow.reporting.dto.FreshnessStatus;
import com.hrflow.reporting.entity.CheckpointProgress;
import com.hrflow.reporting.repository.CheckpointProgressRepository;
import com.hrflow.reporting.service.FreshnessService;
import com.hrflow.reporting.service.TenantSqlScope;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FreshnessBoundaryTest {

    private static final Instant T0 = Instant.parse("2026-06-01T12:00:00Z");
    private final Clock fixedClock = Clock.fixed(T0, ZoneOffset.UTC);

    private CheckpointProgressRepository checkpointRepo;
    private TenantSqlScope tenantScope;
    private FreshnessService freshnessService;

    @BeforeEach
    void setUp() {
        checkpointRepo = mock(CheckpointProgressRepository.class);
        tenantScope = mock(TenantSqlScope.class);
        freshnessService = new FreshnessService(checkpointRepo, tenantScope, fixedClock);
    }

    @Test
    @DisplayName("RPT-18: Exact lag boundary at 300 seconds deterministically distinguishes healthy vs unhealthy")
    void exactLagBoundaryAt300Seconds() {
        UUID tenantId = UUID.randomUUID();
        String topic = "hrflow.attendance.facts";

        // Boundary 1: 299 seconds ago (under 300s threshold) -> HEALTHY
        Instant tMinus299 = T0.minusSeconds(299);
        CheckpointProgress cp299 = new CheckpointProgress(tenantId, topic, 0, 100L, tMinus299, 10);
        when(checkpointRepo.findByTenantAndTopic(tenantId, topic)).thenReturn(List.of(cp299));

        FreshnessStatus status299 = freshnessService.getFreshness(tenantId, topic);
        assertThat(status299.lagSeconds())
                .as("Injected clock must evaluate exact 299s lag")
                .isEqualTo(299L);
        assertThat(status299.isHealthy())
                .as("Lag < 300s must be healthy")
                .isTrue();

        // Boundary 2: 300 seconds ago (exactly at 300s threshold) -> UNHEALTHY
        Instant tMinus300 = T0.minusSeconds(300);
        CheckpointProgress cp300 = new CheckpointProgress(tenantId, topic, 0, 101L, tMinus300, 10);
        when(checkpointRepo.findByTenantAndTopic(tenantId, topic)).thenReturn(List.of(cp300));

        FreshnessStatus status300 = freshnessService.getFreshness(tenantId, topic);
        assertThat(status300.lagSeconds())
                .as("Injected clock must evaluate exact 300s lag")
                .isEqualTo(300L);
        assertThat(status300.isHealthy())
                .as("Lag == 300s must be unhealthy")
                .isFalse();

        // Boundary 3: 301 seconds ago (over 300s threshold) -> UNHEALTHY
        Instant tMinus301 = T0.minusSeconds(301);
        CheckpointProgress cp301 = new CheckpointProgress(tenantId, topic, 0, 102L, tMinus301, 10);
        when(checkpointRepo.findByTenantAndTopic(tenantId, topic)).thenReturn(List.of(cp301));

        FreshnessStatus status301 = freshnessService.getFreshness(tenantId, topic);
        assertThat(status301.lagSeconds())
                .as("Injected clock must evaluate exact 301s lag")
                .isEqualTo(301L);
        assertThat(status301.isHealthy())
                .as("Lag > 300s must be unhealthy")
                .isFalse();
    }

    @Test
    @DisplayName("RPT-18: Exact pending intents boundary at 1000 distinguishes healthy vs unhealthy")
    void exactPendingIntentsBoundaryAt1000() {
        UUID tenantId = UUID.randomUUID();
        String topic = "hrflow.attendance.facts";
        Instant tMinus10 = T0.minusSeconds(10); // lag is only 10s

        // Boundary 1: 999 pending intents (under 1000 threshold) -> HEALTHY
        CheckpointProgress cp999 = new CheckpointProgress(tenantId, topic, 0, 100L, tMinus10, 999);
        when(checkpointRepo.findByTenantAndTopic(tenantId, topic)).thenReturn(List.of(cp999));

        FreshnessStatus status999 = freshnessService.getFreshness(tenantId, topic);
        assertThat(status999.pendingIntents()).isEqualTo(999);
        assertThat(status999.lagSeconds()).isEqualTo(10L);
        assertThat(status999.isHealthy()).isTrue();

        // Boundary 2: 1000 pending intents (at 1000 threshold) -> UNHEALTHY
        CheckpointProgress cp1000 = new CheckpointProgress(tenantId, topic, 0, 101L, tMinus10, 1000);
        when(checkpointRepo.findByTenantAndTopic(tenantId, topic)).thenReturn(List.of(cp1000));

        FreshnessStatus status1000 = freshnessService.getFreshness(tenantId, topic);
        assertThat(status1000.pendingIntents()).isEqualTo(1000);
        assertThat(status1000.isHealthy()).isFalse();

        // Boundary 3: 1001 pending intents (over 1000 threshold) -> UNHEALTHY
        CheckpointProgress cp1001 = new CheckpointProgress(tenantId, topic, 0, 102L, tMinus10, 1001);
        when(checkpointRepo.findByTenantAndTopic(tenantId, topic)).thenReturn(List.of(cp1001));

        FreshnessStatus status1001 = freshnessService.getFreshness(tenantId, topic);
        assertThat(status1001.pendingIntents()).isEqualTo(1001);
        assertThat(status1001.isHealthy()).isFalse();
    }

    @Test
    @DisplayName("RPT-18: Idle stream with ancient timestamp remains 0 lag and healthy")
    void idleStreamWithAncientTimestampRemainsZeroLagAndHealthy() {
        UUID tenantId = UUID.randomUUID();
        String topic = "hrflow.attendance.facts";

        Instant ancientTime = T0.minusSeconds(86400 * 365); // 1 year ago
        CheckpointProgress idleCp = new CheckpointProgress(tenantId, topic, 0, 500L, ancientTime, 0);
        when(checkpointRepo.findByTenantAndTopic(tenantId, topic)).thenReturn(List.of(idleCp));

        FreshnessStatus status = freshnessService.getFreshness(tenantId, topic);
        assertThat(status.pendingIntents()).isEqualTo(0);
        assertThat(status.lagSeconds()).isEqualTo(0L);
        assertThat(status.isHealthy()).isTrue();
        assertThat(status.lastProcessedAt()).isEqualTo(ancientTime);
    }

    @Test
    @DisplayName("RPT-18: Empty partitions report 0 lag, healthy, and uses injected clock instant")
    void emptyPartitionsUsesInjectedClock() {
        UUID tenantId = UUID.randomUUID();
        String topic = "hrflow.attendance.facts";

        when(checkpointRepo.findByTenantAndTopic(tenantId, topic)).thenReturn(List.of());

        FreshnessStatus status = freshnessService.getFreshness(tenantId, topic);
        assertThat(status.pendingIntents()).isEqualTo(0);
        assertThat(status.lagSeconds()).isEqualTo(0L);
        assertThat(status.isHealthy()).isTrue();
        assertThat(status.lastProcessedAt()).isEqualTo(T0);
    }
}
