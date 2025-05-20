package com.cstestforge.project.model.execution;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a test run, which is a collection of test executions run together.
 */
public class TestRun {

    private String id;
    private String name;
    private String description;
    private String projectId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Duration duration;
    private TestRunStatus status;
    private String environment;
    private String triggeredBy;
    private String triggerType; // CI, MANUAL, SCHEDULED, API
    private List<String> testIds;
    private List<String> executionIds;
    private Map<String, Object> metadata;
    private Map<TestExecutionStatus, Integer> statusCounts;
    private int totalTests;
    private int completedTests;
    private TestExecutionSummary summary;
    private String configurationId;
    
    /**
     * Default constructor
     */
    public TestRun() {
        this.testIds = new ArrayList<>();
        this.executionIds = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.statusCounts = new HashMap<>();
        this.status = TestRunStatus.PENDING;
        this.summary = new TestExecutionSummary();
    }
    
    /**
     * Get the unique identifier for this test run
     * 
     * @return Test run ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Set the unique identifier for this test run
     * 
     * @param id Test run ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get the name of this test run
     * 
     * @return Test run name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the name of this test run
     * 
     * @param name Test run name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get the description of this test run
     * 
     * @return Test run description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Set the description of this test run
     * 
     * @param description Test run description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Get the project ID this test run belongs to
     * 
     * @return Project ID
     */
    public String getProjectId() {
        return projectId;
    }
    
    /**
     * Set the project ID this test run belongs to
     * 
     * @param projectId Project ID
     */
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
    
