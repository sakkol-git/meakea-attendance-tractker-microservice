package com.hrflow.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.request.dto.ApprovalStepDto;
import com.hrflow.request.dto.CreateGeneralRequestDto;
import com.hrflow.request.dto.DecisionRequest;
import com.hrflow.request.service.RequestService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.ErrorResponseException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class ServerApprovalChainTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine").withInitScript("context-schema.sql");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", POSTGRES::getUsername);
        p.add("spring.datasource.password", POSTGRES::getPassword);
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @MockitoBean com.hrflow.request.organization.RequestEmployeeBootstrap replay;
    @BeforeEach void referenceReady() { org.mockito.Mockito.when(replay.isReady()).thenReturn(true); }
    @Autowired RequestService requests;
    @Autowired JdbcTemplate jdbc;

    @Test void everyRequestTypeUsesTheServerManagerThenSharedHrChainDespiteClientOverrides() {
        String tenant = id(), employee = id(), manager = id(), stranger = id();
        employee(tenant, employee, manager);
        employee(tenant, manager, null);
        for (String type : List.of("OTHER", "MATERIAL", "REMOTE_WORK", "LATE_ARRIVAL", "EARLY_LEAVE")) {
            var draft = requests.createDraftRequest(tenant, employee, "Synthetic", CreateGeneralRequestDto.builder()
                    .title("Synthetic").type(type).reason("Personal").quantity(1)
                    .approvers(List.of(ApprovalStepDto.builder().approverId(stranger).approverRole("HR_ADMIN").build())).build());
            var submitted = requests.submitRequest(tenant, employee, draft.getId(),
                    List.of(ApprovalStepDto.builder().approverId(employee).approverRole("DIRECTOR").build()), "0");
            assertThat(submitted.getSteps()).extracting(ApprovalStepDto::getApproverRole).containsExactly("MANAGER", "HR_ADMIN");
            assertThat(submitted.getSteps().getFirst().getApproverId()).isEqualTo(manager);
            assertThat(submitted.getSteps().getLast().getApproverId()).isNull();
            assertThat(requests.getInbox(tenant, stranger, Set.of("HR_ADMIN"))).isEmpty();
            var managerDecision = requests.approveRequestStep(tenant, manager, Set.of("MANAGER"), submitted.getId(), null,
                    Long.toString(submitted.getVersion()));
            assertThat(managerDecision.getStatus()).isEqualTo("PENDING_APPROVAL");
            var approved = requests.approveRequestStep(tenant, stranger, Set.of("HR_ADMIN"), submitted.getId(), null,
                    Long.toString(managerDecision.getVersion()));
            assertThat(approved.getStatus()).isEqualTo("APPROVED");
            assertThatThrownBy(() -> requests.approveRequestStep(tenant, id(), Set.of("HR_ADMIN"), submitted.getId(), null,
                    Long.toString(managerDecision.getVersion()))).isInstanceOfSatisfying(ErrorResponseException.class,
                            error -> assertThat(error.getStatusCode().value()).isEqualTo(412));
        }
    }

    @Test void verifiedAbsentManagerIsRecordedSkippedAndIndependentHrIsStillRequired() {
        String tenant = id(), employee = id();
        employee(tenant, employee, null);
        var draft = requests.createDraftRequest(tenant, employee, "Synthetic", CreateGeneralRequestDto.builder()
                .title("Synthetic").type("OTHER").reason("Personal").quantity(1).build());
        var submitted = requests.submitRequest(tenant, employee, draft.getId(), null, "0");
        assertThat(submitted.getStatus()).isEqualTo("PENDING_APPROVAL");
        assertThat(submitted.getSteps().getFirst().getStatus()).isEqualTo("SKIPPED");
        assertThat(submitted.getSteps().getFirst().getDecidedAt()).isNotNull();
        assertThat(submitted.getSteps().getFirst().getDecisionReason()).isEqualTo("NO_MANAGER_ASSIGNED");
        assertThat(submitted.getSteps().getLast().getStatus()).isEqualTo("PENDING");
        assertThatThrownBy(() -> requests.approveRequestStep(tenant, employee, Set.of("HR_ADMIN"), submitted.getId(), null,
                Long.toString(submitted.getVersion()))).isInstanceOfSatisfying(ErrorResponseException.class,
                        error -> assertThat(error.getStatusCode().value()).isEqualTo(403));
        assertThat(requests.getInbox(tenant, id(), Set.of("HR_ADMIN"))).hasSize(1);
    }

    @Test void managerReassignmentRevokesTheOldSnapshotWithoutGivingTheNewManagerAnUnrecordedStep() {
        String tenant = id(), employee = id(), manager = id(), replacement = id();
        employee(tenant, employee, manager); employee(tenant, manager, null); employee(tenant, replacement, null);
        var draft = requests.createDraftRequest(tenant, employee, "Synthetic", CreateGeneralRequestDto.builder()
                .title("Synthetic").type("OTHER").reason("Personal").quantity(1).build());
        var submitted = requests.submitRequest(tenant, employee, draft.getId(), null, "0");
        jdbc.update("UPDATE request.employee_projection SET manager_id=?,revision=2 WHERE tenant_id=? AND employee_id=?", replacement, tenant, employee);
        for (String caller : List.of(manager, replacement)) {
            assertThat(requests.getInbox(tenant, caller, Set.of("MANAGER"))).isEmpty();
            assertThatThrownBy(() -> requests.approveRequestStep(tenant, caller, Set.of("MANAGER"), submitted.getId(), null,
                    Long.toString(submitted.getVersion()))).isInstanceOfSatisfying(ErrorResponseException.class,
                            error -> assertThat(error.getStatusCode().value()).isEqualTo(403));
        }
    }

    private void employee(String tenant, String employee, String manager) {
        jdbc.update("INSERT INTO request.employee_projection(tenant_id,employee_id,manager_id,active,revision) VALUES (?,?,?,true,1)", tenant, employee, manager);
    }
    private static String id() { return UUID.randomUUID().toString(); }
}
