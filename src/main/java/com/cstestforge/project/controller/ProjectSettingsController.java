package com.cstestforge.project.controller;

import com.cstestforge.project.model.ApiResponse;
import com.cstestforge.project.model.ProjectSettings;
import com.cstestforge.project.service.ProjectSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for project settings operations.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/settings")
public class ProjectSettingsController {

    private final ProjectSettingsService projectSettingsService;

    @Autowired
    public ProjectSettingsController(ProjectSettingsService projectSettingsService) {
        this.projectSettingsService = projectSettingsService;
    }

    /**
     * Get settings for a project
     * 
     * @param projectId Project ID
     * @return Project settings
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ProjectSettings>> getSettings(@PathVariable String projectId) {
        try {
            ProjectSettings settings = projectSettingsService.getSettings(projectId);
            return ResponseEntity.ok(ApiResponse.success(settings));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Project not found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving settings", e.getMessage()));
        }
    }

    /**
     * Update settings for a project
     * 
     * @param projectId Project ID
     * @param settings Updated settings
     * @return Updated settings
     */
    @PutMapping
    public ResponseEntity<ApiResponse<ProjectSettings>> updateSettings(
            @PathVariable String projectId, @RequestBody ProjectSettings settings) {
        try {
            ProjectSettings updatedSettings = projectSettingsService.updateSettings(projectId, settings);
            return ResponseEntity.ok(ApiResponse.success(updatedSettings, "Settings updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Project not found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error updating settings", e.getMessage()));
        }
    }

    /**
     * Set a custom setting for a project
     * 
     * @param projectId Project ID
     * @param key Setting key
     * @param valueMap Map containing the setting value
     * @return Updated settings
     */
    @PutMapping("/custom/{key}")
    public ResponseEntity<ApiResponse<ProjectSettings>> setCustomSetting(
            @PathVariable String projectId, @PathVariable String key, @RequestBody Map<String, Object> valueMap) {
        try {
            // Extract value from request body
            if (!valueMap.containsKey("value")) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Invalid request", "Request body must contain a 'value' field"));
            }
            
            Object value = valueMap.get("value");
            ProjectSettings updatedSettings = projectSettingsService.setCustomSetting(projectId, key, value);
            return ResponseEntity.ok(ApiResponse.success(updatedSettings, "Custom setting updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Project not found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error updating custom setting", e.getMessage()));
        }
    }

    /**
     * Remove a custom setting from a project
     * 
     * @param projectId Project ID
     * @param key Setting key
     * @return Updated settings
     */
    @DeleteMapping("/custom/{key}")
    public ResponseEntity<ApiResponse<ProjectSettings>> removeCustomSetting(
            @PathVariable String projectId, @PathVariable String key) {
        try {
            ProjectSettings updatedSettings = projectSettingsService.removeCustomSetting(projectId, key);
            return ResponseEntity.ok(ApiResponse.success(updatedSettings, "Custom setting removed successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Project not found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error removing custom setting", e.getMessage()));
        }
    }

    /**
     * Get a custom setting value
     * 
     * @param projectId Project ID
     * @param key Setting key
     * @return Setting value
     */
    @GetMapping("/custom/{key}")
    public ResponseEntity<ApiResponse<Object>> getCustomSetting(
            @PathVariable String projectId, @PathVariable String key) {
        try {
            return projectSettingsService.getCustomSetting(projectId, key)
                    .map(value -> ResponseEntity.ok(ApiResponse.success(value)))
                    .orElse(ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error("Setting not found", 
                                    "No setting found with key: " + key)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Project not found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving custom setting", e.getMessage()));
        }
    }
} 