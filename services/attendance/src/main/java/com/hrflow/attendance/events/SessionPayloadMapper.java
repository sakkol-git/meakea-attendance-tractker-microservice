package com.hrflow.attendance.events;

import com.hrflow.attendance.entity.AttendanceSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SessionPayloadMapper {
    @Mapping(target = "state", source = "session.status")
    @Mapping(target = "source", source = "source")
    SessionPayload snapshot(AttendanceSession session, String source);
}
