package com.cstestforge.ado.model;

/**
 * Represents an Azure DevOps Test Suite
 */
public class AdoTestSuite {
    
    private String id;
    private String name;
    private String description;
    private String url;
    private String testPlanId;
    private String parentSuiteId;
    private String projectId;
    private boolean isDefault;
    private boolean isRequirementBased;
    
    /**
     * Default constructor
     */
    public AdoTestSuite() {
        // Default constructor
    }
    
    /**
     * Constructor with essential fields
     * 
     * @param id Suite ID
     * @param name Suite name
     * @param testPlanId Test plan ID this suite belongs to
     * @param projectId Project ID this suite belongs to
     */
    public AdoTestSuite(String id, String name, String testPlanId, String projectId) {
        this.id = id;
        this.name = name;
        this.testPlanId = testPlanId;
        this.projectId = projectId;
    }
    
    /**
     * Get the test suite ID
     * 
     * @return Test suite ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Set the test suite ID
     * 
     * @param id Test suite ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get the test suite name
     * 
     * @return Test suite name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the test suite name
     * 
     * @param name Test suite name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get the test suite description
     * 
     * @return Test suite description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Set the test suite description
     * 
     * @param description Test suite description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Get the test suite URL
     * 
     * @return Test suite URL
     */
    public String getUrl() {
        return url;
    }
    
    /**
     * Set the test suite URL
     * 
     * @param url Test suite URL
     */
    public void setUrl(String url) {
        this.url = url;
    }
    
    /**
     * Get the test plan ID this suite belongs to
     * 
     * @return Test plan ID
     */
    public String getTestPlanId() {
        return testPlanId;
    }
    
    /**
     * Set the test plan ID this suite belongs to
     * 
     * @param testPlanId Test plan ID
     */
    public void setTestPlanId(String testPlanId) {
        this.testPlanId = testPlanId;
    }
    
    /**
     * Get the parent suite ID
     * 
     * @return Parent suite ID
     */
    public String getParentSuiteId() {
        return parentSuiteId;
    }
    
    /**
     * Set the parent suite ID
     * 
     * @param parentSuiteId Parent suite ID
     */
    public void setParentSuiteId(String parentSuiteId) {
        this.parentSuiteId = parentSuiteId;
    }
    
    /**
     * Get the project ID this suite belongs to
     * 
     * @return Project ID
     */
    public String getProjectId() {
        return projectId;
    }
    
    /**
     * Set the project ID this suite belongs to
     * 
     * @param projectId Project ID
     */
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
    
    /**
     * Check if this is the default suite
     * 
     * @return True if this is the default suite
     */
    public boolean isDefault() {
        return isDefault;
    }
    
    /**
     * Set whether this is the default suite
     * 
     * @param isDefault True if this is the default suite
     */
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
    
    /**
     * Check if this is a requirement-based suite
     * 
     * @return True if this is a requirement-based suite
     */
    public boolean isRequirementBased() {
        return isRequirementBased;
    }
    
    /**
     * Set whether this is a requirement-based suite
     * 
     * @param isRequirementBased True if this is a requirement-based suite
     */
    public void setRequirementBased(boolean isRequirementBased) {
        this.isRequirementBased = isRequirementBased;
    }
} 