package com.cstestforge.project.model.execution;

import com.cstestforge.project.model.test.TestStatus;
import com.cstestforge.project.model.test.TestType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a single execution instance of a test case.
 */
public class TestExecution {

    private String id;
    private String testId;
    private String projectId;
    private String name;
    private TestExecutionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Duration duration;
    private String environment;
    private String browser;
    private String triggeredBy;
    private String executionAgent;
    private boolean isManual;
    private List<TestStepExecution> stepExecutions;
    private Map<String, String> parameters;
    private Map<String, Object> metadata;
    private Map<String, Object> metrics;
    private TestExecutionSummary summary;
    private String errorMessage;
    private String errorType;
    private List<String> screenshotPaths;
    private String videoPath;
    private String logPath;
    private TestType testType;
    private String testRunId;
    private int retryCount;
    
    /**
     * Default constructor
     */
    public TestExecution() {
        this.stepExecutions = new ArrayList<>();
        this.parameters = new HashMap<>();
        this.metadata = new HashMap<>();
        this.metrics = new HashMap<>();
        this.screenshotPaths = new ArrayList<>();
        this.summary = new TestExecutionSummary();
        this.retryCount = 0;
        this.status = TestExecutionStatus.PENDING;
    }
    
    /**
     * Get the unique identifier for this execution
     * 
     * @return Execution ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Set the unique identifier for this execution
     * 
     * @param id Execution ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get the test ID this execution is for
     * 
     * @return Test ID
     */
    public String getTestId() {
        return testId;
    }
    
    /**
     * Set the test ID this execution is for
     * 
     * @param testId Test ID
     */
    public void setTestId(String testId) {
        this.testId = testId;
    }
    
    /**
     * Get the project ID this execution is for
     * 
     * @return Project ID
     */
    public String getProjectId() {
        return projectId;
    }
    
    /**
     * Set the project ID this execution is for
     * 
     * @param projectId Project ID
     */
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
    
    /**
     * Get the name of this execution
     * 
     * @return Execution name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the name of this execution
     * 
     * @param name Execution name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get the status of this execution
     * 
     * @return Execution status
     */
    public TestExecutionStatus getStatus() {
        return status;
    }
    
    /**
     * Set the status of this execution
     * 
     * @param status Execution status
     */
    public void setStatus(TestExecutionStatus status) {
        this.status = status;
    }
    
