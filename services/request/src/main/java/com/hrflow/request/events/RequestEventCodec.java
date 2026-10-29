package com.hrflow.request.events;

import com.hrflow.contracts.request.RequestApprovalStepFact;
import com.hrflow.contracts.request.RequestWorkflowFact;
import com.hrflow.contracts.request.RequestWorkflowWire;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class RequestEventCodec {
    private final JsonMapper json;

    public RequestWorkflowFact encode(Stored event) {
        var body = json.readValue(event.payload(), RequestSnapshot.class);
        var fact = RequestWorkflowFact.newBuilder().setEventId(event.id()).setType("RequestWorkflowSnapshot.v1")
                .setSchemaVersion(1).setTenantId(event.tenant()).setAggregateId(event.aggregate()).setAggregateVersion(event.revision())
                .setOccurredAt(event.occurredAt()).setCorrelationId(event.correlation().toString()).setCausationId(null)
                .setEmployeeId(body.employeeId()).setRequestType(body.requestType()).setState(body.state()).setCreatedAt(body.createdAt())
                .setSteps(body.steps().stream().map(step -> RequestApprovalStepFact.newBuilder().setOrder(step.order())
                        .setRole(step.role()).setApproverId(step.approverId()).setState(step.state()).build()).toList()).build();
        RequestWorkflowWire.validate(event.key(), fact);
        return fact;
    }

    public record Stored(String id, String tenant, String aggregate, long revision, String key,
            UUID correlation, Instant occurredAt, String payload) {}
}
