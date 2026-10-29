package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.reporting.dto.AuditLogEntry;
import com.hrflow.reporting.dto.FreshnessStatus;
import com.hrflow.reporting.entity.AuditLog;
import com.hrflow.reporting.entity.CheckpointProgress;
import com.hrflow.reporting.repository.AuditLogRepository;
import com.hrflow.reporting.repository.CheckpointProgressRepository;
import com.hrflow.reporting.service.AuditLogService;
import com.hrflow.reporting.service.FreshnessService;
import com.hrflow.reporting.service.TenantSqlScope;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class ExposeFreshnessAuditSafelyTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("hrflow")
            .withUsername("hrflow")
            .withPassword("test-only")
            .withInitScript("context-schema.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired
    AuditLogService auditLogService;

    @Autowired
    AuditLogRepository auditRepo;

    @Autowired
    FreshnessService freshnessService;

    @Autowired
    CheckpointProgressRepository checkpointRepo;

    @Autowired
    TenantSqlScope tenantScope;

    @Autowired
    jakarta.persistence.EntityManager entityManager;

    @Autowired
    org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("TenantSqlScope: use(tenantId) requires an active transaction and throws when called without one")
    void tenantScopeRequiresActiveTransaction() {
        UUID tenantId = UUID.randomUUID();
        assertThatThrownBy(() -> tenantScope.use(tenantId))
                .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
    }

    @Test
    @DisplayName("TenantSqlScope: establishes tenant context within the active transaction")
    void establishesTenantContextWithinActiveTransaction() {
        UUID tenantId = UUID.randomUUID();
        transactionTemplate.executeWithoutResult(status -> {
            tenantScope.use(tenantId);
            String currentSetting = (String) entityManager
                    .createNativeQuery("SELECT current_setting('app.tenant_id', true)")
                    .getSingleResult();
            assertThat(currentSetting).isEqualTo(tenantId.toString());
        });
    }

    @Test
    @DisplayName("W09-S04 to S06: Idle business event age is not falsely reported as lag; freshness uses checkpoint progress")
    void idleEventAgeNotReportedAsLag() {
        UUID tenantId = UUID.randomUUID();

        // Record checkpoint progress with 0 pending intents even if last processed event is old
        Instant oldEventProcessedTime = Instant.now().minusSeconds(86400 * 7); // 7 days ago
        freshnessService.recordCheckpoint(tenantId, "hrflow.attendance.facts", 0, 1050L, oldEventProcessedTime, 0);

        FreshnessStatus freshness = freshnessService.getFreshness(tenantId, "hrflow.attendance.facts");
        assertThat(freshness.isHealthy()).isTrue();
        assertThat(freshness.pendingIntents()).isEqualTo(0);
        assertThat(freshness.lagSeconds()).isEqualTo(0); // Not 7 days!
    }

    @Test
    @DisplayName("RPT-16: Oldest pending partition determines lag and cannot be hidden by fresh partition")
    void oldestPendingPartitionDeterminesLagAndFreshPartitionCannotHideIt() {
        UUID tenantId = UUID.randomUUID();
        String topic = "hrflow.attendance.facts";

        Instant tenMinutesAgo = Instant.now().minusSeconds(600);
        Instant twoSecondsAgo = Instant.now().minusSeconds(2);

        // Partition 0 is lagging: 10 pending intents, last processed 600s ago
        freshnessService.recordCheckpoint(tenantId, topic, 0, 500L, tenMinutesAgo, 10);

        // Partition 1 is fresh: 0 pending intents, last processed 2s ago
        freshnessService.recordCheckpoint(tenantId, topic, 1, 900L, twoSecondsAgo, 0);

        FreshnessStatus freshness = freshnessService.getFreshness(tenantId, topic);

        // Prove lag is determined by partition 0 (>= 590s), NOT masked by partition 1 (2s)
        assertThat(freshness.pendingIntents()).isEqualTo(10);
        assertThat(freshness.lagSeconds())
                .as("Fresh partition must not hide 600s lag of pending partition 0")
                .isGreaterThanOrEqualTo(590L);
        assertThat(freshness.isHealthy()).isFalse();

        // Also prove idle partition with old timestamp does not falsely inflate lag of a fresh pending partition
        UUID tenant2 = UUID.randomUUID();
        Instant sevenDaysAgo = Instant.now().minusSeconds(86400 * 7);
        Instant tenSecondsAgo = Instant.now().minusSeconds(10);
        freshnessService.recordCheckpoint(tenant2, topic, 0, 100L, sevenDaysAgo, 0); // idle partition
        freshnessService.recordCheckpoint(tenant2, topic, 1, 200L, tenSecondsAgo, 5); // active pending partition

        FreshnessStatus freshness2 = freshnessService.getFreshness(tenant2, topic);
        assertThat(freshness2.pendingIntents()).isEqualTo(5);
        assertThat(freshness2.lagSeconds())
                .as("Idle partition must not inflate lag; oldest pending partition is 10s")
                .isLessThan(60L);
        assertThat(freshness2.isHealthy()).isTrue();
    }

    @Test
    @DisplayName("W09-S04 to S06: Audit logs are strictly isolated by tenant and filterable by actor and action")
    void auditLogsAreTenantIsolatedAndFilterable() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID actorA = UUID.randomUUID();
        UUID actorB = UUID.randomUUID();

        // Write log for tenant A (establishes tenant context in transaction)
        auditLogService.record(tenantA, actorA, "admin@alpha.test", "APPROVE_LEAVE", "LEAVE", "leave-101", "{\"status\":\"APPROVED\"}", "192.168.1.10");

        // Write log for tenant B (establishes tenant context in transaction)
        auditLogService.record(tenantB, actorB, "admin@beta.test", "APPROVE_LEAVE", "LEAVE", "leave-202", "{\"status\":\"APPROVED\"}", "192.168.1.20");

        // Query as Tenant A: must only see Tenant A log
        List<AuditLogEntry> logs = auditLogService.search(tenantA, "LEAVE", null, 0, 50);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).actorEmail()).isEqualTo("admin@alpha.test");
        assertThat(logs.get(0).resourceId()).isEqualTo("leave-101");

        // Query as Tenant B: must only see Tenant B log
        List<AuditLogEntry> bLogs = auditLogService.search(tenantB, "LEAVE", null, 0, 50);
        assertThat(bLogs).hasSize(1);
        assertThat(bLogs.get(0).actorEmail()).isEqualTo("admin@beta.test");
        assertThat(bLogs.get(0).resourceId()).isEqualTo("leave-202");
    }

    @Test
    @DisplayName("RPT-17: Lower offsets, negative pending counts, and invalid timestamps are rejected without changing state; duplicate offsets are idempotent no-ops")
    void checkpointRejectsInvalidAndLowerOffsetsPreservingState() {
        UUID tenantId = UUID.randomUUID();
        String topic = "hrflow.attendance.facts";
        int partition = 0;
        Instant initialTime = Instant.now().minusSeconds(100);

        // 1. Initial valid checkpoint
        freshnessService.recordCheckpoint(tenantId, topic, partition, 1000L, initialTime, 5);

        CheckpointProgress initialProgress = checkpointRepo.findByTenantTopicPartition(tenantId, topic, partition).orElseThrow();
        assertThat(initialProgress.getLastOffset()).isEqualTo(1000L);
        assertThat(initialProgress.getPendingIntents()).isEqualTo(5);

        // 2. Reject lower offset without modifying stored state
        assertThatThrownBy(() -> {
            freshnessService.recordCheckpoint(tenantId, topic, partition, 999L, Instant.now(), 2);
        }).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("offset");

        CheckpointProgress afterLowerOffset = checkpointRepo.findByTenantTopicPartition(tenantId, topic, partition).orElseThrow();
        assertThat(afterLowerOffset.getLastOffset()).isEqualTo(1000L);
        assertThat(afterLowerOffset.getPendingIntents()).isEqualTo(5);

        // 3. Reject negative pending count without modifying stored state
        assertThatThrownBy(() -> {
            freshnessService.recordCheckpoint(tenantId, topic, partition, 1005L, Instant.now(), -1);
        }).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("negative");

        CheckpointProgress afterNegativePending = checkpointRepo.findByTenantTopicPartition(tenantId, topic, partition).orElseThrow();
        assertThat(afterNegativePending.getLastOffset()).isEqualTo(1000L);
        assertThat(afterNegativePending.getPendingIntents()).isEqualTo(5);

        // 4. Reject invalid timestamps without modifying stored state
        assertThatThrownBy(() -> {
            freshnessService.recordCheckpoint(tenantId, topic, partition, 1005L, null, 2);
        }).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("timestamp");

        assertThatThrownBy(() -> {
            freshnessService.recordCheckpoint(tenantId, topic, partition, 1005L, Instant.now().plusSeconds(3600), 2);
        }).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("future");

        CheckpointProgress afterInvalidTime = checkpointRepo.findByTenantTopicPartition(tenantId, topic, partition).orElseThrow();
        assertThat(afterInvalidTime.getLastOffset()).isEqualTo(1000L);
        assertThat(afterInvalidTime.getPendingIntents()).isEqualTo(5);

        // 5. Duplicate offset is an idempotent no-op without changing stored state
        freshnessService.recordCheckpoint(tenantId, topic, partition, 1000L, Instant.now(), 99);

        CheckpointProgress afterDuplicate = checkpointRepo.findByTenantTopicPartition(tenantId, topic, partition).orElseThrow();
        assertThat(afterDuplicate.getLastOffset()).isEqualTo(1000L);
        assertThat(afterDuplicate.getPendingIntents()).isEqualTo(5);
        assertThat(afterDuplicate.getLastProcessedAt()).isEqualTo(initialTime);

        // 6. Monotonic higher offset succeeds and updates state
        Instant newTime = Instant.now().minusSeconds(10);
        freshnessService.recordCheckpoint(tenantId, topic, partition, 1050L, newTime, 2);

        CheckpointProgress afterAdvance = checkpointRepo.findByTenantTopicPartition(tenantId, topic, partition).orElseThrow();
        assertThat(afterAdvance.getLastOffset()).isEqualTo(1050L);
        assertThat(afterAdvance.getPendingIntents()).isEqualTo(2);
        assertThat(afterAdvance.getLastProcessedAt()).isEqualTo(newTime);
    }
}
