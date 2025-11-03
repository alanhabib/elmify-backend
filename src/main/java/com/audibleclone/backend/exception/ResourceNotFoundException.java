package com.audibleclone.backend.exception;

/**
 * Exception thrown when a requested resource cannot be found
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceType, Object identifier) {
        super(String.format("%s not found with identifier: %s", resourceType, identifier));
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}