package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.hrflow.reporting.entity.CheckpointProgress;
import com.hrflow.reporting.repository.CheckpointProgressRepository;
import com.hrflow.reporting.service.ReportBootstrapService;
import com.hrflow.reporting.service.TenantSqlScope;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class BootstrapEmptyReportOwnerTest {

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
    private CheckpointProgressRepository checkpointRepo;

    @Autowired
    private ReportBootstrapService bootstrapService;

    @Autowired
    private TenantSqlScope tenantScope;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("RPT-20: Gates readiness until accepted configured projection topic checkpoint completes")
    void gatesReadinessUntilAcceptedConfiguredTopicCheckpointCompletes() {
        UUID tenantId = UUID.randomUUID();

        // 1. Without checkpoint, tenant is not ready
        boolean readyBefore = bootstrapService.isTenantReady(tenantId);
        assertThat(readyBefore).isFalse();

        // 2. Record completed checkpoint using accepted configured projection topic
        String configuredTopic = bootstrapService.getProjectionTopic();
        assertThat(configuredTopic).isEqualTo("hrflow.attendance.facts");

        transactionTemplate.executeWithoutResult(status -> {
            tenantScope.use(tenantId);
            CheckpointProgress cp = new CheckpointProgress(
                    tenantId, configuredTopic, 0, 100L, Instant.now(), 0);
            checkpointRepo.save(cp);
        });

        // 3. Now tenant is ready
        boolean readyAfter = bootstrapService.isTenantReady(tenantId);
        assertThat(readyAfter).isTrue();
    }

    @Test
    @DisplayName("RPT-20: Wrong-topic checkpoint (including invented ATTENDANCE_SNAPSHOT) must not mark readiness")
    void wrongTopicCheckpointMustNotMarkReadiness() {
        UUID tenantId = UUID.randomUUID();

        // 1. Record checkpoint under invented ATTENDANCE_SNAPSHOT topic
        transactionTemplate.executeWithoutResult(status -> {
            tenantScope.use(tenantId);
            CheckpointProgress cp = new CheckpointProgress(
                    tenantId, "ATTENDANCE_SNAPSHOT", 0, 100L, Instant.now(), 0);
            checkpointRepo.save(cp);
        });

        // Must NOT mark tenant ready
        assertThat(bootstrapService.isTenantReady(tenantId))
                .as("Invented ATTENDANCE_SNAPSHOT must not mark tenant readiness")
                .isFalse();

        // 2. Record checkpoint under an arbitrary unaccepted topic
        UUID tenantId2 = UUID.randomUUID();
        transactionTemplate.executeWithoutResult(status -> {
            tenantScope.use(tenantId2);
            CheckpointProgress cp = new CheckpointProgress(
                    tenantId2, "unaccepted.foreign.topic", 0, 200L, Instant.now(), 0);
            checkpointRepo.save(cp);
        });

        assertThat(bootstrapService.isTenantReady(tenantId2))
                .as("Arbitrary unaccepted topic must not mark tenant readiness")
                .isFalse();
    }

    @Test
    @DisplayName("RPT-20: Custom configured topic is respected and rejects default topic")
    void customConfiguredTopicIsRespected() {
        UUID tenantId = UUID.randomUUID();
        String customTopic = "custom.tenant.attendance.facts";
        ReportBootstrapService customService = new ReportBootstrapService(checkpointRepo, tenantScope, customTopic);

        assertThat(customService.getProjectionTopic()).isEqualTo(customTopic);

        // Record on default topic
        transactionTemplate.executeWithoutResult(status -> {
            tenantScope.use(tenantId);
            CheckpointProgress cp = new CheckpointProgress(
                    tenantId, "hrflow.attendance.facts", 0, 100L, Instant.now(), 0);
            checkpointRepo.save(cp);
        });

        // Default topic checkpoint is the WRONG topic for the custom service
        boolean readyWithDefault = transactionTemplate.execute(status -> customService.isTenantReady(tenantId));
        assertThat(readyWithDefault).isFalse();

        // Record on the custom topic
        transactionTemplate.executeWithoutResult(status -> {
            tenantScope.use(tenantId);
            CheckpointProgress cp = new CheckpointProgress(
                    tenantId, customTopic, 0, 100L, Instant.now(), 0);
            checkpointRepo.save(cp);
        });

        // Now custom service marks ready
        boolean readyWithCustom = transactionTemplate.execute(status -> customService.isTenantReady(tenantId));
        assertThat(readyWithCustom).isTrue();
    }

    @Test
    @DisplayName("RPT-20: Null tenantId returns false and does not throw")
    void nullTenantIsNotReady() {
        assertThat(bootstrapService.isTenantReady(null)).isFalse();
    }
}
