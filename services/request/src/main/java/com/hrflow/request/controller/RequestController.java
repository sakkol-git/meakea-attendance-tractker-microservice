package com.hrflow.request.controller;

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
import com.hrflow.request.dto.RequestTypeResponse;
import com.hrflow.request.dto.UpdateGeneralRequestDto;
import com.hrflow.request.service.CommandReceipts;
import com.hrflow.request.service.RequestAttachmentService;
import com.hrflow.request.service.RequestCommentService;
import com.hrflow.request.service.RequestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@PreAuthorize("isAuthenticated()")
public class RequestController {

    private final RequestService requestService;
    private final RequestCommentService commentService;
    private final RequestAttachmentService attachmentService;
    private final CommandReceipts commandReceipts;
    private final com.hrflow.request.service.RequestWorkflowService workflow;

    public RequestController(
            RequestService requestService,
            RequestCommentService commentService,
            RequestAttachmentService attachmentService,
            CommandReceipts commandReceipts, com.hrflow.request.service.RequestWorkflowService workflow) {
        this.requestService = requestService;
        this.commentService = commentService;
        this.attachmentService = attachmentService;
        this.commandReceipts = commandReceipts;
        this.workflow = workflow;
    }

    @PostMapping("/requests")
    public ResponseEntity<GeneralRequestResponse> createDraft(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateGeneralRequestDto dto) {
        String tenant = tenantId(jwt);
        String employee = employeeId(jwt);
        String name = employeeName(jwt);

        GeneralRequestResponse response = commandReceipts.execute(
                tenant,
                actorId(jwt),
                "/requests",
                idempotencyKey,
                dto,
                GeneralRequestResponse.class,
                () -> requestService.createDraftRequest(tenant, employee, name, dto)
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag('"' + Long.toString(response.getVersion()) + '"')
                .body(response);
    }

    @PatchMapping("/requests/{id}")
    public ResponseEntity<GeneralRequestResponse> updateDraft(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @RequestBody UpdateGeneralRequestDto dto) {
        String tenant = tenantId(jwt);
        String employee = employeeId(jwt);

        GeneralRequestResponse response = requestService.updateDraftRequest(tenant, employee, id, dto, ifMatch);
        return ResponseEntity.ok()
                .eTag('"' + Long.toString(response.getVersion()) + '"')
                .body(response);
    }

    @GetMapping("/requests/{id}")
    public ResponseEntity<GeneralRequestResponse> getRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id) {
        String tenant = tenantId(jwt);
        String actor = employeeId(jwt);
        Set<String> roles = roles(jwt);

        GeneralRequestResponse response = requestService.getRequestDetail(tenant, actor, roles, id);
        return ResponseEntity.ok()
                .eTag('"' + Long.toString(response.getVersion()) + '"')
                .body(response);
    }

