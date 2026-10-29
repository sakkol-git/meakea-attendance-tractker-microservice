package com.hrflow.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.request.dto.ApprovalStepDto;
import com.hrflow.request.dto.CreateGeneralRequestDto;
import com.hrflow.request.dto.DecisionRequest;
import com.hrflow.request.dto.GeneralRequestResponse;
import com.hrflow.request.entity.GeneralRequest;
import com.hrflow.request.repository.GeneralRequestRepository;
import com.hrflow.request.service.RequestService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class RejectTheActiveRequestStepTest extends RequestReferenceTestSupport {

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
    RequestService requestService;

    @Autowired
    GeneralRequestRepository generalRequestRepository;

    @Test
    @DisplayName("Blank rejection reason creates no action; valid rejection transitions to REJECTED and duplicate decision is rejected")
    void blankRejectionReasonCreatesNoActionAndDuplicateDecisionRejected() {
        String tenantId = "tenant-rej-1";
        String requesterId = "emp-req-rej";
        String managerId = "emp-mgr-rej";
        referenceTeam(tenantId, requesterId, managerId);

        CreateGeneralRequestDto createDto = CreateGeneralRequestDto.builder()
                .title("Office Supplies")
                .type("OTHER")
                .reason("Need extra whiteboard markers")
                .quantity(1)
                .approvers(List.of(
                        ApprovalStepDto.builder().stepOrder(1).approverId(managerId).approverRole("MANAGER").build()
                ))
                .build();

        GeneralRequestResponse draft = requestService.createDraftRequest(tenantId, requesterId, "Carol", createDto);
        GeneralRequestResponse submitted = requestService.submitRequest(tenantId, requesterId, draft.getId(), null, "0");
        long version = submitted.getVersion();

        // 1. Blank rejection reason rejected with 422 Unprocessable Entity
        assertThatThrownBy(() -> requestService.rejectRequestStep(tenantId, managerId, Set.of("MANAGER"), submitted.getId(),
                DecisionRequest.builder().reason("   ").build(), Long.toString(version)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(422);
                    assertThat(rse.getReason()).contains("Rejection reason is mandatory");
                });

        // Request remains in PENDING_APPROVAL
        GeneralRequest afterBlank = generalRequestRepository.findByTenantIdAndId(tenantId, submitted.getId()).orElseThrow();
        assertThat(afterBlank.getStatus()).isEqualTo("PENDING_APPROVAL");

        // 2. Requester cannot reject own request (403)
        assertThatThrownBy(() -> requestService.rejectRequestStep(tenantId, requesterId, Set.of("MANAGER"), submitted.getId(),
                DecisionRequest.builder().reason("Self reject").build(), Long.toString(version)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));

        // 3. Valid rejection by assigned manager
        GeneralRequestResponse rejected = requestService.rejectRequestStep(tenantId, managerId, Set.of("MANAGER"), submitted.getId(),
                DecisionRequest.builder().reason("Budget exhausted for Q4").build(), Long.toString(version));

        assertThat(rejected.getStatus()).isEqualTo("REJECTED");
        assertThat(rejected.getSteps().get(0).getStatus()).isEqualTo("REJECTED");
        assertThat(rejected.getSteps().get(0).getDecisionReason()).isEqualTo("Budget exhausted for Q4");
        long newVersion = rejected.getVersion();

        // 4. Repeat decision attempt on terminal REJECTED request is rejected with 409 Conflict
        assertThatThrownBy(() -> requestService.rejectRequestStep(tenantId, managerId, Set.of("MANAGER"), submitted.getId(),
                DecisionRequest.builder().reason("Reject again").build(), Long.toString(newVersion)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(409));
    }
}
