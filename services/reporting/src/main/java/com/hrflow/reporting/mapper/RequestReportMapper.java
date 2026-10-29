package com.hrflow.reporting.mapper;

import com.hrflow.contracts.request.RequestWorkflowFact;
import com.hrflow.reporting.dto.RequestFactEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RequestReportMapper {
    @Mapping(target = "requestId", source = "aggregateId")
    @Mapping(target = "requesterId", source = "employeeId")
    @Mapping(target = "aggregateRevision", source = "aggregateVersion")
    @Mapping(target = "status", source = "state")
    RequestFactEvent event(RequestWorkflowFact fact);
}
