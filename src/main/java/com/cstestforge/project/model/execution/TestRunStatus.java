package com.cstestforge.project.model.execution;

/**
 * Enum representing the status of a test run.
 */
public enum TestRunStatus {
    PENDING("Pending"),
    RUNNING("Running"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    ABORTED("Aborted"),
    QUEUED("Queued"),
    CANCELLED("Cancelled");
    
    private final String displayName;
    
    TestRunStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
} 