package com.userservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.service.spi.ServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== VALIDATION ====================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(err -> fieldErrors.put(err.getField(), err.getDefaultMessage()));

        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    // ==================== BUSINESS LOGIC ====================

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<Map<String, Object>> handleServiceException(
            ServiceException ex, WebRequest request) {

        log.warn("Service exception: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request, null);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {

        // Map specific messages to appropriate HTTP status codes
        HttpStatus status = resolveRuntimeStatus(ex.getMessage());
        log.warn("Runtime exception [{}]: {}", status, ex.getMessage());
        return buildResponse(status, ex.getMessage(), request, null);
    }

    // ==================== FALLBACK ====================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {

        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.", request, null);
    }

    // ==================== HELPERS ====================

    private HttpStatus resolveRuntimeStatus(String message) {
        if (message == null) return HttpStatus.INTERNAL_SERVER_ERROR;

        return switch (message) {
            case "Username already taken",
                 "Email already in use"          -> HttpStatus.CONFLICT;
            case "User not found",
                 "User not found with PRN: "     -> HttpStatus.NOT_FOUND;
            case "Contact admin to create new ADMIN user." -> HttpStatus.FORBIDDEN;
            default                              -> HttpStatus.BAD_REQUEST;
        };
    }

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status, String message, WebRequest request,
            Map<String, String> fieldErrors) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));

        if (fieldErrors != null) {
            body.put("fields", fieldErrors);
        }

        return ResponseEntity.status(status).body(body);
    }
}