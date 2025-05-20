package com.cstestforge.ado.model;

/**
 * Represents an Azure DevOps Test Plan
 */
public class AdoTestPlan {
    
    private String id;
    private String name;
    private String description;
    private String url;
    private String areaPath;
    private String iteration;
    private String projectId;
    private String rootSuiteId;
    
    /**
     * Default constructor
     */
    public AdoTestPlan() {
        // Default constructor
    }
    
    /**
     * Constructor with essential fields
     * 
     * @param id Test Plan ID
     * @param name Test Plan name
     * @param projectId Project ID this test plan belongs to
     */
    public AdoTestPlan(String id, String name, String projectId) {
        this.id = id;
        this.name = name;
        this.projectId = projectId;
    }
    
    /**
     * Get the test plan ID
     * 
     * @return Test plan ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Set the test plan ID
     * 
     * @param id Test plan ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get the test plan name
     * 
     * @return Test plan name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the test plan name
     * 
     * @param name Test plan name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get the test plan description
     * 
     * @return Test plan description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Set the test plan description
     * 
     * @param description Test plan description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Get the test plan URL
     * 
     * @return Test plan URL
     */
    public String getUrl() {
        return url;
    }
    
    /**
     * Set the test plan URL
     * 
     * @param url Test plan URL
     */
    public void setUrl(String url) {
        this.url = url;
    }
    
    /**
     * Get the area path
     * 
     * @return Area path
     */
    public String getAreaPath() {
        return areaPath;
    }
    
    /**
     * Set the area path
     * 
     * @param areaPath Area path
     */
    public void setAreaPath(String areaPath) {
        this.areaPath = areaPath;
    }
    
    /**
     * Get the iteration
     * 
     * @return Iteration
     */
    public String getIteration() {
        return iteration;
    }
    
    /**
     * Set the iteration
     * 
     * @param iteration Iteration
     */
    public void setIteration(String iteration) {
        this.iteration = iteration;
    }
    
    /**
     * Get the project ID this test plan belongs to
     * 
     * @return Project ID
     */
    public String getProjectId() {
        return projectId;
    }
    
    /**
     * Set the project ID this test plan belongs to
     * 
     * @param projectId Project ID
     */
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
    
    /**
     * Get the root suite ID of this test plan
     * 
     * @return Root suite ID
     */
    public String getRootSuiteId() {
        return rootSuiteId;
    }
    
    /**
     * Set the root suite ID of this test plan
     * 
     * @param rootSuiteId Root suite ID
     */
    public void setRootSuiteId(String rootSuiteId) {
        this.rootSuiteId = rootSuiteId;
    }
} 