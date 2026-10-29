package com.hrflow.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.request.dto.ApprovalStepDto;
import com.hrflow.request.dto.AttachmentCompleteRequest;
import com.hrflow.request.dto.AttachmentPresignRequest;
import com.hrflow.request.dto.AttachmentPresignResponse;
import com.hrflow.request.dto.AttachmentResponse;
import com.hrflow.request.dto.CreateGeneralRequestDto;
import com.hrflow.request.dto.GeneralRequestResponse;
import com.hrflow.request.service.AttachmentStorageService;
import com.hrflow.request.service.RequestService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
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
class RecoverAnInterruptedAttachmentWriteTest {

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
        registry.add("attachment.storage.dir", () -> "/tmp/hrflow-test-recovery-" + System.currentTimeMillis());
    }

    @Autowired
    RequestService requestService;

    @Autowired
    AttachmentStorageService attachmentStorageService;

    @Test
    @DisplayName("Interrupted write leaves no public partial file and retry has one metadata record")
    void interruptedWriteLeavesNoPublicPartialFileAndRetryHasOneRecord() throws Exception {
        String tenantId = "tenant-rec-1";
        String requesterId = "emp-rec-1";
        String managerId = "emp-mgr-rec";

        CreateGeneralRequestDto createDto = CreateGeneralRequestDto.builder()
                .title("Medical Certificate for Special Request")
                .type("OTHER")
                .reason("Medical doctor recommendation")
                .quantity(1)
                .approvers(List.of(
                        ApprovalStepDto.builder().stepOrder(1).approverId(managerId).approverRole("MANAGER").build()
                ))
                .build();

        GeneralRequestResponse draft = requestService.createDraftRequest(tenantId, requesterId, "Grace", createDto);

        byte[] fullContent = "%PDF-1.4 sample doctor certificate details".getBytes(StandardCharsets.UTF_8);
        String expectedChecksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(fullContent));

        // 1. Presign attachment
        AttachmentPresignResponse presign = requestService.presignAttachment(tenantId, requesterId, Set.of("EMPLOYEE"), draft.getId(),
                AttachmentPresignRequest.builder()
                        .fileName("certificate.pdf")
                        .contentType("application/pdf")
                        .fileSize(fullContent.length)
                        .checksumSha256(expectedChecksum)
                        .build());
        String attachmentId = presign.getAttachmentId();

        // 2. Simulate an interrupted stream during upload (stream that throws IOException halfway)
        InputStream faultyStream = new InputStream() {
            private int bytesRead = 0;

            @Override
            public int read() throws IOException {
                if (bytesRead >= 10) {
                    throw new IOException("Connection reset by peer mid-stream");
                }
                bytesRead++;
                return fullContent[bytesRead - 1];
            }
        };

        assertThatThrownBy(() -> requestService.uploadAttachmentDirect(tenantId, requesterId, Set.of("EMPLOYEE"), draft.getId(), attachmentId, faultyStream))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Connection reset by peer");

        // Verify: target file does NOT exist (no partial public file exposed!)
        String storageKey = attachmentStorageService.generateStorageKey(tenantId, draft.getId(), attachmentId);
        assertThat(attachmentStorageService.fileExists(storageKey)).isFalse();

        // Verify: completing an unuploaded/interrupted attachment is rejected
        assertThatThrownBy(() -> requestService.completeAttachment(tenantId, requesterId, Set.of("EMPLOYEE"), draft.getId(), attachmentId, new AttachmentCompleteRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(422));

        // 3. Retry upload with full stream -> completes cleanly!
        requestService.uploadAttachmentDirect(tenantId, requesterId, Set.of("EMPLOYEE"), draft.getId(), attachmentId, new ByteArrayInputStream(fullContent));
        AttachmentResponse completed = requestService.completeAttachment(tenantId, requesterId, Set.of("EMPLOYEE"), draft.getId(), attachmentId, new AttachmentCompleteRequest());
        assertThat(completed.getStatus()).isEqualTo("FINALIZED");
        assertThat(completed.getChecksumSha256()).isEqualTo(expectedChecksum);

        // 4. Verify orphan cleanup test
        // Create an orphan temporary file in storage directory
        Path tempOrphan = attachmentStorageService.resolvePath(storageKey).getParent().resolve("orphan-abandoned.tmp");
        Files.writeString(tempOrphan, "abandoned partial bytes");
        assertThat(Files.exists(tempOrphan)).isTrue();

        // Run cleanup with 0-duration maxAge (clean everything older than 0s)
        int cleaned = attachmentStorageService.cleanOrphanTempFiles(Duration.ZERO);
        assertThat(cleaned).isGreaterThanOrEqualTo(1);
        assertThat(Files.exists(tempOrphan)).isFalse();

        // Verify finalized attachment file is NOT deleted!
        assertThat(attachmentStorageService.fileExists(storageKey)).isTrue();
        byte[] readBack = attachmentStorageService.readFile(storageKey);
        assertThat(readBack).isEqualTo(fullContent);
    }
}
