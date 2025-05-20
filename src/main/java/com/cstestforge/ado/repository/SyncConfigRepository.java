package com.cstestforge.ado.repository;

import com.cstestforge.ado.model.SyncConfig;
import com.cstestforge.ado.model.SyncStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing test case synchronization configurations with Azure DevOps.
 * Sync configurations are stored at "ado/sync/config/{projectId}.json"
 * Sync statuses are stored at "ado/sync/status/{projectId}.json"
 */
public interface SyncConfigRepository {

    /**
     * Find all sync configurations
     *
     * @return List of all sync configurations
     */
    List<SyncConfig> findAllConfigs();
    
    /**
     * Find sync configuration for a project
     *
     * @param projectId Project ID
     * @return Optional containing the sync configuration if found
     */
    Optional<SyncConfig> findConfigByProjectId(String projectId);
    
    /**
     * Save or update sync configuration for a project
     *
     * @param projectId Project ID
     * @param config Sync configuration to save
     * @return Saved sync configuration
     */
    SyncConfig saveConfig(String projectId, SyncConfig config);
    
    /**
     * Delete sync configuration for a project
     *
     * @param projectId Project ID
     * @return true if deleted successfully
     */
    boolean deleteConfig(String projectId);
    
    /**
     * Get sync status for a project
     *
     * @param projectId Project ID
     * @return Optional containing the sync status if found
     */
    Optional<SyncStatus> findStatusByProjectId(String projectId);
    
    /**
     * Save or update sync status for a project
     *
     * @param projectId Project ID
     * @param status Sync status to save
     * @return Saved sync status
     */
    SyncStatus saveStatus(String projectId, SyncStatus status);
} 