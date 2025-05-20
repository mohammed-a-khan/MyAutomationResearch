package com.cstestforge.export.model;

import java.util.List;

/**
 * Request model for test export operations
 */
public class ExportRequest {
    private String projectId;
    private List<String> testIds;
    private String framework;
    private String language;
    private String buildTool;
    private boolean generateBDD;
    private String packageName;
    
    /**
     * Default constructor
     */
    public ExportRequest() {
    }
    
    /**
     * Parameterized constructor
     * 
     * @param projectId The project ID
     * @param testIds List of test IDs to export
     * @param framework Target framework (e.g., "selenium", "playwright")
     * @param language Target language (e.g., "java", "typescript")
     * @param buildTool Build tool to use (e.g., "maven", "gradle", "npm")
     */
    public ExportRequest(String projectId, List<String> testIds, String framework, String language, String buildTool) {
        this.projectId = projectId;
        this.testIds = testIds;
        this.framework = framework;
        this.language = language;
        this.buildTool = buildTool;
        this.generateBDD = false;
        this.packageName = "com.cstestforge.generated";
    }

    // Getters and setters
    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public List<String> getTestIds() {
        return testIds;
    }

    public void setTestIds(List<String> testIds) {
        this.testIds = testIds;
    }

    public String getFramework() {
        return framework;
    }

    public void setFramework(String framework) {
        this.framework = framework;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getBuildTool() {
        return buildTool;
    }

    public void setBuildTool(String buildTool) {
        this.buildTool = buildTool;
    }
    
    public boolean isGenerateBDD() {
        return generateBDD;
    }
    
    public void setGenerateBDD(boolean generateBDD) {
        this.generateBDD = generateBDD;
    }
    
    public String getPackageName() {
        return packageName;
    }
    
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    
    /**
     * Get a unique identifier for this framework+language combination
     * 
     * @return A string identifier in the format "framework-language"
     */
    public String getFrameworkIdentifier() {
        return framework.toLowerCase() + "-" + language.toLowerCase();
    }
} 