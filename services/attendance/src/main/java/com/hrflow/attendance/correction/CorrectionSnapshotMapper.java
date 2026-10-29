package com.hrflow.attendance.correction;

import com.hrflow.attendance.entity.AttendanceCorrection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CorrectionSnapshotMapper {
    @Mapping(target = "workDate", source = "date")
    @Mapping(target = "state", source = "status")
    CorrectionSnapshot snapshot(AttendanceCorrection correction);
}
