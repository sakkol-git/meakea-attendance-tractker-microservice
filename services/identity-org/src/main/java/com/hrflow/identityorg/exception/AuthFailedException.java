package com.hrflow.identityorg.exception;

import org.springframework.http.HttpStatus;

public class AuthFailedException extends ApiException {
    public AuthFailedException(HttpStatus status, String code, String message) {
        super(status, code, message, null);
    }
    public AuthFailedException(HttpStatus status, String code, String message, Long retryAfterSeconds) {
        super(status, code, message, retryAfterSeconds);
    }

    public static AuthFailedException invalid(String message) {
        return new AuthFailedException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", message);
    }
}
