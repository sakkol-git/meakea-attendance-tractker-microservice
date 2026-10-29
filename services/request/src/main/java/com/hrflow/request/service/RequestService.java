package com.hrflow.request.service;

import com.hrflow.request.dto.ApprovalStepDto;
import com.hrflow.request.dto.AttachmentCompleteRequest;
import com.hrflow.request.dto.AttachmentDownloadResponse;
import com.hrflow.request.dto.AttachmentPresignRequest;
import com.hrflow.request.dto.AttachmentPresignResponse;
import com.hrflow.request.dto.AttachmentResponse;
import com.hrflow.request.dto.CommentDto;
import com.hrflow.request.dto.CommentResponse;
import com.hrflow.request.dto.CreateGeneralRequestDto;
import com.hrflow.request.dto.DecisionRequest;
import com.hrflow.request.dto.FulfillRequest;
import com.hrflow.request.dto.GeneralRequestResponse;
import com.hrflow.request.dto.MaterialItemResponse;
import com.hrflow.request.dto.RequestLineDto;
import com.hrflow.request.dto.RequestTypeResponse;
import com.hrflow.request.dto.UpdateGeneralRequestDto;
import com.hrflow.request.entity.ApprovalStep;
import com.hrflow.request.entity.GeneralRequest;
import com.hrflow.request.entity.RequestLine;
import com.hrflow.request.mapper.ApprovalStepMapper;
import com.hrflow.request.mapper.AttachmentMapper;
import com.hrflow.request.mapper.CommentMapper;
import com.hrflow.request.mapper.GeneralRequestMapper;
import com.hrflow.request.mapper.MaterialItemMapper;
import com.hrflow.request.mapper.RequestLineMapper;
import com.hrflow.request.mapper.RequestTypeMapper;
import com.hrflow.request.repository.ApprovalStepRepository;
import com.hrflow.request.repository.GeneralRequestRepository;
import com.hrflow.request.repository.MaterialItemRepository;
import com.hrflow.request.repository.RequestAttachmentRepository;
import com.hrflow.request.repository.RequestCommentRepository;
import com.hrflow.request.repository.RequestLineRepository;
import com.hrflow.request.repository.RequestTypeRepository;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@lombok.RequiredArgsConstructor
@Transactional
public class RequestService {

    private final GeneralRequestRepository generalRequestRepository;
    private final RequestLines requestLines;
    private final RequestFormData forms;
    private final ApprovalStepRepository approvalStepRepository;
    private final RequestCommentRepository requestCommentRepository;
    private final RequestAttachmentRepository requestAttachmentRepository;
    private final RequestTypeRepository requestTypeRepository;
    private final MaterialItemRepository materialItemRepository;
    private final TenantSqlScope tenantSqlScope;
    private final RequestParticipantPolicy participantPolicy;
    private final RequestWorkflowService workflowService;
    private final RequestCommentService commentService;
    private final RequestAttachmentService attachmentService;

    private final GeneralRequestMapper requestMapper;
    private final ApprovalStepMapper stepMapper;
    private final CommentMapper commentMapper;
    private final AttachmentMapper attachmentMapper;
    private final RequestTypeMapper typeMapper;
    private final MaterialItemMapper materialMapper;

    public List<RequestTypeResponse> listTypes(String tenantId) {
        tenantSqlScope.use(tenantId);
        return requestTypeRepository.findByTenantIdAndActiveTrue(tenantId)
                .stream().map(typeMapper::toResponse).toList();
    }

    public List<MaterialItemResponse> listMaterials(String tenantId) {
        tenantSqlScope.use(tenantId);
        return materialItemRepository.findByTenantIdAndActiveTrue(tenantId)
                .stream().map(materialMapper::toResponse).toList();
    }

    public GeneralRequestResponse createDraftRequest(String tenantId, String employeeId, String employeeName, CreateGeneralRequestDto dto) {
        tenantSqlScope.use(tenantId);

        if (dto.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Quantity must be positive");
        }
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Title is required");
        }
        if (dto.getType() == null || dto.getType().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Type is required");
        }
        if (dto.getReason() == null || dto.getReason().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Reason is required");
        }

