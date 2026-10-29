package com.hrflow.identityorg.audit;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record AuditQuery(int page, int size, UUID actorId, String entity, String actor,
        String action, String target, LocalDate from, LocalDate to) {
    public AuditQuery {
        if (page < 0 || size < 1 || size > 100 || from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid audit range or pagination");
        }
        for (String filter : new String[]{entity, actor, action, target}) {
            if (filter != null && filter.length() > 120) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Audit filter is too long");
            }
        }
    }
}
