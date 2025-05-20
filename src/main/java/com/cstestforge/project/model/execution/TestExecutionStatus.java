package com.cstestforge.project.model.execution;

/**
 * Enum representing the status of a test execution.
 */
public enum TestExecutionStatus {
    PENDING("Pending"),
    RUNNING("Running"),
    PASSED("Passed"),
    FAILED("Failed"),
    ERROR("Error"),
    SKIPPED("Skipped"),
    ABORTED("Aborted"),
    QUEUED("Queued"),
    BLOCKED("Blocked"),
    WARNING("Warning");
    
    private final String displayName;
    
    TestExecutionStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
} 