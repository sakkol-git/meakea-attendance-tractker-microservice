package com.hrflow.request.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class RevisionPreconditions {
    private RevisionPreconditions() {}

    public static void require(String ifMatch, long version) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "If-Match header is required");
        }
        String cleanIfMatch = ifMatch.trim();
        if (cleanIfMatch.startsWith("\"") && cleanIfMatch.endsWith("\"")) {
            cleanIfMatch = cleanIfMatch.substring(1, cleanIfMatch.length() - 1);
        }
        if (!cleanIfMatch.equals(Long.toString(version))) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "Version mismatch: stale entity");
        }
    }
}
