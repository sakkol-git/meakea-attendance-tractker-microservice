package com.hrflow.request.mapper;

import com.hrflow.request.dto.RequestLineDto;
import com.hrflow.request.entity.RequestLine;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RequestLineMapper {
    RequestLineDto toDto(RequestLine entity);
}
