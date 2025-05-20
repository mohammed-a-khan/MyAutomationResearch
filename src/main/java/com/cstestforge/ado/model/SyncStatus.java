package com.cstestforge.ado.model;

/**
 * Represents the status of test case synchronization with Azure DevOps
 */
public class SyncStatus {
    
    private String projectId;
    private long lastSyncTime;
    private String lastSyncStatus; // "success", "failed", "in-progress", "not-started"
    private String lastSyncMessage;
    private long nextScheduledSync;
    private int totalItems;
    private int processedItems;
    
    /**
     * Default constructor
     */
    public SyncStatus() {
        // Default constructor
    }
    
    /**
     * Constructor with essential fields
     * 
     * @param projectId Project ID 
     * @param lastSyncStatus Last sync status
     */
    public SyncStatus(String projectId, String lastSyncStatus) {
        this.projectId = projectId;
        this.lastSyncStatus = lastSyncStatus;
        this.lastSyncTime = System.currentTimeMillis();
    }
    
    /**
     * Get the project ID this status belongs to
     * 
     * @return Project ID
     */
    public String getProjectId() {
        return projectId;
    }
    
    /**
     * Set the project ID this status belongs to
     * 
     * @param projectId Project ID
     */
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
    
    /**
     * Get the last sync timestamp
     * 
     * @return Last sync timestamp in milliseconds since epoch
     */
    public long getLastSyncTime() {
        return lastSyncTime;
    }
    
    /**
     * Set the last sync timestamp
     * 
     * @param lastSyncTime Last sync timestamp in milliseconds since epoch
     */
    public void setLastSyncTime(long lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }
    
    /**
     * Get the last sync status
     * 
     * @return Last sync status ("success", "failed", "in-progress", "not-started")
     */
    public String getLastSyncStatus() {
        return lastSyncStatus;
    }
    
    /**
     * Set the last sync status
     * 
     * @param lastSyncStatus Last sync status
     */
    public void setLastSyncStatus(String lastSyncStatus) {
        this.lastSyncStatus = lastSyncStatus;
    }
    
    /**
     * Get the last sync message
     * 
     * @return Last sync message
     */
    public String getLastSyncMessage() {
        return lastSyncMessage;
    }
    
    /**
     * Set the last sync message
     * 
     * @param lastSyncMessage Last sync message
     */
    public void setLastSyncMessage(String lastSyncMessage) {
        this.lastSyncMessage = lastSyncMessage;
    }
    
    /**
     * Get the next scheduled sync timestamp
     * 
     * @return Next scheduled sync timestamp in milliseconds since epoch
     */
    public long getNextScheduledSync() {
        return nextScheduledSync;
    }
    
    /**
     * Set the next scheduled sync timestamp
     * 
     * @param nextScheduledSync Next scheduled sync timestamp in milliseconds since epoch
     */
    public void setNextScheduledSync(long nextScheduledSync) {
        this.nextScheduledSync = nextScheduledSync;
    }
    
    /**
     * Get the total number of items to sync
     * 
     * @return Total items
     */
    public int getTotalItems() {
        return totalItems;
    }
    
    /**
     * Set the total number of items to sync
     * 
     * @param totalItems Total items
     */
    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }
    
    /**
     * Get the number of processed items
     * 
     * @return Processed items
     */
    public int getProcessedItems() {
        return processedItems;
    }
    
    /**
     * Set the number of processed items
     * 
     * @param processedItems Processed items
     */
    public void setProcessedItems(int processedItems) {
        this.processedItems = processedItems;
    }
    
    /**
     * Get the current progress percentage
     * 
     * @return Progress percentage or 0 if total items is 0
     */
    public int getProgressPercentage() {
        if (totalItems <= 0) {
            return 0;
        }
        return (int) (((double) processedItems / totalItems) * 100);
    }
} 