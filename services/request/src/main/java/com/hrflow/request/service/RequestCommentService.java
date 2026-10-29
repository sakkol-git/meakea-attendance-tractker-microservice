package com.hrflow.request.service;

import com.hrflow.request.dto.CommentDto;
import com.hrflow.request.dto.CommentResponse;
import com.hrflow.request.entity.GeneralRequest;
import com.hrflow.request.entity.RequestComment;
import com.hrflow.request.mapper.CommentMapper;
import com.hrflow.request.repository.GeneralRequestRepository;
import com.hrflow.request.repository.RequestCommentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class RequestCommentService {

    private final GeneralRequestRepository generalRequestRepository;
    private final RequestCommentRepository requestCommentRepository;
    private final RequestParticipantPolicy participantPolicy;
    private final TenantSqlScope tenantSqlScope;
    private final CommentMapper commentMapper;

    public RequestCommentService(
            GeneralRequestRepository generalRequestRepository,
            RequestCommentRepository requestCommentRepository,
            RequestParticipantPolicy participantPolicy,
            TenantSqlScope tenantSqlScope,
            CommentMapper commentMapper) {
        this.generalRequestRepository = generalRequestRepository;
        this.requestCommentRepository = requestCommentRepository;
        this.participantPolicy = participantPolicy;
        this.tenantSqlScope = tenantSqlScope;
        this.commentMapper = commentMapper;
    }

    public CommentResponse addComment(String tenantId, String actorId, String actorName, Set<String> actorRoles, String requestId, CommentDto commentDto) {
        tenantSqlScope.use(tenantId);
        GeneralRequest request = generalRequestRepository.findByTenantIdAndId(tenantId, requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        participantPolicy.requireParticipant(tenantId, actorId, actorRoles, request);

        if (commentDto == null || commentDto.effectiveText() == null || commentDto.effectiveText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Comment text is required");
        }

        RequestComment comment = RequestComment.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .requestId(requestId)
                .authorId(actorId)
                .authorName(actorName != null ? actorName : actorId)
                .commentText(commentDto.effectiveText().trim())
                .createdAt(Instant.now())
                .build();

        requestCommentRepository.save(comment);
        return commentMapper.toResponse(comment);
    }

    public List<CommentResponse> listComments(String tenantId, String actorId, Set<String> actorRoles, String requestId) {
        tenantSqlScope.use(tenantId);
        GeneralRequest request = generalRequestRepository.findByTenantIdAndId(tenantId, requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        participantPolicy.requireParticipant(tenantId, actorId, actorRoles, request);
        return requestCommentRepository.findByTenantIdAndRequestIdOrderByCreatedAtAsc(tenantId, requestId)
                .stream().map(commentMapper::toResponse).toList();
    }
}
