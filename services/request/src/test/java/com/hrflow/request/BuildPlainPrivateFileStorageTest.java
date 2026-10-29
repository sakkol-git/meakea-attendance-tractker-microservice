package com.hrflow.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.request.dto.ApprovalStepDto;
import com.hrflow.request.dto.AttachmentCompleteRequest;
import com.hrflow.request.dto.AttachmentDownloadResponse;
import com.hrflow.request.dto.AttachmentPresignRequest;
import com.hrflow.request.dto.AttachmentPresignResponse;
import com.hrflow.request.dto.AttachmentResponse;
import com.hrflow.request.dto.CreateGeneralRequestDto;
import com.hrflow.request.dto.GeneralRequestResponse;
import com.hrflow.request.service.AttachmentStorageService;
import com.hrflow.request.service.RequestService;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
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
class BuildPlainPrivateFileStorageTest extends RequestReferenceTestSupport {

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
        registry.add("attachment.storage.dir", () -> "/tmp/hrflow-test-attachments-" + System.currentTimeMillis());
    }

    @Autowired
    RequestService requestService;

    @Autowired
    AttachmentStorageService attachmentStorageService;

    @Test
    @DisplayName("Traversal, spoofed MIME and oversized bytes are rejected; wrong participant receives no bytes")
    void traversalSpoofedMimeOversizedRejectedAndWrongParticipantReceivesNoBytes() throws Exception {
        String tenantId = "tenant-att-1";
        String requesterId = "emp-att-requester";
        String managerId = "emp-att-manager";
        referenceTeam(tenantId, requesterId, managerId);
        String strangerId = "emp-att-stranger";

        CreateGeneralRequestDto createDto = CreateGeneralRequestDto.builder()
                .title("Ergonomic Keyboard with Receipt")
                .type("OTHER")
                .reason("Hardware reimbursement")
                .quantity(1)
                .approvers(List.of(
                        ApprovalStepDto.builder().stepOrder(1).approverId(managerId).approverRole("MANAGER").build()
                ))
                .build();

        GeneralRequestResponse draft = requestService.createDraftRequest(tenantId, requesterId, "Frank", createDto);

        // 1. Path traversal rejected (400)
        assertThatThrownBy(() -> requestService.presignAttachment(tenantId, requesterId, Set.of("EMPLOYEE"), draft.getId(),
                AttachmentPresignRequest.builder()
                        .fileName("../../etc/passwd")
                        .contentType("application/pdf")
                        .fileSize(1024)
                        .checksumSha256("aabbcc")
                        .build()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));

        // 2. Spoofed or disallowed MIME type rejected (422)
        assertThatThrownBy(() -> requestService.presignAttachment(tenantId, requesterId, Set.of("EMPLOYEE"), draft.getId(),
                AttachmentPresignRequest.builder()
                        .fileName("malware.exe")
                        .contentType("application/x-msdownload")
                        .fileSize(1024)
                        .checksumSha256("aabbcc")
                        .build()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(422));

        // 3. Oversized file rejected (422)
        assertThatThrownBy(() -> requestService.presignAttachment(tenantId, requesterId, Set.of("EMPLOYEE"), draft.getId(),
                AttachmentPresignRequest.builder()
                        .fileName("giant_file.pdf")
                        .contentType("application/pdf")
                        .fileSize(20 * 1024 * 1024) // 20 MB > 10 MB limit
                        .checksumSha256("aabbcc")
                        .build()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(422));

        // 4. Valid presign
        byte[] validBytes = "%PDF-1.4 sample invoice content for test".getBytes(StandardCharsets.UTF_8);
        String expectedChecksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(validBytes));

        AttachmentPresignResponse presignResp = requestService.presignAttachment(tenantId, requesterId, Set.of("EMPLOYEE"), draft.getId(),
                AttachmentPresignRequest.builder()
                        .fileName("receipt.pdf")
                        .contentType("application/pdf")
                        .fileSize(validBytes.length)
                        .checksumSha256(expectedChecksum)
                        .build());
        assertThat(presignResp.getAttachmentId()).isNotNull();

        // 5. Upload bytes
        requestService.uploadAttachmentDirect(tenantId, requesterId, Set.of("EMPLOYEE"), draft.getId(), presignResp.getAttachmentId(),
                new ByteArrayInputStream(validBytes));

        // 6. Complete attachment
        AttachmentResponse completed = requestService.completeAttachment(tenantId, requesterId, Set.of("EMPLOYEE"), draft.getId(),
                presignResp.getAttachmentId(), new AttachmentCompleteRequest());
        assertThat(completed.getStatus()).isEqualTo("FINALIZED");

        // 7. Legitimate participant can download
        AttachmentDownloadResponse download = requestService.downloadAttachment(tenantId, requesterId, Set.of("EMPLOYEE"), draft.getId(), presignResp.getAttachmentId());
        assertThat(download.getFileName()).isEqualTo("receipt.pdf");
        byte[] readBytes = requestService.getAttachmentBytes(tenantId, requesterId, Set.of("EMPLOYEE"), draft.getId(), presignResp.getAttachmentId());
        assertThat(readBytes).isEqualTo(validBytes);

        // 8. Assigned approver can also download
        // A requester-supplied draft suggestion is not a participant grant.
        assertThatThrownBy(() -> requestService.getAttachmentBytes(tenantId, managerId, Set.of("MANAGER"), draft.getId(), presignResp.getAttachmentId()))
                .isInstanceOfSatisfying(ResponseStatusException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(403));
        requestService.submitRequest(tenantId, requesterId, draft.getId(), null, "0");
        byte[] mgrReadBytes = requestService.getAttachmentBytes(tenantId, managerId, Set.of("MANAGER"), draft.getId(), presignResp.getAttachmentId());
        assertThat(mgrReadBytes).isEqualTo(validBytes);

        // 9. Wrong participant (stranger) receives 403 Forbidden and NO file bytes!
        assertThatThrownBy(() -> requestService.getAttachmentBytes(tenantId, strangerId, Set.of("EMPLOYEE"), draft.getId(), presignResp.getAttachmentId()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));

        // 10. Cross-tenant actor receives 404 / 403 and NO file bytes!
        assertThatThrownBy(() -> requestService.getAttachmentBytes("tenant-foreign", requesterId, Set.of("EMPLOYEE"), draft.getId(), presignResp.getAttachmentId()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isIn(403, 404));
    }
}
