package com.cstestforge.ado.repository;

import com.cstestforge.ado.model.SyncConfig;
import com.cstestforge.ado.model.SyncStatus;
import com.cstestforge.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of SyncConfigRepository using file system storage.
 * Sync configurations are stored at "ado/sync/config/{projectId}.json"
 * Sync statuses are stored at "ado/sync/status/{projectId}.json"
 */
@Repository
public class SyncConfigRepositoryImpl implements SyncConfigRepository {

    private static final Logger logger = LoggerFactory.getLogger(SyncConfigRepositoryImpl.class);
    private static final String SYNC_CONFIG_DIR = "ado/sync/config";
    private static final String SYNC_STATUS_DIR = "ado/sync/status";
    private static final String SYNC_CONFIG_FILE = "ado/sync/config/%s.json";
    private static final String SYNC_STATUS_FILE = "ado/sync/status/%s.json";
    
    private final StorageManager storageManager;
    
    @Autowired
    public SyncConfigRepositoryImpl(StorageManager storageManager) {
        this.storageManager = storageManager;
        
        // Ensure directories exist
        storageManager.createDirectory(SYNC_CONFIG_DIR);
        storageManager.createDirectory(SYNC_STATUS_DIR);
    }

    @Override
    public List<SyncConfig> findAllConfigs() {
        File configDir = new File(storageManager.getAbsolutePath(SYNC_CONFIG_DIR));
        List<SyncConfig> configs = new ArrayList<>();
        
        if (!configDir.exists() || !configDir.isDirectory()) {
            return configs;
        }
        
        File[] configFiles = configDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (configFiles == null) {
            return configs;
        }
        
        for (File file : configFiles) {
            String fileName = file.getName();
            String projectId = fileName.substring(0, fileName.lastIndexOf('.'));
            
            findConfigByProjectId(projectId).ifPresent(configs::add);
        }
        
        return configs;
    }

    @Override
    public Optional<SyncConfig> findConfigByProjectId(String projectId) {
        if (projectId == null || projectId.isEmpty()) {
            return Optional.empty();
        }
        
        String configPath = String.format(SYNC_CONFIG_FILE, projectId);
        if (!storageManager.exists(configPath)) {
            return Optional.empty();
        }
        
        SyncConfig config = storageManager.read(configPath, SyncConfig.class);
        return Optional.ofNullable(config);
    }

    @Override
    public SyncConfig saveConfig(String projectId, SyncConfig config) {
        if (projectId == null || projectId.isEmpty()) {
            throw new IllegalArgumentException("Project ID cannot be null or empty");
        }
        
        if (config == null) {
            throw new IllegalArgumentException("Sync configuration cannot be null");
        }
        
        // Ensure project ID is set correctly
        config.setProjectId(projectId);
        
        // Set default values if not provided
        if (config.getLastSyncTimestamp() <= 0) {
            config.setLastSyncTimestamp(System.currentTimeMillis());
        }
        
        // Save configuration
        String configPath = String.format(SYNC_CONFIG_FILE, projectId);
        storageManager.write(configPath, config);
        
        return config;
    }

    @Override
    public boolean deleteConfig(String projectId) {
        if (projectId == null || projectId.isEmpty()) {
            return false;
        }
        
        String configPath = String.format(SYNC_CONFIG_FILE, projectId);
        return storageManager.delete(configPath);
    }

    @Override
    public Optional<SyncStatus> findStatusByProjectId(String projectId) {
        if (projectId == null || projectId.isEmpty()) {
            return Optional.empty();
        }
        
        String statusPath = String.format(SYNC_STATUS_FILE, projectId);
        if (!storageManager.exists(statusPath)) {
            return Optional.empty();
        }
        
        SyncStatus status = storageManager.read(statusPath, SyncStatus.class);
        return Optional.ofNullable(status);
    }

    @Override
    public SyncStatus saveStatus(String projectId, SyncStatus status) {
        if (projectId == null || projectId.isEmpty()) {
            throw new IllegalArgumentException("Project ID cannot be null or empty");
        }
        
        if (status == null) {
            throw new IllegalArgumentException("Sync status cannot be null");
        }
        
        // Ensure project ID is set correctly
        status.setProjectId(projectId);
        
        // Save status
        String statusPath = String.format(SYNC_STATUS_FILE, projectId);
        storageManager.write(statusPath, status);
        
        return status;
    }
}