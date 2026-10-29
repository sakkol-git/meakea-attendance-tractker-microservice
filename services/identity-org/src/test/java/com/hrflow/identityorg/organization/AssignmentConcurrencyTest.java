package com.hrflow.identityorg.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.hrflow.identityorg.organization.dto.ShiftAssignmentInput;
import com.hrflow.identityorg.organization.service.ShiftAssignmentService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(properties = {
        "hrflow.auth.local-bootstrap=true",
        "hrflow.auth.dev-password=synthetic-local-password",
        "spring.kafka.listener.auto-startup=false"
})
class AssignmentConcurrencyTest {
    private static final Path KEY_DIRECTORY = temporaryKeys();

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("identity-init.sql");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        properties.add("spring.flyway.user", POSTGRES::getUsername);
        properties.add("spring.flyway.password", POSTGRES::getPassword);
        properties.add("hrflow.auth.key-directory", KEY_DIRECTORY::toString);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired ShiftAssignmentService assignments;

    @Test
    void concurrentOverlappingAssignmentsCommitOnePeriodAndOneFact() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID department = UUID.randomUUID();
        UUID position = UUID.randomUUID();
        UUID employee = UUID.randomUUID();
        UUID site = UUID.randomUUID();
        UUID shift = UUID.randomUUID();
        jdbc.update("INSERT INTO identity_org.auth_tenant(id, slug) VALUES (?, ?)", tenant, "shift-race-" + tenant);
        jdbc.update("INSERT INTO identity_org.department(id, tenant_id, code, name) VALUES (?, ?, 'D', 'Dept')",
                department, tenant);
        jdbc.update("INSERT INTO identity_org.position(id, tenant_id, code, name) VALUES (?, ?, 'P', 'Position')",
                position, tenant);
        jdbc.update("""
                INSERT INTO identity_org.employee(id, tenant_id, employee_code, name, email, department_id, position_id)
                VALUES (?, ?, 'E', 'Employee', 'race@example.test', ?, ?)
                """, employee, tenant, department, position);
        jdbc.update("""
                INSERT INTO identity_org.work_site(id, tenant_id, name, latitude, longitude, radius_m, timezone)
                VALUES (?, ?, 'Main', 11.56, 104.92, 100, 'Asia/Phnom_Penh')
                """, site, tenant);
        jdbc.update("""
                INSERT INTO identity_org.shift(id, tenant_id, name, start_time, end_time,
                    grace_minutes, working_days, site_id, timezone)
                VALUES (?, ?, 'Day', '08:00', '17:00', 10, 31, ?, 'Asia/Phnom_Penh')
                """, shift, tenant, site);
        CountDownLatch start = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> {
                assignAfterStart(start, tenant, employee, shift, LocalDate.of(2026, 11, 1));
                return true;
            });
            var second = workers.submit(() -> {
                assignAfterStart(start, tenant, employee, shift, LocalDate.of(2026, 11, 2));
                return true;
            });
            start.countDown();
            int success = 0;
            for (var result : java.util.List.of(first, second)) {
                try {
                    result.get(10, TimeUnit.SECONDS);
                    success++;
                } catch (ExecutionException expectedConflict) {
                    var conflict = assertInstanceOf(ResponseStatusException.class, expectedConflict.getCause());
                    assertEquals(409, conflict.getStatusCode().value());
                }
            }
            assertEquals(1, success);
        }
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM identity_org.employee_shift WHERE tenant_id = ? AND employee_id = ?
                """, Integer.class, tenant, employee));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM identity_org.organization_outbox
                WHERE tenant_id = ? AND topic = 'org.employee.v1'
                """, Integer.class, tenant));
    }

    private void assignAfterStart(CountDownLatch start, UUID tenant, UUID employee,
            UUID shift, LocalDate from) throws InterruptedException {
        start.await();
        assignments.assign(tenant, UUID.randomUUID(), employee, UUID.randomUUID().toString(),
                new ShiftAssignmentInput(shift, from, null));
    }

    private static Path temporaryKeys() {
        try {
            return Files.createTempDirectory("hrflow-assignment-race-keys-");
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }
}
