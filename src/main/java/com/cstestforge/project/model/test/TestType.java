package com.cstestforge.project.model.test;

/**
 * Enum representing different types of tests.
 */
public enum TestType {
    UI("UI Test"),
    API("API Test"),
    INTEGRATION("Integration Test"),
    PERFORMANCE("Performance Test"),
    SECURITY("Security Test"),
    MANUAL("Manual Test"),
    EXPLORATORY("Exploratory Test");
    
    private final String displayName;
    
    TestType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
} 