package com.cstestforge.project.model.test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a test case in the system.
 */
public class Test {

    private String id;
    private String name;
    private String description;
    private TestType type;
    private TestStatus status;
    private Set<String> tags;
    private String projectId;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private int version;
    private List<TestStep> steps;
    private TestConfig config;
    
    // Default constructor
    public Test() {
        this.tags = new HashSet<>();
        this.steps = new ArrayList<>();
        this.config = new TestConfig();
    }
    
    /**
     * Get the unique identifier for this test
     * 
     * @return Test ID
     */
    public String getId() {
        return id;
    }

    /**
     * Set the unique identifier for this test
     * 
     * @param id Test ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the name of this test
     * 
     * @return Test name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this test
     * 
     * @param name Test name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the description of this test
     * 
     * @return Test description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the description of this test
     * 
     * @param description Test description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the type of this test
     * 
     * @return Test type
     */
    public TestType getType() {
        return type;
    }

    /**
     * Set the type of this test
     * 
     * @param type Test type
     */
    public void setType(TestType type) {
        this.type = type;
    }
    
    /**
     * Get the status of this test
     * 
     * @return Test status
     */
    public TestStatus getStatus() {
        return status;
    }

    /**
     * Set the status of this test
     * 
     * @param status Test status
     */
    public void setStatus(TestStatus status) {
        this.status = status;
    }

    /**
     * Get the tags associated with this test
     * 
     * @return Set of tags
     */
    public Set<String> getTags() {
        return tags;
    }

    /**
     * Set the tags associated with this test
     * 
     * @param tags Set of tags
     */
    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    /**
     * Get the project ID this test belongs to
     * 
     * @return Project ID
     */
    public String getProjectId() {
        return projectId;
    }

    /**
     * Set the project ID this test belongs to
     * 
     * @param projectId Project ID
     */
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    /**
     * Get the user who created this test
     * 
     * @return Creator username
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Set the user who created this test
     * 
     * @param createdBy Creator username
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Get the creation timestamp
     * 
     * @return Creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Set the creation timestamp
     * 
     * @param createdAt Creation timestamp
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Get the user who last updated this test
     * 
     * @return Updater username
     */
    public String getUpdatedBy() {
        return updatedBy;
    }

    /**
     * Set the user who last updated this test
     * 
     * @param updatedBy Updater username
     */
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    /**
     * Get the last update timestamp
     * 
     * @return Update timestamp
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Set the last update timestamp
     * 
     * @param updatedAt Update timestamp
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * Get the version number
     * 
     * @return Version number
     */
    public int getVersion() {
        return version;
    }

    /**
     * Set the version number
     * 
     * @param version Version number
     */
    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * Get the steps of this test
     * 
     * @return List of test steps
     */
    public List<TestStep> getSteps() {
        return steps;
    }

    /**
     * Set the steps of this test
     * 
     * @param steps List of test steps
     */
    public void setSteps(List<TestStep> steps) {
        this.steps = steps;
    }

    /**
     * Get the configuration for this test
     * 
     * @return Test configuration
     */
    public TestConfig getConfig() {
        return config;
    }

    /**
     * Set the configuration for this test
     * 
     * @param config Test configuration
     */
    public void setConfig(TestConfig config) {
        this.config = config;
    }

    /**
     * Add a tag to this test
     * 
     * @param tag Tag to add
     * @return This test instance for method chaining
     */
    public Test addTag(String tag) {
        if (this.tags == null) {
            this.tags = new HashSet<>();
        }
        this.tags.add(tag);
        return this;
    }

    /**
     * Add a test step
     * 
     * @param step Test step to add
     * @return This test instance for method chaining
     */
    public Test addStep(TestStep step) {
        if (this.steps == null) {
            this.steps = new ArrayList<>();
        }
        this.steps.add(step);
        return this;
    }
} 