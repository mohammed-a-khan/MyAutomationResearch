package com.cstestforge.project.exception;

/**
 * Exception thrown when a concurrent operation conflicts with another operation.
 */
public class ConcurrencyException extends RuntimeException {
    
    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    public ConcurrencyException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("Concurrent operation conflict for %s with %s: '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public ConcurrencyException(String message) {
        super(message);
        this.resourceName = null;
        this.fieldName = null;
        this.fieldValue = null;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getFieldValue() {
        return fieldValue;
    }
} 