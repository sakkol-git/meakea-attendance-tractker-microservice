package com.hrflow.request.mapper;

import com.hrflow.request.dto.GeneralRequestResponse;
import com.hrflow.request.entity.GeneralRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = RequestFormMapper.class)
public interface GeneralRequestMapper {

    @Mapping(target = "form", source = "formData")
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "lines", ignore = true)
    @Mapping(target = "steps", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    GeneralRequestResponse toResponse(GeneralRequest entity);
}
