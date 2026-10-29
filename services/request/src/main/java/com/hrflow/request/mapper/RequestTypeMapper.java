package com.hrflow.request.mapper;

import com.hrflow.request.dto.RequestTypeResponse;
import com.hrflow.request.entity.RequestType;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RequestTypeMapper {
    RequestTypeResponse toResponse(RequestType entity);
}
