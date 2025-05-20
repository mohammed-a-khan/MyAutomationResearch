package com.cstestforge.project.service;

import com.cstestforge.project.model.ProjectSettings;

import java.util.Optional;

/**
 * Service for managing project settings.
 */
public interface ProjectSettingsService {

    /**
     * Get settings for a project
     * 
     * @param projectId Project ID
     * @return Project settings or default settings if not found
     */
    ProjectSettings getSettings(String projectId);

    /**
     * Update settings for a project
     * 
     * @param projectId Project ID
     * @param settings Updated settings
     * @return Updated settings
     */
    ProjectSettings updateSettings(String projectId, ProjectSettings settings);

    /**
     * Set a custom setting for a project
     * 
     * @param projectId Project ID
     * @param key Setting key
     * @param value Setting value
     * @return Updated settings
     */
    ProjectSettings setCustomSetting(String projectId, String key, Object value);

    /**
     * Remove a custom setting from a project
     * 
     * @param projectId Project ID
     * @param key Setting key
     * @return Updated settings
     */
    ProjectSettings removeCustomSetting(String projectId, String key);

    /**
     * Get the value of a custom setting
     * 
     * @param projectId Project ID
     * @param key Setting key
     * @return Optional containing the setting value if found
     */
    Optional<Object> getCustomSetting(String projectId, String key);
} 