    @PostMapping("/requests/{id}/submit")
    public ResponseEntity<GeneralRequestResponse> submitRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) tools.jackson.databind.JsonNode submission) {
        String tenant = tenantId(jwt);
        String employee = employeeId(jwt);

        if (submission != null && !submission.isNull() && !submission.isArray() && !submission.isObject())
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Submit accepts an optional command object or legacy approver array");
        GeneralRequestResponse response = commandReceipts.execute(
                tenant,
                actorId(jwt),
                "/requests/" + id + "/submit",
                idempotencyKey,
                submission != null && !submission.isNull() ? submission : id,
                GeneralRequestResponse.class,
                () -> {
                    workflow.submitRequest(tenant, caller(jwt), id, ifMatch);
                    return requestService.getRequestDetail(tenant, employee, roles(jwt), id);
                }
        );

        return ResponseEntity.ok()
                .eTag('"' + Long.toString(response.getVersion()) + '"')
                .body(response);
    }

    @PostMapping("/requests/{id}/approve")
    public ResponseEntity<GeneralRequestResponse> approveRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) DecisionRequest decision) {
        String tenant = tenantId(jwt);
        String actor = employeeId(jwt);
        Set<String> roles = roles(jwt);

        GeneralRequestResponse response = commandReceipts.execute(
                tenant,
                actorId(jwt),
                "/requests/" + id + "/approve",
                idempotencyKey,
                decision != null ? decision : id,
                GeneralRequestResponse.class,
                () -> {
                    workflow.approveRequestStep(tenant, caller(jwt), id, decision, ifMatch);
                    return requestService.getRequestDetail(tenant, actor, roles, id);
                }
        );

        return ResponseEntity.ok()
                .eTag('"' + Long.toString(response.getVersion()) + '"')
                .body(response);
    }

    @PostMapping("/requests/{id}/reject")
    public ResponseEntity<GeneralRequestResponse> rejectRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) DecisionRequest decision) {
        String tenant = tenantId(jwt);
        String actor = employeeId(jwt);
        Set<String> roles = roles(jwt);

        GeneralRequestResponse response = commandReceipts.execute(
                tenant,
                actorId(jwt),
                "/requests/" + id + "/reject",
                idempotencyKey,
                decision != null ? decision : id,
                GeneralRequestResponse.class,
                () -> {
                    workflow.rejectRequestStep(tenant, caller(jwt), id, decision, ifMatch);
                    return requestService.getRequestDetail(tenant, actor, roles, id);
                }
        );

        return ResponseEntity.ok()
                .eTag('"' + Long.toString(response.getVersion()) + '"')
                .body(response);
    }

    @PostMapping("/requests/{id}/cancel")
    public ResponseEntity<GeneralRequestResponse> cancelRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) DecisionRequest decision) {
        String tenant = tenantId(jwt);
        String actor = employeeId(jwt);
        Set<String> roles = roles(jwt);

        GeneralRequestResponse response = commandReceipts.execute(
                tenant,
                actorId(jwt),
                "/requests/" + id + "/cancel",
                idempotencyKey,
                decision != null ? decision : id,
                GeneralRequestResponse.class,
                () -> {
                    workflow.cancelRequest(tenant, caller(jwt), id, ifMatch);
                    return requestService.getRequestDetail(tenant, actor, roles, id);
                }
        );

        return ResponseEntity.ok()
                .eTag('"' + Long.toString(response.getVersion()) + '"')
                .body(response);
    }

    @PostMapping("/requests/{id}/fulfill")
    public ResponseEntity<GeneralRequestResponse> fulfillRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) FulfillRequest fulfill) {
        String tenant = tenantId(jwt);
        String actor = employeeId(jwt);
        Set<String> roles = roles(jwt);

        GeneralRequestResponse response = commandReceipts.execute(
                tenant,
                actorId(jwt),
                "/requests/" + id + "/fulfill",
                idempotencyKey,
                fulfill != null ? fulfill : id,
                GeneralRequestResponse.class,
                () -> {
                    workflow.fulfillApprovedMaterial(tenant, caller(jwt), id, fulfill, ifMatch);
                    return requestService.getRequestDetail(tenant, actor, roles, id);
                }
        );

        return ResponseEntity.ok()
                .eTag('"' + Long.toString(response.getVersion()) + '"')
                .body(response);
    }

    @PostMapping("/requests/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CommentDto dto) {
        String tenant = tenantId(jwt);
        String actor = employeeId(jwt);
        String name = employeeName(jwt);
        Set<String> roles = roles(jwt);

        CommentResponse response = commandReceipts.execute(tenant, actorId(jwt), "/requests/" + id + "/comments",
                idempotencyKey, dto, CommentResponse.class, () -> commentService.addComment(tenant, actor, name, roles, id, dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/requests/{id}/comments")
    public ResponseEntity<List<CommentResponse>> listComments(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id) {
        String tenant = tenantId(jwt);
        String actor = employeeId(jwt);
        Set<String> roles = roles(jwt);

        return ResponseEntity.ok(commentService.listComments(tenant, actor, roles, id));
    }

    @PostMapping("/requests/{id}/attachments/presign")
    public ResponseEntity<AttachmentPresignResponse> presignAttachment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AttachmentPresignRequest presign) {
        String tenant = tenantId(jwt);
        String actor = employeeId(jwt);
        Set<String> roles = roles(jwt);

        return ResponseEntity.ok(commandReceipts.execute(tenant, actorId(jwt), "/requests/" + id + "/attachments/presign",
                idempotencyKey, presign, AttachmentPresignResponse.class, () -> attachmentService.presignAttachment(tenant, actor, roles, id, presign)));
    }

    @PutMapping("/requests/{id}/attachments/{attachmentId}/upload")
    public ResponseEntity<Void> uploadAttachment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @PathVariable String attachmentId,
            HttpServletRequest request) throws IOException {
        String tenant = tenantId(jwt);
        String actor = employeeId(jwt);
        Set<String> roles = roles(jwt);

        attachmentService.uploadAttachmentDirect(tenant, actor, roles, id, attachmentId, request.getInputStream());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/requests/{id}/attachments/{attachmentId}/complete")
    public ResponseEntity<AttachmentResponse> completeAttachment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @PathVariable String attachmentId,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) AttachmentCompleteRequest complete) {
        String tenant = tenantId(jwt);
        String actor = employeeId(jwt);
        Set<String> roles = roles(jwt);

        return ResponseEntity.ok(commandReceipts.execute(tenant, actorId(jwt), "/requests/" + id + "/attachments/" + attachmentId + "/complete",
                idempotencyKey, complete, AttachmentResponse.class, () -> attachmentService.completeAttachment(tenant, actor, roles, id, attachmentId, complete)));
    }

    @GetMapping("/requests/{id}/attachments/{attachmentId}/download")
    public ResponseEntity<AttachmentDownloadResponse> downloadAttachment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @PathVariable String attachmentId) {
        String tenant = tenantId(jwt);
        String actor = employeeId(jwt);
        Set<String> roles = roles(jwt);

        return ResponseEntity.ok(attachmentService.downloadAttachment(tenant, actor, roles, id, attachmentId));
    }

    @GetMapping("/requests/{id}/attachments/{attachmentId}/file")
    public ResponseEntity<byte[]> getAttachmentFile(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @PathVariable String attachmentId) throws IOException {
        String tenant = tenantId(jwt);
        String actor = employeeId(jwt);
        Set<String> roles = roles(jwt);

        byte[] bytes = attachmentService.getAttachmentBytes(tenant, actor, roles, id, attachmentId);
        AttachmentDownloadResponse meta = attachmentService.downloadAttachment(tenant, actor, roles, id, attachmentId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(meta.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, org.springframework.http.ContentDisposition.attachment().filename(meta.getFileName(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(bytes);
    }

    static String tenantId(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication token");
        }
        String claim = jwt.getClaimAsString("tenantId");
        if (claim == null || claim.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing tenant claim");
        }
        return claim;
    }

    private String actorId(Jwt jwt) {
        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid actor subject");
        }
        return sub;
    }

    static String employeeId(Jwt jwt) {
        String claim = jwt.getClaimAsString("employeeId");
        if (claim != null && !claim.isBlank()) {
            return claim;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Employee linkage is required");
    }

    private com.hrflow.request.service.RequestCaller caller(Jwt jwt) {
        return new com.hrflow.request.service.RequestCaller(actorId(jwt), employeeId(jwt), roles(jwt));
    }

    private String employeeName(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        return name != null ? name : "Employee";
    }

    static Set<String> roles(Jwt jwt) {
        List<String> list = jwt.getClaimAsStringList("roles");
        return list != null ? new HashSet<>(list) : Set.of();
    }
}
