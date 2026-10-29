package com.hrflow.leave.mapper;

import com.hrflow.leave.dto.LeaveBalanceResponse;
import com.hrflow.leave.entity.LeaveBalance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LeaveBalanceMapper {

    @Mapping(target = "type", source = "leaveTypeCode")
    @Mapping(target = "name", source = "leaveTypeCode")
    @Mapping(target = "entitled", source = "allocated")
    @Mapping(target = "unit", constant = "DAYS")
    LeaveBalanceResponse toResponse(LeaveBalance entity);
}
