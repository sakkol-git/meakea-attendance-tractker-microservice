package com.hrflow.leave.mapper;

import com.hrflow.leave.dto.LeaveCalculationResult;
import com.hrflow.leave.dto.LeavePreviewResponse;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface LeavePreviewMapper {
    LeavePreviewResponse toResponse(LeaveCalculationResult result);
}
