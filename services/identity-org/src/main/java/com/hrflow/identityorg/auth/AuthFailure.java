package com.hrflow.identityorg.auth;

import org.springframework.http.HttpStatus;

public class AuthFailure extends RuntimeException {
    final HttpStatus status;
    final String code;

    AuthFailure(HttpStatus status, String code, String detail) {
        super(detail);
        this.status = status;
        this.code = code;
    }

    static AuthFailure invalid() {
        return new AuthFailure(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials or session.");
    }
}
