package com.cstestforge.testing.model;

import com.cstestforge.project.model.test.Test;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a test suite that groups related test cases.
 */
public class TestSuite {

    private String id;
    private String name;
    private String description;
    private String projectId;
    private Set<String> testIds;
    private Set<String> tags;
    private String parentSuiteId;
    private List<TestSuite> childSuites;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private int version;

    /**
     * Default constructor
     */
    public TestSuite() {
        this.testIds = new HashSet<>();
        this.tags = new HashSet<>();
        this.childSuites = new ArrayList<>();
    }

    /**
     * Get the unique identifier for this test suite
     * 
     * @return Test suite ID
     */
    public String getId() {
        return id;
    }

    /**
     * Set the unique identifier for this test suite
     * 
     * @param id Test suite ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the name of this test suite
     * 
     * @return Test suite name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this test suite
     * 
     * @param name Test suite name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the description of this test suite
     * 
     * @return Test suite description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the description of this test suite
     * 
     * @param description Test suite description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the project ID this test suite belongs to
     * 
     * @return Project ID
     */
    public String getProjectId() {
        return projectId;
    }

    /**
     * Set the project ID this test suite belongs to
     * 
     * @param projectId Project ID
     */
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    /**
     * Get the test IDs in this suite
     * 
     * @return Set of test IDs
     */
    public Set<String> getTestIds() {
        return testIds;
    }

    /**
     * Set the test IDs in this suite
     * 
     * @param testIds Set of test IDs
     */
    public void setTestIds(Set<String> testIds) {
        this.testIds = testIds;
    }

    /**
     * Get the tags associated with this test suite
     * 
     * @return Set of tags
     */
    public Set<String> getTags() {
        return tags;
    }

    /**
     * Set the tags associated with this test suite
     * 
     * @param tags Set of tags
     */
    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    /**
     * Get the parent suite ID if this is a child suite
     * 
     * @return Parent suite ID
     */
    public String getParentSuiteId() {
        return parentSuiteId;
    }

    /**
     * Set the parent suite ID if this is a child suite
     * 
     * @param parentSuiteId Parent suite ID
     */
    public void setParentSuiteId(String parentSuiteId) {
        this.parentSuiteId = parentSuiteId;
    }

    /**
     * Get the child suites
     * 
     * @return List of child suites
     */
    public List<TestSuite> getChildSuites() {
        return childSuites;
    }

    /**
     * Set the child suites
     * 
     * @param childSuites List of child suites
     */
    public void setChildSuites(List<TestSuite> childSuites) {
        this.childSuites = childSuites;
    }

    /**
     * Get the user who created this test suite
     * 
     * @return Creator username
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Set the user who created this test suite
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
     * Get the user who last updated this test suite
     * 
     * @return Updater username
     */
    public String getUpdatedBy() {
        return updatedBy;
    }

    /**
     * Set the user who last updated this test suite
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
     * Add a test to this suite
     * 
     * @param testId ID of the test to add
     * @return This test suite instance for method chaining
     */
    public TestSuite addTest(String testId) {
        if (this.testIds == null) {
            this.testIds = new HashSet<>();
        }
        this.testIds.add(testId);
        return this;
    }

    /**
     * Remove a test from this suite
     * 
     * @param testId ID of the test to remove
     * @return This test suite instance for method chaining
     */
    public TestSuite removeTest(String testId) {
        if (this.testIds != null) {
            this.testIds.remove(testId);
        }
        return this;
    }

    /**
     * Add a tag to this test suite
     * 
     * @param tag Tag to add
     * @return This test suite instance for method chaining
     */
    public TestSuite addTag(String tag) {
        if (this.tags == null) {
            this.tags = new HashSet<>();
        }
        this.tags.add(tag);
        return this;
    }

    /**
     * Add a child suite to this suite
     * 
     * @param childSuite Child suite to add
     * @return This test suite instance for method chaining
     */
    public TestSuite addChildSuite(TestSuite childSuite) {
        if (this.childSuites == null) {
            this.childSuites = new ArrayList<>();
        }
        childSuite.setParentSuiteId(this.id);
        this.childSuites.add(childSuite);
        return this;
    }
} 