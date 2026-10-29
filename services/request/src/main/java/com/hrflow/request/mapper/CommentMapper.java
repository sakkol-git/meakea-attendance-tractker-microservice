package com.hrflow.request.mapper;

import com.hrflow.request.dto.CommentResponse;
import com.hrflow.request.entity.RequestComment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommentMapper {
    CommentResponse toResponse(RequestComment entity);
}
