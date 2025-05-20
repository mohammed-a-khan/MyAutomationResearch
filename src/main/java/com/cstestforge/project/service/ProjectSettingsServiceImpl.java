package com.cstestforge.project.service;

import com.cstestforge.project.model.ProjectSettings;
import com.cstestforge.project.storage.FileLock;
import com.cstestforge.project.storage.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of the ProjectSettingsService using file-based storage.
 */
@Service
public class ProjectSettingsServiceImpl implements ProjectSettingsService {

    private final FileStorageService fileStorageService;

    @Autowired
    public ProjectSettingsServiceImpl(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public ProjectSettings getSettings(String projectId) {
        // Check if project exists
        String projectPath = String.format("projects/%s/project.json", projectId);
        if (!fileStorageService.fileExists(projectPath)) {
            throw new IllegalArgumentException("Project not found with ID: " + projectId);
        }

        // Check if settings exist
        String settingsPath = String.format("projects/%s/settings.json", projectId);
        if (!fileStorageService.fileExists(settingsPath)) {
            // Create default settings
            ProjectSettings defaultSettings = new ProjectSettings(projectId);
            fileStorageService.saveToJson(settingsPath, defaultSettings);
            return defaultSettings;
        }

        // Load settings
        ProjectSettings settings = fileStorageService.readFromJson(settingsPath, ProjectSettings.class);
        if (settings == null) {
            // Create default settings if file exists but is invalid
            settings = new ProjectSettings(projectId);
            fileStorageService.saveToJson(settingsPath, settings);
        }

        return settings;
    }

    @Override
    public ProjectSettings updateSettings(String projectId, ProjectSettings settings) {
        // Check if project exists
        String projectPath = String.format("projects/%s/project.json", projectId);
        if (!fileStorageService.fileExists(projectPath)) {
            throw new IllegalArgumentException("Project not found with ID: " + projectId);
        }

        // Ensure projectId is set
        settings.setProjectId(projectId);

        // Save settings
        String settingsPath = String.format("projects/%s/settings.json", projectId);
        fileStorageService.saveToJson(settingsPath, settings);

        return settings;
    }

    @Override
    public ProjectSettings setCustomSetting(String projectId, String key, Object value) {
        // Get current settings
        ProjectSettings settings = getSettings(projectId);

        // Set custom setting
        settings.setCustomSetting(key, value);

        // Save settings
        String settingsPath = String.format("projects/%s/settings.json", projectId);
        fileStorageService.saveToJson(settingsPath, settings);

        return settings;
    }

    @Override
    public ProjectSettings removeCustomSetting(String projectId, String key) {
        // Get current settings
        ProjectSettings settings = getSettings(projectId);

        // Remove custom setting
        settings.removeCustomSetting(key);

        // Save settings
        String settingsPath = String.format("projects/%s/settings.json", projectId);
        fileStorageService.saveToJson(settingsPath, settings);

        return settings;
    }

    @Override
    public Optional<Object> getCustomSetting(String projectId, String key) {
        // Get current settings
        ProjectSettings settings = getSettings(projectId);

        // Get custom setting
        Object value = settings.getCustomSetting(key);

        return Optional.ofNullable(value);
    }
} 