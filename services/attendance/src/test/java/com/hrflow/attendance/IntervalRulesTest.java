package com.hrflow.attendance;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
class IntervalRulesTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("context-schema.sql");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.flyway.user", POSTGRES::getUsername);
        properties.add("spring.flyway.password", POSTGRES::getPassword);
    }

    @Autowired JdbcTemplate jdbc;

    @Test
    void directInvalidIntervalInsertIsRejectedByDatabaseCheckConstraint() {
        UUID tenant = UUID.randomUUID();
        UUID employee = UUID.randomUUID();
        Instant clockIn = Instant.parse("2026-11-16T17:00:00Z");
        Instant clockOutEarlier = Instant.parse("2026-11-16T08:00:00Z"); // earlier than clock_in

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO attendance.attendance_session
                    (id, tenant_id, employee_id, clock_in, clock_out, status, version)
                VALUES (?, ?, ?, ?, ?, 'COMPLETED', 0)
                """, UUID.randomUUID(), tenant, employee, Timestamp.from(clockIn), Timestamp.from(clockOutEarlier)));
    }

    @Test
    void secondOpenSessionIsRejectedByPartialUniqueIndex() {
        UUID tenant = UUID.randomUUID();
        UUID employee = UUID.randomUUID();
        Instant now = Instant.now();

        // First open session succeeds
        jdbc.update("""
                INSERT INTO attendance.attendance_session
                    (id, tenant_id, employee_id, clock_in, clock_out, status, version)
                VALUES (?, ?, ?, ?, NULL, 'OPEN', 0)
                """, UUID.randomUUID(), tenant, employee, Timestamp.from(now));

        // Second open session for the same employee in same tenant fails
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO attendance.attendance_session
                    (id, tenant_id, employee_id, clock_in, clock_out, status, version)
                VALUES (?, ?, ?, ?, NULL, 'OPEN', 0)
                """, UUID.randomUUID(), tenant, employee, Timestamp.from(now.plusSeconds(60))));
    }

    @Test
    void unsupportedOvernightShiftIsRejectedByProjectionConstraint() {
        UUID tenant = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        UUID siteId = UUID.randomUUID();

        // Overnight shift where end_time (06:00) <= start_time (22:00) violates shift_projection_same_day
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO attendance.shift_projection
                    (tenant_id, shift_id, name, start_time, end_time, timezone, grace_minutes, working_days,
                     site_id, site_name, latitude, longitude, radius_m, revision)
                VALUES (?, ?, 'Night Shift', ?, ?, 'Asia/Phnom_Penh', 15, 31,
                        ?, 'HQ', 11.56, 104.92, 100.0, 1)
                """, tenant, shiftId, Time.valueOf("22:00:00"), Time.valueOf("06:00:00"), siteId));
    }
}
