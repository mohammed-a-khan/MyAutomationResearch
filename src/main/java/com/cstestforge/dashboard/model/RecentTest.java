package com.cstestforge.dashboard.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Model class for recent test executions to display in the dashboard
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecentTest {
    
    private String id;
    private String name;
    private String status;
    private long duration;
    private long timestamp;
    private String testId;
    private String projectId;
    private String environment;
    private String browser;
    
    /**
     * Default constructor
     */
    public RecentTest() {
    }
    
    /**
     * Constructor with required parameters
     * 
     * @param id Execution ID
     * @param name Test name
     * @param status Execution status (passed, failed, skipped, etc.)
     * @param duration Execution duration in milliseconds
     * @param timestamp Execution timestamp
     */
    public RecentTest(String id, String name, String status, long duration, long timestamp) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.duration = duration;
        this.timestamp = timestamp;
    }
    
    /**
     * Get the execution ID
     * @return Execution ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Set the execution ID
     * @param id Execution ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get the test name
     * @return Test name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the test name
     * @param name Test name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get the execution status
     * @return Status (passed, failed, skipped, etc.)
     */
    public String getStatus() {
        return status;
    }
    
    /**
     * Set the execution status
     * @param status Status (passed, failed, skipped, etc.)
     */
    public void setStatus(String status) {
        this.status = status;
    }
    
    /**
     * Get the execution duration in milliseconds
     * @return Duration in milliseconds
     */
    public long getDuration() {
        return duration;
    }
    
    /**
     * Set the execution duration in milliseconds
     * @param duration Duration in milliseconds
     */
    public void setDuration(long duration) {
        this.duration = duration;
    }
    
    /**
     * Get the execution timestamp (epoch milliseconds)
     * @return Timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Set the execution timestamp (epoch milliseconds)
     * @param timestamp Timestamp
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Get the test ID
     * @return Test ID
     */
    public String getTestId() {
        return testId;
    }
    
    /**
     * Set the test ID
     * @param testId Test ID
     */
    public void setTestId(String testId) {
        this.testId = testId;
    }
    
    /**
     * Get the project ID
     * @return Project ID
     */
    public String getProjectId() {
        return projectId;
    }
    
    /**
     * Set the project ID
     * @param projectId Project ID
     */
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
    
    /**
     * Get the environment name
     * @return Environment name
     */
    public String getEnvironment() {
        return environment;
    }
    
    /**
     * Set the environment name
     * @param environment Environment name
     */
    public void setEnvironment(String environment) {
        this.environment = environment;
    }
    
    /**
     * Get the browser name
     * @return Browser name
     */
    public String getBrowser() {
        return browser;
    }
    
    /**
     * Set the browser name
     * @param browser Browser name
     */
    public void setBrowser(String browser) {
        this.browser = browser;
    }
} 