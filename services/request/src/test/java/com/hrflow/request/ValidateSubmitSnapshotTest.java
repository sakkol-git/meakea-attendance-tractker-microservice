package com.hrflow.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.request.dto.ApprovalStepDto;
import com.hrflow.request.dto.CreateGeneralRequestDto;
import com.hrflow.request.dto.GeneralRequestResponse;
import com.hrflow.request.dto.UpdateGeneralRequestDto;
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
class ValidateSubmitSnapshotTest extends RequestReferenceTestSupport {

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
    @DisplayName("Unknown or self-manager relationships reject submit without losing draft")
    void unknownOrSelfManagerReferenceRejectsSubmitWithoutLosingDraft() {
        String tenantId = "tenant-snap-1";
        String employeeId = "emp-requester-1";

        // 1. Create a valid draft request with NO approvers
        CreateGeneralRequestDto createDto = CreateGeneralRequestDto.builder()
                .title("Ergonomic chair request")
                .type("OTHER")
                .reason("Back support required")
                .quantity(1)
                .build();

        GeneralRequestResponse draft = requestService.createDraftRequest(tenantId, employeeId, "Alice Requester", createDto);
        assertThat(draft.getId()).isNotNull();
        assertThat(draft.getStatus()).isEqualTo("DRAFT");
        assertThat(draft.getVersion()).isEqualTo(0L);

        // 2. Draft edits: update title and quantity
        UpdateGeneralRequestDto updateDto = UpdateGeneralRequestDto.builder()
                .title("Ergonomic chair request - updated")
                .quantity(2)
                .build();
        GeneralRequestResponse updatedDraft = requestService.updateDraftRequest(tenantId, employeeId, draft.getId(), updateDto, "0");
        assertThat(updatedDraft.getTitle()).isEqualTo("Ergonomic chair request - updated");
        assertThat(updatedDraft.getQuantity()).isEqualTo(2);

        // Unknown relationship is unavailable, not proof of an absent manager.
        assertThatThrownBy(() -> requestService.submitRequest(tenantId, employeeId, draft.getId(), List.of(), "1"))
                .isInstanceOfSatisfying(org.springframework.web.ErrorResponseException.class,
                        error -> assertThat(error.getStatusCode().value()).isEqualTo(503));

        // Verify draft is NOT lost! Status is still DRAFT
        GeneralRequest persistedAfterMissing = generalRequestRepository.findByTenantIdAndId(tenantId, draft.getId()).orElseThrow();
        assertThat(persistedAfterMissing.getStatus()).isEqualTo("DRAFT");

        referenceTeam(tenantId, employeeId, employeeId);
        // 4. Attempt submit with self-only approver chain (requester approving own request) -> Expect 422
        List<ApprovalStepDto> selfOnlyApprovers = List.of(
                ApprovalStepDto.builder().stepOrder(1).approverId(employeeId).approverRole("MANAGER").build()
        );
        assertThatThrownBy(() -> requestService.submitRequest(tenantId, employeeId, draft.getId(), selfOnlyApprovers, "1"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(422);
                    assertThat(rse.getReason()).contains("Independent manager required");
                });

        // Verify draft is STILL NOT lost! Status is still DRAFT
        GeneralRequest persistedAfterSelfOnly = generalRequestRepository.findByTenantIdAndId(tenantId, draft.getId()).orElseThrow();
        assertThat(persistedAfterSelfOnly.getStatus()).isEqualTo("DRAFT");

        // 5. Submit with valid independent approver chain -> Success!
        List<ApprovalStepDto> validApprovers = List.of(
                ApprovalStepDto.builder().stepOrder(1).approverId("emp-mgr-99").approverName("Bob Manager").approverRole("MANAGER").build(),
                ApprovalStepDto.builder().stepOrder(2).approverId("emp-dir-100").approverName("Carol Director").approverRole("DIRECTOR").build()
        );
        referenceTeam(tenantId, employeeId, "emp-mgr-99");
        GeneralRequestResponse submitted = requestService.submitRequest(tenantId, employeeId, draft.getId(), validApprovers, "1");
        assertThat(submitted.getStatus()).isEqualTo("PENDING_APPROVAL");
        assertThat(submitted.getVersion()).isGreaterThan(0L);
        assertThat(submitted.getSteps()).hasSize(2);
        assertThat(submitted.getSteps().get(0).getApproverId()).isEqualTo("emp-mgr-99");
        assertThat(submitted.getSteps().get(0).getStatus()).isEqualTo("PENDING");
        assertThat(submitted.getSteps().get(1).getApproverRole()).isEqualTo("HR_ADMIN");
        assertThat(submitted.getSteps().get(1).getApproverId()).isNull();
    }
}
