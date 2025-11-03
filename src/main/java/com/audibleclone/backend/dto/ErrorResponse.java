package com.audibleclone.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Standardized error response format for the API
 */
@Schema(description = "Standard error response format")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    @Schema(description = "HTTP status code", example = "400")
    int status,
    
    @Schema(description = "Error code for programmatic handling", example = "VALIDATION_FAILED")
    String error,
    
    @Schema(description = "Human-readable error message", example = "The request contains invalid data")
    String message,
    
    @Schema(description = "API path where error occurred", example = "/api/v1/users")
    String path,
    
    @Schema(description = "Timestamp when error occurred")
    Instant timestamp,
    
    @Schema(description = "Unique identifier for this error instance")
    String traceId,
    
    @Schema(description = "Validation errors (if applicable)")
    List<ValidationError> validationErrors
) {
    
    public ErrorResponse(int status, String error, String message, String path) {
        this(status, error, message, path, Instant.now(), null, null);
    }
    
    public ErrorResponse(int status, String error, String message, String path, String traceId) {
        this(status, error, message, path, Instant.now(), traceId, null);
    }
    
    public ErrorResponse(int status, String error, String message, String path, List<ValidationError> validationErrors) {
        this(status, error, message, path, Instant.now(), null, validationErrors);
    }
    
    @Schema(description = "Individual validation error")
    public record ValidationError(
        @Schema(description = "Field name", example = "email")
        String field,
        
        @Schema(description = "Rejected value")
        Object rejectedValue,
        
        @Schema(description = "Validation error message", example = "must be a valid email address")
        String message
    ) {}
}