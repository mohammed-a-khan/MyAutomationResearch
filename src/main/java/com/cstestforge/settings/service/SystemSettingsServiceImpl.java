package com.cstestforge.settings.service;

import com.cstestforge.project.storage.FileStorageService;
import com.cstestforge.settings.model.SystemSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of SystemSettingsService
 */
@Service
public class SystemSettingsServiceImpl implements SystemSettingsService {

    private static final Logger logger = LoggerFactory.getLogger(SystemSettingsServiceImpl.class);
    
    private static final String SETTINGS_FILE = "config/system_settings.json";
    
    private final FileStorageService fileStorageService;
    private SystemSettings systemSettings;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Autowired
    public SystemSettingsServiceImpl(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * Initialize settings on startup
     */
    @PostConstruct
    public void init() {
        lock.writeLock().lock();
        try {
            // Load settings from file or create default if file doesn't exist
            if (fileStorageService.fileExists(SETTINGS_FILE)) {
                systemSettings = fileStorageService.readFromJson(SETTINGS_FILE, SystemSettings.class);
                if (systemSettings == null) {
                    systemSettings = new SystemSettings();
                    saveSettings();
                }
            } else {
                systemSettings = new SystemSettings();
                saveSettings();
            }
            logger.info("System settings loaded successfully");
        } catch (Exception e) {
            logger.error("Error loading system settings", e);
            systemSettings = new SystemSettings();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public SystemSettings getGeneralSettings() {
        lock.readLock().lock();
        try {
            return systemSettings;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public SystemSettings updateGeneralSettings(SystemSettings settings) {
        lock.writeLock().lock();
        try {
            // Update only non-null fields to preserve defaults
            if (settings.getDefaultBrowser() != null) {
                systemSettings.setDefaultBrowser(settings.getDefaultBrowser());
            }
            if (settings.getDefaultEnvironment() != null) {
                systemSettings.setDefaultEnvironment(settings.getDefaultEnvironment());
            }
            
            // Update primitive fields
            systemSettings.setDefaultTimeout(settings.getDefaultTimeout());
            systemSettings.setMaxRetries(settings.getMaxRetries());
            systemSettings.setScreenshotOnError(settings.isScreenshotOnError());
            systemSettings.setLogsEnabled(settings.isLogsEnabled());
            
            // Update parallel execution settings
            systemSettings.setParallelExecutionEnabled(settings.isParallelExecutionEnabled());
            systemSettings.setMaxParallelExecutions(settings.getMaxParallelExecutions());
            
            // Update notification settings
            systemSettings.setEmailNotificationsEnabled(settings.isEmailNotificationsEnabled());
            if (settings.getEmailRecipients() != null) {
                systemSettings.setEmailRecipients(settings.getEmailRecipients());
            }
            systemSettings.setSlackIntegrationEnabled(settings.isSlackIntegrationEnabled());
            if (settings.getSlackWebhook() != null) {
                systemSettings.setSlackWebhook(settings.getSlackWebhook());
            }
            systemSettings.setNotifyOnSuccess(settings.isNotifyOnSuccess());
            systemSettings.setNotifyOnFailure(settings.isNotifyOnFailure());
            
            // Save updated settings
            saveSettings();
            
            return systemSettings;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<Object> getSetting(String key) {
        lock.readLock().lock();
        try {
            // First check system settings properties
            switch (key) {
                case "defaultBrowser":
                    return Optional.ofNullable(systemSettings.getDefaultBrowser());
                case "defaultTimeout":
                    return Optional.of(systemSettings.getDefaultTimeout());
                case "maxRetries":
                    return Optional.of(systemSettings.getMaxRetries());
                case "screenshotOnError":
                    return Optional.of(systemSettings.isScreenshotOnError());
                case "logsEnabled":
                    return Optional.of(systemSettings.isLogsEnabled());
                case "defaultEnvironment":
                    return Optional.ofNullable(systemSettings.getDefaultEnvironment());
                case "parallelExecutionEnabled":
                    return Optional.of(systemSettings.isParallelExecutionEnabled());
                case "maxParallelExecutions":
                    return Optional.of(systemSettings.getMaxParallelExecutions());
                case "emailNotificationsEnabled":
                    return Optional.of(systemSettings.isEmailNotificationsEnabled());
                case "emailRecipients":
                    return Optional.ofNullable(systemSettings.getEmailRecipients());
                case "slackIntegrationEnabled":
                    return Optional.of(systemSettings.isSlackIntegrationEnabled());
                case "slackWebhook":
                    return Optional.ofNullable(systemSettings.getSlackWebhook());
                case "notifyOnSuccess":
                    return Optional.of(systemSettings.isNotifyOnSuccess());
                case "notifyOnFailure":
                    return Optional.of(systemSettings.isNotifyOnFailure());
                default:
                    // Then check custom settings
                    return Optional.ofNullable(systemSettings.getCustomSetting(key));
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public SystemSettings setSetting(String key, Object value) {
        lock.writeLock().lock();
        try {
            // First try to set system settings property
            boolean systemPropertySet = false;
            switch (key) {
                case "defaultBrowser":
                    if (value instanceof String) {
                        systemSettings.setDefaultBrowser((String) value);
                        systemPropertySet = true;
                    }
                    break;
                case "defaultTimeout":
                    if (value instanceof Number) {
                        systemSettings.setDefaultTimeout(((Number) value).intValue());
                        systemPropertySet = true;
                    } else if (value instanceof String) {
                        try {
                            systemSettings.setDefaultTimeout(Integer.parseInt((String) value));
                            systemPropertySet = true;
                        } catch (NumberFormatException e) {
                            // Invalid number format, will be handled by falling through
                        }
                    }
                    break;
                case "maxRetries":
                    if (value instanceof Number) {
                        systemSettings.setMaxRetries(((Number) value).intValue());
                        systemPropertySet = true;
                    } else if (value instanceof String) {
                        try {
                            systemSettings.setMaxRetries(Integer.parseInt((String) value));
                            systemPropertySet = true;
                        } catch (NumberFormatException e) {
                            // Invalid number format, will be handled by falling through
                        }
                    }
                    break;
                case "screenshotOnError":
                    if (value instanceof Boolean) {
                        systemSettings.setScreenshotOnError((Boolean) value);
                        systemPropertySet = true;
                    } else if (value instanceof String) {
                        systemSettings.setScreenshotOnError(Boolean.parseBoolean((String) value));
                        systemPropertySet = true;
                    }
                    break;
                case "logsEnabled":
                    if (value instanceof Boolean) {
                        systemSettings.setLogsEnabled((Boolean) value);
                        systemPropertySet = true;
                    } else if (value instanceof String) {
                        systemSettings.setLogsEnabled(Boolean.parseBoolean((String) value));
                        systemPropertySet = true;
                    }
                    break;
                case "defaultEnvironment":
                    if (value instanceof String) {
                        systemSettings.setDefaultEnvironment((String) value);
                        systemPropertySet = true;
                    }
                    break;
                case "parallelExecutionEnabled":
                    if (value instanceof Boolean) {
                        systemSettings.setParallelExecutionEnabled((Boolean) value);
                        systemPropertySet = true;
                    } else if (value instanceof String) {
                        systemSettings.setParallelExecutionEnabled(Boolean.parseBoolean((String) value));
                        systemPropertySet = true;
                    }
                    break;
                case "maxParallelExecutions":
                    if (value instanceof Number) {
                        systemSettings.setMaxParallelExecutions(((Number) value).intValue());
                        systemPropertySet = true;
                    } else if (value instanceof String) {
                        try {
                            systemSettings.setMaxParallelExecutions(Integer.parseInt((String) value));
                            systemPropertySet = true;
                        } catch (NumberFormatException e) {
                            // Invalid number format, will be handled by falling through
                        }
                    }
                    break;
                case "emailNotificationsEnabled":
                    if (value instanceof Boolean) {
                        systemSettings.setEmailNotificationsEnabled((Boolean) value);
                        systemPropertySet = true;
                    } else if (value instanceof String) {
                        systemSettings.setEmailNotificationsEnabled(Boolean.parseBoolean((String) value));
                        systemPropertySet = true;
                    }
                    break;
                case "emailRecipients":
                    if (value instanceof String) {
                        systemSettings.setEmailRecipients((String) value);
                        systemPropertySet = true;
                    }
                    break;
                case "slackIntegrationEnabled":
                    if (value instanceof Boolean) {
                        systemSettings.setSlackIntegrationEnabled((Boolean) value);
                        systemPropertySet = true;
                    } else if (value instanceof String) {
                        systemSettings.setSlackIntegrationEnabled(Boolean.parseBoolean((String) value));
                        systemPropertySet = true;
                    }
                    break;
                case "slackWebhook":
                    if (value instanceof String) {
                        systemSettings.setSlackWebhook((String) value);
                        systemPropertySet = true;
                    }
                    break;
                case "notifyOnSuccess":
                    if (value instanceof Boolean) {
                        systemSettings.setNotifyOnSuccess((Boolean) value);
                        systemPropertySet = true;
                    } else if (value instanceof String) {
                        systemSettings.setNotifyOnSuccess(Boolean.parseBoolean((String) value));
                        systemPropertySet = true;
                    }
                    break;
                case "notifyOnFailure":
                    if (value instanceof Boolean) {
                        systemSettings.setNotifyOnFailure((Boolean) value);
                        systemPropertySet = true;
                    } else if (value instanceof String) {
                        systemSettings.setNotifyOnFailure(Boolean.parseBoolean((String) value));
                        systemPropertySet = true;
                    }
                    break;
            }
            
            // If not a system property or failed to set, add to custom settings
            if (!systemPropertySet) {
                systemSettings.setCustomSetting(key, value);
            }
            
            // Save updated settings
            saveSettings();
            
            return systemSettings;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public SystemSettings deleteSetting(String key) {
        lock.writeLock().lock();
        try {
            // First check if it's a system property
            switch (key) {
                case "defaultBrowser":
                    systemSettings.setDefaultBrowser("chrome");
                    break;
                case "defaultTimeout":
                    systemSettings.setDefaultTimeout(30);
                    break;
                case "maxRetries":
                    systemSettings.setMaxRetries(1);
                    break;
                case "screenshotOnError":
                    systemSettings.setScreenshotOnError(true);
                    break;
                case "logsEnabled":
                    systemSettings.setLogsEnabled(true);
                    break;
                case "defaultEnvironment":
                    systemSettings.setDefaultEnvironment("DEV");
                    break;
                case "parallelExecutionEnabled":
                    systemSettings.setParallelExecutionEnabled(false);
                    break;
                case "maxParallelExecutions":
                    systemSettings.setMaxParallelExecutions(4);
                    break;
                case "emailNotificationsEnabled":
                    systemSettings.setEmailNotificationsEnabled(false);
                    break;
                case "emailRecipients":
                    systemSettings.setEmailRecipients("");
                    break;
                case "slackIntegrationEnabled":
                    systemSettings.setSlackIntegrationEnabled(false);
                    break;
                case "slackWebhook":
                    systemSettings.setSlackWebhook("");
                    break;
                case "notifyOnSuccess":
                    systemSettings.setNotifyOnSuccess(false);
                    break;
                case "notifyOnFailure":
                    systemSettings.setNotifyOnFailure(true);
                    break;
                default:
                    // Otherwise, remove from custom settings
                    systemSettings.removeCustomSetting(key);
                    break;
            }
            
            // Save updated settings
            saveSettings();
            
            return systemSettings;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public SystemSettings resetSettings() {
        lock.writeLock().lock();
        try {
            // Create new settings with default values
            systemSettings = new SystemSettings();
            
            // Save updated settings
            saveSettings();
            
            return systemSettings;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Map<String, Object> getAllSettings() {
        lock.readLock().lock();
        try {
            Map<String, Object> allSettings = new HashMap<>();
            
            // Add system properties
            allSettings.put("defaultBrowser", systemSettings.getDefaultBrowser());
            allSettings.put("defaultTimeout", systemSettings.getDefaultTimeout());
            allSettings.put("maxRetries", systemSettings.getMaxRetries());
            allSettings.put("screenshotOnError", systemSettings.isScreenshotOnError());
            allSettings.put("logsEnabled", systemSettings.isLogsEnabled());
            allSettings.put("defaultEnvironment", systemSettings.getDefaultEnvironment());
            
            allSettings.put("parallelExecutionEnabled", systemSettings.isParallelExecutionEnabled());
            allSettings.put("maxParallelExecutions", systemSettings.getMaxParallelExecutions());
            
            allSettings.put("emailNotificationsEnabled", systemSettings.isEmailNotificationsEnabled());
            allSettings.put("emailRecipients", systemSettings.getEmailRecipients());
            allSettings.put("slackIntegrationEnabled", systemSettings.isSlackIntegrationEnabled());
            allSettings.put("slackWebhook", systemSettings.getSlackWebhook());
            allSettings.put("notifyOnSuccess", systemSettings.isNotifyOnSuccess());
            allSettings.put("notifyOnFailure", systemSettings.isNotifyOnFailure());
            
            // Add custom settings
            allSettings.putAll(systemSettings.getCustomSettings());
            
            return allSettings;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Save settings to file
     */
    private void saveSettings() {
        try {
            // Ensure directory exists
            fileStorageService.createDirectoryIfNotExists("config");
            
            // Save settings to file
            fileStorageService.saveToJson(SETTINGS_FILE, systemSettings);
            logger.debug("System settings saved successfully");
        } catch (Exception e) {
            logger.error("Error saving system settings", e);
        }
    }
} 