package com.cstestforge.settings.model;

import java.util.HashMap;
import java.util.Map;

/**
 * System-wide application settings
 */
public class SystemSettings {

    // General settings
    private String defaultBrowser;
    private int defaultTimeout;
    private int maxRetries;
    private boolean screenshotOnError;
    private boolean logsEnabled;
    private String defaultEnvironment;
    
    // Parallel execution settings
    private boolean parallelExecutionEnabled;
    private int maxParallelExecutions;
    
    // Notification settings
    private boolean emailNotificationsEnabled;
    private String emailRecipients;
    private boolean slackIntegrationEnabled;
    private String slackWebhook;
    private boolean notifyOnSuccess;
    private boolean notifyOnFailure;
    
    // Custom settings
    private Map<String, Object> customSettings;

    /**
     * Default constructor
     */
    public SystemSettings() {
        // Set default values
        this.defaultBrowser = "chrome";
        this.defaultTimeout = 30;
        this.maxRetries = 1;
        this.screenshotOnError = true;
        this.logsEnabled = true;
        this.defaultEnvironment = "DEV";
        this.parallelExecutionEnabled = false;
        this.maxParallelExecutions = 4;
        this.emailNotificationsEnabled = false;
        this.emailRecipients = "";
        this.slackIntegrationEnabled = false;
        this.slackWebhook = "";
        this.notifyOnSuccess = false;
        this.notifyOnFailure = true;
        this.customSettings = new HashMap<>();
    }

    /**
     * Get the default browser
     * 
     * @return Default browser name
     */
    public String getDefaultBrowser() {
        return defaultBrowser;
    }

    /**
     * Set the default browser
     * 
     * @param defaultBrowser Default browser name
     */
    public void setDefaultBrowser(String defaultBrowser) {
        this.defaultBrowser = defaultBrowser;
    }

    /**
     * Get the default timeout in seconds
     * 
     * @return Default timeout in seconds
     */
    public int getDefaultTimeout() {
        return defaultTimeout;
    }

    /**
     * Set the default timeout in seconds
     * 
     * @param defaultTimeout Default timeout in seconds
     */
    public void setDefaultTimeout(int defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }

    /**
     * Get the maximum number of retries
     * 
     * @return Maximum retries
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Set the maximum number of retries
     * 
     * @param maxRetries Maximum retries
     */
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /**
     * Check if screenshots on error are enabled
     * 
     * @return true if screenshots on error are enabled
     */
    public boolean isScreenshotOnError() {
        return screenshotOnError;
    }

    /**
     * Set whether to take screenshots on error
     * 
     * @param screenshotOnError true to take screenshots on error
     */
    public void setScreenshotOnError(boolean screenshotOnError) {
        this.screenshotOnError = screenshotOnError;
    }

    /**
     * Check if logs are enabled
     * 
     * @return true if logs are enabled
     */
    public boolean isLogsEnabled() {
        return logsEnabled;
    }

    /**
     * Set whether to enable logs
     * 
     * @param logsEnabled true to enable logs
     */
    public void setLogsEnabled(boolean logsEnabled) {
        this.logsEnabled = logsEnabled;
    }

    /**
     * Get the default environment
     * 
     * @return Default environment name
     */
    public String getDefaultEnvironment() {
        return defaultEnvironment;
    }

    /**
     * Set the default environment
     * 
     * @param defaultEnvironment Default environment name
     */
    public void setDefaultEnvironment(String defaultEnvironment) {
        this.defaultEnvironment = defaultEnvironment;
    }

    /**
     * Check if parallel execution is enabled
     * 
     * @return true if parallel execution is enabled
     */
    public boolean isParallelExecutionEnabled() {
        return parallelExecutionEnabled;
    }

    /**
     * Set whether to enable parallel execution
     * 
     * @param parallelExecutionEnabled true to enable parallel execution
     */
    public void setParallelExecutionEnabled(boolean parallelExecutionEnabled) {
        this.parallelExecutionEnabled = parallelExecutionEnabled;
    }

