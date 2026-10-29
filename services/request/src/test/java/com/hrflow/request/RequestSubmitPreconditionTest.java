package com.hrflow.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hrflow.request.dto.CreateGeneralRequestDto;
import com.hrflow.request.dto.UpdateGeneralRequestDto;
import com.hrflow.request.service.RequestService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RequestSubmitPreconditionTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine").withInitScript("context-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", () -> "request_app");
        p.add("spring.datasource.password", () -> "test-pass");
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @Autowired MockMvc mvc;
    @Autowired RequestService requests;
    @MockitoBean com.hrflow.request.organization.RequestEmployeeBootstrap replay;

    @Test void submissionRequiresTheReviewedDraftRevisionAndExactRetryKeepsItsCommittedOutcome() throws Exception {
        String tenant = UUID.randomUUID().toString(), employee = UUID.randomUUID().toString(), account = UUID.randomUUID().toString();
        var actor = jwt().jwt(token -> token.subject(account).claim("tenantId", tenant).claim("employeeId", employee).claim("roles", List.of("EMPLOYEE")));
        var draft = requests.createDraftRequest(tenant, employee, "Synthetic", CreateGeneralRequestDto.builder()
                .title("Original").type("OTHER").reason("Private").quantity(1).build());
        requests.updateDraftRequest(tenant, employee, draft.getId(), UpdateGeneralRequestDto.builder().title("Edited after review").build(), "0");
        // Preconditions must be checked even while organization references have not arrived.
        mvc.perform(post("/requests/{id}/submit", draft.getId()).with(actor).header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isPreconditionRequired());
        for (String stale : List.of("\"0\"", "\"1\"\"", "*")) {
            mvc.perform(post("/requests/{id}/submit", draft.getId()).with(actor).header("If-Match", stale)
                    .header("Idempotency-Key", UUID.randomUUID().toString())).andExpect(status().isPreconditionFailed());
        }
        var admin = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        assertThat(admin.queryForObject("SELECT status FROM request.general_request WHERE tenant_id=?", String.class, tenant)).isEqualTo("DRAFT");
        assertThat(admin.queryForObject("SELECT count(*) FROM request.approval_step WHERE tenant_id=?", Integer.class, tenant)).isZero();
        assertThat(admin.queryForObject("SELECT count(*) FROM request.command_receipt_v2 WHERE tenant_id=?", Integer.class, tenant)).isZero();
        org.mockito.Mockito.when(replay.isReady()).thenReturn(true);
        admin.update("INSERT INTO request.employee_projection(tenant_id,employee_id,manager_id,active,revision) VALUES (?,?,null,true,1)", tenant, employee);
        String key = UUID.randomUUID().toString();
        String first = mvc.perform(post("/requests/{id}/submit", draft.getId()).with(actor).header("If-Match", "\"1\"").header("Idempotency-Key", key)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"note\":\"Please submit\",\"reason\":\"Please submit\"}"))
                .andExpect(status().isOk()).andExpect(header().string("ETag", "\"2\"")).andReturn().getResponse().getContentAsString();
        String retry = mvc.perform(post("/requests/{id}/submit", draft.getId()).with(actor).header("If-Match", "\"1\"").header("Idempotency-Key", key)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"note\":\"Please submit\",\"reason\":\"Please submit\"}"))
                .andExpect(status().isOk()).andExpect(header().string("ETag", "\"2\"")).andReturn().getResponse().getContentAsString();
        assertThat(retry).isEqualTo(first);
        assertThat(admin.queryForObject("SELECT count(*) FROM request.approval_step WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(2);
        assertThat(admin.queryForObject("SELECT count(*) FROM request.command_receipt_v2 WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(1);
        assertThat(admin.queryForObject("SELECT count(*) FROM request.workflow_history WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(1);
    }
}
