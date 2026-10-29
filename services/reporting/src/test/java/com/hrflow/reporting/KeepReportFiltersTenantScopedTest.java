package com.hrflow.reporting;

import com.hrflow.reporting.entity.AttendanceFactProjection;
import com.hrflow.reporting.repository.AttendanceFactProjectionRepository;
import com.hrflow.reporting.service.ReportingDashboardService;
import com.hrflow.reporting.service.TenantSqlScope;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class KeepReportFiltersTenantScopedTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
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
    private AttendanceFactProjectionRepository attendanceRepo;

    @Autowired
    private ReportingDashboardService dashboardService;

    @Autowired
    private TenantSqlScope tenantScope;

    @Autowired
    private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        attendanceRepo.deleteAll();

        // Seed Tenant A records
        transactionTemplate.executeWithoutResult(status -> {
            tenantScope.use(tenantA);
            AttendanceFactProjection a1 = new AttendanceFactProjection(
                    tenantA,
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    LocalDate.now().minusDays(1),
                    "PRESENT",
                    0,
                    480,
                    1L
            );
            attendanceRepo.save(a1);
        });

        // Seed Tenant B records
        transactionTemplate.executeWithoutResult(status -> {
            tenantScope.use(tenantB);
            AttendanceFactProjection b1 = new AttendanceFactProjection(
                    tenantB,
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    LocalDate.now().minusDays(1),
                    "ABSENT",
                    0,
                    0,
                    1L
            );
            attendanceRepo.save(b1);
        });
    }

    @Test
    @DisplayName("W09-S19/S20: Removing all caller filters preserves strict tenant isolation")
    void removingAllFiltersStillPreservesTenantScope() {
        // When querying for Tenant A with null start, null end, null sort
        List<AttendanceFactProjection> results = dashboardService.getAttendanceReport(tenantA, null, null, null);

        // Then only Tenant A results are returned
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTenantId()).isEqualTo(tenantA);
        assertThat(results.get(0).getStatus()).isEqualTo("PRESENT");

        // When querying for Tenant B
        List<AttendanceFactProjection> bResults = dashboardService.getAttendanceReport(tenantB, null, null, null);
        assertThat(bResults).hasSize(1);
        assertThat(bResults.get(0).getTenantId()).isEqualTo(tenantB);
        assertThat(bResults.get(0).getStatus()).isEqualTo("ABSENT");
    }

    @Test
    @DisplayName("W09-S21: Disallowed or malicious sort fields are strictly rejected")
    void unknownSortFieldsAreRejected() {
        assertThatThrownBy(() -> dashboardService.getAttendanceReport(tenantA, null, null, "drop table users;"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown or disallowed sort field");

        assertThatThrownBy(() -> dashboardService.getAttendanceReport(tenantA, null, null, "password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown or disallowed sort field");
    }
}
