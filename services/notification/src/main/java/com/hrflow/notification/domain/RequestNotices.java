package com.hrflow.notification.domain;

import com.hrflow.contracts.request.RequestWorkflowFact;
import com.hrflow.contracts.request.RequestWorkflowWire;
import com.hrflow.notification.organization.NotificationReferences;
import java.util.Comparator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RequestNotices {
    private final NoticeDelivery delivery;
    private final NotificationReferences references;

    @Transactional
    public void apply(String key, RequestWorkflowFact fact) {
        var id = RequestWorkflowWire.validate(key, fact);
        var context = new NoticeDelivery.Context(id.tenant(), id.event(), id.request(), id.employee(), "request.workflow.v1", fact.getAggregateVersion());
        if (!delivery.begin(context)) return;
        var requester = references.employee(id.tenant(), id.employee());
        if ("PENDING_APPROVAL".equals(fact.getState())) {
            var step = fact.getSteps().stream().filter(value -> "PENDING".equals(value.getState()))
                    .min(Comparator.comparingInt(value -> value.getOrder())).orElseThrow(() -> new IllegalArgumentException("Pending request has no pending approval"));
            if ("MANAGER".equals(step.getRole())) {
                UUID assigned = UUID.fromString(step.getApproverId());
                var manager = references.employee(id.tenant(), assigned);
                if (assigned.equals(id.employee()) || !assigned.equals(requester.manager()) || !manager.active() || !manager.roles().contains("MANAGER")) throw unavailable();
                delivery.create(context, manager, "Request approval needed", "request", true);
            } else if ("HR_ADMIN".equals(step.getRole())) {
                var hr = references.hr(id.tenant(), id.employee());
                if (hr.isEmpty()) throw unavailable();
                for (var recipient : hr) delivery.create(context, recipient, "Request approval needed", "request", true);
            } else throw new IllegalArgumentException("Unsupported approval role");
        } else {
            String title = switch (fact.getState()) {
                case "APPROVED" -> "Request approved";
                case "REJECTED" -> "Request rejected";
                case "CANCELLED" -> "Request cancelled";
                case "FULFILLED" -> "Request fulfilled";
                default -> throw new IllegalArgumentException("Unsupported request state");
            };
            delivery.create(context, requester, title, "request", false);
        }
    }
    private ResponseStatusException unavailable() { return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Current independent approval recipient is not available"); }
}
