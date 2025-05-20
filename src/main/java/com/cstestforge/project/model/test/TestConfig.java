package com.cstestforge.project.model.test;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration settings for a test.
 */
public class TestConfig {

    private String browser;
    private String environment;
    private Map<String, String> variables;
    private int timeoutSeconds;
    private boolean screenshotsEnabled;
    private boolean headless;
    private String framework;
    private Map<String, Object> frameworkOptions;
    private String reportingFormat;
    private int retryCount;
    
    /**
     * Default constructor
     */
    public TestConfig() {
        this.variables = new HashMap<>();
        this.frameworkOptions = new HashMap<>();
        this.timeoutSeconds = 30; // Default timeout
        this.screenshotsEnabled = true;
        this.retryCount = 0;
    }
    
    /**
     * Get the browser to use for this test
     * 
     * @return Browser name
     */
    public String getBrowser() {
        return browser;
    }
    
    /**
     * Set the browser to use for this test
     * 
     * @param browser Browser name
     */
    public void setBrowser(String browser) {
        this.browser = browser;
    }
    
    /**
     * Get the environment to run this test in
     * 
     * @return Environment name
     */
    public String getEnvironment() {
        return environment;
    }
    
    /**
     * Set the environment to run this test in
     * 
     * @param environment Environment name
     */
    public void setEnvironment(String environment) {
        this.environment = environment;
    }
    
    /**
     * Get the variables for this test
     * 
     * @return Map of variables
     */
    public Map<String, String> getVariables() {
        return variables;
    }
    
    /**
     * Set the variables for this test
     * 
     * @param variables Map of variables
     */
    public void setVariables(Map<String, String> variables) {
        this.variables = variables;
    }
    
    /**
     * Get the timeout in seconds
     * 
     * @return Timeout in seconds
     */
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }
    
    /**
     * Set the timeout in seconds
     * 
     * @param timeoutSeconds Timeout in seconds
     */
    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
    
    /**
     * Check if screenshots are enabled
     * 
     * @return True if screenshots are enabled
     */
    public boolean isScreenshotsEnabled() {
        return screenshotsEnabled;
    }
    
    /**
     * Set whether screenshots are enabled
     * 
     * @param screenshotsEnabled True to enable screenshots
     */
    public void setScreenshotsEnabled(boolean screenshotsEnabled) {
        this.screenshotsEnabled = screenshotsEnabled;
    }
    
    /**
     * Check if headless mode is enabled
     * 
     * @return True if headless mode is enabled
     */
    public boolean isHeadless() {
        return headless;
    }
    
    /**
     * Set whether headless mode is enabled
     * 
     * @param headless True to enable headless mode
     */
    public void setHeadless(boolean headless) {
        this.headless = headless;
    }
    
    /**
     * Get the test framework
     * 
     * @return Framework name
     */
    public String getFramework() {
        return framework;
    }
    
    /**
     * Set the test framework
     * 
     * @param framework Framework name
     */
    public void setFramework(String framework) {
        this.framework = framework;
    }
    
    /**
     * Get the framework-specific options
     * 
     * @return Map of options
     */
    public Map<String, Object> getFrameworkOptions() {
        return frameworkOptions;
    }
    
    /**
     * Set the framework-specific options
     * 
     * @param frameworkOptions Map of options
     */
    public void setFrameworkOptions(Map<String, Object> frameworkOptions) {
        this.frameworkOptions = frameworkOptions;
    }
    
    /**
     * Get the reporting format
     * 
     * @return Reporting format
     */
    public String getReportingFormat() {
        return reportingFormat;
    }
    
    /**
     * Set the reporting format
     * 
     * @param reportingFormat Reporting format
     */
    public void setReportingFormat(String reportingFormat) {
        this.reportingFormat = reportingFormat;
    }
    
    /**
     * Get the retry count
     * 
     * @return Number of retries
     */
    public int getRetryCount() {
        return retryCount;
    }
    
    /**
     * Set the retry count
     * 
     * @param retryCount Number of retries
     */
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    
    /**
     * Add a variable
     * 
     * @param key Variable name
     * @param value Variable value
     * @return This config instance for method chaining
     */
    public TestConfig addVariable(String key, String value) {
        if (this.variables == null) {
            this.variables = new HashMap<>();
        }
        this.variables.put(key, value);
        return this;
    }
    
    /**
     * Add a framework option
     * 
     * @param key Option name
     * @param value Option value
     * @return This config instance for method chaining
     */
    public TestConfig addFrameworkOption(String key, Object value) {
        if (this.frameworkOptions == null) {
            this.frameworkOptions = new HashMap<>();
        }
        this.frameworkOptions.put(key, value);
        return this;
    }
} 