package com.hrflow.notification.domain;

import com.hrflow.notification.organization.NotificationReferences;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Single-stage owner workflows share reviewer authority, not the general-request approval chain. */
@Component
@RequiredArgsConstructor
public class ReviewRecipients {
    private final NotificationReferences references;
    public List<NotificationReferences.Employee> current(UUID tenant, NotificationReferences.Employee employee, boolean includeManager) {
        var reviewers = new LinkedHashMap<UUID, NotificationReferences.Employee>();
        for (var hr : references.hr(tenant, employee.employee())) reviewers.put(hr.employee(), hr);
        if (includeManager && employee.manager() != null) {
            var manager = references.employee(tenant, employee.manager());
            if (!manager.active() || !manager.roles().contains("MANAGER") || manager.employee().equals(employee.employee())) throw unavailable();
            reviewers.put(manager.employee(), manager);
        }
        if (reviewers.isEmpty()) throw unavailable();
        return List.copyOf(reviewers.values());
    }
    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Current independent reviewer is not available");
    }
}
