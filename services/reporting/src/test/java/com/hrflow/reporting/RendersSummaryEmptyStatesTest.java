package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.hrflow.reporting.dto.DashboardKpiResponse;
import com.hrflow.reporting.dto.FreshnessStatus;
import com.hrflow.reporting.service.FreshnessService;
import com.hrflow.reporting.service.ReportingDashboardService;
import java.time.Instant;
import java.time.LocalDate;
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
class RendersSummaryEmptyStatesTest {

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
    ReportingDashboardService dashboardService;

    @Autowired
    FreshnessService freshnessService;

    @Test
    @DisplayName("W09-S07 to S09: Empty summary provides explicit asOf date and metadata, never ambiguous fresh zero")
    void emptySummaryProvidesExplicitDateAndMetadata() {
        UUID tenantId = UUID.randomUUID();

        DashboardKpiResponse kpis = dashboardService.getDashboardKpis(tenantId);

        assertThat(kpis.summary()).isNotNull();
        assertThat(kpis.summary().get("date")).isEqualTo(LocalDate.now().toString());
        assertThat(kpis.totalPresentToday()).isEqualTo(0L);
        assertThat(kpis.totalLateToday()).isEqualTo(0L);
        assertThat(kpis.totalAbsentToday()).isEqualTo(0L);
    }

    @Test
    @DisplayName("W09-S07 to S09: Stale/lagging partition progress is labeled accurately, never masked as fresh")
    void staleLaggingPartitionIsLabeledAccurately() {
        UUID tenantId = UUID.randomUUID();

        // Record lagging checkpoint: 45 pending intents, last processed 600s ago
        Instant processedAgo = Instant.now().minusSeconds(600);
        freshnessService.recordCheckpoint(tenantId, "hrflow.leave.facts", 0, 800L, processedAgo, 45);

        FreshnessStatus status = freshnessService.getFreshness(tenantId, "hrflow.leave.facts");
        assertThat(status.isHealthy()).isFalse();
        assertThat(status.pendingIntents()).isEqualTo(45);
        assertThat(status.lagSeconds()).isGreaterThanOrEqualTo(590L);
    }
}
