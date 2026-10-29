package com.hrflow.request.service;

import com.hrflow.request.dto.AttachmentCompleteRequest;
import com.hrflow.request.dto.AttachmentDownloadResponse;
import com.hrflow.request.dto.AttachmentPresignRequest;
import com.hrflow.request.dto.AttachmentPresignResponse;
import com.hrflow.request.dto.AttachmentResponse;
import com.hrflow.request.entity.GeneralRequest;
import com.hrflow.request.entity.RequestAttachment;
import com.hrflow.request.mapper.AttachmentMapper;
import com.hrflow.request.repository.GeneralRequestRepository;
import com.hrflow.request.repository.RequestAttachmentRepository;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class RequestAttachmentService {

    private final GeneralRequestRepository generalRequestRepository;
    private final RequestAttachmentRepository requestAttachmentRepository;
    private final AttachmentStorageService attachmentStorageService;
    private final RequestParticipantPolicy participantPolicy;
    private final TenantSqlScope tenantSqlScope;
    private final AttachmentMapper attachmentMapper;

    public RequestAttachmentService(
            GeneralRequestRepository generalRequestRepository,
            RequestAttachmentRepository requestAttachmentRepository,
            AttachmentStorageService attachmentStorageService,
            RequestParticipantPolicy participantPolicy,
            TenantSqlScope tenantSqlScope,
            AttachmentMapper attachmentMapper) {
        this.generalRequestRepository = generalRequestRepository;
        this.requestAttachmentRepository = requestAttachmentRepository;
        this.attachmentStorageService = attachmentStorageService;
        this.participantPolicy = participantPolicy;
        this.tenantSqlScope = tenantSqlScope;
        this.attachmentMapper = attachmentMapper;
    }

    public AttachmentPresignResponse presignAttachment(String tenantId, String actorId, Set<String> actorRoles, String requestId, AttachmentPresignRequest presign) {
        tenantSqlScope.use(tenantId);
        GeneralRequest request = generalRequestRepository.findByTenantIdAndId(tenantId, requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        participantPolicy.requireParticipant(tenantId, actorId, actorRoles, request);
        attachmentStorageService.validateAttachmentMetadata(presign.getFileName(), presign.getContentType(), presign.getFileSize());

        String attachmentId = UUID.randomUUID().toString();
        String storageKey = attachmentStorageService.generateStorageKey(tenantId, requestId, attachmentId);

        RequestAttachment attachment = RequestAttachment.builder()
                .id(attachmentId)
                .tenantId(tenantId)
                .requestId(requestId)
                .fileName(presign.getFileName().trim())
                .contentType(presign.getContentType().toLowerCase())
                .fileSize(presign.getFileSize())
                .checksumSha256(presign.getChecksumSha256().toLowerCase())
                .storageKey(storageKey)
                .status("PENDING")
                .createdAt(Instant.now())
                .build();

        requestAttachmentRepository.save(attachment);

        String uploadUrl = "/api/v1/requests/" + requestId + "/attachments/" + attachmentId + "/upload";
        return AttachmentPresignResponse.builder()
                .attachmentId(attachmentId)
                .uploadUrl(uploadUrl)
                .requiredHeaders(Map.of("Content-Type", attachment.getContentType()))
                .build();
    }

    public void uploadAttachmentDirect(String tenantId, String actorId, Set<String> actorRoles, String requestId, String attachmentId, InputStream inputStream) throws IOException {
        tenantSqlScope.use(tenantId);
        GeneralRequest request = generalRequestRepository.findByTenantIdAndId(tenantId, requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        participantPolicy.requireParticipant(tenantId, actorId, actorRoles, request);

        RequestAttachment attachment = requestAttachmentRepository.lock(tenantId, requestId, attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment record not found"));

        attachmentStorageService.storeFile(attachment.getStorageKey(), inputStream, attachment.getChecksumSha256(), attachment.getFileSize(), attachment.getContentType());
    }

    public AttachmentResponse completeAttachment(String tenantId, String actorId, Set<String> actorRoles, String requestId, String attachmentId, AttachmentCompleteRequest complete) {
        tenantSqlScope.use(tenantId);
        GeneralRequest request = generalRequestRepository.findByTenantIdAndId(tenantId, requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        participantPolicy.requireParticipant(tenantId, actorId, actorRoles, request);

        RequestAttachment attachment = requestAttachmentRepository.lock(tenantId, requestId, attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment record not found"));

        if (complete != null && (complete.getFileSize() != null && complete.getFileSize() != attachment.getFileSize()
                || complete.getChecksumSha256() != null && !complete.getChecksumSha256().equalsIgnoreCase(attachment.getChecksumSha256()))) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Completion metadata does not match the reservation");
        }
        if ("FINALIZED".equals(attachment.getStatus())) return attachmentMapper.toResponse(attachment);
        if (!attachmentStorageService.fileExists(attachment.getStorageKey())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Attachment file was not uploaded to storage");
        }

        attachment.complete(Instant.now());
        requestAttachmentRepository.saveAndFlush(attachment);

        return attachmentMapper.toResponse(attachment);
    }

    public AttachmentDownloadResponse downloadAttachment(String tenantId, String actorId, Set<String> actorRoles, String requestId, String attachmentId) {
        tenantSqlScope.use(tenantId);
        GeneralRequest request = generalRequestRepository.findByTenantIdAndId(tenantId, requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        participantPolicy.requireParticipant(tenantId, actorId, actorRoles, request);

        RequestAttachment attachment = requestAttachmentRepository.findByTenantIdAndRequestIdAndId(tenantId, requestId, attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));

        if (!"FINALIZED".equals(attachment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment upload is incomplete or pending");
        }

        String downloadUrl = "/api/v1/requests/" + requestId + "/attachments/" + attachmentId + "/file";
        return AttachmentDownloadResponse.builder()
                .downloadUrl(downloadUrl)
                .fileName(attachment.getFileName())
                .contentType(attachment.getContentType())
                .fileSize(attachment.getFileSize())
                .checksumSha256(attachment.getChecksumSha256())
                .build();
    }

    public byte[] getAttachmentBytes(String tenantId, String actorId, Set<String> actorRoles, String requestId, String attachmentId) throws IOException {
        tenantSqlScope.use(tenantId);
        GeneralRequest request = generalRequestRepository.findByTenantIdAndId(tenantId, requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        participantPolicy.requireParticipant(tenantId, actorId, actorRoles, request);

        RequestAttachment attachment = requestAttachmentRepository.findByTenantIdAndRequestIdAndId(tenantId, requestId, attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));

        if (!"FINALIZED".equals(attachment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment upload is not finalized");
        }

        return attachmentStorageService.readFile(attachment.getStorageKey());
    }
}
