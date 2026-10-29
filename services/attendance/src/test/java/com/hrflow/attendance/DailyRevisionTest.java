package com.hrflow.attendance;

import static org.assertj.core.api.Assertions.assertThat;

import com.hrflow.attendance.dto.CheckInRequest;
import com.hrflow.attendance.service.AttendanceService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = "hrflow.attendance.daily-status.enabled=true")
@Testcontainers
class DailyRevisionTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("context-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", POSTGRES::getUsername);
        p.add("spring.datasource.password", POSTGRES::getPassword);
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @Autowired AttendanceService attendance;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.hrflow.attendance.daily.DailyStatusWriter writer;
    @Autowired org.springframework.transaction.support.TransactionTemplate transactions;

    @Test void committedCommandsProduceOneMonotonicDayAggregateAndRetryDoesNotAddARevision() {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        AttendanceFixtures.schedule(jdbc, tenant, employee);
        var input = new CheckInRequest(null, null, null, null);
        String key = UUID.randomUUID().toString();
        var opened = attendance.checkIn(tenant, employee, employee, key, input);
        attendance.checkIn(tenant, employee, employee, key, input);
        assertThat(intents(tenant)).isEqualTo(1);
        attendance.checkOut(tenant, employee, employee, UUID.randomUUID().toString(), "\"0\"", input);
        assertThat(intents(tenant)).isEqualTo(2);
        transactions.executeWithoutResult(tx -> writer.refresh(tenant, employee,
                opened.clockIn().atZone(java.time.ZoneId.of("Asia/Phnom_Penh")).toLocalDate()));
        assertThat(intents(tenant)).isEqualTo(2);
        var versions = jdbc.queryForList("""
                SELECT aggregate_revision FROM attendance.attendance_outbox
                WHERE tenant_id=? AND topic='attendance.daily-status.v1' ORDER BY aggregate_revision
                """, Long.class, tenant);
        assertThat(versions).containsExactly(1L, 2L);
        assertThat(jdbc.queryForObject("""
                SELECT count(DISTINCT aggregate_id) FROM attendance.attendance_outbox
                WHERE tenant_id=? AND topic='attendance.daily-status.v1'
                """, Integer.class, tenant)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM attendance.daily_status WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(1);
        assertThat(opened.id()).isNotNull();
    }

    @Test void rollbackRemovesDayFactIntentEvidenceAndReceiptTogether() {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        AttendanceFixtures.schedule(jdbc, tenant, employee);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> transactions.executeWithoutResult(tx -> {
            attendance.checkIn(tenant, employee, employee, UUID.randomUUID().toString(), new CheckInRequest(null, null, null, null));
            throw new IllegalStateException("owner rollback");
        })).isInstanceOf(IllegalStateException.class);
        for (String table : java.util.List.of("daily_status", "daily_schedule", "attendance_outbox", "attendance_session", "attendance_evidence", "command_receipt")) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM attendance." + table + " WHERE tenant_id=?", Integer.class, tenant)).isZero();
        }
    }

    @Test void unknownOlderCalendarQueuesRepairWithoutRejectingAGenuineTap() {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        AttendanceFixtures.schedule(jdbc, tenant, employee);
        // Legacy closed record has no verified working-day calendar.
        jdbc.update("""
                INSERT INTO attendance.attendance_session(id,tenant_id,employee_id,clock_in,clock_out,status)
                VALUES (?,?,?,now()-interval '2 minutes',now()-interval '1 minute','COMPLETED')
                """, UUID.randomUUID(), tenant, employee);
        var result = attendance.checkIn(tenant, employee, employee, UUID.randomUUID().toString(), new CheckInRequest(null, null, null, null));
        assertThat(result.id()).isNotNull();
        assertThat(intents(tenant)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM attendance.daily_recalculation WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM attendance.attendance_evidence WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(1);
    }

    private int intents(UUID tenant) {
        return jdbc.queryForObject("SELECT count(*) FROM attendance.attendance_outbox WHERE tenant_id=? AND topic='attendance.daily-status.v1'", Integer.class, tenant);
    }
}
