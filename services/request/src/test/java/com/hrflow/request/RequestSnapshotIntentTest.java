package com.hrflow.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.request.dto.CreateGeneralRequestDto;
import com.hrflow.request.service.RequestService;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@Testcontainers
class RequestSnapshotIntentTest extends RequestReferenceTestSupport {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine").withInitScript("context-schema.sql");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", POSTGRES::getUsername);
        p.add("spring.datasource.password", POSTGRES::getPassword);
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @Autowired RequestService requests;
    @Autowired TransactionTemplate transactions;

    @Test void committedOwnerSnapshotRetainsWorkflowFactsWithoutPrivateTextAndRollsBackWithTheCommand() {
        String tenant = UUID.randomUUID().toString(), employee = UUID.randomUUID().toString(), manager = UUID.randomUUID().toString();
        referenceTeam(tenant, employee, manager);
        var draft = requests.createDraftRequest(tenant, employee, "Private name", CreateGeneralRequestDto.builder()
                .title("Private title").type("OTHER").reason("Private medical detail").quantity(1).build());
        assertThatThrownBy(() -> transactions.executeWithoutResult(tx -> {
            requests.submitRequest(tenant, employee, draft.getId(), null, "0");
            throw new IllegalStateException("interrupt");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(referenceJdbc.queryForObject("SELECT count(*) FROM request.request_outbox WHERE tenant_id=?", Integer.class, tenant)).isZero();
        assertThat(referenceJdbc.queryForObject("SELECT count(*) FROM request.workflow_history WHERE tenant_id=?", Integer.class, tenant)).isZero();
        var pending = requests.submitRequest(tenant, employee, draft.getId(), null, "0");
        var first = referenceJdbc.queryForMap("SELECT payload::text AS payload,revision FROM request.request_outbox WHERE tenant_id=?", tenant);
        var json = JsonMapper.builder().build().readTree((String) first.get("payload"));
        assertThat(json.path("state").asText()).isEqualTo("PENDING_APPROVAL");
        assertThat(json.path("employeeId").asText()).isEqualTo(employee);
        assertThat(json.path("requestType").asText()).isEqualTo("OTHER");
        assertThat(json.path("createdAt").asText()).isNotBlank();
        assertThat(json.path("steps").size()).isEqualTo(2);
        assertThat(json.path("steps").get(0).path("approverId").asText()).isEqualTo(manager);
        assertThat((String) first.get("payload")).doesNotContain("Private name", "Private title", "medical detail");
        assertThat(((Number) first.get("revision")).longValue()).isEqualTo(pending.getVersion() + 1);
        assertThat(referenceJdbc.queryForObject("SELECT actor_account_id FROM request.workflow_history WHERE tenant_id=?", String.class, tenant)).isEqualTo(employee);
        assertThatThrownBy(() -> referenceJdbc.update("DELETE FROM request.workflow_history WHERE tenant_id=?", tenant))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        var next = requests.approveRequestStep(tenant, manager, Set.of("MANAGER"), draft.getId(), null, Long.toString(pending.getVersion()));
        assertThat(referenceJdbc.queryForList("SELECT revision FROM request.request_outbox WHERE tenant_id=? ORDER BY revision", Long.class, tenant))
                .containsExactly(pending.getVersion() + 1, next.getVersion() + 1);
    }
}
