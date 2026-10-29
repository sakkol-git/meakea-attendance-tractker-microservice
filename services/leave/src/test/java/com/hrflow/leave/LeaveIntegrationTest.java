package com.hrflow.leave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hrflow.leave.entity.LeaveBalance;
import com.hrflow.leave.entity.LeaveType;
import com.hrflow.leave.repository.LeaveBalanceRepository;
import com.hrflow.leave.repository.LeaveTypeRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@AutoConfigureMockMvc
@Testcontainers
class LeaveIntegrationTest {
    @org.springframework.test.context.bean.override.mockito.MockitoBean java.time.Clock clock;
    @org.junit.jupiter.api.BeforeEach void futureLeaveAtTheFixtureClock() {
        org.mockito.Mockito.when(clock.instant()).thenReturn(java.time.Instant.parse("2026-05-31T00:00:00Z"));
    }
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.hrflow.leave.organization.LeaveCalendarBootstrap calendarBootstrap;
    @org.junit.jupiter.api.BeforeEach void calendarReferenceReady() {
        org.mockito.Mockito.when(calendarBootstrap.isReady()).thenReturn(true);
    }

    // Isolates authorization/transaction tests; the actual broker replay is verified separately.
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.hrflow.leave.organization.LeaveEmployeeBootstrap bootstrap;
    @org.junit.jupiter.api.BeforeEach void readyReferenceGate() {
        org.mockito.Mockito.when(bootstrap.isReady()).thenReturn(true);
    }

    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

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
    MockMvc mvc;

    @Autowired
    LeaveTypeRepository leaveTypeRepository;

