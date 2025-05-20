package com.cstestforge.recorder.model;

/**
 * Represents a request to start a recording session
 */
public class RecordingRequest {
    private String projectId;
    private String browserType;
    private String framework;
    private String url;
    
    // Getters and setters
    public String getProjectId() {
        return projectId;
    }
    
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
    
    public String getBrowserType() {
        return browserType;
    }
    
    public void setBrowserType(String browserType) {
        this.browserType = browserType;
    }
    
    public String getFramework() {
        return framework;
    }
    
    public void setFramework(String framework) {
        this.framework = framework;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
} 