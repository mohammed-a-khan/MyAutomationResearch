package com.cstestforge.ado.repository;

import com.cstestforge.ado.model.PipelineConfig;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing ADO pipeline configurations.
 * Pipeline configurations are stored at "ado/pipeline/config/{projectId}.json"
 */
public interface PipelineConfigRepository {

    /**
     * Find all pipeline configurations
     *
     * @return List of all pipeline configurations
     */
    List<PipelineConfig> findAll();
    
    /**
     * Find pipeline configuration for a project
     *
     * @param projectId Project ID
     * @return Optional containing the pipeline configuration if found
     */
    Optional<PipelineConfig> findByProjectId(String projectId);
    
    /**
     * Save or update pipeline configuration for a project
     *
     * @param projectId Project ID
     * @param config Pipeline configuration to save
     * @return Saved pipeline configuration
     */
    PipelineConfig save(String projectId, PipelineConfig config);
    
    /**
     * Delete pipeline configuration for a project
     *
     * @param projectId Project ID
     * @return true if deleted successfully
     */
    boolean delete(String projectId);
    
    /**
     * Check if a pipeline configuration exists for a project
     *
     * @param projectId Project ID
     * @return true if the pipeline configuration exists
     */
    boolean exists(String projectId);
} 