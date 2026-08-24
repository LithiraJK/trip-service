package com.tripvisito.tripservice.exception;

import com.tripvisito.tripservice.dto.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Centralised exception handler for trip-service. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles validation errors (e.g. @NotBlank, @NotEmpty, @Min, @Max failures).
     * Returns field-level error details so the frontend can display per-field messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(e ->
                errors.put(((FieldError) e).getField(), e.getDefaultMessage()));

        String summary = errors.values().stream()
                .collect(Collectors.joining("; "));

        log.warn("[GlobalExceptionHandler] Validation failed: {}", errors);

        return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, String>>builder()
                        .status(400)
                        .message("Validation failed: " + summary)
                        .data(errors)
                        .build());
    }

    /**
     * Handles malformed JSON or type mismatch errors in request bodies.
     * For example: sending a number where a string array is expected.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(HttpMessageNotReadableException ex) {
        log.warn("[GlobalExceptionHandler] Malformed request body: {}", ex.getMessage());

        String userMessage = "Invalid request format. Please check your input data and try again.";

        // Provide more specific messages for common issues
        String cause = ex.getMessage();
        if (cause != null) {
            if (cause.contains("Cannot deserialize")) {
                userMessage = "Invalid data type in request. Please ensure all fields have correct types.";
            } else if (cause.contains("Required request body is missing")) {
                userMessage = "Request body is missing. Please provide the required trip details.";
            }
        }

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, userMessage));
    }

    /**
     * Handles missing required headers (e.g. X-User-Id not present).
     * This typically means the request bypassed the API gateway or the user is not authenticated.
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException ex) {
        log.warn("[GlobalExceptionHandler] Missing header: {}", ex.getHeaderName());

        String message = "Authentication required. Please log in and try again.";
        if ("X-User-Id".equalsIgnoreCase(ex.getHeaderName())) {
            message = "Authentication required. Please log in to generate a trip.";
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("[GlobalExceptionHandler] Invalid argument: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, ex.getMessage()));
    }

    @ExceptionHandler(TripNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(TripNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, ex.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(403, ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntime(RuntimeException ex) {
        log.error("[GlobalExceptionHandler] {}", ex.getMessage(), ex);

        // Provide user-friendly messages for known runtime errors
        String message = ex.getMessage();
        if (message != null && message.contains("Failed to generate trip with AI")) {
            message = "AI trip generation failed. Please try again in a few moments.";
        } else if (message != null && message.contains("Timed out")) {
            message = "The request timed out. Please try again.";
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, message != null ? message : "An unexpected error occurred."));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
            "status", 404,
            "error", "Not Found",
            "message", "Endpoint not found: " + ex.getResourcePath()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("[GlobalExceptionHandler] Unexpected: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Internal Server Error. Please try again later."));
    }
}
