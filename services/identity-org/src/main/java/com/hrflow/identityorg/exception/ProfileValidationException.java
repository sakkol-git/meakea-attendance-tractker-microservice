package com.hrflow.identityorg.exception;

import org.springframework.http.HttpStatus;

public final class ProfileValidationException extends ApiException {
    public ProfileValidationException(String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_ERROR", message, null);
    }
}
