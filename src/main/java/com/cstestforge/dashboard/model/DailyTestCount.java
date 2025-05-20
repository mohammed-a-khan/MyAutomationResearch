package com.cstestforge.dashboard.model;

/**
 * Model class representing daily test execution counts
 */
public class DailyTestCount {
    
    private String date;
    private int total;
    private int passed;
    private int failed;
    private int skipped;
    
    /**
     * Default constructor
     */
    public DailyTestCount() {
    }
    
    /**
     * Constructor with required parameters
     * 
     * @param date Date string in format YYYY-MM-DD
     * @param total Total tests executed on that day
     * @param passed Number of passed tests
     * @param failed Number of failed tests
     * @param skipped Number of skipped tests
     */
    public DailyTestCount(String date, int total, int passed, int failed, int skipped) {
        this.date = date;
        this.total = total;
        this.passed = passed;
        this.failed = failed;
        this.skipped = skipped;
    }
    
    /**
     * Get the date
     * @return Date string in format YYYY-MM-DD
     */
    public String getDate() {
        return date;
    }
    
    /**
     * Set the date
     * @param date Date string in format YYYY-MM-DD
     */
    public void setDate(String date) {
        this.date = date;
    }
    
    /**
     * Get total tests for this day
     * @return Total tests
     */
    public int getTotal() {
        return total;
    }
    
    /**
     * Set total tests for this day
     * @param total Total tests
     */
    public void setTotal(int total) {
        this.total = total;
    }
    
    /**
     * Get passed tests for this day
     * @return Passed tests
     */
    public int getPassed() {
        return passed;
    }
    
    /**
     * Set passed tests for this day
     * @param passed Passed tests
     */
    public void setPassed(int passed) {
        this.passed = passed;
    }
    
    /**
     * Get failed tests for this day
     * @return Failed tests
     */
    public int getFailed() {
        return failed;
    }
    
    /**
     * Set failed tests for this day
     * @param failed Failed tests
     */
    public void setFailed(int failed) {
        this.failed = failed;
    }
    
    /**
     * Get skipped tests for this day
     * @return Skipped tests
     */
    public int getSkipped() {
        return skipped;
    }
    
    /**
     * Set skipped tests for this day
     * @param skipped Skipped tests
     */
    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }
} 