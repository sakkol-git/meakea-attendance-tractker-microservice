package com.hrflow.attendance.mapper;

import com.hrflow.attendance.dto.CorrectionResponse;
import com.hrflow.attendance.entity.AttendanceCorrection;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = OriginalSessionJson.class)
public interface CorrectionMapper {
    CorrectionResponse toResponse(AttendanceCorrection correction);
}
