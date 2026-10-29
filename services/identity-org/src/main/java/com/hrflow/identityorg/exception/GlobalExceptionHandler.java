package com.hrflow.identityorg.exception;

import java.net.URI;
import java.time.Instant;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(AuthFailedException.class)
    public ResponseEntity<ProblemDetail> auth(AuthFailedException failure) {
        return getProblemDetailResponseEntity(failure);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> userNotFound(UserNotFoundException failure) {
        return getProblemDetailResponseEntity(failure);
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<ProblemDetail> profileNotFound(ProfileNotFoundException failure) {
        return getProblemDetailResponseEntity(failure);
    }

    @ExceptionHandler(ProfileValidationException.class)
    public ResponseEntity<ProblemDetail> profileValidation(ProfileValidationException failure) {
        return getProblemDetailResponseEntity(failure);
    }

    @NonNull
    private  ResponseEntity<ProblemDetail> getProblemDetailResponseEntity(ApiException failure) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(failure.status, failure.getMessage());
        problem.setType(URI.create("urn:hrflow:problem:" + failure.code.toLowerCase().replace('_', '-')));
        problem.setTitle(failure.code);
        problem.setProperty("code", failure.code);
        problem.setProperty("timestamp", Instant.now()); // custom field
        ResponseEntity.BodyBuilder response = ResponseEntity.status(failure.status)
                .header(HttpHeaders.CACHE_CONTROL, "no-store");
        if (failure.retryAfterSeconds != null) {
            response.header(HttpHeaders.RETRY_AFTER, Long.toString(failure.retryAfterSeconds));
        }
        return response.body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ignored,
                                                                    HttpHeaders headers,
                                                                    HttpStatusCode status,
                                                                    WebRequest request) {
        return validationProblem();
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ignored,
                                                                   HttpHeaders headers,
                                                                   HttpStatusCode status,
                                                                   WebRequest request) {
        return validationProblem();
    }

    private ResponseEntity<Object> validationProblem() {
        ResponseEntity<ProblemDetail> response = auth(new AuthFailedException(HttpStatus.UNPROCESSABLE_CONTENT,
                "VALIDATION_ERROR", "Check the submitted fields."));
        return new ResponseEntity<>(response.getBody(), response.getHeaders(), response.getStatusCode());
    }
}
