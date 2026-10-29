package com.hrflow.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.request.dto.ApprovalStepDto;
import com.hrflow.request.dto.CreateGeneralRequestDto;
import com.hrflow.request.dto.DecisionRequest;
import com.hrflow.request.dto.GeneralRequestResponse;
import com.hrflow.request.entity.GeneralRequest;
import com.hrflow.request.repository.GeneralRequestRepository;
import com.hrflow.request.service.CommandReceipts;
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
class CancelAPendingOwnRequestTest extends RequestReferenceTestSupport {

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

    @Test
    @DisplayName("Foreign requester and post-final cancellation are denied; duplicate idempotency key has one effect")
    void foreignRequesterAndPostFinalCancellationDeniedAndDuplicateKeyHasOneEffect() {
        String tenantId = "tenant-cnc-1";
        String requesterId = "emp-requester-cnc";
        String foreignEmployeeId = "emp-foreign-cnc";
        String managerId = "emp-mgr-cnc";
        referenceTeam(tenantId, requesterId, managerId);

        CreateGeneralRequestDto createDto = CreateGeneralRequestDto.builder()
                .title("Special Software License")
                .type("OTHER")
                .reason("IDE subscription")
                .quantity(1)
                .approvers(List.of(
                        ApprovalStepDto.builder().stepOrder(1).approverId(managerId).approverRole("MANAGER").build()
                ))
                .build();

        GeneralRequestResponse draft = requestService.createDraftRequest(tenantId, requesterId, "Dan", createDto);
        GeneralRequestResponse submitted = requestService.submitRequest(tenantId, requesterId, draft.getId(), null, "0");
        long version = submitted.getVersion();

        // 1. Foreign requester denied (403)
        assertThatThrownBy(() -> requestService.cancelRequest(tenantId, foreignEmployeeId, Set.of("EMPLOYEE"), submitted.getId(), Long.toString(version)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));

        // 2. Owner cancels pending request via CommandReceipts idempotency wrapper
        String idempotencyKey = java.util.UUID.randomUUID().toString();
        GeneralRequestResponse cancelled = commandReceipts.execute(
                tenantId,
                requesterId,
                "/requests/" + submitted.getId() + "/cancel",
                idempotencyKey,
                "cancel-body",
                GeneralRequestResponse.class,
                () -> requestService.cancelRequest(tenantId, requesterId, Set.of("EMPLOYEE"), submitted.getId(), Long.toString(version))
        );

        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");

        // Verify status in DB
        GeneralRequest inDb = generalRequestRepository.findByTenantIdAndId(tenantId, submitted.getId()).orElseThrow();
        assertThat(inDb.getStatus()).isEqualTo("CANCELLED");

        // 3. Retry with same idempotency key returns identical result without duplicating effect
        GeneralRequestResponse replayed = commandReceipts.execute(
                tenantId,
                requesterId,
                "/requests/" + submitted.getId() + "/cancel",
                idempotencyKey,
                "cancel-body",
                GeneralRequestResponse.class,
                () -> requestService.cancelRequest(tenantId, requesterId, Set.of("EMPLOYEE"), submitted.getId(), Long.toString(version))
        );
        assertThat(replayed.getStatus()).isEqualTo("CANCELLED");
        assertThat(replayed.getId()).isEqualTo(cancelled.getId());

        // 4. Post-final cancellation denied (attempting to cancel settled request without valid key -> 409 Conflict)
        assertThatThrownBy(() -> requestService.cancelRequest(tenantId, requesterId, Set.of("EMPLOYEE"), submitted.getId(), Long.toString(inDb.getVersion())))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(409));
    }
}