        String requestId = UUID.randomUUID().toString();
        GeneralRequest request = GeneralRequest.builder()
                .id(requestId)
                .tenantId(tenantId)
                .employeeId(employeeId)
                .employeeName(employeeName)
                .type(dto.getType().toUpperCase())
                .title(dto.getTitle().trim())
                .reason(dto.getReason().trim())
                .status("DRAFT")
                .quantity(dto.getQuantity())
                .formData(forms.merge(null, dto))
                .version(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        generalRequestRepository.save(request);

        requestLines.replace(tenantId, requestId, dto.getType(), dto.getLines());

        return getRequestDetail(tenantId, employeeId, Set.of("EMPLOYEE"), requestId);
    }

    public GeneralRequestResponse updateDraftRequest(String tenantId, String employeeId, String requestId, UpdateGeneralRequestDto dto, String ifMatch) {
        tenantSqlScope.use(tenantId);
        GeneralRequest request = generalRequestRepository.findByTenantIdAndId(tenantId, requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        if (!request.getEmployeeId().equals(employeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only requester can modify draft");
        }
        if (!"DRAFT".equals(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only draft requests can be edited");
        }
        RevisionPreconditions.require(ifMatch, request.getVersion());

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            request.setTitle(dto.getTitle().trim());
        }
        if (dto.getReason() != null && !dto.getReason().isBlank()) {
            request.setReason(dto.getReason().trim());
        }
        if (dto.getQuantity() != null) {
            if (dto.getQuantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Quantity must be positive");
            }
            request.setQuantity(dto.getQuantity());
        }
        request.setFormData(forms.merge(request.getFormData(), dto));
        request.setUpdatedAt(Instant.now());

        requestLines.replace(tenantId, requestId, request.getType(), dto.getLines());

        generalRequestRepository.save(request);
        return getRequestDetail(tenantId, employeeId, Set.of("EMPLOYEE"), requestId);
    }

    public GeneralRequestResponse submitRequest(String tenantId, String employeeId, String requestId, List<ApprovalStepDto> overrideApprovers, String ifMatch) {
        workflowService.submitRequest(tenantId, employeeId, requestId, overrideApprovers, ifMatch);
        return getRequestDetail(tenantId, employeeId, Set.of("EMPLOYEE"), requestId);
    }

    public GeneralRequestResponse approveRequestStep(String tenantId, String actorId, Set<String> actorRoles, String requestId, DecisionRequest decision, String ifMatch) {
        workflowService.approveRequestStep(tenantId, actorId, actorRoles, requestId, decision, ifMatch);
        return getRequestDetail(tenantId, actorId, actorRoles, requestId);
    }

    public GeneralRequestResponse rejectRequestStep(String tenantId, String actorId, Set<String> actorRoles, String requestId, DecisionRequest decision, String ifMatch) {
        workflowService.rejectRequestStep(tenantId, actorId, actorRoles, requestId, decision, ifMatch);
        return getRequestDetail(tenantId, actorId, actorRoles, requestId);
    }

    public GeneralRequestResponse cancelRequest(String tenantId, String actorId, Set<String> actorRoles, String requestId, String ifMatch) {
        workflowService.cancelRequest(tenantId, actorId, actorRoles, requestId, ifMatch);
        return getRequestDetail(tenantId, actorId, actorRoles, requestId);
    }

    public GeneralRequestResponse fulfillApprovedMaterial(String tenantId, String actorId, Set<String> actorRoles, String requestId, FulfillRequest fulfill, String ifMatch) {
        workflowService.fulfillApprovedMaterial(tenantId, actorId, actorRoles, requestId, fulfill, ifMatch);
        return getRequestDetail(tenantId, actorId, actorRoles, requestId);
    }

    public GeneralRequestResponse getRequestDetail(String tenantId, String actorId, Set<String> actorRoles, String requestId) {
        tenantSqlScope.use(tenantId);
        GeneralRequest request = generalRequestRepository.findByTenantIdAndId(tenantId, requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        participantPolicy.requireParticipant(tenantId, actorId, actorRoles, request);

        GeneralRequestResponse response = requestMapper.toResponse(request);

        response.setLines(requestLines.read(tenantId, requestId));
        response.setItems(response.getLines());

        List<ApprovalStep> steps = approvalStepRepository.findByTenantIdAndRequestIdOrderByStepOrderAsc(tenantId, requestId);
        response.setSteps(steps.stream().map(stepMapper::toDto).toList());

        List<com.hrflow.request.entity.RequestComment> comments = requestCommentRepository.findByTenantIdAndRequestIdOrderByCreatedAtAsc(tenantId, requestId);
        response.setComments(comments.stream().map(commentMapper::toResponse).toList());

        List<com.hrflow.request.entity.RequestAttachment> attachments = requestAttachmentRepository.findByTenantIdAndRequestId(tenantId, requestId);
        response.setAttachments(attachments.stream().map(attachmentMapper::toResponse).toList());

        return response;
    }

    public List<GeneralRequestResponse> getMyRequests(String tenantId, String employeeId) {
        tenantSqlScope.use(tenantId);
        return generalRequestRepository.findByTenantIdAndEmployeeIdOrderByCreatedAtDesc(tenantId, employeeId)
                .stream().map(requestMapper::toResponse).toList();
    }

    public List<GeneralRequestResponse> getInbox(String tenantId, String approverId, Set<String> approverRoles) {
        tenantSqlScope.use(tenantId);
        return generalRequestRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, "PENDING_APPROVAL")
                .stream().filter(request -> !request.getEmployeeId().equals(approverId))
                .filter(request -> approvalStepRepository.findByTenantIdAndRequestIdOrderByStepOrderAsc(tenantId, request.getId())
                        .stream().filter(step -> "PENDING".equals(step.getStatus()))
                        .min(Comparator.comparingInt(ApprovalStep::getStepOrder))
                        .map(step -> participantPolicy.canDecide(step, approverId, approverRoles)).orElse(false))
                .map(requestMapper::toResponse).toList();
    }

    // Delegations for comments & attachments keeping backward compatibility & SRP
    public CommentResponse addComment(String tenantId, String actorId, String actorName, Set<String> actorRoles, String requestId, CommentDto commentDto) {
        return commentService.addComment(tenantId, actorId, actorName, actorRoles, requestId, commentDto);
    }

    public List<CommentResponse> listComments(String tenantId, String actorId, Set<String> actorRoles, String requestId) {
        return commentService.listComments(tenantId, actorId, actorRoles, requestId);
    }

    public AttachmentPresignResponse presignAttachment(String tenantId, String actorId, Set<String> actorRoles, String requestId, AttachmentPresignRequest presign) {
        return attachmentService.presignAttachment(tenantId, actorId, actorRoles, requestId, presign);
    }

    public void uploadAttachmentDirect(String tenantId, String actorId, Set<String> actorRoles, String requestId, String attachmentId, InputStream inputStream) throws IOException {
        attachmentService.uploadAttachmentDirect(tenantId, actorId, actorRoles, requestId, attachmentId, inputStream);
    }

    public AttachmentResponse completeAttachment(String tenantId, String actorId, Set<String> actorRoles, String requestId, String attachmentId, AttachmentCompleteRequest complete) {
        return attachmentService.completeAttachment(tenantId, actorId, actorRoles, requestId, attachmentId, complete);
    }

    public AttachmentDownloadResponse downloadAttachment(String tenantId, String actorId, Set<String> actorRoles, String requestId, String attachmentId) {
        return attachmentService.downloadAttachment(tenantId, actorId, actorRoles, requestId, attachmentId);
    }

    public byte[] getAttachmentBytes(String tenantId, String actorId, Set<String> actorRoles, String requestId, String attachmentId) throws IOException {
        return attachmentService.getAttachmentBytes(tenantId, actorId, actorRoles, requestId, attachmentId);
    }
}
