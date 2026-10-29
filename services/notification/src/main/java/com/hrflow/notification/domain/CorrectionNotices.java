package com.hrflow.notification.domain;

import com.hrflow.contracts.attendance.AttendanceCorrectionFact;
import com.hrflow.contracts.attendance.AttendanceCorrectionWire;
import com.hrflow.notification.organization.NotificationReferences;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CorrectionNotices {
    private final NoticeDelivery delivery;
    private final NotificationReferences references;
    private final ReviewRecipients reviewers;

    @Transactional
    public void apply(String key, AttendanceCorrectionFact fact) {
        var id = AttendanceCorrectionWire.validate(key, fact);
        var context = new NoticeDelivery.Context(id.tenant(), id.event(), id.correction(), id.employee(), "attendance.correction.v1", fact.getAggregateVersion());
        if (!delivery.begin(context)) return;
        var employee = references.employee(id.tenant(), id.employee());
        if ("PENDING".equals(fact.getState())) {
            for (var reviewer : reviewers.current(id.tenant(), employee, true))
                delivery.create(context, reviewer, "Attendance correction approval needed", "correction", true);
        } else {
            String title = "APPROVED".equals(fact.getState()) ? "Attendance correction approved" : "Attendance correction rejected";
            delivery.create(context, employee, title, "correction", false);
        }
    }
}
