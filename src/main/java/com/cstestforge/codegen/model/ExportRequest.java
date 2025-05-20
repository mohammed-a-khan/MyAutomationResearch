package com.cstestforge.codegen.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a request to export generated code to a framework-specific format
 */
public class ExportRequest {
    private String projectId;
    private String framework; // selenium-java, playwright-typescript
    private String exportName; // Name for the exported project
    private List<String> generatedCodeIds; // IDs of generated code to include
    private List<String> testIds; // IDs of tests to include (will be generated)
    private String packageName; // For Java exports
    private String destinationPath; // Where to export the files
    private boolean includeFrameworkFiles; // Whether to include framework files
    private boolean includeTestData; // Whether to include test data
    private boolean includeRunners; // Whether to include test runners
    private boolean includeDependencies; // Whether to include dependency files (e.g., pom.xml, package.json)
    private boolean includeDocumentation; // Whether to include documentation
    private Map<String, Object> exportOptions; // Framework-specific export options
    private Map<String, Object> config; // Configuration settings for the export
    
    public ExportRequest() {
        this.generatedCodeIds = new ArrayList<>();
        this.testIds = new ArrayList<>();
        this.includeFrameworkFiles = true;
        this.includeTestData = true;
        this.includeRunners = true;
        this.includeDependencies = true;
        this.includeDocumentation = true;
        this.exportOptions = new HashMap<>();
        this.config = new HashMap<>();
    }
    
    public ExportRequest(String projectId, String framework, String exportName) {
        this();
        this.projectId = projectId;
        this.framework = framework;
        this.exportName = exportName;
    }
    
    // Getters and Setters
    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getFramework() {
        return framework;
    }

    public void setFramework(String framework) {
        this.framework = framework;
    }

    public String getExportName() {
        return exportName;
    }

    public void setExportName(String exportName) {
        this.exportName = exportName;
    }

    public List<String> getGeneratedCodeIds() {
        return generatedCodeIds;
    }

    public void setGeneratedCodeIds(List<String> generatedCodeIds) {
        this.generatedCodeIds = generatedCodeIds;
    }
    
    public void addGeneratedCodeId(String generatedCodeId) {
        this.generatedCodeIds.add(generatedCodeId);
    }

    public List<String> getTestIds() {
        return testIds;
    }

    public void setTestIds(List<String> testIds) {
        this.testIds = testIds;
    }
    
    public void addTestId(String testId) {
        this.testIds.add(testId);
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    public boolean isIncludeFrameworkFiles() {
        return includeFrameworkFiles;
    }

    public void setIncludeFrameworkFiles(boolean includeFrameworkFiles) {
        this.includeFrameworkFiles = includeFrameworkFiles;
    }

    public boolean isIncludeTestData() {
        return includeTestData;
    }

    public void setIncludeTestData(boolean includeTestData) {
        this.includeTestData = includeTestData;
    }

    public boolean isIncludeRunners() {
        return includeRunners;
    }

    public void setIncludeRunners(boolean includeRunners) {
        this.includeRunners = includeRunners;
    }

    public boolean isIncludeDependencies() {
        return includeDependencies;
    }

    public void setIncludeDependencies(boolean includeDependencies) {
        this.includeDependencies = includeDependencies;
    }

    public boolean isIncludeDocumentation() {
        return includeDocumentation;
    }

    public void setIncludeDocumentation(boolean includeDocumentation) {
        this.includeDocumentation = includeDocumentation;
    }

    public Map<String, Object> getExportOptions() {
        return exportOptions;
    }

    public void setExportOptions(Map<String, Object> exportOptions) {
        this.exportOptions = exportOptions;
    }
    
    public void addExportOption(String key, Object value) {
        this.exportOptions.put(key, value);
    }
    
    public Map<String, Object> getConfig() {
        return config;
    }
    
    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }
    
    public void addConfig(String key, Object value) {
        this.config.put(key, value);
    }
} 