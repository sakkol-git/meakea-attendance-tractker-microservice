package com.hrflow.attendance.mapper;

import com.hrflow.attendance.entity.AttendanceSession;
import com.hrflow.attendance.entity.OriginalAttendanceSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OriginalSessionMapper {
    @Mapping(target = "source", source = "effectiveSource")
    OriginalAttendanceSession snapshot(AttendanceSession session);
}
