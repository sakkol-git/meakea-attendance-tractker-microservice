package com.hrflow.attendance.daily;

import com.hrflow.attendance.dto.DailyStatusResponse;
import com.hrflow.attendance.service.AttendanceScheduleResolver.Resolved;
import java.time.ZoneId;
import java.util.List;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DailySnapshotMapper {
    @Mapping(target = "timezone", source = "basis.zone")
    @Mapping(target = "shiftId", source = "basis.shiftId")
    @Mapping(target = "shiftRevision", source = "basis.revision")
    @Mapping(target = "scheduledStart", source = "basis.startsAt")
    @Mapping(target = "scheduledEnd", source = "basis.endsAt")
    @Mapping(target = "graceMinutes", source = "basis.graceMinutes")
    @Mapping(target = "workingDays", source = "basis.workingDays")
    DailySnapshot snapshot(DailyStatusResponse overview, Resolved basis,
            List<DailySnapshot.LeaveBasis> leaveReferences, List<DailySnapshot.SessionBasis> sessionReferences);

    default String timezone(ZoneId zone) { return zone.getId(); }
}
