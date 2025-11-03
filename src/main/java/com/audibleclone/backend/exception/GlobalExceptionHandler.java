package com.audibleclone.backend.exception;

import com.audibleclone.backend.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.UUID;

/**
 * Global exception handler for the application.
 * Provides consistent error responses across all endpoints.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle JWT-related authentication failures
     */
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(JwtException e, HttpServletRequest request) {
        String traceId = generateTraceId();
        log.warn("JWT validation failed: {} [traceId: {}]", e.getMessage(), traceId);
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            "JWT_INVALID",
            "Authentication failed",
            request.getRequestURI(),
            traceId
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle missing authentication credentials
     */
    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationCredentialsNotFound(
            AuthenticationCredentialsNotFoundException e, HttpServletRequest request) {
        String traceId = generateTraceId();
        log.warn("Authentication credentials not found: {} [traceId: {}]", e.getMessage(), traceId);
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            "AUTHENTICATION_REQUIRED",
            "Authentication credentials are required",
            request.getRequestURI(),
            traceId
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle access denied (authorization failures)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        String traceId = generateTraceId();
        log.warn("Access denied: {} [traceId: {}]", e.getMessage(), traceId);
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            "ACCESS_DENIED",
            "Insufficient privileges to access this resource",
            request.getRequestURI(),
            traceId
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        String traceId = generateTraceId();
        
        List<ErrorResponse.ValidationError> validationErrors = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::mapFieldError)
            .toList();
        
        log.warn("Validation failed with {} errors [traceId: {}]", validationErrors.size(), traceId);
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "VALIDATION_FAILED",
            "Request validation failed",
            request.getRequestURI(),
            validationErrors
        );
        
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Handle method argument type mismatch (e.g., string passed where number expected)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String traceId = generateTraceId();
        log.warn("Type mismatch for parameter '{}': {} [traceId: {}]", 
            e.getName(), e.getMessage(), traceId);
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "INVALID_PARAMETER_TYPE",
            String.format("Invalid type for parameter '%s'", e.getName()),
            request.getRequestURI(),
            traceId
        );
        
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Handle business logic exceptions
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e, HttpServletRequest request) {
        String traceId = generateTraceId();
        log.warn("Business exception: {} [traceId: {}]", e.getMessage(), traceId);
        
        ErrorResponse error = new ErrorResponse(
            e.getStatus().value(),
            e.getErrorCode(),
            e.getMessage(),
            request.getRequestURI(),
            traceId
        );
        
        return ResponseEntity.status(e.getStatus()).body(error);
    }

    /**
     * Handle resource not found exceptions
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException e, HttpServletRequest request) {
        String traceId = generateTraceId();
        log.warn("Resource not found: {} [traceId: {}]", e.getMessage(), traceId);
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "RESOURCE_NOT_FOUND",
            e.getMessage(),
            request.getRequestURI(),
            traceId
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle all other unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e, HttpServletRequest request) {
        String traceId = generateTraceId();
        log.error("Unexpected error occurred [traceId: {}]", traceId, e);
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            request.getRequestURI(),
            traceId
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private ErrorResponse.ValidationError mapFieldError(FieldError fieldError) {
        return new ErrorResponse.ValidationError(
            fieldError.getField(),
            fieldError.getRejectedValue(),
            fieldError.getDefaultMessage()
        );
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }
}