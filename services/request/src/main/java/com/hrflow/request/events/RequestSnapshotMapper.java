package com.hrflow.request.events;

import com.hrflow.request.entity.ApprovalStep;
import com.hrflow.request.entity.GeneralRequest;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RequestSnapshotMapper {
    @Mapping(target = "requestType", source = "request.type")
    @Mapping(target = "state", source = "request.status")
    @Mapping(target = "steps", source = "steps")
    RequestSnapshot snapshot(GeneralRequest request, List<ApprovalStep> steps);

    @Mapping(target = "order", source = "stepOrder")
    @Mapping(target = "role", source = "approverRole")
    @Mapping(target = "state", source = "status")
    RequestSnapshot.Step step(ApprovalStep step);
}
