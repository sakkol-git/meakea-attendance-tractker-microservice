package com.hrflow.reporting.consumer;

import com.hrflow.contracts.attendance.AttendanceDailyFact;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DailyReportMapper {
    DailyReportBody snapshot(AttendanceDailyFact fact);
}
