package com.hrflow.reporting.service;

import com.hrflow.reporting.dto.FreshnessStatus;
import com.hrflow.reporting.entity.CheckpointProgress;
import com.hrflow.reporting.repository.CheckpointProgressRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FreshnessService {

    private final CheckpointProgressRepository checkpointRepo;
    private final TenantSqlScope tenantScope;
    private final java.time.Clock clock;

    public FreshnessService(
            CheckpointProgressRepository checkpointRepo,
            TenantSqlScope tenantScope) {
        this(checkpointRepo, tenantScope, java.time.Clock.systemUTC());
    }

    public FreshnessService(
            CheckpointProgressRepository checkpointRepo,
            TenantSqlScope tenantScope,
            java.time.Clock clock) {
        this.checkpointRepo = checkpointRepo;
        this.tenantScope = tenantScope;
        this.clock = (clock != null) ? clock : java.time.Clock.systemUTC();
    }

    @org.springframework.beans.factory.annotation.Autowired
    public FreshnessService(
            CheckpointProgressRepository checkpointRepo,
            TenantSqlScope tenantScope,
            java.util.Optional<java.time.Clock> clock) {
        this(checkpointRepo, tenantScope, clock.orElse(java.time.Clock.systemUTC()));
    }

    public java.time.Clock getClock() {
        return this.clock;
    }

    @Transactional
    public void recordCommittedOffset(
            UUID tenantId,
            String topic,
            int partitionNum,
            long committedOffset,
            Instant processedAt) {
        recordCheckpoint(
                tenantId,
                topic,
                partitionNum,
                committedOffset,
                processedAt != null ? processedAt : Instant.now(this.clock),
                0);
    }

    @Transactional
    public void recordCheckpoint(
            UUID tenantId,
            String topic,
            int partitionNum,
            long lastOffset,
            Instant lastProcessedAt,
            int pendingIntents) {
        CheckpointProgress.validate(lastOffset, lastProcessedAt, pendingIntents);
        if (lastProcessedAt != null && lastProcessedAt.isAfter(Instant.now(this.clock).plusSeconds(300))) {
            throw new IllegalArgumentException("Checkpoint timestamp cannot be in the future: " + lastProcessedAt);
        }
        tenantScope.use(tenantId);
        Optional<CheckpointProgress> existing = checkpointRepo.findByTenantTopicPartition(tenantId, topic,
                partitionNum);

        if (existing.isPresent()) {
            CheckpointProgress progress = existing.get();
            boolean updated = progress.updateProgress(lastOffset, lastProcessedAt, pendingIntents);
            if (updated) {
                checkpointRepo.save(progress);
            }
        } else {
            CheckpointProgress progress = new CheckpointProgress(
                    tenantId,
                    topic,
                    partitionNum,
                    lastOffset,
                    lastProcessedAt,
                    pendingIntents);
            checkpointRepo.save(progress);
        }
    }

    @Transactional(readOnly = true)
    public FreshnessStatus getFreshness(UUID tenantId, String topic) {
        tenantScope.use(tenantId);
        List<CheckpointProgress> partitions = checkpointRepo.findByTenantAndTopic(tenantId, topic);

        if (partitions.isEmpty()) {
            return new FreshnessStatus(tenantId, topic, true, 0, 0L, Instant.now(this.clock));
        }

        int totalPending = partitions.stream().mapToInt(CheckpointProgress::getPendingIntents).sum();

        // Key domain rule: if there are no pending intents (idle stream), lag is 0
        // seconds
        // Age of idle business events is not falsely reported as lag
        if (totalPending == 0) {
            Instant latestProcess = partitions.stream()
                    .map(CheckpointProgress::getLastProcessedAt)
                    .max(Instant::compareTo)
                    .orElse(Instant.now(this.clock));
            return new FreshnessStatus(tenantId, topic, true, 0, 0L, latestProcess);
        }

        // When work is pending, lag must use the oldest pending checkpoint
        // so a fresh partition cannot hide an old pending partition.
        Instant oldestPendingProcess = partitions.stream()
                .filter(p -> p.getPendingIntents() > 0)
                .map(CheckpointProgress::getLastProcessedAt)
                .min(Instant::compareTo)
                .orElse(Instant.now(this.clock));

        long lagSeconds = Math.max(0, Duration.between(oldestPendingProcess, Instant.now(this.clock)).toSeconds());
        boolean isHealthy = lagSeconds < 300 && totalPending < 1000;

        return new FreshnessStatus(tenantId, topic, isHealthy, totalPending, lagSeconds, oldestPendingProcess);
    }
}
