package com.cstestforge.ado.service;

import com.cstestforge.ado.model.*;

import java.util.List;

/**
 * Service for Azure DevOps integration
 */
public interface AdoService {

    /**
     * Get all ADO connections
     * 
     * @return List of ADO connections
     */
    List<AdoConnection> getConnections();
    
    /**
     * Get ADO connection by ID
     * 
     * @param id Connection ID
     * @return The connection or null if not found
     */
    AdoConnection getConnectionById(String id);
    
    /**
     * Create a new ADO connection
     * 
     * @param connection Connection to create
     * @return Created connection with ID
     */
    AdoConnection createConnection(AdoConnection connection);
    
    /**
     * Update an existing ADO connection
     * 
     * @param id Connection ID
     * @param connection Updated connection data
     * @return Updated connection
     */
    AdoConnection updateConnection(String id, AdoConnection connection);
    
    /**
     * Delete an ADO connection
     * 
     * @param id Connection ID
     * @return True if deleted successfully
     */
    boolean deleteConnection(String id);
    
    /**
     * Validate ADO connection credentials
     * 
     * @param url Azure DevOps URL
     * @param pat Personal access token
     * @param organizationName Organization name
     * @param projectName Project name
     * @return True if connection is valid
     */
    boolean validateConnection(String url, String pat, String organizationName, String projectName);
    
    /**
     * Get ADO projects for a connection
     * 
     * @param connectionId Connection ID
     * @return List of ADO projects
     */
    List<AdoProject> getProjects(String connectionId);
    
    /**
     * Get ADO test plans for a project
     * 
     * @param connectionId Connection ID
     * @param projectId Project ID
     * @return List of test plans
     */
    List<AdoTestPlan> getTestPlans(String connectionId, String projectId);
    
    /**
     * Get ADO test suites for a test plan
     * 
     * @param connectionId Connection ID
     * @param projectId Project ID
     * @param testPlanId Test plan ID
     * @return List of test suites
     */
    List<AdoTestSuite> getTestSuites(String connectionId, String projectId, String testPlanId);
    
    /**
     * Get ADO pipelines for a project
     * 
     * @param connectionId Connection ID
     * @param projectId Project ID
     * @return List of pipelines
     */
    List<AdoPipeline> getPipelines(String connectionId, String projectId);
    
    /**
     * Get synchronization configuration for a project
     * 
     * @param projectId Project ID
     * @return Sync configuration or null if not configured
     */
    SyncConfig getSyncConfig(String projectId);
    
    /**
     * Save synchronization configuration for a project
     * 
     * @param projectId Project ID
     * @param config Sync configuration
     * @return Updated sync configuration
     */
    SyncConfig saveSyncConfig(String projectId, SyncConfig config);
    
    /**
     * Get synchronization status for a project
     * 
     * @param projectId Project ID
     * @return Sync status or null if not available
     */
    SyncStatus getSyncStatus(String projectId);
    
    /**
     * Start synchronization for a project
     * 
     * @param projectId Project ID
     * @return True if sync was started successfully
     */
    boolean startSync(String projectId);
    
    /**
     * Get pipeline configuration for a project
     * 
     * @param projectId Project ID
     * @return Pipeline configuration or null if not configured
     */
    PipelineConfig getPipelineConfig(String projectId);
    
    /**
     * Save pipeline configuration for a project
     * 
     * @param projectId Project ID
     * @param config Pipeline configuration
     * @return Updated pipeline configuration
     */
    PipelineConfig savePipelineConfig(String projectId, PipelineConfig config);
    
    /**
     * Trigger a pipeline run for a project
     * 
     * @param projectId Project ID
     * @return True if pipeline was triggered successfully
     */
    boolean triggerPipeline(String projectId);
} 