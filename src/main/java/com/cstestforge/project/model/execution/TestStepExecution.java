package com.cstestforge.project.model.execution;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the execution of a single test step.
 */
public class TestStepExecution {

    private String id;
    private String stepId;
    private int order;
    private String name;
    private TestExecutionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Duration duration;
    private String errorMessage;
    private String errorStackTrace;
    private Map<String, Object> metadata;
    private Map<String, Object> metrics;
    private List<String> screenshotPaths;
    
    /**
     * Default constructor
     */
    public TestStepExecution() {
        this.metadata = new HashMap<>();
        this.metrics = new HashMap<>();
        this.screenshotPaths = new ArrayList<>();
    }

    /**
     * Get the unique identifier for this step execution
     * 
     * @return Step execution ID
     */
    public String getId() {
        return id;
    }

    /**
     * Set the unique identifier for this step execution
     * 
     * @param id Step execution ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the ID of the test step being executed
     * 
     * @return Step ID
     */
    public String getStepId() {
        return stepId;
    }

    /**
     * Set the ID of the test step being executed
     * 
     * @param stepId Step ID
     */
    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    /**
     * Get the execution order of this step
     * 
     * @return Order number
     */
    public int getOrder() {
        return order;
    }

    /**
     * Set the execution order of this step
     * 
     * @param order Order number
     */
    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * Get the name of this step execution
     * 
     * @return Step name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this step execution
     * 
     * @param name Step name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the status of this step execution
     * 
     * @return Step execution status
     */
    public TestExecutionStatus getStatus() {
        return status;
    }

    /**
     * Set the status of this step execution
     * 
     * @param status Step execution status
     */
    public void setStatus(TestExecutionStatus status) {
        this.status = status;
    }

    /**
     * Get the start time of this step execution
     * 
     * @return Start time
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }

    /**
     * Set the start time of this step execution
     * 
     * @param startTime Start time
     */
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
        updateDuration();
    }

    /**
     * Get the end time of this step execution
     * 
     * @return End time
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }

    /**
     * Set the end time of this step execution
     * 
     * @param endTime End time
     */
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        updateDuration();
    }

    /**
     * Get the duration of this step execution
     * 
     * @return Duration
     */
    public Duration getDuration() {
        return duration;
    }

    /**
     * Set the duration of this step execution
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
     * Get the error message if this step execution failed
     * 
     * @return Error message
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Set the error message if this step execution failed
     * 
     * @param errorMessage Error message
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Get the error stack trace if this step execution failed
     * 
     * @return Error stack trace
     */
    public String getErrorStackTrace() {
        return errorStackTrace;
    }

    /**
     * Set the error stack trace if this step execution failed
     * 
     * @param errorStackTrace Error stack trace
     */
    public void setErrorStackTrace(String errorStackTrace) {
        this.errorStackTrace = errorStackTrace;
    }

    /**
     * Get the metadata for this step execution
     * 
     * @return Map of metadata
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Set the metadata for this step execution
     * 
     * @param metadata Map of metadata
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /**
     * Get the metrics for this step execution
     * 
     * @return Map of metrics
     */
    public Map<String, Object> getMetrics() {
        return metrics;
    }

    /**
     * Set the metrics for this step execution
     * 
     * @param metrics Map of metrics
     */
    public void setMetrics(Map<String, Object> metrics) {
        this.metrics = metrics;
    }

    /**
     * Get the paths to screenshots taken during this step execution
     * 
     * @return List of screenshot paths
     */
    public List<String> getScreenshotPaths() {
        return screenshotPaths;
    }

    /**
     * Set the paths to screenshots taken during this step execution
     * 
     * @param screenshotPaths List of screenshot paths
     */
    public void setScreenshotPaths(List<String> screenshotPaths) {
        this.screenshotPaths = screenshotPaths;
    }

    /**
     * Add a metadata entry
     * 
     * @param key Metadata key
     * @param value Metadata value
     * @return This instance for method chaining
     */
    public TestStepExecution addMetadata(String key, Object value) {
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
    public TestStepExecution addMetric(String key, Object value) {
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
    public TestStepExecution addScreenshotPath(String path) {
        if (this.screenshotPaths == null) {
            this.screenshotPaths = new ArrayList<>();
        }
        this.screenshotPaths.add(path);
        return this;
    }
} 