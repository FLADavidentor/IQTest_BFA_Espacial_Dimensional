package com.iqtest.bfaespacial.web.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Catch-all so unexpected exceptions never leak a stack trace to clients (§ Phase 7).
 * Known ResponseStatusExceptions keep their status; everything else becomes a clean 500.
 * Thymeleaf page errors are rendered via templates/error.html.
 */
@RestControllerAdvice(basePackages = {
        "com.iqtest.bfaespacial.web.api",
        "com.iqtest.bfaespacial.integracion"})
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail onResponseStatus(ResponseStatusException ex) {
        return ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail onUnexpected(Exception ex) {
        log.error("Error no controlado", ex); // full detail stays server-side only
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno");
    }
}
