package com.cstestforge.execution.model;

/**
 * Enumeration of possible test execution statuses
 */
public enum TestStatus {
    /**
     * Test is queued for execution
     */
    QUEUED,
    
    /**
     * Test is currently running
     */
    RUNNING,
    
    /**
     * Test has passed successfully
     */
    PASSED,
    
    /**
     * Test has failed
     */
    FAILED,
    
    /**
     * Test was skipped
     */
    SKIPPED,
    
    /**
     * Test encountered an error during execution
     */
    ERROR,
    
    /**
     * Test execution was blocked
     */
    BLOCKED,
    
    /**
     * Test execution was aborted
     */
    ABORTED;
    
    /**
     * Get a color code for the status (for UI display)
     * 
     * @return CSS color code
     */
    public String getColorCode() {
        switch (this) {
            case PASSED:
                return "#4caf50"; // Green
            case FAILED:
                return "#f44336"; // Red
            case ERROR:
                return "#e91e63"; // Pink
            case RUNNING:
                return "#2196f3"; // Blue
            case QUEUED:
                return "#9e9e9e"; // Gray
            case SKIPPED:
                return "#ff9800"; // Orange
            case BLOCKED:
                return "#795548"; // Brown
            case ABORTED:
                return "#607d8b"; // Blue Gray
            default:
                return "#9e9e9e"; // Gray
        }
    }
} 