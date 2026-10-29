package com.hrflow.leave.events;

import com.hrflow.leave.entity.LeaveRequest;
import java.time.LocalDate;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LeaveSnapshotMapper {
    @Mapping(target = "leaveType", source = "request.type")
    @Mapping(target = "chargedDays", source = "request.days")
    @Mapping(target = "state", source = "request.status")
    @Mapping(target = "workingDates", source = "workingDates")
    LeaveSnapshot snapshot(LeaveRequest request, List<LocalDate> workingDates);
}
