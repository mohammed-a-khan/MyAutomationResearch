package com.cstestforge.recorder.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for a recording session.
 */
public class RecordingConfig {
    
    private String browserType;
    private Viewport viewport;
    private boolean captureScreenshots;
    private boolean captureNetwork;
    private boolean captureConsole;
    private String baseUrl;
    private Map<String, String> environmentVariables;
    private int maxEventCount;
    private int idleTimeoutSeconds;
    private int commandTimeoutSeconds;
    private String playwrightServiceUrl;
    
    /**
     * Default constructor
     */
    public RecordingConfig() {
        this.viewport = new Viewport();
        this.captureScreenshots = true;
        this.captureNetwork = false;
        this.captureConsole = false;
        this.environmentVariables = new HashMap<>();
        this.maxEventCount = 1000;
        this.idleTimeoutSeconds = 600; // 10 minutes
        this.commandTimeoutSeconds = 60; // 1 minute
        this.playwrightServiceUrl = "http://localhost:3500"; // Default Playwright service URL
    }
    
    /**
     * Get the browser type
     * 
     * @return Browser type (chrome, firefox, edge, etc.)
     */
    public String getBrowserType() {
        return browserType;
    }
    
    /**
     * Set the browser type
     * 
     * @param browserType Browser type
     */
    public void setBrowserType(String browserType) {
        this.browserType = browserType;
    }
    
    /**
     * Get the viewport configuration
     * 
     * @return Viewport configuration
     */
    public Viewport getViewport() {
        return viewport;
    }
    
    /**
     * Set the viewport configuration
     * 
     * @param viewport Viewport configuration
     */
    public void setViewport(Viewport viewport) {
        this.viewport = viewport;
    }
    
    /**
     * Check if screenshots should be captured
     * 
     * @return True if screenshots should be captured
     */
    public boolean isCaptureScreenshots() {
        return captureScreenshots;
    }
    
    /**
     * Set whether screenshots should be captured
     * 
     * @param captureScreenshots True to capture screenshots
     */
    public void setCaptureScreenshots(boolean captureScreenshots) {
        this.captureScreenshots = captureScreenshots;
    }
    
    /**
     * Check if network activity should be captured
     * 
     * @return True if network activity should be captured
     */
    public boolean isCaptureNetwork() {
        return captureNetwork;
    }
    
    /**
     * Set whether network activity should be captured
     * 
     * @param captureNetwork True to capture network activity
     */
    public void setCaptureNetwork(boolean captureNetwork) {
        this.captureNetwork = captureNetwork;
    }
    
    /**
     * Check if console output should be captured
     * 
     * @return True if console output should be captured
     */
    public boolean isCaptureConsole() {
        return captureConsole;
    }
    
    /**
     * Set whether console output should be captured
     * 
     * @param captureConsole True to capture console output
     */
    public void setCaptureConsole(boolean captureConsole) {
        this.captureConsole = captureConsole;
    }
    
    /**
     * Get the base URL
     * 
     * @return Base URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }
    
    /**
     * Set the base URL
     * 
     * @param baseUrl Base URL
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    /**
     * Get the environment variables
     * 
     * @return Map of environment variables
     */
    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }
    
    /**
     * Set the environment variables
     * 
     * @param environmentVariables Map of environment variables
     */
    public void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }
    
    /**
     * Get the maximum number of events to record
     * 
     * @return Maximum event count
     */
    public int getMaxEventCount() {
        return maxEventCount;
    }
    
    /**
     * Set the maximum number of events to record
     * 
     * @param maxEventCount Maximum event count
     */
    public void setMaxEventCount(int maxEventCount) {
        this.maxEventCount = maxEventCount;
    }
    
    /**
     * Get the idle timeout in seconds
     * 
     * @return Idle timeout in seconds
     */
    public int getIdleTimeoutSeconds() {
        return idleTimeoutSeconds;
    }
    
    /**
     * Set the idle timeout in seconds
     * 
     * @param idleTimeoutSeconds Idle timeout in seconds
     */
    public void setIdleTimeoutSeconds(int idleTimeoutSeconds) {
        this.idleTimeoutSeconds = idleTimeoutSeconds;
    }
    
    /**
     * Get the command timeout in seconds
     * 
     * @return Command timeout in seconds
     */
    public int getCommandTimeoutSeconds() {
        return commandTimeoutSeconds;
    }
    
    /**
     * Set the command timeout in seconds
     * 
     * @param commandTimeoutSeconds Command timeout in seconds
     */
    public void setCommandTimeoutSeconds(int commandTimeoutSeconds) {
        this.commandTimeoutSeconds = commandTimeoutSeconds;
    }
    
    /**
     * Add an environment variable
     * 
     * @param key Variable name
     * @param value Variable value
     * @return This config instance for method chaining
     */
    public RecordingConfig addEnvironmentVariable(String key, String value) {
        if (this.environmentVariables == null) {
            this.environmentVariables = new HashMap<>();
        }
        this.environmentVariables.put(key, value);
        return this;
    }
    
    /**
     * Get the Playwright service URL
     * 
     * @return The URL of the Playwright service
     */
    public String getPlaywrightServiceUrl() {
        return playwrightServiceUrl;
    }
    
    /**
     * Set the Playwright service URL
     * 
     * @param playwrightServiceUrl The URL of the Playwright service
     */
    public void setPlaywrightServiceUrl(String playwrightServiceUrl) {
        this.playwrightServiceUrl = playwrightServiceUrl;
    }
} 