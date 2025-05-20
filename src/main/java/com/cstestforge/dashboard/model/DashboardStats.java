package com.cstestforge.dashboard.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Model class for dashboard statistics
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardStats {
    
    private int totalTests;
    private int passedTests;
    private int failedTests;
    private int skippedTests;
    private double successRate;
    private long avgDuration;
    private List<DailyTestCount> testsByDay;
    
    /**
     * Default constructor
     */
    public DashboardStats() {
    }
    
    /**
     * Constructor with required parameters
     * 
     * @param totalTests Total number of tests
     * @param passedTests Number of passed tests
     * @param failedTests Number of failed tests
     * @param skippedTests Number of skipped tests
     * @param successRate Success rate (0-100)
     * @param avgDuration Average test duration in milliseconds
     */
    public DashboardStats(int totalTests, int passedTests, int failedTests, int skippedTests,
                         double successRate, long avgDuration) {
        this.totalTests = totalTests;
        this.passedTests = passedTests;
        this.failedTests = failedTests;
        this.skippedTests = skippedTests;
        this.successRate = successRate;
        this.avgDuration = avgDuration;
    }
    
    /**
     * Get total number of tests
     * @return Total tests
     */
    public int getTotalTests() {
        return totalTests;
    }
    
    /**
     * Set total number of tests
     * @param totalTests Total tests
     */
    public void setTotalTests(int totalTests) {
        this.totalTests = totalTests;
    }
    
    /**
     * Get number of passed tests
     * @return Passed tests
     */
    public int getPassedTests() {
        return passedTests;
    }
    
    /**
     * Set number of passed tests
     * @param passedTests Passed tests
     */
    public void setPassedTests(int passedTests) {
        this.passedTests = passedTests;
    }
    
    /**
     * Get number of failed tests
     * @return Failed tests
     */
    public int getFailedTests() {
        return failedTests;
    }
    
    /**
     * Set number of failed tests
     * @param failedTests Failed tests
     */
    public void setFailedTests(int failedTests) {
        this.failedTests = failedTests;
    }
    
    /**
     * Get number of skipped tests
     * @return Skipped tests
     */
    public int getSkippedTests() {
        return skippedTests;
    }
    
    /**
     * Set number of skipped tests
     * @param skippedTests Skipped tests
     */
    public void setSkippedTests(int skippedTests) {
        this.skippedTests = skippedTests;
    }
    
    /**
     * Get success rate (0-100)
     * @return Success rate
     */
    public double getSuccessRate() {
        return successRate;
    }
    
    /**
     * Set success rate (0-100)
     * @param successRate Success rate
     */
    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }
    
    /**
     * Get average test duration in milliseconds
     * @return Average duration
     */
    public long getAvgDuration() {
        return avgDuration;
    }
    
    /**
     * Set average test duration in milliseconds
     * @param avgDuration Average duration
     */
    public void setAvgDuration(long avgDuration) {
        this.avgDuration = avgDuration;
    }
    
    /**
     * Get daily test counts
     * @return List of daily test counts
     */
    public List<DailyTestCount> getTestsByDay() {
        return testsByDay;
    }
    
    /**
     * Set daily test counts
     * @param testsByDay List of daily test counts
     */
    public void setTestsByDay(List<DailyTestCount> testsByDay) {
        this.testsByDay = testsByDay;
    }
} 