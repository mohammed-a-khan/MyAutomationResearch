package com.cstestforge.codegen.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for code generation templates and settings for a project
 */
public class CodeGenerationConfig {
    private String id;
    private String projectId;
    private Map<String, String> defaultTemplates; // framework-id -> template-id mapping
    private Map<String, Object> frameworkSettings; // framework-specific settings
    private String defaultPackageName;
    private String defaultAuthor;
    private List<String> imports; // Custom imports to include 
    private boolean formatCode;
    private boolean includeComments;
    private Map<String, String> customMappings; // Custom event-to-code mappings
    private Map<String, String> variableNames; // Custom variable name mappings
    
    // Added fields for test generation
    private String testStyle; // "TestNG", "BDD", "Standard"
    private String testName;
    private String packageName;
    private boolean generatePageObjects;
    private String pageObjectName;
    
    public CodeGenerationConfig() {
        this.defaultTemplates = new HashMap<>();
        this.frameworkSettings = new HashMap<>();
        this.imports = new ArrayList<>();
        this.formatCode = true;
        this.includeComments = true;
        this.customMappings = new HashMap<>();
        this.variableNames = new HashMap<>();
        
        // Default values for new fields
        this.testStyle = "TestNG"; // Default to TestNG for Java, will be overridden for TypeScript
        this.testName = "GeneratedTest";
        this.packageName = "com.cstestforge.generated";
        this.generatePageObjects = true;
        this.pageObjectName = "GeneratedPage";
    }
    
    public CodeGenerationConfig(String projectId) {
        this();
        this.projectId = projectId;
        this.id = projectId; // Using projectId as the config id
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public Map<String, String> getDefaultTemplates() {
        return defaultTemplates;
    }

    public void setDefaultTemplates(Map<String, String> defaultTemplates) {
        this.defaultTemplates = defaultTemplates;
    }
    
    public void setDefaultTemplate(String frameworkId, String templateId) {
        this.defaultTemplates.put(frameworkId, templateId);
    }
    
    public String getDefaultTemplate(String frameworkId) {
        return this.defaultTemplates.get(frameworkId);
    }

    public Map<String, Object> getFrameworkSettings() {
        return frameworkSettings;
    }

    public void setFrameworkSettings(Map<String, Object> frameworkSettings) {
        this.frameworkSettings = frameworkSettings;
    }
    
    public void addFrameworkSetting(String frameworkId, String key, Object value) {
        String mapKey = frameworkId + "." + key;
        this.frameworkSettings.put(mapKey, value);
    }
    
    public Object getFrameworkSetting(String frameworkId, String key) {
        String mapKey = frameworkId + "." + key;
        return this.frameworkSettings.get(mapKey);
    }

    public String getDefaultPackageName() {
        return defaultPackageName;
    }

    public void setDefaultPackageName(String defaultPackageName) {
        this.defaultPackageName = defaultPackageName;
    }

    public String getDefaultAuthor() {
        return defaultAuthor;
    }

    public void setDefaultAuthor(String defaultAuthor) {
        this.defaultAuthor = defaultAuthor;
    }

    public List<String> getImports() {
        return imports;
    }

    public void setImports(List<String> imports) {
        this.imports = imports;
    }
    
    public void addImport(String importStatement) {
        this.imports.add(importStatement);
    }

    public boolean isFormatCode() {
        return formatCode;
    }

    public void setFormatCode(boolean formatCode) {
        this.formatCode = formatCode;
    }

    public boolean isIncludeComments() {
        return includeComments;
    }

    public void setIncludeComments(boolean includeComments) {
        this.includeComments = includeComments;
    }

    public Map<String, String> getCustomMappings() {
        return customMappings;
    }

    public void setCustomMappings(Map<String, String> customMappings) {
        this.customMappings = customMappings;
    }
    
    public void addCustomMapping(String eventType, String codeTemplate) {
        this.customMappings.put(eventType, codeTemplate);
    }
    
    public String getCustomMapping(String eventType) {
        return this.customMappings.get(eventType);
    }

    public Map<String, String> getVariableNames() {
        return variableNames;
    }

    public void setVariableNames(Map<String, String> variableNames) {
        this.variableNames = variableNames;
    }
    
    public void setVariableName(String elementId, String variableName) {
        this.variableNames.put(elementId, variableName);
    }
    
    public String getVariableName(String elementId) {
        return this.variableNames.getOrDefault(elementId, "element_" + elementId);
    }
    
    // New getters and setters for test generation fields
    
    /**
     * Get the test style (TestNG, BDD, Standard)
     * 
     * @return The test style
     */
    public String getTestStyle() {
        return testStyle;
    }
    
    /**
     * Set the test style (TestNG, BDD, Standard)
     * 
     * @param testStyle The test style
     */
    public void setTestStyle(String testStyle) {
        this.testStyle = testStyle;
    }
    
    /**
     * Get the test name
     * 
     * @return The test name
     */
    public String getTestName() {
        return testName;
    }
    
    /**
     * Set the test name
     * 
     * @param testName The test name
     */
    public void setTestName(String testName) {
        this.testName = testName;
    }
    
    /**
     * Get the package name
     * 
     * @return The package name
     */
    public String getPackageName() {
        return packageName;
    }
    
    /**
     * Set the package name
     * 
     * @param packageName The package name
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    
    /**
     * Check if page objects should be generated
     * 
     * @return True if page objects should be generated
     */
    public boolean isGeneratePageObjects() {
        return generatePageObjects;
    }
    
    /**
     * Set whether page objects should be generated
     * 
     * @param generatePageObjects True if page objects should be generated
     */
    public void setGeneratePageObjects(boolean generatePageObjects) {
        this.generatePageObjects = generatePageObjects;
    }
    
    /**
     * Get the page object name
     * 
     * @return The page object name
     */
    public String getPageObjectName() {
        return pageObjectName;
    }
    
    /**
     * Set the page object name
     * 
     * @param pageObjectName The page object name
     */
    public void setPageObjectName(String pageObjectName) {
        this.pageObjectName = pageObjectName;
    }
} 