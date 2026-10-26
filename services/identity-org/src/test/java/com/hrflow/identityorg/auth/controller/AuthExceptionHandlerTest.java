package com.hrflow.identityorg.auth.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.hrflow.identityorg.exception.AuthExceptionHandler;
import com.hrflow.identityorg.exception.AuthFailedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

class AuthExceptionHandlerTest {
    @Test
    void returnsCanonicalCodeAndRetryAfterForThrottling() {
        AuthFailedException failure = new AuthFailedException(HttpStatus.TOO_MANY_REQUESTS,
                "TOO_MANY_ATTEMPTS", "Wait before trying again.", 120L);

        ResponseEntity<ProblemDetail> response = new AuthExceptionHandler().auth(failure);

        ProblemDetail body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.getProperties());
        assertEquals("TOO_MANY_ATTEMPTS", body.getProperties().get("code"));
        assertEquals(429, body.getStatus());
        assertEquals("120", response.getHeaders().getFirst("Retry-After"));
        assertEquals("no-store", response.getHeaders().getCacheControl());
    }
}
