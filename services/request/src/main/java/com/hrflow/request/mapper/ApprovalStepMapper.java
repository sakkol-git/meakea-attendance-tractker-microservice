package com.hrflow.request.mapper;

import com.hrflow.request.dto.ApprovalStepDto;
import com.hrflow.request.entity.ApprovalStep;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ApprovalStepMapper {
    ApprovalStepDto toDto(ApprovalStep entity);
}
