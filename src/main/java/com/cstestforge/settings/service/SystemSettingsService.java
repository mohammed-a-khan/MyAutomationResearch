package com.cstestforge.settings.service;

import com.cstestforge.settings.model.SystemSettings;

import java.util.Map;
import java.util.Optional;

/**
 * Service for managing system-wide settings
 */
public interface SystemSettingsService {

    /**
     * Get the general settings
     * 
     * @return General settings
     */
    SystemSettings getGeneralSettings();
    
    /**
     * Update the general settings
     * 
     * @param settings Updated settings
     * @return Updated settings
     */
    SystemSettings updateGeneralSettings(SystemSettings settings);
    
    /**
     * Get a specific setting
     * 
     * @param key Setting key
     * @return Setting value if found
     */
    Optional<Object> getSetting(String key);
    
    /**
     * Set a specific setting
     * 
     * @param key Setting key
     * @param value Setting value
     * @return Updated settings
     */
    SystemSettings setSetting(String key, Object value);
    
    /**
     * Delete a specific setting
     * 
     * @param key Setting key
     * @return Updated settings
     */
    SystemSettings deleteSetting(String key);
    
    /**
     * Reset settings to default values
     * 
     * @return Default settings
     */
    SystemSettings resetSettings();
    
    /**
     * Get all settings as a map
     * 
     * @return Map of all settings
     */
    Map<String, Object> getAllSettings();
} 