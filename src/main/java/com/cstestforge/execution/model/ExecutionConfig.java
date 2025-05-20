package com.cstestforge.execution.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for test execution
 */
public class ExecutionConfig {

    private String environment;
    private BrowserType browser;
    private boolean headless;
    private boolean parallel;
    private int maxParallel;
    private int timeoutSeconds;
    private int retryCount;
    private boolean screenshotsEnabled;
    private boolean videoEnabled;
    private Map<String, Object> customSettings;

    /**
     * Default constructor
     */
    public ExecutionConfig() {
        // Set default values
        this.environment = "DEV";
        this.browser = BrowserType.CHROME;
        this.headless = false;
        this.parallel = true;
        this.maxParallel = 3;
        this.timeoutSeconds = 30;
        this.retryCount = 1;
        this.screenshotsEnabled = true;
        this.videoEnabled = false;
        this.customSettings = new HashMap<>();
    }

    /**
     * Constructor with required parameters
     * 
     * @param environment Target environment
     * @param browser Browser to use
     */
    public ExecutionConfig(String environment, BrowserType browser) {
        this();
        this.environment = environment;
        this.browser = browser;
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
     * Get the browser to use
     * 
     * @return Browser type
     */
    public BrowserType getBrowser() {
        return browser;
    }

    /**
     * Set the browser to use
     * 
     * @param browser Browser type
     */
    public void setBrowser(BrowserType browser) {
        this.browser = browser;
    }

    /**
     * Check if headless mode is enabled
     * 
     * @return true if headless mode is enabled
     */
    public boolean isHeadless() {
        return headless;
    }

    /**
     * Set whether to run in headless mode
     * 
     * @param headless true to run in headless mode
     */
    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    /**
     * Check if parallel execution is enabled
     * 
     * @return true if parallel execution is enabled
     */
    public boolean isParallel() {
        return parallel;
    }

    /**
     * Set whether to run tests in parallel
     * 
     * @param parallel true to run tests in parallel
     */
    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    /**
     * Get the maximum number of parallel test executions
     * 
     * @return Maximum parallel executions
     */
    public int getMaxParallel() {
        return maxParallel;
    }

    /**
     * Set the maximum number of parallel test executions
     * 
     * @param maxParallel Maximum parallel executions
     */
    public void setMaxParallel(int maxParallel) {
        this.maxParallel = maxParallel;
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
     * Get the number of retry attempts for failed tests
     * 
     * @return Retry count
     */
    public int getRetryCount() {
        return retryCount;
    }

    /**
     * Set the number of retry attempts for failed tests
     * 
     * @param retryCount Retry count
     */
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    /**
     * Check if screenshots are enabled
     * 
     * @return true if screenshots are enabled
     */
    public boolean isScreenshotsEnabled() {
        return screenshotsEnabled;
    }

    /**
     * Set whether to capture screenshots
     * 
     * @param screenshotsEnabled true to capture screenshots
     */
    public void setScreenshotsEnabled(boolean screenshotsEnabled) {
        this.screenshotsEnabled = screenshotsEnabled;
    }

    /**
     * Check if video recording is enabled
     * 
     * @return true if video recording is enabled
     */
    public boolean isVideoEnabled() {
        return videoEnabled;
    }

    /**
     * Set whether to record video
     * 
     * @param videoEnabled true to record video
     */
    public void setVideoEnabled(boolean videoEnabled) {
        this.videoEnabled = videoEnabled;
    }

    /**
     * Get custom settings
     * 
     * @return Map of custom settings
     */
    public Map<String, Object> getCustomSettings() {
        return customSettings;
    }

    /**
     * Set custom settings
     * 
     * @param customSettings Map of custom settings
     */
    public void setCustomSettings(Map<String, Object> customSettings) {
        this.customSettings = customSettings != null ? customSettings : new HashMap<>();
    }

    /**
     * Add a custom setting
     * 
     * @param key Setting key
     * @param value Setting value
     */
    public void addCustomSetting(String key, Object value) {
        if (customSettings == null) {
            customSettings = new HashMap<>();
        }
        customSettings.put(key, value);
    }

    /**
     * Get a custom setting
     * 
     * @param key Setting key
     * @return Setting value or null if not found
     */
    public Object getCustomSetting(String key) {
        return customSettings != null ? customSettings.get(key) : null;
    }
} 