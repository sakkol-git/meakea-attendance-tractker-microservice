package com.hrflow.identityorg.exception;



import org.springframework.http.HttpStatus;

public abstract class ApiException extends RuntimeException {
    public final HttpStatus status;
    public final String code;
    public final Long retryAfterSeconds;

    protected ApiException(HttpStatus status, String code, String message, Long retryAfterSeconds) {
        super(message);
        this.status = status;
        this.code = code;
        this.retryAfterSeconds = retryAfterSeconds;
    }
}