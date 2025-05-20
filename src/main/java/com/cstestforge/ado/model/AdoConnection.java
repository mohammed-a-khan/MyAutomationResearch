package com.cstestforge.ado.model;

/**
 * Represents a connection to Azure DevOps for integration
 */
public class AdoConnection {

    private String id;
    private String name;
    private String url;
    private String pat;
    private String organizationName;
    private String projectName;
    private boolean isActive;
    private long createdAt;
    private long updatedAt;
    
    /**
     * Default constructor
     */
    public AdoConnection() {
        // Default constructor
    }
    
    /**
     * Constructor with essential fields
     * 
     * @param name Connection name
     * @param url Azure DevOps URL
     * @param pat Personal access token
     * @param organizationName Organization name
     * @param projectName Project name
     */
    public AdoConnection(String name, String url, String pat, String organizationName, String projectName) {
        this.name = name;
        this.url = url;
        this.pat = pat;
        this.organizationName = organizationName;
        this.projectName = projectName;
        this.isActive = true;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    /**
     * Get the unique ID of this connection
     * 
     * @return Connection ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Set the unique ID of this connection
     * 
     * @param id Connection ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get the name of this connection
     * 
     * @return Connection name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the name of this connection
     * 
     * @param name Connection name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get the Azure DevOps URL for this connection
     * 
     * @return Azure DevOps URL
     */
    public String getUrl() {
        return url;
    }
    
    /**
     * Set the Azure DevOps URL for this connection
     * 
     * @param url Azure DevOps URL
     */
    public void setUrl(String url) {
        this.url = url;
    }
    
    /**
     * Get the personal access token for this connection
     * 
     * @return Personal access token
     */
    public String getPat() {
        return pat;
    }
    
    /**
     * Set the personal access token for this connection
     * 
     * @param pat Personal access token
     */
    public void setPat(String pat) {
        this.pat = pat;
    }
    
    /**
     * Get the organization name for this connection
     * 
     * @return Organization name
     */
    public String getOrganizationName() {
        return organizationName;
    }
    
    /**
     * Set the organization name for this connection
     * 
     * @param organizationName Organization name
     */
    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }
    
    /**
     * Get the project name for this connection
     * 
     * @return Project name
     */
    public String getProjectName() {
        return projectName;
    }
    
    /**
     * Set the project name for this connection
     * 
     * @param projectName Project name
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    
    /**
     * Check if this connection is active
     * 
     * @return True if active
     */
    public boolean isActive() {
        return isActive;
    }
    
    /**
     * Set whether this connection is active
     * 
     * @param active True to set active
     */
    public void setActive(boolean active) {
        isActive = active;
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