package com.hrflow.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
class CreateEvidenceSessionIntegrityTest {

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

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void duplicateObservationViolatesUniqueConstraintWithoutLosingEvidence() {
        UUID tenant = UUID.randomUUID();
        UUID employee = UUID.randomUUID();
        UUID observationId = UUID.randomUUID();
        Instant now = Instant.now();

        // First observation succeeds
        jdbc.update("""
                INSERT INTO attendance.attendance_evidence
                    (id, tenant_id, employee_id, client_observation_id, recorded_at, latitude, longitude, accuracy_meters, device_model)
                VALUES (?, ?, ?, ?, ?, 11.56, 104.92, 10.0, 'Pixel 8')
                """, UUID.randomUUID(), tenant, employee, observationId, Timestamp.from(now));

        // Duplicate observation with same client_observation_id fails
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO attendance.attendance_evidence
                    (id, tenant_id, employee_id, client_observation_id, recorded_at, latitude, longitude, accuracy_meters, device_model)
                VALUES (?, ?, ?, ?, ?, 11.56, 104.92, 10.0, 'Pixel 8')
                """, UUID.randomUUID(), tenant, employee, observationId, Timestamp.from(now.plusSeconds(5))));

        // Verify the original evidence is preserved intact
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM attendance.attendance_evidence
                WHERE tenant_id = ? AND client_observation_id = ?
                """, Integer.class, tenant, observationId));
    }

    @Test
    void secondOpenSessionViolatesUniqueConstraintWithoutLosingEvidence() {
        UUID tenant = UUID.randomUUID();
        UUID employee = UUID.randomUUID();
        UUID evidence1 = UUID.randomUUID();
        UUID evidence2 = UUID.randomUUID();
        Instant now = Instant.now();

        jdbc.update("""
                INSERT INTO attendance.attendance_evidence
                    (id, tenant_id, employee_id, recorded_at, latitude, longitude, accuracy_meters, device_model)
                VALUES (?, ?, ?, ?, 11.56, 104.92, 10.0, 'iPhone 15')
                """, evidence1, tenant, employee, Timestamp.from(now));

        jdbc.update("""
                INSERT INTO attendance.attendance_evidence
                    (id, tenant_id, employee_id, recorded_at, latitude, longitude, accuracy_meters, device_model)
                VALUES (?, ?, ?, ?, 11.56, 104.92, 10.0, 'iPhone 15')
                """, evidence2, tenant, employee, Timestamp.from(now.plusSeconds(60)));

        UUID sessionId1 = UUID.randomUUID();
        // First open session succeeds
        jdbc.update("""
                INSERT INTO attendance.attendance_session
                    (id, tenant_id, employee_id, clock_in, clock_out, status, in_evidence_id)
                VALUES (?, ?, ?, ?, NULL, 'OPEN', ?)
                """, sessionId1, tenant, employee, Timestamp.from(now), evidence1);

        // Second open session for same employee and tenant violates partial unique index
        UUID sessionId2 = UUID.randomUUID();
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO attendance.attendance_session
                    (id, tenant_id, employee_id, clock_in, clock_out, status, in_evidence_id)
                VALUES (?, ?, ?, ?, NULL, 'OPEN', ?)
                """, sessionId2, tenant, employee, Timestamp.from(now.plusSeconds(60)), evidence2));

        // Both evidence rows remain persisted and uncorrupted
        assertEquals(2, jdbc.queryForObject("""
                SELECT count(*) FROM attendance.attendance_evidence WHERE tenant_id = ? AND employee_id = ?
                """, Integer.class, tenant, employee));

        // Exactly one open session exists
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM attendance.attendance_session WHERE tenant_id = ? AND employee_id = ?
                """, Integer.class, tenant, employee));

        // After closing the first session, a new session can be opened
        jdbc.update("""
                UPDATE attendance.attendance_session
                SET clock_out = ?, status = 'COMPLETED', out_evidence_id = ?, version = version + 1
                WHERE id = ? AND tenant_id = ?
                """, Timestamp.from(now.plusSeconds(3600)), evidence2, sessionId1, tenant);

        UUID sessionId3 = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO attendance.attendance_session
                    (id, tenant_id, employee_id, clock_in, clock_out, status, in_evidence_id)
                VALUES (?, ?, ?, ?, NULL, 'OPEN', ?)
                """, sessionId3, tenant, employee, Timestamp.from(now.plusSeconds(7200)), evidence2);

        // Now two total sessions exist: one COMPLETED, one OPEN
        assertEquals(2, jdbc.queryForObject("""
                SELECT count(*) FROM attendance.attendance_session WHERE tenant_id = ? AND employee_id = ?
                """, Integer.class, tenant, employee));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM attendance.attendance_session WHERE tenant_id = ? AND employee_id = ? AND clock_out IS NULL
                """, Integer.class, tenant, employee));
    }

    @Test
    void clockOutBeforeClockInViolatesCheckConstraint() {
        UUID tenant = UUID.randomUUID();
        UUID employee = UUID.randomUUID();
        Instant now = Instant.now();

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO attendance.attendance_session
                    (id, tenant_id, employee_id, clock_in, clock_out, status)
                VALUES (?, ?, ?, ?, ?, 'COMPLETED')
                """, UUID.randomUUID(), tenant, employee, Timestamp.from(now), Timestamp.from(now.minusSeconds(3600))));
    }
}