    /**
     * Get the start time of this test run
     * 
     * @return Start time
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    /**
     * Set the start time of this test run
     * 
     * @param startTime Start time
     */
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
        updateDuration();
    }
    
    /**
     * Get the end time of this test run
     * 
     * @return End time
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    /**
     * Set the end time of this test run
     * 
     * @param endTime End time
     */
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        updateDuration();
    }
    
    /**
     * Get the duration of this test run
     * 
     * @return Duration
     */
    public Duration getDuration() {
        return duration;
    }
    
    /**
     * Set the duration of this test run
     * 
     * @param duration Duration
     */
    public void setDuration(Duration duration) {
        this.duration = duration;
    }
    
    /**
     * Update the duration based on start and end times
     */
    private void updateDuration() {
        if (startTime != null && endTime != null) {
            this.duration = Duration.between(startTime, endTime);
        }
    }
    
    /**
     * Get the status of this test run
     * 
     * @return Test run status
     */
    public TestRunStatus getStatus() {
        return status;
    }
    
    /**
     * Set the status of this test run
     * 
     * @param status Test run status
     */
    public void setStatus(TestRunStatus status) {
        this.status = status;
    }
    
    /**
     * Get the environment this test run was executed in
     * 
     * @return Environment name
     */
    public String getEnvironment() {
        return environment;
    }
    
    /**
     * Set the environment this test run was executed in
     * 
     * @param environment Environment name
     */
    public void setEnvironment(String environment) {
        this.environment = environment;
    }
    
    /**
     * Get who triggered this test run
     * 
     * @return Username or system identifier
     */
    public String getTriggeredBy() {
        return triggeredBy;
    }
    
    /**
     * Set who triggered this test run
     * 
     * @param triggeredBy Username or system identifier
     */
    public void setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }
    
    /**
     * Get the type of trigger that started this test run
     * 
     * @return Trigger type
     */
    public String getTriggerType() {
        return triggerType;
    }
    
    /**
     * Set the type of trigger that started this test run
     * 
     * @param triggerType Trigger type
     */
    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }
    
    /**
     * Get the list of test IDs in this test run
     * 
     * @return List of test IDs
     */
    public List<String> getTestIds() {
        return testIds;
    }
    
    /**
     * Set the list of test IDs in this test run
     * 
     * @param testIds List of test IDs
     */
    public void setTestIds(List<String> testIds) {
        this.testIds = testIds;
    }
    
    /**
     * Get the list of execution IDs in this test run
     * 
     * @return List of execution IDs
     */
    public List<String> getExecutionIds() {
        return executionIds;
    }
    
    /**
     * Set the list of execution IDs in this test run
     * 
     * @param executionIds List of execution IDs
     */
    public void setExecutionIds(List<String> executionIds) {
        this.executionIds = executionIds;
    }
    
    /**
     * Get the metadata for this test run
     * 
     * @return Map of metadata
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    /**
     * Set the metadata for this test run
     * 
     * @param metadata Map of metadata
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    /**
     * Get the status counts for this test run
     * 
     * @return Map of status to count
     */
    public Map<TestExecutionStatus, Integer> getStatusCounts() {
        return statusCounts;
    }
    
    /**
     * Set the status counts for this test run
     * 
     * @param statusCounts Map of status to count
     */
    public void setStatusCounts(Map<TestExecutionStatus, Integer> statusCounts) {
        this.statusCounts = statusCounts;
    }
    
    /**
     * Get the total number of tests in this test run
     * 
     * @return Total test count
     */
    public int getTotalTests() {
        return totalTests;
    }
    
    /**
     * Set the total number of tests in this test run
     * 
     * @param totalTests Total test count
     */
    public void setTotalTests(int totalTests) {
        this.totalTests = totalTests;
    }
    
    /**
     * Get the number of completed tests in this test run
     * 
     * @return Completed test count
     */
    public int getCompletedTests() {
        return completedTests;
    }
    
    /**
     * Set the number of completed tests in this test run
     * 
     * @param completedTests Completed test count
     */
    public void setCompletedTests(int completedTests) {
        this.completedTests = completedTests;
    }
    
    /**
     * Get the summary of this test run
     * 
     * @return Test run summary
     */
    public TestExecutionSummary getSummary() {
        return summary;
    }
    
    /**
     * Set the summary of this test run
     * 
     * @param summary Test run summary
     */
    public void setSummary(TestExecutionSummary summary) {
        this.summary = summary;
    }
    
    /**
     * Get the configuration ID used for this test run
     * 
     * @return Configuration ID
     */
    public String getConfigurationId() {
        return configurationId;
    }
    
    /**
     * Set the configuration ID used for this test run
     * 
     * @param configurationId Configuration ID
     */
    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
    }
    
    /**
     * Add a test ID to this test run
     * 
     * @param testId Test ID
     * @return This test run instance for method chaining
     */
    public TestRun addTestId(String testId) {
        if (this.testIds == null) {
            this.testIds = new ArrayList<>();
        }
        this.testIds.add(testId);
        return this;
    }
    
    /**
     * Add an execution ID to this test run
     * 
     * @param executionId Execution ID
     * @return This test run instance for method chaining
     */
    public TestRun addExecutionId(String executionId) {
        if (this.executionIds == null) {
            this.executionIds = new ArrayList<>();
        }
        this.executionIds.add(executionId);
        return this;
    }
    
    /**
     * Add a metadata entry
     * 
     * @param key Metadata key
     * @param value Metadata value
     * @return This test run instance for method chaining
     */
    public TestRun addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }
    
    /**
     * Increment the count for a specific execution status
     * 
     * @param status Execution status
     * @return This test run instance for method chaining
     */
    public TestRun incrementStatusCount(TestExecutionStatus status) {
        if (this.statusCounts == null) {
            this.statusCounts = new HashMap<>();
        }
        this.statusCounts.put(status, this.statusCounts.getOrDefault(status, 0) + 1);
        return this;
    }
    
    /**
     * Calculate the completion percentage
     * 
     * @return Completion percentage (0-100)
     */
    public double getCompletionPercentage() {
        if (totalTests == 0) {
            return 0;
        }
        return (double) completedTests / totalTests * 100;
    }
    
    /**
     * Calculate the success rate
     * 
     * @return Success rate (0-100)
     */
    public double getSuccessRate() {
        int passed = this.statusCounts.getOrDefault(TestExecutionStatus.PASSED, 0);
        int total = completedTests;
        
        if (total == 0) {
            return 0;
        }
        
        return (double) passed / total * 100;
    }
} 