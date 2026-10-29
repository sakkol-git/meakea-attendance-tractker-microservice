package com.hrflow.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.request.dto.ApprovalStepDto;
import com.hrflow.request.dto.CreateGeneralRequestDto;
import com.hrflow.request.dto.DecisionRequest;
import com.hrflow.request.dto.FulfillRequest;
import com.hrflow.request.dto.GeneralRequestResponse;
import com.hrflow.request.dto.RequestLineDto;
import com.hrflow.request.entity.GeneralRequest;
import com.hrflow.request.entity.MaterialItem;
import com.hrflow.request.repository.GeneralRequestRepository;
import com.hrflow.request.repository.MaterialItemRepository;
import com.hrflow.request.service.CommandReceipts;
import com.hrflow.request.service.RequestService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
class FulfillApprovedMaterialHandoverTest extends RequestReferenceTestSupport {

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
    CommandReceipts commandReceipts;

    @Autowired
    GeneralRequestRepository generalRequestRepository;

    @Autowired
    MaterialItemRepository materialItemRepository;

    @Test
    void approvedNonMaterialCannotBecomeAFakeHandover() {
        String tenant = UUID.randomUUID().toString(), employee = UUID.randomUUID().toString();
        String manager = UUID.randomUUID().toString(), hr = UUID.randomUUID().toString();
        referenceTeam(tenant, employee, manager);
        var draft = requestService.createDraftRequest(tenant, employee, "Synthetic", CreateGeneralRequestDto.builder()
                .title("General approval").type("OTHER").reason("UAT").build());
        var pending = requestService.submitRequest(tenant, employee, draft.getId(), null, "0");
        var managed = requestService.approveRequestStep(tenant, manager, Set.of("MANAGER"), draft.getId(), null, Long.toString(pending.getVersion()));
        var approved = requestService.approveRequestStep(tenant, hr, Set.of("HR_ADMIN"), draft.getId(), null, Long.toString(managed.getVersion()));
        assertThatThrownBy(() -> requestService.fulfillApprovedMaterial(tenant, hr, Set.of("HR_ADMIN"), draft.getId(),
                FulfillRequest.builder().handoverReason("Cannot issue an approval as equipment").build(), Long.toString(approved.getVersion())))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(422));
        assertThat(generalRequestRepository.findByTenantIdAndId(tenant, draft.getId()).orElseThrow().getStatus()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("Unapproved request cannot be fulfilled and repeated handover cannot duplicate its action")
    void unapprovedCannotBeFulfilledAndRepeatedHandoverCannotDuplicate() {
        String tenantId = "tenant-ful-1";
        String requesterId = "emp-requester-ful";
        String managerId = "emp-mgr-ful";
        referenceTeam(tenantId, requesterId, managerId);
        String hrId = "emp-hr-ful";
        String nonHrId = "emp-regular-ful";

        // Create material item in catalogue with advisory stock = 5
        String matId = UUID.randomUUID().toString();
        materialItemRepository.save(MaterialItem.builder()
                .id(matId)
                .tenantId(tenantId)
                .code("MONITOR-34-UW")
                .name("34 inch Ultrawide Monitor")
                .category("EQUIPMENT")
                .advisoryStock(5)
                .unit("UNIT")
                .active(true)
                .build());

        CreateGeneralRequestDto createDto = CreateGeneralRequestDto.builder()
                .title("Ultrawide monitor for dev work")
                .type("MATERIAL")
                .reason("Replaces 2 faulty displays")
                .quantity(1)
                .lines(List.of(
                        RequestLineDto.builder().materialItemId(matId).quantity(1).notes("Desk 4B").build()
                ))
                .approvers(List.of(
                        ApprovalStepDto.builder().stepOrder(1).approverId(managerId).approverRole("MANAGER").build()
                ))
                .build();

        GeneralRequestResponse draft = requestService.createDraftRequest(tenantId, requesterId, "Eve", createDto);
        GeneralRequestResponse submitted = requestService.submitRequest(tenantId, requesterId, draft.getId(), null, "0");
        long version = submitted.getVersion();

        // 1. Unapproved request cannot be fulfilled (409 Conflict)
        assertThatThrownBy(() -> requestService.fulfillApprovedMaterial(tenantId, hrId, Set.of("HR_ADMIN"), submitted.getId(),
                FulfillRequest.builder().handoverReason("Issued serial #1234").build(), Long.toString(version)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(409));

        // 2. Manager approves the request
        GeneralRequestResponse approved = requestService.approveRequestStep(tenantId, managerId, Set.of("MANAGER"), submitted.getId(),
                DecisionRequest.builder().reason("Approved by manager").build(), Long.toString(version));
        assertThat(approved.getStatus()).isEqualTo("PENDING_APPROVAL");
        approved = requestService.approveRequestStep(tenantId, hrId, Set.of("HR_ADMIN"), submitted.getId(), null, Long.toString(approved.getVersion()));
        assertThat(approved.getStatus()).isEqualTo("APPROVED");
        long approvedVersion = approved.getVersion();

        // 3. Non-HR actor denied from fulfillment (403 Forbidden)
        assertThatThrownBy(() -> requestService.fulfillApprovedMaterial(tenantId, nonHrId, Set.of("EMPLOYEE"), submitted.getId(),
                FulfillRequest.builder().handoverReason("Handing over").build(), Long.toString(approvedVersion)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));

        // 4. Blank handover notes rejected (422 Unprocessable Entity)
        assertThatThrownBy(() -> requestService.fulfillApprovedMaterial(tenantId, hrId, Set.of("HR_ADMIN"), submitted.getId(),
                FulfillRequest.builder().handoverReason("   ").build(), Long.toString(approvedVersion)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(422));

        // 5. Valid fulfillment by HR with handover reason and idempotency key
        String cmdKey = java.util.UUID.randomUUID().toString();
        GeneralRequestResponse fulfilled = commandReceipts.execute(
                tenantId,
                hrId,
                "/requests/" + submitted.getId() + "/fulfill",
                cmdKey,
                "fulfill-body",
                GeneralRequestResponse.class,
                () -> requestService.fulfillApprovedMaterial(tenantId, hrId, Set.of("HR_ADMIN"), submitted.getId(),
                        FulfillRequest.builder().handoverReason("Handed over monitor serial MON-998877 to Eve at IT desk").build(),
                        Long.toString(approvedVersion))
        );

        assertThat(fulfilled.getStatus()).isEqualTo("FULFILLED");
        assertThat(fulfilled.getHandoverNotes()).contains("MON-998877");

        // 6. Idempotent retry returns identical fulfilled response
        GeneralRequestResponse replayed = commandReceipts.execute(
                tenantId,
                hrId,
                "/requests/" + submitted.getId() + "/fulfill",
                cmdKey,
                "fulfill-body",
                GeneralRequestResponse.class,
                () -> requestService.fulfillApprovedMaterial(tenantId, hrId, Set.of("HR_ADMIN"), submitted.getId(),
                        FulfillRequest.builder().handoverReason("Handed over monitor serial MON-998877 to Eve at IT desk").build(),
                        Long.toString(approvedVersion))
        );
        assertThat(replayed.getStatus()).isEqualTo("FULFILLED");

        // 7. Repeated handover without idempotency key fails with 409 Conflict
        GeneralRequest inDb = generalRequestRepository.findByTenantIdAndId(tenantId, submitted.getId()).orElseThrow();
        assertThatThrownBy(() -> requestService.fulfillApprovedMaterial(tenantId, hrId, Set.of("HR_ADMIN"), submitted.getId(),
                FulfillRequest.builder().handoverReason("Duplicate attempt").build(), Long.toString(inDb.getVersion())))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(409));
    }
}