    /**
     * Get the start time of this execution
     * 
     * @return Start time
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    /**
     * Set the start time of this execution
     * 
     * @param startTime Start time
     */
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
        updateDuration();
    }
    
    /**
     * Get the end time of this execution
     * 
     * @return End time
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    /**
     * Set the end time of this execution
     * 
     * @param endTime End time
     */
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        updateDuration();
    }
    
    /**
     * Get the duration of this execution
     * 
     * @return Duration
     */
    public Duration getDuration() {
        return duration;
    }
    
    /**
     * Set the duration of this execution
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
     * Get the environment this execution ran in
     * 
     * @return Environment name
     */
    public String getEnvironment() {
        return environment;
    }
    
    /**
     * Set the environment this execution ran in
     * 
     * @param environment Environment name
     */
    public void setEnvironment(String environment) {
        this.environment = environment;
    }
    
    /**
     * Get the browser used for this execution
     * 
     * @return Browser name
     */
    public String getBrowser() {
        return browser;
    }
    
    /**
     * Set the browser used for this execution
     * 
     * @param browser Browser name
     */
    public void setBrowser(String browser) {
        this.browser = browser;
    }
    
    /**
     * Get who triggered this execution
     * 
     * @return Username or system identifier
     */
    public String getTriggeredBy() {
        return triggeredBy;
    }
    
    /**
     * Set who triggered this execution
     * 
     * @param triggeredBy Username or system identifier
     */
    public void setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }
    
    /**
     * Get the execution agent that ran this execution
     * 
     * @return Agent identifier
     */
    public String getExecutionAgent() {
        return executionAgent;
    }
    
    /**
     * Set the execution agent that ran this execution
     * 
     * @param executionAgent Agent identifier
     */
    public void setExecutionAgent(String executionAgent) {
        this.executionAgent = executionAgent;
    }
    
    /**
     * Check if this was a manual execution
     * 
     * @return True if manual execution
     */
    public boolean isManual() {
        return isManual;
    }
    
    /**
     * Set whether this was a manual execution
     * 
     * @param manual True if manual execution
     */
    public void setManual(boolean manual) {
        isManual = manual;
    }
    
    /**
     * Get the step executions in this test run
     * 
     * @return List of step executions
     */
    public List<TestStepExecution> getStepExecutions() {
        return stepExecutions;
    }
    
    /**
     * Set the step executions in this test run
     * 
     * @param stepExecutions List of step executions
     */
    public void setStepExecutions(List<TestStepExecution> stepExecutions) {
        this.stepExecutions = stepExecutions;
    }
    
    /**
     * Get the parameters used in this execution
     * 
     * @return Map of parameters
     */
    public Map<String, String> getParameters() {
        return parameters;
    }
    
    /**
     * Set the parameters used in this execution
     * 
     * @param parameters Map of parameters
     */
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
    
    /**
     * Get the metadata for this execution
     * 
     * @return Map of metadata
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    /**
     * Set the metadata for this execution
     * 
     * @param metadata Map of metadata
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    /**
     * Get the performance metrics for this execution
     * 
     * @return Map of metrics
     */
    public Map<String, Object> getMetrics() {
        return metrics;
    }
    
    /**
     * Set the performance metrics for this execution
     * 
     * @param metrics Map of metrics
     */
    public void setMetrics(Map<String, Object> metrics) {
        this.metrics = metrics;
    }
    
    /**
     * Get the summary of this execution
     * 
     * @return Execution summary
     */
    public TestExecutionSummary getSummary() {
        return summary;
    }
    
    /**
     * Set the summary of this execution
     * 
     * @param summary Execution summary
     */
    public void setSummary(TestExecutionSummary summary) {
        this.summary = summary;
    }
    
    /**
     * Get the error message if this execution failed
     * 
     * @return Error message
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Set the error message if this execution failed
     * 
     * @param errorMessage Error message
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    /**
     * Get the error type if this execution failed
     * 
     * @return Error type
     */
    public String getErrorType() {
        return errorType;
    }
    
    /**
     * Set the error type if this execution failed
     * 
     * @param errorType Error type
     */
    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }
    
    /**
     * Get the paths to screenshots taken during this execution
     * 
     * @return List of screenshot paths
     */
    public List<String> getScreenshotPaths() {
        return screenshotPaths;
    }
    
    /**
     * Set the paths to screenshots taken during this execution
     * 
     * @param screenshotPaths List of screenshot paths
     */
    public void setScreenshotPaths(List<String> screenshotPaths) {
        this.screenshotPaths = screenshotPaths;
    }
    
    /**
     * Get the path to the video recording of this execution
     * 
     * @return Video path
     */
    public String getVideoPath() {
        return videoPath;
    }
    
    /**
     * Set the path to the video recording of this execution
     * 
     * @param videoPath Video path
     */
    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }
    
    /**
     * Get the path to the log file for this execution
     * 
     * @return Log path
     */
    public String getLogPath() {
        return logPath;
    }
    
    /**
     * Set the path to the log file for this execution
     * 
     * @param logPath Log path
     */
    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }
    
    /**
     * Get the type of test that was executed
     * 
     * @return Test type
     */
    public TestType getTestType() {
        return testType;
    }
    
    /**
     * Set the type of test that was executed
     * 
     * @param testType Test type
     */
    public void setTestType(TestType testType) {
        this.testType = testType;
    }
    
    /**
     * Get the test run ID this execution is part of
     * 
     * @return Test run ID
     */
    public String getTestRunId() {
        return testRunId;
    }
    
    /**
     * Set the test run ID this execution is part of
     * 
     * @param testRunId Test run ID
     */
    public void setTestRunId(String testRunId) {
        this.testRunId = testRunId;
    }
    
    /**
     * Get the retry count for this execution
     * 
     * @return Retry count
     */
    public int getRetryCount() {
        return retryCount;
    }
    
    /**
     * Set the retry count for this execution
     * 
     * @param retryCount Retry count
     */
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    
    /**
     * Add a step execution
     * 
     * @param stepExecution Step execution to add
     * @return This instance for method chaining
     */
    public TestExecution addStepExecution(TestStepExecution stepExecution) {
        if (this.stepExecutions == null) {
            this.stepExecutions = new ArrayList<>();
        }
        this.stepExecutions.add(stepExecution);
        return this;
    }
    
    /**
     * Add a parameter
     * 
     * @param key Parameter name
     * @param value Parameter value
     * @return This instance for method chaining
     */
    public TestExecution addParameter(String key, String value) {
        if (this.parameters == null) {
            this.parameters = new HashMap<>();
        }
        this.parameters.put(key, value);
        return this;
    }
    
    /**
     * Add a metadata entry
     * 
     * @param key Metadata key
     * @param value Metadata value
     * @return This instance for method chaining
     */
    public TestExecution addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }
    
    /**
     * Add a metric
     * 
     * @param key Metric name
     * @param value Metric value
     * @return This instance for method chaining
     */
    public TestExecution addMetric(String key, Object value) {
        if (this.metrics == null) {
            this.metrics = new HashMap<>();
        }
        this.metrics.put(key, value);
        return this;
    }
    
    /**
     * Add a screenshot path
     * 
     * @param path Path to screenshot
     * @return This instance for method chaining
     */
    public TestExecution addScreenshotPath(String path) {
        if (this.screenshotPaths == null) {
            this.screenshotPaths = new ArrayList<>();
        }
        this.screenshotPaths.add(path);
        return this;
    }
} 