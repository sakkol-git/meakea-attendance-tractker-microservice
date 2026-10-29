package com.hrflow.request.mapper;

import com.hrflow.request.dto.AttachmentResponse;
import com.hrflow.request.entity.RequestAttachment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AttachmentMapper {
    @org.mapstruct.Mapping(target = "title", source = "fileName")
    @org.mapstruct.Mapping(target = "details", expression = "java(metadata(entity))")
    @org.mapstruct.Mapping(target = "history", expression = "java(java.util.List.of())")
    AttachmentResponse toResponse(RequestAttachment entity);

    default java.util.Map<String, Object> metadata(RequestAttachment entity) {
        return java.util.Map.of("fileName", entity.getFileName(), "contentType", entity.getContentType(),
                "size", entity.getFileSize(), "sha256", entity.getChecksumSha256());
    }
}
