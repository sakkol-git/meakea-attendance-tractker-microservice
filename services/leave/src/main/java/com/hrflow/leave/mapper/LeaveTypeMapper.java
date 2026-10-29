package com.hrflow.leave.mapper;

import com.hrflow.leave.dto.LeaveTypeResponse;
import com.hrflow.leave.entity.LeaveType;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LeaveTypeMapper {

    LeaveTypeResponse toResponse(LeaveType entity);
}
