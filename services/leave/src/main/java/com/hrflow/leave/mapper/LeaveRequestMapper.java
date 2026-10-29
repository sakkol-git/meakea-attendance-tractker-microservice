package com.hrflow.leave.mapper;

import com.hrflow.leave.dto.LeaveRecordResponse;
import com.hrflow.leave.entity.LeaveRequest;
import java.time.Instant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface LeaveRequestMapper {

    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "instantToString")
    LeaveRecordResponse toResponse(LeaveRequest entity);

    @Named("instantToString")
    default String instantToString(Instant instant) {
        return instant != null ? instant.toString() : null;
    }
}
