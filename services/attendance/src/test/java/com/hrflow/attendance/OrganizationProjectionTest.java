package com.hrflow.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.hrflow.attendance.organization.OrganizationProjectionListener;
import com.hrflow.attendance.organization.OrganizationProjectionService;
import com.hrflow.attendance.organization.OrganizationProjectionService.Result;
import com.hrflow.contracts.organization.AssignmentSnapshot;
import com.hrflow.contracts.organization.EmployeeSnapshot;
import com.hrflow.contracts.organization.HolidaySnapshot;
import com.hrflow.contracts.organization.OrganizationFact;
import com.hrflow.contracts.organization.ShiftSnapshot;
import com.hrflow.contracts.organization.SiteSnapshot;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
class OrganizationProjectionTest {
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

    @Autowired OrganizationProjectionService projections;
    @Autowired JdbcTemplate jdbc;

    @Test
    void appliesSnapshotsAndReceiptTogetherWithRevisionAndTenantGuards() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID foreign = UUID.randomUUID();
        UUID employee = UUID.randomUUID();
        UUID shift = UUID.randomUUID();
        UUID holiday = UUID.randomUUID();
        var assignment = new AssignmentSnapshot(UUID.randomUUID().toString(), shift.toString(),
                "2026-11-01", null);
        var employeeSnapshot = new EmployeeSnapshot(employee.toString(), null, "E-1", "Test Person",
                "person@example.test", null, null, null, null, null, null, null,
                "ACTIVE", null, null, List.of(assignment), null, null);
        var site = new SiteSnapshot(UUID.randomUUID().toString(), "Main", 11.56, 104.92, 100.0,
                "Asia/Phnom_Penh", null, null);
        var shiftSnapshot = new ShiftSnapshot(shift.toString(), "Day", "08:00", "17:00",
                "Asia/Phnom_Penh", 10, 31, site, null);
        var holidaySnapshot = new HolidaySnapshot(holiday.toString(), "2026-11-09", "Company Day",
                "COMPANY", null);
        OrganizationFact employeeFact = fact(tenant, employee, 1, "org.employee.created.v1", employeeSnapshot);
        assertEquals(Result.APPLIED, projections.apply("org.employee.v1", key(tenant, employee), employeeFact));
        assertEquals(Result.DUPLICATE, projections.apply("org.employee.v1", key(tenant, employee), employeeFact));
        assertEquals(Result.APPLIED, projections.apply("org.shift.v1", key(tenant, shift),
                fact(tenant, shift, 1, "org.shift.defined.v1", shiftSnapshot)));
        assertEquals(Result.APPLIED, projections.apply("org.holiday.v1", key(tenant, holiday),
                fact(tenant, holiday, 1, "org.holiday.added.v1", holidaySnapshot)));
        assertEquals(3, count("attendance.organization_inbox", tenant));
        assertEquals(1, count("attendance.employee_projection", tenant));
        assertEquals(1, count("attendance.shift_projection", tenant));
        assertEquals(1, count("attendance.holiday_projection", tenant));
        assertEquals(0, count("attendance.employee_projection", foreign));

        var corrected = new HolidaySnapshot(holiday.toString(), "2026-11-10", "Corrected Day",
                "COMPANY", null);
        assertEquals(Result.APPLIED, projections.apply("org.holiday.v1", key(tenant, holiday),
                fact(tenant, holiday, 2, "org.holiday.updated.v1", corrected)));
        assertEquals(Result.STALE, projections.apply("org.holiday.v1", key(tenant, holiday),
                fact(tenant, holiday, 1, "org.holiday.added.v1", holidaySnapshot)));
        assertEquals("Corrected Day", jdbc.queryForObject("""
                SELECT name FROM attendance.holiday_projection
                WHERE tenant_id = ? AND holiday_id = ?
                """, String.class, tenant, holiday));

