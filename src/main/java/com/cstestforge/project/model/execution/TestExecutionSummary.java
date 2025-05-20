package com.cstestforge.project.model.execution;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Summary of a test execution with statistics and metrics.
 */
public class TestExecutionSummary {

    private int totalSteps;
    private int passedSteps;
    private int failedSteps;
    private int skippedSteps;
    private int blockedSteps;
    private int warningSteps;
    private int errorSteps;
    private Duration totalDuration;
    private Duration averageStepDuration;
    private Duration maxStepDuration;
    private String slowestStep;
    private String fastestStep;
    private Map<String, Object> customMetrics;
    
    /**
     * Default constructor
     */
    public TestExecutionSummary() {
        this.totalSteps = 0;
        this.passedSteps = 0;
        this.failedSteps = 0;
        this.skippedSteps = 0;
        this.blockedSteps = 0;
        this.warningSteps = 0;
        this.errorSteps = 0;
        this.totalDuration = Duration.ZERO;
        this.averageStepDuration = Duration.ZERO;
        this.maxStepDuration = Duration.ZERO;
        this.customMetrics = new HashMap<>();
    }
    
    /**
     * Get the total number of steps
     * 
     * @return Total steps
     */
    public int getTotalSteps() {
        return totalSteps;
    }
    
    /**
     * Set the total number of steps
     * 
     * @param totalSteps Total steps
     */
    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }
    
    /**
     * Get the number of passed steps
     * 
     * @return Passed steps
     */
    public int getPassedSteps() {
        return passedSteps;
    }
    
    /**
     * Set the number of passed steps
     * 
     * @param passedSteps Passed steps
     */
    public void setPassedSteps(int passedSteps) {
        this.passedSteps = passedSteps;
    }
    
    /**
     * Get the number of failed steps
     * 
     * @return Failed steps
     */
    public int getFailedSteps() {
        return failedSteps;
    }
    
    /**
     * Set the number of failed steps
     * 
     * @param failedSteps Failed steps
     */
    public void setFailedSteps(int failedSteps) {
        this.failedSteps = failedSteps;
    }
    
    /**
     * Get the number of skipped steps
     * 
     * @return Skipped steps
     */
    public int getSkippedSteps() {
        return skippedSteps;
    }
    
    /**
     * Set the number of skipped steps
     * 
     * @param skippedSteps Skipped steps
     */
    public void setSkippedSteps(int skippedSteps) {
        this.skippedSteps = skippedSteps;
    }
    
    /**
     * Get the number of blocked steps
     * 
     * @return Blocked steps
     */
    public int getBlockedSteps() {
        return blockedSteps;
    }
    
    /**
     * Set the number of blocked steps
     * 
     * @param blockedSteps Blocked steps
     */
    public void setBlockedSteps(int blockedSteps) {
        this.blockedSteps = blockedSteps;
    }
    
    /**
     * Get the number of steps with warnings
     * 
     * @return Warning steps
     */
    public int getWarningSteps() {
        return warningSteps;
    }
    
    /**
     * Set the number of steps with warnings
     * 
     * @param warningSteps Warning steps
     */
    public void setWarningSteps(int warningSteps) {
        this.warningSteps = warningSteps;
    }
    
    /**
     * Get the number of steps with errors
     * 
     * @return Error steps
     */
    public int getErrorSteps() {
        return errorSteps;
    }
    
    /**
     * Set the number of steps with errors
     * 
     * @param errorSteps Error steps
     */
    public void setErrorSteps(int errorSteps) {
        this.errorSteps = errorSteps;
    }
    
    /**
     * Get the total duration of the test
     * 
     * @return Total duration
     */
    public Duration getTotalDuration() {
        return totalDuration;
    }
    
    /**
     * Set the total duration of the test
     * 
     * @param totalDuration Total duration
     */
    public void setTotalDuration(Duration totalDuration) {
        this.totalDuration = totalDuration;
    }
    
    /**
     * Get the average duration of steps
     * 
     * @return Average step duration
     */
    public Duration getAverageStepDuration() {
        return averageStepDuration;
    }
    
    /**
     * Set the average duration of steps
     * 
     * @param averageStepDuration Average step duration
     */
    public void setAverageStepDuration(Duration averageStepDuration) {
        this.averageStepDuration = averageStepDuration;
    }
    
    /**
     * Get the maximum duration of any step
     * 
     * @return Maximum step duration
     */
    public Duration getMaxStepDuration() {
        return maxStepDuration;
    }
    
    /**
     * Set the maximum duration of any step
     * 
     * @param maxStepDuration Maximum step duration
     */
    public void setMaxStepDuration(Duration maxStepDuration) {
        this.maxStepDuration = maxStepDuration;
    }
    
    /**
     * Get the name of the slowest step
     * 
     * @return Slowest step name
     */
    public String getSlowestStep() {
        return slowestStep;
    }
    
    /**
     * Set the name of the slowest step
     * 
     * @param slowestStep Slowest step name
     */
    public void setSlowestStep(String slowestStep) {
        this.slowestStep = slowestStep;
    }
    
    /**
     * Get the name of the fastest step
     * 
     * @return Fastest step name
     */
    public String getFastestStep() {
        return fastestStep;
    }
    
    /**
     * Set the name of the fastest step
     * 
     * @param fastestStep Fastest step name
     */
    public void setFastestStep(String fastestStep) {
        this.fastestStep = fastestStep;
    }
    
    /**
     * Get the custom metrics
     * 
     * @return Map of custom metrics
     */
    public Map<String, Object> getCustomMetrics() {
        return customMetrics;
    }
    
    /**
     * Set the custom metrics
     * 
     * @param customMetrics Map of custom metrics
     */
    public void setCustomMetrics(Map<String, Object> customMetrics) {
        this.customMetrics = customMetrics;
    }
    
    /**
     * Calculate the pass rate as a percentage
     * 
     * @return Pass rate (0-100)
     */
    public double getPassRate() {
        if (totalSteps == 0) {
            return 0;
        }
        return (double) passedSteps / totalSteps * 100;
    }
    
    /**
     * Calculate the fail rate as a percentage
     * 
     * @return Fail rate (0-100)
     */
    public double getFailRate() {
        if (totalSteps == 0) {
            return 0;
        }
        return (double) failedSteps / totalSteps * 100;
    }
    
    /**
     * Add a custom metric
     * 
     * @param key Metric name
     * @param value Metric value
     * @return This instance for method chaining
     */
    public TestExecutionSummary addCustomMetric(String key, Object value) {
        if (this.customMetrics == null) {
            this.customMetrics = new HashMap<>();
        }
        this.customMetrics.put(key, value);
        return this;
    }
    
    /**
     * Update the summary with data from a step execution
     * 
     * @param step Step execution to include in the summary
     * @return This instance for method chaining
     */
    public TestExecutionSummary updateWithStep(TestStepExecution step) {
        totalSteps++;
        
        // Update counts based on status
        if (step.getStatus() != null) {
            switch (step.getStatus()) {
                case PASSED:
                    passedSteps++;
                    break;
                case FAILED:
                    failedSteps++;
                    break;
                case SKIPPED:
                    skippedSteps++;
                    break;
                case BLOCKED:
                    blockedSteps++;
                    break;
                case WARNING:
                    warningSteps++;
                    break;
                case ERROR:
                    errorSteps++;
                    break;
                default:
                    break;
            }
        }
        
        // Update duration metrics
        if (step.getDuration() != null) {
            Duration stepDuration = step.getDuration();
            
            // Update total duration
            totalDuration = totalDuration.plus(stepDuration);
            
            // Update max duration if this step is longer
            if (maxStepDuration.compareTo(stepDuration) < 0) {
                maxStepDuration = stepDuration;
                slowestStep = step.getName();
            }
            
            // Update average duration
            averageStepDuration = totalDuration.dividedBy(totalSteps);
        }
        
        return this;
    }
} 