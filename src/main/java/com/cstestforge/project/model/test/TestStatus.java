package com.cstestforge.project.model.test;

/**
 * Enum representing the status of a test.
 */
public enum TestStatus {
    DRAFT("Draft"),
    ACTIVE("Active"),
    DEPRECATED("Deprecated"),
    ARCHIVED("Archived");
    
    private final String displayName;
    
    TestStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
} 