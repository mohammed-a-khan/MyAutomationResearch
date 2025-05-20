package com.cstestforge.settings.controller;

import com.cstestforge.project.model.ApiResponse;
import com.cstestforge.settings.model.SystemSettings;
import com.cstestforge.settings.service.SystemSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for system settings
 */
@RestController
@RequestMapping("/api/settings")
public class SystemSettingsController {

    private static final Logger logger = LoggerFactory.getLogger(SystemSettingsController.class);
    
    private final SystemSettingsService settingsService;
    
    @Autowired
    public SystemSettingsController(SystemSettingsService settingsService) {
        this.settingsService = settingsService;
    }
    
    /**
     * Get general settings
     * 
     * @return General settings
     */
    @GetMapping("/general")
    public ResponseEntity<ApiResponse<SystemSettings>> getGeneralSettings() {
        try {
            SystemSettings settings = settingsService.getGeneralSettings();
            return ResponseEntity.ok(ApiResponse.success(settings));
        } catch (Exception e) {
            logger.error("Error retrieving general settings", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving settings", e.getMessage()));
        }
    }
    
    /**
     * Update general settings
     * 
     * @param settings Updated settings
     * @return Updated settings
     */
    @PutMapping("/general")
    public ResponseEntity<ApiResponse<SystemSettings>> updateGeneralSettings(@RequestBody SystemSettings settings) {
        try {
            SystemSettings updatedSettings = settingsService.updateGeneralSettings(settings);
            return ResponseEntity.ok(ApiResponse.success(updatedSettings, "Settings updated successfully"));
        } catch (Exception e) {
            logger.error("Error updating general settings", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error updating settings", e.getMessage()));
        }
    }
    
    /**
     * Get all settings
     * 
     * @return All settings as a map
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllSettings() {
        try {
            Map<String, Object> settings = settingsService.getAllSettings();
            return ResponseEntity.ok(ApiResponse.success(settings));
        } catch (Exception e) {
            logger.error("Error retrieving all settings", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving settings", e.getMessage()));
        }
    }
    
    /**
     * Get a specific setting
     * 
     * @param key Setting key
     * @return Setting value
     */
    @GetMapping("/{key}")
    public ResponseEntity<ApiResponse<Object>> getSetting(@PathVariable String key) {
        try {
            return settingsService.getSetting(key)
                    .map(value -> ResponseEntity.ok(ApiResponse.success(value)))
                    .orElse(ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error("Setting not found", "No setting found with key: " + key)));
        } catch (Exception e) {
            logger.error("Error retrieving setting", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving setting", e.getMessage()));
        }
    }
    
    /**
     * Set a specific setting
     * 
     * @param key Setting key
     * @param valueMap Map containing the setting value
     * @return Updated settings
     */
    @PutMapping("/{key}")
    public ResponseEntity<ApiResponse<SystemSettings>> setSetting(
            @PathVariable String key, @RequestBody Map<String, Object> valueMap) {
        try {
            // Extract value from request body
            if (!valueMap.containsKey("value")) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Invalid request", "Request body must contain a 'value' field"));
            }
            
            Object value = valueMap.get("value");
            SystemSettings updatedSettings = settingsService.setSetting(key, value);
            return ResponseEntity.ok(ApiResponse.success(updatedSettings, "Setting updated successfully"));
        } catch (Exception e) {
            logger.error("Error updating setting", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error updating setting", e.getMessage()));
        }
    }
    
    /**
     * Delete a specific setting
     * 
     * @param key Setting key
     * @return Updated settings
     */
    @DeleteMapping("/{key}")
    public ResponseEntity<ApiResponse<SystemSettings>> deleteSetting(@PathVariable String key) {
        try {
            SystemSettings updatedSettings = settingsService.deleteSetting(key);
            return ResponseEntity.ok(ApiResponse.success(updatedSettings, "Setting deleted successfully"));
        } catch (Exception e) {
            logger.error("Error deleting setting", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error deleting setting", e.getMessage()));
        }
    }
    
    /**
     * Reset settings to defaults
     * 
     * @return Default settings
     */
    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<SystemSettings>> resetSettings() {
        try {
            SystemSettings defaultSettings = settingsService.resetSettings();
            return ResponseEntity.ok(ApiResponse.success(defaultSettings, "Settings reset to defaults successfully"));
        } catch (Exception e) {
            logger.error("Error resetting settings", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error resetting settings", e.getMessage()));
        }
    }
} 