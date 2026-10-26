package com.hrflow.identityorg.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends ApiException {
    public UserNotFoundException(String tenantId, String email) {
        super(HttpStatus.NOT_FOUND,
                "USER_NOT_FOUND",
                "User not found with tenant ID: " + tenantId + " email: " + email,
                null);
    }
}
