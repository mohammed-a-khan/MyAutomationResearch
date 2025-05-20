package com.cstestforge.ado.model;

/**
 * Represents an Azure DevOps project
 */
public class AdoProject {
    
    private String id;
    private String name;
    private String description;
    private String url;
    private String state;
    
    /**
     * Default constructor
     */
    public AdoProject() {
        // Default constructor
    }
    
    /**
     * Constructor with essential fields
     * 
     * @param id Project ID
     * @param name Project name
     * @param url Project URL
     */
    public AdoProject(String id, String name, String url) {
        this.id = id;
        this.name = name;
        this.url = url;
    }
    
    /**
     * Get the project ID
     * 
     * @return Project ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Set the project ID
     * 
     * @param id Project ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get the project name
     * 
     * @return Project name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the project name
     * 
     * @param name Project name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get the project description
     * 
     * @return Project description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Set the project description
     * 
     * @param description Project description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Get the project URL
     * 
     * @return Project URL
     */
    public String getUrl() {
        return url;
    }
    
    /**
     * Set the project URL
     * 
     * @param url Project URL
     */
    public void setUrl(String url) {
        this.url = url;
    }
    
    /**
     * Get the project state
     * 
     * @return Project state
     */
    public String getState() {
        return state;
    }
    
    /**
     * Set the project state
     * 
     * @param state Project state
     */
    public void setState(String state) {
        this.state = state;
    }
} 