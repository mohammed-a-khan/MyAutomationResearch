package com.cstestforge.export.model;

/**
 * Represents the status of an export operation
 */
public enum ExportStatus {
    PENDING("Export job is queued for processing"),
    IN_PROGRESS("Export is in progress"),
    COMPLETED("Export completed successfully"),
    FAILED("Export failed"),
    CANCELLED("Export was cancelled");
    
    private final String description;
    
    ExportStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
} 