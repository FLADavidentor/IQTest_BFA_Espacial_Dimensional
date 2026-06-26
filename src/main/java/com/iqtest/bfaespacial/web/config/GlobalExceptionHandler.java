package com.iqtest.bfaespacial.web.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * REST error shape: {"error": "...", "status": N}. Never leaks a stack trace (7-A).
 * Scoped to the JSON controllers; Thymeleaf page errors render templates/error.html.
 */
@RestControllerAdvice(basePackages = {
        "com.iqtest.bfaespacial.web.api",
        "com.iqtest.bfaespacial.integracion"})
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, Object>> onResponseStatus(ResponseStatusException ex) {
        int status = ex.getStatusCode().value();
        return ResponseEntity.status(status).body(body(ex.getReason(), status));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, Object>> onBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(body(ex.getMessage(), 400));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> onUnexpected(Exception ex) {
        log.error("Error no controlado", ex); // full detail stays server-side only
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body("Error interno", 500));
    }

    private Map<String, Object> body(String error, int status) {
        return Map.of("error", error == null || error.isBlank() ? "error" : error, "status", status);
    }
}
