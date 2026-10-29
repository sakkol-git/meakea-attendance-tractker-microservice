package com.hrflow.leave.organization;

import com.hrflow.contracts.organization.AssignmentSnapshot;
import com.hrflow.leave.service.LeaveWorkCalendar.Assignment;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface LeaveAssignmentMapper {
    Assignment snapshot(AssignmentSnapshot source);
}
