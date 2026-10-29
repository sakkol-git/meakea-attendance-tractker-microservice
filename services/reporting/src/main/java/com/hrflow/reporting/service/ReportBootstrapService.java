package com.hrflow.reporting.service;

import com.hrflow.reporting.entity.CheckpointProgress;
import com.hrflow.reporting.repository.CheckpointProgressRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportBootstrapService {

    public static final String DEFAULT_PROJECTION_TOPIC = "hrflow.attendance.facts";

    private final CheckpointProgressRepository checkpointRepo;
    private final TenantSqlScope tenantScope;
    private final String projectionTopic;

    public ReportBootstrapService(
            CheckpointProgressRepository checkpointRepo,
            TenantSqlScope tenantScope) {
        this(checkpointRepo, tenantScope, DEFAULT_PROJECTION_TOPIC);
    }

    @Autowired
    public ReportBootstrapService(
            CheckpointProgressRepository checkpointRepo,
            TenantSqlScope tenantScope,
            @Value("${hrflow.reporting.topics.attendance-facts:hrflow.attendance.facts}") String projectionTopic) {
        this.checkpointRepo = checkpointRepo;
        this.tenantScope = tenantScope;
        this.projectionTopic = (projectionTopic != null && !projectionTopic.isBlank())
                ? projectionTopic
                : DEFAULT_PROJECTION_TOPIC;
    }

    public String getProjectionTopic() {
        return this.projectionTopic;
    }

    @Transactional(readOnly = true)
    public boolean isTenantReady(UUID tenantId) {
        if (tenantId == null) {
            return false;
        }
        tenantScope.use(tenantId);
        List<CheckpointProgress> checkpoints = checkpointRepo.findByTenantAndTopic(tenantId, this.projectionTopic);
        return checkpoints != null && !checkpoints.isEmpty();
    }
}
