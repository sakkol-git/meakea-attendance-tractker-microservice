package com.hrflow.contracts.request;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Shared v1 transport validation; recipients, authorization and workflow decisions remain owner policy. */
public final class RequestWorkflowWire {
    private RequestWorkflowWire() {}

    public static Identity validate(String key, RequestWorkflowFact fact) {
        if (fact == null || fact.getSchemaVersion() != 1 || fact.getAggregateVersion() < 1
                || !"RequestWorkflowSnapshot.v1".equals(fact.getType()) || fact.getOccurredAt() == null
                || fact.getCreatedAt() == null || fact.getCreatedAt().isAfter(fact.getOccurredAt())
                || fact.getRequestType() == null || fact.getRequestType().isBlank() || fact.getSteps() == null
                || fact.getState() == null || !Set.of("PENDING_APPROVAL", "APPROVED", "REJECTED", "CANCELLED", "FULFILLED").contains(fact.getState())) {
            throw new IllegalArgumentException("Invalid request workflow envelope or owner facts");
        }
        var id = new Identity(UUID.fromString(fact.getTenantId()), UUID.fromString(fact.getAggregateId()),
                UUID.fromString(fact.getEmployeeId()), UUID.fromString(fact.getEventId()));
        UUID.fromString(fact.getCorrelationId());
        if (!(id.tenant() + ":" + id.request()).equals(key)) throw new IllegalArgumentException("Request routing mismatch");
        Set<Integer> orders = new HashSet<>();
        for (var step : fact.getSteps()) {
            if (step == null || step.getOrder() < 1 || !orders.add(step.getOrder()) || step.getRole() == null || step.getRole().isBlank()
                    || step.getState() == null || !Set.of("PENDING", "APPROVED", "REJECTED", "SKIPPED").contains(step.getState())
                    || step.getApproverId() == null && !"HR_ADMIN".equals(step.getRole()) && !"SKIPPED".equals(step.getState())) {
                throw new IllegalArgumentException("Invalid request approval snapshot");
            }
            if (step.getApproverId() != null) UUID.fromString(step.getApproverId());
        }
        return id;
    }

    public record Identity(UUID tenant, UUID request, UUID employee, UUID event) {}
}
