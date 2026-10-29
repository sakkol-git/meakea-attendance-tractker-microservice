package com.hrflow.request.mapper;

import com.hrflow.request.dto.MaterialItemResponse;
import com.hrflow.request.entity.MaterialItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MaterialItemMapper {
    MaterialItemResponse toResponse(MaterialItem entity);
}
