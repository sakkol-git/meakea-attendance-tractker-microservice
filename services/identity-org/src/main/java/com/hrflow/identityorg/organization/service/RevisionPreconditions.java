package com.hrflow.identityorg.organization.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** All owner updates compare the opaque quoted version sent by the client. */
final class RevisionPreconditions {
    private RevisionPreconditions() {}

    static void require(String ifMatch, long version) {
        if (ifMatch == null) throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED);
        if (!ifMatch.equals('"' + Long.toString(version) + '"')) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED);
        }
    }
}