    @Autowired
    LeaveBalanceRepository leaveBalanceRepository;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void commandReplayIsBoundToTheVerifiedAccountAndMissingKeysCannotMutateLeave() throws Exception {
        UUID tenant = UUID.randomUUID(), first = UUID.randomUUID(), second = UUID.randomUUID();
        leaveTypeRepository.save(new LeaveType(UUID.randomUUID().toString(), tenant.toString(), "UNPAID", "Unpaid",
                BigDecimal.ZERO, false, true, false, 0, BigDecimal.ZERO));
        LeaveReferenceFixtures.calendar(jdbc, tenant.toString(), first.toString(), 127);
        LeaveReferenceFixtures.calendar(jdbc, tenant.toString(), second.toString(), 127);
        String body = """
                {"type":"UNPAID","startDate":"2026-11-02","endDate":"2026-11-02","portion":"FULL_DAY","reason":"Personal"}
                """;
        String key = UUID.randomUUID().toString();
        mvc.perform(post("/leave/requests").with(employee(tenant, first)).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.employeeId").value(first.toString()));
        mvc.perform(post("/leave/requests").with(employee(tenant, second)).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.employeeId").value(second.toString()));
        mvc.perform(post("/leave/requests").with(employee(tenant, first))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isPreconditionRequired());
        mvc.perform(post("/leave/requests").with(employee(tenant, first)).header("Idempotency-Key", "not-a-key")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isPreconditionRequired());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM leave.leave_request WHERE tenant_id=?", Integer.class, tenant.toString())).isEqualTo(2);
    }

    @Test
    void explicitCatalogPagesAreBoundedAndPreserveLegacyArraysAndEmployeeScope() throws Exception {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID(), other = UUID.randomUUID();
        for (int index = 0; index < 3; index++) {
            leaveTypeRepository.save(new LeaveType(UUID.randomUUID().toString(), tenant.toString(),
                    "TYPE_" + index, "Type " + index, BigDecimal.TEN, true, true, false, 0, BigDecimal.ZERO));
            leaveBalanceRepository.save(new LeaveBalance(UUID.randomUUID().toString(), tenant.toString(),
                    employee.toString(), "TYPE_" + index, 2026, BigDecimal.TEN, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.TEN));
        }
        leaveBalanceRepository.save(new LeaveBalance(UUID.randomUUID().toString(), tenant.toString(),
                other.toString(), "TYPE_0", 2026, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN));
        mvc.perform(get("/leave/types").param("page", "0").param("size", "2").with(employee(tenant, employee)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items.length()").value(2)).andExpect(jsonPath("$.items[0].code").value("TYPE_0"));
        mvc.perform(get("/leave/types").param("page", "1").param("size", "2").with(employee(tenant, employee)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].code").value("TYPE_2"));
        mvc.perform(get("/leave/balances/me").param("page", "0").param("size", "2").param("year", "2026")
                .with(employee(tenant, employee))).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3)).andExpect(jsonPath("$.items.length()").value(2));
        mvc.perform(get("/leave/types").with(employee(tenant, employee))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").exists());
        mvc.perform(get("/leave/types").param("page", "-1").with(employee(tenant, employee)))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/leave/balances/me").param("page", "0").param("size", "101").with(employee(tenant, employee)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Full leave workflow: preview, submit with idempotency, approve, and cancel")
    void fullLeaveWorkflowLifecycle() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        LeaveReferenceFixtures.employee(jdbc, tenantId.toString(), managerId.toString(), null);
        LeaveReferenceFixtures.employee(jdbc, tenantId.toString(), employeeId.toString(), managerId.toString());
        LeaveReferenceFixtures.calendar(jdbc, tenantId.toString(), employeeId.toString(), 31);

        // 1. Seed Type and Balance
        leaveTypeRepository.save(new LeaveType(
                UUID.randomUUID().toString(),
                tenantId.toString(),
                "ANNUAL",
                "Annual Leave",
                new BigDecimal("20.00"),
                true,
                true,
                false,
                0,
                new BigDecimal("5.00")
        ));

        leaveBalanceRepository.save(new LeaveBalance(
                UUID.randomUUID().toString(),
                tenantId.toString(),
                employeeId.toString(),
                "ANNUAL",
                2026,
                new BigDecimal("15.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("15.00")
        ));

        // 2. GET /leave/types
        mvc.perform(get("/leave/types").with(employee(tenantId, employeeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("ANNUAL"))
                .andExpect(jsonPath("$[0].name").value("Annual Leave"));

        // 3. GET /leave/balances/me
        mvc.perform(get("/leave/balances/me").with(employee(tenantId, employeeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("ANNUAL"))
                .andExpect(jsonPath("$[0].available").value(15.0));

        // 4. POST /leave/requests/preview (2 working days: June 1 and 2, 2026)
        String previewPayload = """
                {
                  "type": "ANNUAL",
                  "startDate": "2026-06-01",
                  "endDate": "2026-06-02",
                  "portion": "FULL_DAY"
                }
                """;
        mvc.perform(post("/leave/requests/preview").with(employee(tenantId, employeeId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(previewPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chargeableDays").value(2.0))
                .andExpect(jsonPath("$.balanceBefore").value(15.0))
                .andExpect(jsonPath("$.balanceAfter").value(13.0));

        // 5. POST /leave/requests with Idempotency-Key
        String submitPayload = """
                {
                  "type": "ANNUAL",
                  "startDate": "2026-06-01",
                  "endDate": "2026-06-02",
                  "portion": "FULL_DAY",
                  "reason": "Family camping trip",
                  "title": "Summer Camp"
                }
                """;
        String idempotencyKey = UUID.randomUUID().toString();

        String createdJson = mvc.perform(post("/leave/requests").with(employee(tenantId, employeeId))
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(submitPayload))
                .andExpect(status().isCreated())
                .andExpect(header().string("ETag", "\"1\""))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.days").value(2.0))
                .andExpect(jsonPath("$.version").value(1))
                .andReturn().getResponse().getContentAsString();

        JsonNode createdNode = jsonMapper.readTree(createdJson);
        String requestId = createdNode.get("id").asText();

        // 6. Replay submit with same Idempotency-Key returns identical response without duplicating
        mvc.perform(post("/leave/requests").with(employee(tenantId, employeeId))
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(submitPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(requestId))
                .andExpect(jsonPath("$.version").value(1));

        // 7. GET /leave/requests/me
        mvc.perform(get("/leave/requests/me").with(employee(tenantId, employeeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(requestId));

        // 8. GET /leave/requests/pending (as manager)
        mvc.perform(get("/leave/requests/pending").with(manager(tenantId, managerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(requestId))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        // 9. POST /leave/requests/{id}/approve (as manager)
        String decisionPayload = """
                {
                  "reason": "Have fun!",
                  "note": "Coverage arranged"
                }
                """;
        String approvalKey = UUID.randomUUID().toString();
        mvc.perform(post("/leave/requests/{id}/approve", requestId).with(manager(tenantId, managerId))
                .header("Idempotency-Key", approvalKey)
                .header("If-Match", "\"1\"")
                .contentType(MediaType.APPLICATION_JSON)
                .content(decisionPayload))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"2\""))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.version").value(2));

        mvc.perform(post("/leave/requests/{id}/approve", requestId).with(manager(tenantId, managerId))
                .header("Idempotency-Key", approvalKey).header("If-Match", "\"1\"")
                .contentType(MediaType.APPLICATION_JSON).content(decisionPayload))
                .andExpect(status().isOk()).andExpect(header().string("ETag", "\"2\""));

        // 10. POST /leave/requests/{id}/cancel (as employee)
        String cancelPayload = """
                {
                  "reason": "Cancelled trip due to weather"
                }
                """;
        mvc.perform(post("/leave/requests/{id}/cancel", requestId).header("Idempotency-Key", UUID.randomUUID().toString()).with(employee(tenantId, employeeId))
                .header("If-Match", "\"2\"")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cancelPayload))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"3\""))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.version").value(3));

        // 11. Verify balance restored back to 15 available
        mvc.perform(get("/leave/balances/me").with(employee(tenantId, employeeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].available").value(15.0))
                .andExpect(jsonPath("$[0].used").value(0.0))
                .andExpect(jsonPath("$[0].pending").value(0.0));
    }

    private static JwtRequestPostProcessor employee(UUID tenantId, UUID employeeId) {
        return jwt().jwt(token -> token
                .subject(employeeId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("employeeId", employeeId.toString())
                .claim("name", "Jane Doe")
                .claim("roles", List.of("EMPLOYEE"))
        ).authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
    }

    private static JwtRequestPostProcessor manager(UUID tenantId, UUID managerId) {
        return jwt().jwt(token -> token
                .subject(managerId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("employeeId", managerId.toString())
                .claim("name", "Manager Bob")
                .claim("roles", List.of("MANAGER"))
        ).authorities(new SimpleGrantedAuthority("ROLE_MANAGER"));
    }
}