    /**
     * Get the maximum number of parallel executions
     * 
     * @return Maximum parallel executions
     */
    public int getMaxParallelExecutions() {
        return maxParallelExecutions;
    }

    /**
     * Set the maximum number of parallel executions
     * 
     * @param maxParallelExecutions Maximum parallel executions
     */
    public void setMaxParallelExecutions(int maxParallelExecutions) {
        this.maxParallelExecutions = maxParallelExecutions;
    }

    /**
     * Check if email notifications are enabled
     * 
     * @return true if email notifications are enabled
     */
    public boolean isEmailNotificationsEnabled() {
        return emailNotificationsEnabled;
    }

    /**
     * Set whether to enable email notifications
     * 
     * @param emailNotificationsEnabled true to enable email notifications
     */
    public void setEmailNotificationsEnabled(boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }

    /**
     * Get the email recipients
     * 
     * @return Email recipients
     */
    public String getEmailRecipients() {
        return emailRecipients;
    }

    /**
     * Set the email recipients
     * 
     * @param emailRecipients Email recipients
     */
    public void setEmailRecipients(String emailRecipients) {
        this.emailRecipients = emailRecipients;
    }

    /**
     * Check if Slack integration is enabled
     * 
     * @return true if Slack integration is enabled
     */
    public boolean isSlackIntegrationEnabled() {
        return slackIntegrationEnabled;
    }

    /**
     * Set whether to enable Slack integration
     * 
     * @param slackIntegrationEnabled true to enable Slack integration
     */
    public void setSlackIntegrationEnabled(boolean slackIntegrationEnabled) {
        this.slackIntegrationEnabled = slackIntegrationEnabled;
    }

    /**
     * Get the Slack webhook URL
     * 
     * @return Slack webhook URL
     */
    public String getSlackWebhook() {
        return slackWebhook;
    }

    /**
     * Set the Slack webhook URL
     * 
     * @param slackWebhook Slack webhook URL
     */
    public void setSlackWebhook(String slackWebhook) {
        this.slackWebhook = slackWebhook;
    }

    /**
     * Check if notifications on success are enabled
     * 
     * @return true if notifications on success are enabled
     */
    public boolean isNotifyOnSuccess() {
        return notifyOnSuccess;
    }

    /**
     * Set whether to notify on success
     * 
     * @param notifyOnSuccess true to notify on success
     */
    public void setNotifyOnSuccess(boolean notifyOnSuccess) {
        this.notifyOnSuccess = notifyOnSuccess;
    }

    /**
     * Check if notifications on failure are enabled
     * 
     * @return true if notifications on failure are enabled
     */
    public boolean isNotifyOnFailure() {
        return notifyOnFailure;
    }

    /**
     * Set whether to notify on failure
     * 
     * @param notifyOnFailure true to notify on failure
     */
    public void setNotifyOnFailure(boolean notifyOnFailure) {
        this.notifyOnFailure = notifyOnFailure;
    }

    /**
     * Get the custom settings
     * 
     * @return Custom settings map
     */
    public Map<String, Object> getCustomSettings() {
        return customSettings;
    }

    /**
     * Set the custom settings
     * 
     * @param customSettings Custom settings map
     */
    public void setCustomSettings(Map<String, Object> customSettings) {
        this.customSettings = customSettings != null ? customSettings : new HashMap<>();
    }

    /**
     * Get a custom setting
     * 
     * @param key Setting key
     * @return Setting value or null if not found
     */
    public Object getCustomSetting(String key) {
        return customSettings.get(key);
    }

    /**
     * Set a custom setting
     * 
     * @param key Setting key
     * @param value Setting value
     */
    public void setCustomSetting(String key, Object value) {
        customSettings.put(key, value);
    }

    /**
     * Remove a custom setting
     * 
     * @param key Setting key
     * @return The removed value
     */
    public Object removeCustomSetting(String key) {
        return customSettings.remove(key);
    }
} 