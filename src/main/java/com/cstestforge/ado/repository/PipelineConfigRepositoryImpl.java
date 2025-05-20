package com.cstestforge.ado.repository;

import com.cstestforge.ado.model.PipelineConfig;
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
 * Implementation of PipelineConfigRepository using file system storage.
 * Pipeline configurations are stored at "ado/pipeline/config/{projectId}.json"
 */
@Repository
public class PipelineConfigRepositoryImpl implements PipelineConfigRepository {

    private static final Logger logger = LoggerFactory.getLogger(PipelineConfigRepositoryImpl.class);
    private static final String PIPELINE_CONFIG_DIR = "ado/pipeline/config";
    private static final String PIPELINE_CONFIG_FILE = "ado/pipeline/config/%s.json";
    
    private final StorageManager storageManager;
    
    @Autowired
    public PipelineConfigRepositoryImpl(StorageManager storageManager) {
        this.storageManager = storageManager;
        
        // Ensure directory exists
        storageManager.createDirectory(PIPELINE_CONFIG_DIR);
    }

    @Override
    public List<PipelineConfig> findAll() {
        File configDir = new File(storageManager.getAbsolutePath(PIPELINE_CONFIG_DIR));
        List<PipelineConfig> configs = new ArrayList<>();
        
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
            
            findByProjectId(projectId).ifPresent(configs::add);
        }
        
        return configs;
    }

    @Override
    public Optional<PipelineConfig> findByProjectId(String projectId) {
        if (projectId == null || projectId.isEmpty()) {
            return Optional.empty();
        }
        
        String configPath = String.format(PIPELINE_CONFIG_FILE, projectId);
        if (!storageManager.exists(configPath)) {
            return Optional.empty();
        }
        
        PipelineConfig config = storageManager.read(configPath, PipelineConfig.class);
        return Optional.ofNullable(config);
    }

    @Override
    public PipelineConfig save(String projectId, PipelineConfig config) {
        if (projectId == null || projectId.isEmpty()) {
            throw new IllegalArgumentException("Project ID cannot be null or empty");
        }
        
        if (config == null) {
            throw new IllegalArgumentException("Pipeline configuration cannot be null");
        }
        
        // Ensure project ID is set correctly
        config.setProjectId(projectId);
        
        // Set timestamps if not already set
        long now = System.currentTimeMillis();
        if (config.getCreatedAt() <= 0) {
            config.setCreatedAt(now);
        }
        config.setUpdatedAt(now);
        
        // Save configuration
        String configPath = String.format(PIPELINE_CONFIG_FILE, projectId);
        storageManager.write(configPath, config);
        
        return config;
    }

    @Override
    public boolean delete(String projectId) {
        if (projectId == null || projectId.isEmpty()) {
            return false;
        }
        
        String configPath = String.format(PIPELINE_CONFIG_FILE, projectId);
        return storageManager.delete(configPath);
    }

    @Override
    public boolean exists(String projectId) {
        if (projectId == null || projectId.isEmpty()) {
            return false;
        }
        
        String configPath = String.format(PIPELINE_CONFIG_FILE, projectId);
        return storageManager.exists(configPath);
    }
} 