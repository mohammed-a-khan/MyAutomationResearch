package com.cstestforge.project.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Settings configuration for a project.
 */
public class ProjectSettings {
    private String projectId;
    private int defaultTimeout;
    private String defaultBrowser;
    private String defaultEnvironment;
    private boolean screenshotsEnabled;
    private boolean videoRecordingEnabled;
    private boolean parallelExecutionEnabled;
    private int maxParallelInstances;
    private boolean retryFailedTests;
    private int maxRetries;
    private Map<String, Object> customSettings;

    public ProjectSettings() {
        // Set default values
        this.defaultTimeout = 30000;
        this.defaultBrowser = "chrome";
        this.screenshotsEnabled = true;
        this.videoRecordingEnabled = false;
        this.parallelExecutionEnabled = false;
        this.maxParallelInstances = 1;
        this.retryFailedTests = false;
        this.maxRetries = 0;
        this.customSettings = new HashMap<>();
    }

    public ProjectSettings(String projectId) {
        this();
        this.projectId = projectId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public int getDefaultTimeout() {
        return defaultTimeout;
    }

    public void setDefaultTimeout(int defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }

    public String getDefaultBrowser() {
        return defaultBrowser;
    }

    public void setDefaultBrowser(String defaultBrowser) {
        this.defaultBrowser = defaultBrowser;
    }

    public String getDefaultEnvironment() {
        return defaultEnvironment;
    }

    public void setDefaultEnvironment(String defaultEnvironment) {
        this.defaultEnvironment = defaultEnvironment;
    }

    public boolean isScreenshotsEnabled() {
        return screenshotsEnabled;
    }

    public void setScreenshotsEnabled(boolean screenshotsEnabled) {
        this.screenshotsEnabled = screenshotsEnabled;
    }

    public boolean isVideoRecordingEnabled() {
        return videoRecordingEnabled;
    }

    public void setVideoRecordingEnabled(boolean videoRecordingEnabled) {
        this.videoRecordingEnabled = videoRecordingEnabled;
    }

    public boolean isParallelExecutionEnabled() {
        return parallelExecutionEnabled;
    }

    public void setParallelExecutionEnabled(boolean parallelExecutionEnabled) {
        this.parallelExecutionEnabled = parallelExecutionEnabled;
    }

    public int getMaxParallelInstances() {
        return maxParallelInstances;
    }

    public void setMaxParallelInstances(int maxParallelInstances) {
        this.maxParallelInstances = maxParallelInstances;
    }

    public boolean isRetryFailedTests() {
        return retryFailedTests;
    }

    public void setRetryFailedTests(boolean retryFailedTests) {
        this.retryFailedTests = retryFailedTests;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Map<String, Object> getCustomSettings() {
        return customSettings;
    }

    public void setCustomSettings(Map<String, Object> customSettings) {
        this.customSettings = customSettings;
    }

    /**
     * Add or update a custom setting
     * 
     * @param key Setting key
     * @param value Setting value
     */
    public void setCustomSetting(String key, Object value) {
        this.customSettings.put(key, value);
    }

    /**
     * Get a custom setting value
     * 
     * @param key Setting key
     * @return The setting value or null if not found
     */
    public Object getCustomSetting(String key) {
        return this.customSettings.get(key);
    }

    /**
     * Remove a custom setting
     * 
     * @param key Setting key
     * @return The previous value or null if not found
     */
    public Object removeCustomSetting(String key) {
        return this.customSettings.remove(key);
    }
} 