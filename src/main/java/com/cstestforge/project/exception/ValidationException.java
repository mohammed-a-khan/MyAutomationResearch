package com.cstestforge.project.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when validation of user input fails.
 */
public class ValidationException extends RuntimeException {
    
    private final Map<String, String> errors;

    public ValidationException(String message) {
        super(message);
        this.errors = new HashMap<>();
    }
    
    public ValidationException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }
    
    public ValidationException addError(String field, String message) {
        this.errors.put(field, message);
        return this;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
} 