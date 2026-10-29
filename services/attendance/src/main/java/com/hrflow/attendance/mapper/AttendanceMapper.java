package com.hrflow.attendance.mapper;

import com.hrflow.attendance.dto.AttendanceSessionResponse;
import com.hrflow.attendance.entity.AttendanceSession;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AttendanceMapper {
    AttendanceSessionResponse toResponse(AttendanceSession session);
}
