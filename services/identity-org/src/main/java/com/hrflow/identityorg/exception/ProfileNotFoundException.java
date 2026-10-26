package com.hrflow.identityorg.exception;

import org.springframework.http.HttpStatus;

public final class ProfileNotFoundException extends ApiException {
    public ProfileNotFoundException() {
        super(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND", "Profile not found.", null);
    }
}
