package com.cstestforge.execution.model;

import java.util.List;

/**
 * Request object for test execution
 */
public class TestExecutionRequest {
    
    private String projectId;
    private List<String> testIds;
    private String suiteId;
    private ExecutionConfig config;

    /**
     * Default constructor
     */
    public TestExecutionRequest() {
    }

    /**
     * Constructor with required parameters
     * 
     * @param projectId Project ID
     * @param testIds List of test IDs to execute
     * @param config Execution configuration
     */
    public TestExecutionRequest(String projectId, List<String> testIds, ExecutionConfig config) {
        this.projectId = projectId;
        this.testIds = testIds;
        this.config = config;
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
     * Get the list of test IDs to execute
     * 
     * @return List of test IDs
     */
    public List<String> getTestIds() {
        return testIds;
    }
    
    /**
     * Set the list of test IDs to execute
     * 
     * @param testIds List of test IDs
     */
    public void setTestIds(List<String> testIds) {
        this.testIds = testIds;
    }
    
    /**
     * Get the suite ID if running a test suite
     * 
     * @return Suite ID or null
     */
    public String getSuiteId() {
        return suiteId;
    }
    
    /**
     * Set the suite ID if running a test suite
     * 
     * @param suiteId Suite ID
     */
    public void setSuiteId(String suiteId) {
        this.suiteId = suiteId;
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
} 