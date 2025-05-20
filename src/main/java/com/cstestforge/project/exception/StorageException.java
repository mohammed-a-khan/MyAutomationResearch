package com.cstestforge.project.exception;

/**
 * Exception thrown when file storage operations fail.
 */
public class StorageException extends RuntimeException {
    
    private final String operation;
    private final String path;

    public StorageException(String message) {
        super(message);
        this.operation = "unknown";
        this.path = "unknown";
    }
    
    public StorageException(String message, String operation, String path) {
        super(message);
        this.operation = operation;
        this.path = path;
    }
    
    public StorageException(String message, String operation, String path, Throwable cause) {
        super(message, cause);
        this.operation = operation;
        this.path = path;
    }

    public String getOperation() {
        return operation;
    }

    public String getPath() {
        return path;
    }
} 