package com.hrflow.identityorg.auth;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class AuthProblems {
    @ExceptionHandler(AuthFailure.class)
    ResponseEntity<Map<String, Object>> auth(AuthFailure failure) {
        return problem(failure.status, failure.code, failure.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class})
    ResponseEntity<Map<String, Object>> invalid(Exception error) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_ERROR", "Check the submitted fields.");
    }

    private ResponseEntity<Map<String, Object>> problem(HttpStatus status, String code, String detail) {
        return ResponseEntity.status(status).contentType(MediaType.valueOf("application/problem+json"))
                .body(Map.of("type", "about:blank", "title", status.getReasonPhrase(),
                        "status", status.value(), "code", code, "detail", detail));
    }
}
