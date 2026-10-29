package com.hrflow.leave;

import static org.assertj.core.api.Assertions.assertThat;

import com.hrflow.leave.dto.CreateLeaveRequestDto;
import com.hrflow.leave.dto.DecisionRequest;
import com.hrflow.leave.entity.LeaveBalance;
import com.hrflow.leave.entity.LeaveType;
import com.hrflow.leave.repository.LeaveBalanceRepository;
import com.hrflow.leave.repository.LeaveTypeRepository;
import com.hrflow.leave.service.LeaveSubmissionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@Testcontainers
class LeaveSnapshotIntentTest {
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.hrflow.leave.organization.LeaveCalendarBootstrap calendarBootstrap;
    @org.junit.jupiter.api.BeforeEach void calendarReferenceReady() {
        org.mockito.Mockito.when(calendarBootstrap.isReady()).thenReturn(true);
    }

    @org.springframework.test.context.bean.override.mockito.MockitoBean com.hrflow.leave.organization.LeaveEmployeeBootstrap bootstrap;
    @org.junit.jupiter.api.BeforeEach void referenceReady() { org.mockito.Mockito.when(bootstrap.isReady()).thenReturn(true); }

    @Autowired com.hrflow.leave.service.LeaveDecisions decisions;
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("context-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", POSTGRES::getUsername);
        p.add("spring.datasource.password", POSTGRES::getPassword);
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @Autowired LeaveSubmissionService leave;
    @Autowired LeaveTypeRepository types;
    @Autowired LeaveBalanceRepository balances;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;

    @Test void pendingAndApprovedIntentsCarryTheActualOwnerSnapshotAndCommittedRevision() {
        String tenant = UUID.randomUUID().toString(), employee = UUID.randomUUID().toString();
        var day = LocalDate.now().plusDays(7).with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY));
        types.save(new LeaveType(UUID.randomUUID().toString(), tenant, "ANNUAL", "Annual", new BigDecimal("20.00"),
                true, true, false, 0, BigDecimal.ZERO));
        balances.save(new LeaveBalance(UUID.randomUUID().toString(), tenant, employee, "ANNUAL", day.getYear(),
                new BigDecimal("20.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("20.00")));
        LeaveReferenceFixtures.calendar(jdbc, tenant, employee, 31);
        var pending = leave.submitLeaveRequest(tenant, employee, "Private name",
                new CreateLeaveRequestDto("ANNUAL", day, day, "FULL_DAY", "Private medical detail", "Private title"));
        assertSnapshot(tenant, pending.id(), "PENDING");
        var approved = decisions.approveLeaveRequest(tenant, UUID.randomUUID().toString(), List.of("HR_ADMIN"), pending.id(),
                "\"" + pending.version() + "\"", new DecisionRequest("Verified", "Reviewed"));
        assertSnapshot(tenant, pending.id(), "APPROVED");
        decisions.cancelLeaveRequest(tenant, employee, pending.id(), "\"" + approved.version() + "\"", "Changed plans");
        assertSnapshot(tenant, pending.id(), "CANCELLED");
    }

    private void assertSnapshot(String tenant, String id, String state) {
        var row = jdbc.queryForMap("SELECT * FROM leave.leave_outbox WHERE tenant_id=? AND aggregate_id=? ORDER BY stream_position DESC LIMIT 1", tenant, id);
        assertThat(((Number) row.get("wire_version")).intValue()).isEqualTo(1);
        assertThat(row.get("topic")).isEqualTo("leave.request.v1");
        long owner = jdbc.queryForObject("SELECT version FROM leave.leave_request WHERE tenant_id=? AND id=?", Long.class, tenant, id);
        assertThat(row.get("revision")).isEqualTo(owner + 1);
        var body = json.readTree(row.get("payload").toString());
        assertThat(body.path("state").asText()).isEqualTo(state);
        assertThat(body.path("workingDates").size()).isEqualTo(1);
        assertThat(new BigDecimal(body.path("chargedDays").asText())).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(body.toString()).doesNotContain("Private name", "Private medical detail", "Private title");
    }
}
