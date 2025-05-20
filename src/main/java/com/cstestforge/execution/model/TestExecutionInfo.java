package com.cstestforge.execution.model;

import java.time.LocalDateTime;

/**
 * Information about a test execution
 */
public class TestExecutionInfo {

    private String id;
    private String projectId;
    private TestStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long duration;
    private String environment;
    private BrowserType browser;
    private int totalTests;
    private int passedTests;
    private int failedTests;
    private int skippedTests;
    private int errorTests;
    private int runningTests;
    private int queuedTests;
    private ExecutionConfig config;
    private String createdBy;

    /**
     * Default constructor
     */
    public TestExecutionInfo() {
    }

    /**
     * Constructor with essential properties
     * 
     * @param id Execution ID
     * @param projectId Project ID
     * @param status Current status
     * @param startTime Start time
     * @param environment Target environment
     * @param browser Browser used
     */
    public TestExecutionInfo(String id, String projectId, TestStatus status, 
                             LocalDateTime startTime, String environment, BrowserType browser) {
        this.id = id;
        this.projectId = projectId;
        this.status = status;
        this.startTime = startTime;
        this.environment = environment;
        this.browser = browser;
        this.totalTests = 0;
        this.passedTests = 0;
        this.failedTests = 0;
        this.skippedTests = 0;
        this.errorTests = 0;
        this.runningTests = 0;
        this.queuedTests = 0;
    }

    /**
     * Get the execution ID
     * 
     * @return Execution ID
     */
    public String getId() {
        return id;
    }

    /**
     * Set the execution ID
     * 
     * @param id Execution ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the project ID
     * 
     * @return Project ID
     */
    public String getProjectId() {
        return projectId;
    }

    /**
     * Set the project ID
     * 
     * @param projectId Project ID
     */
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    /**
     * Get the execution status
     * 
     * @return Execution status
     */
    public TestStatus getStatus() {
        return status;
    }

    /**
     * Set the execution status
     * 
     * @param status Execution status
     */
    public void setStatus(TestStatus status) {
        this.status = status;
    }

    /**
     * Get the execution start time
     * 
     * @return Start time
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }

    /**
     * Set the execution start time
     * 
     * @param startTime Start time
     */
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    /**
     * Get the execution end time
     * 
     * @return End time
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }

    /**
     * Set the execution end time
     * 
     * @param endTime End time
     */
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        
        // Calculate duration when end time is set
        if (this.startTime != null && endTime != null) {
            this.duration = java.time.Duration.between(this.startTime, endTime).toMillis();
        }
    }

    /**
     * Get the execution duration in milliseconds
     * 
     * @return Duration in milliseconds
     */
    public Long getDuration() {
        return duration;
    }

    /**
     * Set the execution duration
     * 
     * @param duration Duration in milliseconds
     */
    public void setDuration(Long duration) {
        this.duration = duration;
    }

    /**
     * Get the target environment
     * 
     * @return Environment name
     */
    public String getEnvironment() {
        return environment;
    }

    /**
     * Set the target environment
     * 
     * @param environment Environment name
     */
    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    /**
     * Get the browser used
     * 
     * @return Browser type
     */
    public BrowserType getBrowser() {
        return browser;
    }

    /**
     * Set the browser used
     * 
     * @param browser Browser type
     */
    public void setBrowser(BrowserType browser) {
        this.browser = browser;
    }

    /**
     * Get the total number of tests
     * 
     * @return Total tests
     */
    public int getTotalTests() {
        return totalTests;
    }

    /**
     * Set the total number of tests
     * 
     * @param totalTests Total tests
     */
    public void setTotalTests(int totalTests) {
        this.totalTests = totalTests;
    }

    /**
     * Get the number of passed tests
     * 
     * @return Passed tests
     */
    public int getPassedTests() {
        return passedTests;
    }

    /**
     * Set the number of passed tests
     * 
     * @param passedTests Passed tests
     */
    public void setPassedTests(int passedTests) {
        this.passedTests = passedTests;
    }

    /**
     * Get the number of failed tests
     * 
     * @return Failed tests
     */
    public int getFailedTests() {
        return failedTests;
    }

    /**
     * Set the number of failed tests
     * 
     * @param failedTests Failed tests
     */
    public void setFailedTests(int failedTests) {
        this.failedTests = failedTests;
    }

    /**
     * Get the number of skipped tests
     * 
     * @return Skipped tests
     */
    public int getSkippedTests() {
        return skippedTests;
    }

    /**
     * Set the number of skipped tests
     * 
     * @param skippedTests Skipped tests
     */
    public void setSkippedTests(int skippedTests) {
        this.skippedTests = skippedTests;
    }

    /**
     * Get the number of tests with errors
     * 
     * @return Error tests
     */
    public int getErrorTests() {
        return errorTests;
    }

    /**
     * Set the number of tests with errors
     * 
     * @param errorTests Error tests
     */
    public void setErrorTests(int errorTests) {
        this.errorTests = errorTests;
    }

    /**
     * Get the number of currently running tests
     * 
     * @return Running tests
     */
    public int getRunningTests() {
        return runningTests;
    }

    /**
     * Set the number of currently running tests
     * 
     * @param runningTests Running tests
     */
    public void setRunningTests(int runningTests) {
        this.runningTests = runningTests;
    }

    /**
     * Get the number of queued tests
     * 
     * @return Queued tests
     */
    public int getQueuedTests() {
        return queuedTests;
    }

    /**
     * Set the number of queued tests
     * 
     * @param queuedTests Queued tests
     */
    public void setQueuedTests(int queuedTests) {
        this.queuedTests = queuedTests;
    }

    /**
     * Get the execution configuration
     * 
     * @return Execution configuration
     */
    public ExecutionConfig getConfig() {
        return config;
    }

    /**
     * Set the execution configuration
     * 
     * @param config Execution configuration
     */
    public void setConfig(ExecutionConfig config) {
        this.config = config;
    }

    /**
     * Get the user who created this execution
     * 
     * @return Creator username
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Set the user who created this execution
     * 
     * @param createdBy Creator username
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
} 