        var invalid = new HolidaySnapshot(holiday.toString(), "2026-11-11", "Bad Day", "UNKNOWN", null);
        UUID invalidEvent = UUID.randomUUID();
        var invalidFact = new OrganizationFact(invalidEvent.toString(), "org.holiday.updated.v1", 1,
                tenant.toString(), holiday.toString(), 3L, "2026-10-27T00:00:00Z",
                UUID.randomUUID().toString(), null, invalid);
        assertThrows(IllegalArgumentException.class,
                () -> projections.apply("org.holiday.v1", key(tenant, holiday), invalidFact));
        assertEquals(0, jdbc.queryForObject("""
                SELECT count(*) FROM attendance.organization_inbox WHERE event_id = ?
                """, Integer.class, invalidEvent));
        var removed = new HolidaySnapshot(holiday.toString(), "2026-11-10", "Corrected Day",
                "COMPANY", "2026-11-11T00:00:00Z");
        assertEquals(Result.APPLIED, projections.apply("org.holiday.v1", key(tenant, holiday),
                fact(tenant, holiday, 3, "org.holiday.removed.v1", removed)));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM attendance.holiday_projection
                WHERE tenant_id = ? AND holiday_id = ? AND deleted_at IS NOT NULL
                """, Integer.class, tenant, holiday));
        assertThrows(IllegalArgumentException.class,
                () -> projections.apply("org.holiday.v1", key(foreign, holiday), invalidFact));
    }

    @Test
    void nonownerCannotSeePersonalProjectionWithoutTenantContext() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID holiday = UUID.randomUUID();
        projections.apply("org.holiday.v1", key(tenant, holiday),
                fact(tenant, holiday, 1, "org.holiday.added.v1",
                        new HolidaySnapshot(holiday.toString(), "2026-11-09", "Tenant Day", "COMPANY", null)));
        jdbc.execute("CREATE ROLE hrflow_attendance_rls_test NOLOGIN");
        jdbc.execute("GRANT USAGE ON SCHEMA attendance TO hrflow_attendance_rls_test");
        jdbc.execute("GRANT SELECT ON attendance.holiday_projection TO hrflow_attendance_rls_test");
        try (Connection connection = jdbc.getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            assertEquals(0, scopedCount(connection, null));
            connection.rollback();
            assertEquals(1, scopedCount(connection, tenant));
            connection.rollback();
            assertEquals(0, scopedCount(connection, UUID.randomUUID()));
            connection.rollback();
            assertEquals(0, scopedCount(connection, null));
            connection.rollback();
        }
    }

    @Test
    void listenerAcknowledgesOnlyAfterProjectionCommit() {
        UUID tenant = UUID.randomUUID();
        UUID holiday = UUID.randomUUID();
        OrganizationFact fact = fact(tenant, holiday, 1, "org.holiday.added.v1",
                new HolidaySnapshot(holiday.toString(), "2026-11-09", "Company Day", "COMPANY", null));
        Acknowledgment ack = mock(Acknowledgment.class);
        doAnswer(invocation -> {
            assertEquals(1, count("attendance.holiday_projection", tenant));
            assertEquals(1, count("attendance.organization_inbox", tenant));
            return null;
        }).when(ack).acknowledge();
        var listener = new OrganizationProjectionListener(projections);
        listener.onFact(new ConsumerRecord<>("org.holiday.v1", 0, 0L, key(tenant, holiday), fact), ack);
        verify(ack).acknowledge();

        Acknowledgment rejected = mock(Acknowledgment.class);
        assertThrows(IllegalArgumentException.class, () -> listener.onFact(
                new ConsumerRecord<>("org.holiday.v1", 0, 1L, "wrong-key", fact), rejected));
        verify(rejected, never()).acknowledge();
    }

    private int scopedCount(Connection connection, UUID tenant) throws Exception {
        try (var role = connection.createStatement()) {
            role.execute("SET LOCAL ROLE hrflow_attendance_rls_test");
        }
        if (tenant != null) {
            try (PreparedStatement set = connection.prepareStatement("SELECT set_config('app.tenant_id', ?, true)")) {
                set.setString(1, tenant.toString());
                set.execute();
            }
        }
        try (var query = connection.createStatement();
                ResultSet rows = query.executeQuery("SELECT count(*) FROM attendance.holiday_projection")) {
            rows.next();
            return rows.getInt(1);
        }
    }

    private int count(String table, UUID tenant) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE tenant_id = ?", Integer.class, tenant);
    }

    private OrganizationFact fact(UUID tenant, UUID aggregate, long revision, String type, Object snapshot) {
        return new OrganizationFact(UUID.randomUUID().toString(), type, 1, tenant.toString(),
                aggregate.toString(), revision, "2026-10-27T00:00:00Z", UUID.randomUUID().toString(),
                null, snapshot);
    }

    private String key(UUID tenant, UUID aggregate) {
        return tenant + ":" + aggregate;
    }
}
