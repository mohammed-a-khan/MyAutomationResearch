package com.cstestforge.testing.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Model class representing a test case
 */
public class TestCase {
    private String id;
    private String name;
    private String projectId;
    private String description;
    private String baseUrl;
    private List<TestStep> steps;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    
    /**
     * Default constructor
     */
    public TestCase() {
    }
    
    /**
     * Constructor with essential fields
     * 
     * @param id ID of the test case
     * @param name Name of the test case
     * @param projectId ID of the project this test belongs to
     * @param baseUrl Base URL for the test
     */
    public TestCase(String id, String name, String projectId, String baseUrl) {
        this.id = id;
        this.name = name;
        this.projectId = projectId;
        this.baseUrl = baseUrl;
        this.createdDate = LocalDateTime.now();
        this.lastModifiedDate = LocalDateTime.now();
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<TestStep> getSteps() {
        return steps;
    }

    public void setSteps(List<TestStep> steps) {
        this.steps = steps;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
} 