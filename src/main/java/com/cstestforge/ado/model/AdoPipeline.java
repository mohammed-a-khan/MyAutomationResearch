package com.cstestforge.ado.model;

/**
 * Represents an Azure DevOps Pipeline
 */
public class AdoPipeline {
    
    private String id;
    private String name;
    private String projectId;
    private String url;
    private String revision;
    private String folderPath;
    private String type;
    private String status;
    
    /**
     * Default constructor
     */
    public AdoPipeline() {
        // Default constructor
    }
    
    /**
     * Constructor with essential fields
     * 
     * @param id Pipeline ID
     * @param name Pipeline name
     * @param projectId Project ID this pipeline belongs to
     * @param url Pipeline URL
     */
    public AdoPipeline(String id, String name, String projectId, String url) {
        this.id = id;
        this.name = name;
        this.projectId = projectId;
        this.url = url;
    }
    
    /**
     * Get the pipeline ID
     * 
     * @return Pipeline ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Set the pipeline ID
     * 
     * @param id Pipeline ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get the pipeline name
     * 
     * @return Pipeline name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the pipeline name
     * 
     * @param name Pipeline name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get the project ID this pipeline belongs to
     * 
     * @return Project ID
     */
    public String getProjectId() {
        return projectId;
    }
    
    /**
     * Set the project ID this pipeline belongs to
     * 
     * @param projectId Project ID
     */
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
    
    /**
     * Get the pipeline URL
     * 
     * @return Pipeline URL
     */
    public String getUrl() {
        return url;
    }
    
    /**
     * Set the pipeline URL
     * 
     * @param url Pipeline URL
     */
    public void setUrl(String url) {
        this.url = url;
    }
    
    /**
     * Get the pipeline revision
     * 
     * @return Pipeline revision
     */
    public String getRevision() {
        return revision;
    }
    
    /**
     * Set the pipeline revision
     * 
     * @param revision Pipeline revision
     */
    public void setRevision(String revision) {
        this.revision = revision;
    }
    
    /**
     * Get the pipeline folder path
     * 
     * @return Folder path
     */
    public String getFolderPath() {
        return folderPath;
    }
    
    /**
     * Set the pipeline folder path
     * 
     * @param folderPath Folder path
     */
    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }
    
    /**
     * Get the pipeline type
     * 
     * @return Pipeline type
     */
    public String getType() {
        return type;
    }
    
    /**
     * Set the pipeline type
     * 
     * @param type Pipeline type
     */
    public void setType(String type) {
        this.type = type;
    }
    
    /**
     * Get the pipeline status
     * 
     * @return Pipeline status
     */
    public String getStatus() {
        return status;
    }
    
    /**
     * Set the pipeline status
     * 
     * @param status Pipeline status
     */
    public void setStatus(String status) {
        this.status = status;
    }
} 