package com.cstestforge.ado.model;

/**
 * Configuration for test case synchronization with Azure DevOps
 */
public class SyncConfig {
    
    private String id;
    private String projectId;
    private String connectionId;
    private String testPlanId;
    private String testSuiteId;
    private boolean autoSync;
    private boolean syncOnExecution;
    private boolean twoWaySync;
    private String mappingField;
    private long syncInterval;
    private long lastSyncTimestamp;
    
    /**
     * Default constructor
     */
    public SyncConfig() {
        // Default constructor
    }
    
    /**
     * Constructor with essential fields
     * 
     * @param projectId Project ID this configuration belongs to
     * @param connectionId ADO connection ID
     */
    public SyncConfig(String projectId, String connectionId) {
        this.projectId = projectId;
        this.connectionId = connectionId;
        this.autoSync = false;
        this.syncOnExecution = false;
        this.twoWaySync = false;
        this.syncInterval = 3600000; // Default: 1 hour
        this.lastSyncTimestamp = System.currentTimeMillis();
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
     * Get the test plan ID to sync with
     * 
     * @return Test plan ID
     */
    public String getTestPlanId() {
        return testPlanId;
    }
    
    /**
     * Set the test plan ID to sync with
     * 
     * @param testPlanId Test plan ID
     */
    public void setTestPlanId(String testPlanId) {
        this.testPlanId = testPlanId;
    }
    
    /**
     * Get the test suite ID to sync with
     * 
     * @return Test suite ID
     */
    public String getTestSuiteId() {
        return testSuiteId;
    }
    
    /**
     * Set the test suite ID to sync with
     * 
     * @param testSuiteId Test suite ID
     */
    public void setTestSuiteId(String testSuiteId) {
        this.testSuiteId = testSuiteId;
    }
    
    /**
     * Check if auto-sync is enabled
     * 
     * @return True if auto-sync is enabled
     */
    public boolean isAutoSync() {
        return autoSync;
    }
    
    /**
     * Set whether auto-sync is enabled
     * 
     * @param autoSync True to enable auto-sync
     */
    public void setAutoSync(boolean autoSync) {
        this.autoSync = autoSync;
    }
    
    /**
     * Check if sync on execution is enabled
     * 
     * @return True if sync on execution is enabled
     */
    public boolean isSyncOnExecution() {
        return syncOnExecution;
    }
    
    /**
     * Set whether sync on execution is enabled
     * 
     * @param syncOnExecution True to enable sync on execution
     */
    public void setSyncOnExecution(boolean syncOnExecution) {
        this.syncOnExecution = syncOnExecution;
    }
    
    /**
     * Check if two-way sync is enabled
     * 
     * @return True if two-way sync is enabled
     */
    public boolean isTwoWaySync() {
        return twoWaySync;
    }
    
    /**
     * Set whether two-way sync is enabled
     * 
     * @param twoWaySync True to enable two-way sync
     */
    public void setTwoWaySync(boolean twoWaySync) {
        this.twoWaySync = twoWaySync;
    }
    
    /**
     * Get the field used for mapping test cases
     * 
     * @return Mapping field name
     */
    public String getMappingField() {
        return mappingField;
    }
    
    /**
     * Set the field used for mapping test cases
     * 
     * @param mappingField Mapping field name
     */
    public void setMappingField(String mappingField) {
        this.mappingField = mappingField;
    }
    
    /**
     * Get the sync interval in milliseconds
     * 
     * @return Sync interval in milliseconds
     */
    public long getSyncInterval() {
        return syncInterval;
    }
    
    /**
     * Set the sync interval in milliseconds
     * 
     * @param syncInterval Sync interval in milliseconds
     */
    public void setSyncInterval(long syncInterval) {
        this.syncInterval = syncInterval;
    }
    
    /**
     * Get the timestamp of the last sync
     * 
     * @return Last sync timestamp in milliseconds since epoch
     */
    public long getLastSyncTimestamp() {
        return lastSyncTimestamp;
    }
    
    /**
     * Set the timestamp of the last sync
     * 
     * @param lastSyncTimestamp Last sync timestamp in milliseconds since epoch
     */
    public void setLastSyncTimestamp(long lastSyncTimestamp) {
        this.lastSyncTimestamp = lastSyncTimestamp;
    }
}