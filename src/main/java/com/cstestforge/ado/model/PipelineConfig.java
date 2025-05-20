package com.cstestforge.ado.model;

/**
 * Configuration for Azure DevOps pipeline integration
 */
public class PipelineConfig {
    
    private String id;
    private String projectId;
    private String connectionId;
    private String pipelineId;
    private String triggerMode;
    private boolean includeTests;
    private boolean notifyOnCompletion;
    private String[] parameters;
    private boolean enabled;
    private long createdAt;
    private long updatedAt;
    
    /**
     * Default constructor
     */
    public PipelineConfig() {
        this.triggerMode = "manual";
        this.includeTests = true;
        this.notifyOnCompletion = true;
        this.enabled = true;
    }
    
    /**
     * Constructor with essential fields
     * 
     * @param projectId Project ID this configuration belongs to
     * @param connectionId ADO connection ID
     * @param pipelineId Pipeline ID
     */
    public PipelineConfig(String projectId, String connectionId, String pipelineId) {
        this.projectId = projectId;
        this.connectionId = connectionId;
        this.pipelineId = pipelineId;
        this.triggerMode = "manual";
        this.includeTests = true;
        this.notifyOnCompletion = true;
        this.enabled = true;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    /**
     * Get the unique ID of this configuration
     * 
     * @return Configuration ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Set the unique ID of this configuration
     * 
     * @param id Configuration ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get the project ID this configuration belongs to
     * 
     * @return Project ID
     */
    public String getProjectId() {
        return projectId;
    }
    
    /**
     * Set the project ID this configuration belongs to
     * 
     * @param projectId Project ID
     */
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
    
    /**
     * Get the ADO connection ID
     * 
     * @return Connection ID
     */
    public String getConnectionId() {
        return connectionId;
    }
    
    /**
     * Set the ADO connection ID
     * 
     * @param connectionId Connection ID
     */
    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }
    
    /**
     * Get the pipeline ID
     * 
     * @return Pipeline ID
     */
    public String getPipelineId() {
        return pipelineId;
    }
    
    /**
     * Set the pipeline ID
     * 
     * @param pipelineId Pipeline ID
     */
    public void setPipelineId(String pipelineId) {
        this.pipelineId = pipelineId;
    }
    
    /**
     * Get the trigger mode
     * 
     * @return Trigger mode (e.g., "manual", "automatic", "scheduled")
     */
    public String getTriggerMode() {
        return triggerMode;
    }
    
    /**
     * Set the trigger mode
     * 
     * @param triggerMode Trigger mode (e.g., "manual", "automatic", "scheduled")
     */
    public void setTriggerMode(String triggerMode) {
        this.triggerMode = triggerMode;
    }
    
    /**
     * Check if tests should be included
     * 
     * @return True if tests should be included
     */
    public boolean isIncludeTests() {
        return includeTests;
    }
    
    /**
     * Set whether tests should be included
     * 
     * @param includeTests True if tests should be included
     */
    public void setIncludeTests(boolean includeTests) {
        this.includeTests = includeTests;
    }
    
    /**
     * Check if notification on completion is enabled
     * 
     * @return True if notification on completion is enabled
     */
    public boolean isNotifyOnCompletion() {
        return notifyOnCompletion;
    }
    
    /**
     * Set whether notification on completion is enabled
     * 
     * @param notifyOnCompletion True if notification on completion is enabled
     */
    public void setNotifyOnCompletion(boolean notifyOnCompletion) {
        this.notifyOnCompletion = notifyOnCompletion;
    }
    
    /**
     * Get the pipeline parameters
     * 
     * @return Pipeline parameters
     */
    public String[] getParameters() {
        return parameters;
    }
    
    /**
     * Set the pipeline parameters
     * 
     * @param parameters Pipeline parameters
     */
    public void setParameters(String[] parameters) {
        this.parameters = parameters;
    }
    
    /**
     * Check if this configuration is enabled
     * 
     * @return True if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Set whether this configuration is enabled
     * 
     * @param enabled True if enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Get the creation timestamp
     * 
     * @return Creation timestamp in milliseconds since epoch
     */
    public long getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Set the creation timestamp
     * 
     * @param createdAt Creation timestamp in milliseconds since epoch
     */
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * Get the last update timestamp
     * 
     * @return Last update timestamp in milliseconds since epoch
     */
    public long getUpdatedAt() {
        return updatedAt;
    }
    
    /**
     * Set the last update timestamp
     * 
     * @param updatedAt Last update timestamp in milliseconds since epoch
     */
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
} 