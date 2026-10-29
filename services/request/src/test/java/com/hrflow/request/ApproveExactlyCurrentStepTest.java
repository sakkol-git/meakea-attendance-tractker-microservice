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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
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
class ApproveExactlyCurrentStepTest extends RequestReferenceTestSupport {

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
    void unrelatedManagerCannotReadOrDecideAnAssignedManagersRequest() {
        String tenant = "tenant-role-denial", employee = "employee-role-denial", assigned = "manager-assigned", stranger = "manager-other";
        referenceTeam(tenant, employee, assigned);
        var draft = requestService.createDraftRequest(tenant, employee, "Synthetic", CreateGeneralRequestDto.builder()
                .title("Private").type("OTHER").reason("Private reason").quantity(1)
                .approvers(List.of(ApprovalStepDto.builder().approverId(assigned).approverRole("MANAGER").build())).build());
        var pending = requestService.submitRequest(tenant, employee, draft.getId(), null, "0");
        long before = pending.getVersion();
        assertThatThrownBy(() -> requestService.getRequestDetail(tenant, stranger, Set.of("MANAGER"), pending.getId()))
                .isInstanceOfSatisfying(ResponseStatusException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(403));
        assertThat(requestService.getInbox(tenant, stranger, Set.of("MANAGER"))).isEmpty();
        assertThatThrownBy(() -> requestService.approveRequestStep(tenant, stranger, Set.of("MANAGER"), pending.getId(), null, "\"" + before + "\""))
                .isInstanceOfSatisfying(ResponseStatusException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(403));
        assertThatThrownBy(() -> requestService.rejectRequestStep(tenant, stranger, Set.of("MANAGER"), pending.getId(),
                DecisionRequest.builder().reason("No").build(), "\"" + before + "\""))
                .isInstanceOfSatisfying(ResponseStatusException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(403));
        var unchanged = requestService.getRequestDetail(tenant, employee, Set.of("EMPLOYEE"), pending.getId());
        assertThat(unchanged.getVersion()).isEqualTo(before);
        assertThat(unchanged.getStatus()).isEqualTo("PENDING_APPROVAL");
        assertThat(unchanged.getSteps().getFirst().getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Only current assigned independent approver advances step and stale If-Match is rejected")
    void onlyCurrentAssignedIndependentApproverAdvancesStep() {
        String tenantId = "tenant-appr-1";
        String requesterId = "emp-req-1";
        String managerId = "emp-mgr-1";
        referenceTeam(tenantId, requesterId, managerId);
        String directorId = "emp-dir-1";
        String strangerId = "emp-stranger-1";

        CreateGeneralRequestDto createDto = CreateGeneralRequestDto.builder()
                .title("Conference Attendance")
                .type("OTHER")
                .reason("Java enterprise training")
                .quantity(1)
                .approvers(List.of(
                        ApprovalStepDto.builder().stepOrder(1).approverId(managerId).approverRole("MANAGER").build(),
                        ApprovalStepDto.builder().stepOrder(2).approverId(directorId).approverRole("DIRECTOR").build()
                ))
                .build();

        GeneralRequestResponse draft = requestService.createDraftRequest(tenantId, requesterId, "Alice", createDto);
        GeneralRequestResponse submitted = requestService.submitRequest(tenantId, requesterId, draft.getId(), null, "0");
        assertThat(submitted.getStatus()).isEqualTo("PENDING_APPROVAL");
        long initialVersion = submitted.getVersion();

        // 1. Stranger is denied (403)
        assertThatThrownBy(() -> requestService.approveRequestStep(tenantId, strangerId, Set.of("EMPLOYEE"), submitted.getId(),
                DecisionRequest.builder().reason("Looks good").build(), Long.toString(initialVersion)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));

        // 2. Requester cannot approve own request (self-approval denied, 403)
        assertThatThrownBy(() -> requestService.approveRequestStep(tenantId, requesterId, Set.of("MANAGER"), submitted.getId(),
                DecisionRequest.builder().reason("Self approved").build(), Long.toString(initialVersion)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));

        // 3. Step 2 approver cannot approve while Step 1 is still pending
        assertThatThrownBy(() -> requestService.approveRequestStep(tenantId, directorId, Set.of("HR_ADMIN"), submitted.getId(),
                DecisionRequest.builder().reason("Early approval").build(), Long.toString(initialVersion)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));

        // 4. Stale If-Match is rejected with 412 Precondition Failed
        assertThatThrownBy(() -> requestService.approveRequestStep(tenantId, managerId, Set.of("MANAGER"), submitted.getId(),
                DecisionRequest.builder().reason("Approved").build(), "999"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(412));

        // 5. Manager approves Step 1 with correct If-Match
        GeneralRequestResponse step1Approved = requestService.approveRequestStep(tenantId, managerId, Set.of("MANAGER"), submitted.getId(),
                DecisionRequest.builder().reason("Manager approved").build(), Long.toString(initialVersion));
        assertThat(step1Approved.getStatus()).isEqualTo("PENDING_APPROVAL");
        assertThat(step1Approved.getSteps().get(0).getStatus()).isEqualTo("APPROVED");
        assertThat(step1Approved.getSteps().get(1).getStatus()).isEqualTo("PENDING");
        assertThat(step1Approved.getVersion()).isEqualTo(initialVersion + 1);

        // 6. Director approves Step 2
        GeneralRequestResponse step2Approved = requestService.approveRequestStep(tenantId, directorId, Set.of("HR_ADMIN"), submitted.getId(),
                DecisionRequest.builder().reason("Director approved").build(), Long.toString(step1Approved.getVersion()));
        assertThat(step2Approved.getStatus()).isEqualTo("APPROVED");
        assertThat(step2Approved.getSteps().get(1).getStatus()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("Concurrent approval race advances exactly one step and preserves determinism")
    void concurrentApprovalRaceHandledDeterministically() throws Exception {
        String tenantId = "tenant-appr-2";
        String requesterId = "emp-req-2";
        String managerId = "emp-mgr-2";
        referenceTeam(tenantId, requesterId, managerId);

        CreateGeneralRequestDto createDto = CreateGeneralRequestDto.builder()
                .title("New Monitor")
                .type("MATERIAL")
                .reason("Hardware replacement")
                .quantity(1)
                .approvers(List.of(
                        ApprovalStepDto.builder().stepOrder(1).approverId(managerId).approverRole("MANAGER").build()
                ))
                .build();

        GeneralRequestResponse draft = requestService.createDraftRequest(tenantId, requesterId, "Bob", createDto);
        GeneralRequestResponse submitted = requestService.submitRequest(tenantId, requesterId, draft.getId(), null, "0");
        long version = submitted.getVersion();

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictOrPreconditionFailedCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    requestService.approveRequestStep(tenantId, managerId, Set.of("MANAGER"), submitted.getId(),
                            DecisionRequest.builder().reason("Concurrent approval").build(), Long.toString(version));
                    successCount.incrementAndGet();
                } catch (ResponseStatusException rse) {
                    if (rse.getStatusCode().value() == 412 || rse.getStatusCode().value() == 409) {
                        conflictOrPreconditionFailedCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictOrPreconditionFailedCount.get()).isEqualTo(1);

        GeneralRequest finalized = generalRequestRepository.findByTenantIdAndId(tenantId, submitted.getId()).orElseThrow();
        assertThat(finalized.getStatus()).isEqualTo("PENDING_APPROVAL");
        assertThat(approvalStepsPending(tenantId, submitted.getId())).isEqualTo(1L);
    }
    private long approvalStepsPending(String tenant, String id) {
        return referenceJdbc.queryForObject("SELECT count(*) FROM request.approval_step WHERE tenant_id=? AND request_id=? AND status='PENDING'", Long.class, tenant, id);
    }
}
