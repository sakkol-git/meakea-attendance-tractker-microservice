package com.hrflow.notification.domain;

import com.hrflow.contracts.leave.LeaveRequestFact;
import com.hrflow.contracts.leave.LeaveRequestWire;
import com.hrflow.notification.organization.NotificationReferences;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reflects the leave owner's actual single decision stage; never invents a general-request approval chain. */
@Service
@RequiredArgsConstructor
public class LeaveNotices {
    private final NoticeDelivery delivery;
    private final NotificationReferences references;
    private final ReviewRecipients reviewers;

    @Transactional
    public void apply(String key, LeaveRequestFact fact) {
        var id = LeaveRequestWire.validate(key, fact);
        var context = new NoticeDelivery.Context(id.tenant(), id.event(), id.leave(), id.employee(), "leave.request.v1", fact.getAggregateVersion());
        if (!delivery.begin(context)) return;
        var employee = references.employee(id.tenant(), id.employee());
        if ("PENDING".equals(fact.getState()) || "NEEDS_REVIEW".equals(fact.getState())) {
            String title = "PENDING".equals(fact.getState()) ? "Leave approval needed" : "Leave needs HR review";
            for (var recipient : reviewers.current(id.tenant(), employee, "PENDING".equals(fact.getState()))) delivery.create(context, recipient, title, "leave", true);
            if ("NEEDS_REVIEW".equals(fact.getState())) delivery.create(context, employee, title, "leave", false);
        } else {
            String title = switch (fact.getState()) {
                case "APPROVED" -> "Leave approved";
                case "REJECTED" -> "Leave rejected";
                case "CANCELLED" -> "Leave cancelled";
                default -> throw new IllegalArgumentException("Unsupported leave state");
            };
            delivery.create(context, employee, title, "leave", false);
        }
    }
}
