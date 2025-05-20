package com.cstestforge.codegen.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents generated code from a test definition or recording session
 */
public class GeneratedCode {
    private String id;
    private String sourceId; // Recording session ID or test ID
    private String projectId;
    private String language; // java, typescript
    private String framework; // selenium-java, playwright-typescript
    private String code;
    private String pageObjectCode; // Generated page object code
    private LocalDateTime generatedAt;
    private Map<String, Object> metadata;
    
    // Default constructor
    public GeneratedCode() {
        this.id = UUID.randomUUID().toString();
        this.generatedAt = LocalDateTime.now();
        this.metadata = new HashMap<>();
    }
    
    // Constructor with basic fields
    public GeneratedCode(String sourceId, String projectId, String language, String framework, String code) {
        this();
        this.sourceId = sourceId;
        this.projectId = projectId;
        this.language = language;
        this.framework = framework;
        this.code = code;
    }
    
    // Constructor with page object code
    public GeneratedCode(String sourceId, String projectId, String language, String framework, String code, String pageObjectCode) {
        this(sourceId, projectId, language, framework, code);
        this.pageObjectCode = pageObjectCode;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getFramework() {
        return framework;
    }

    public void setFramework(String framework) {
        this.framework = framework;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
    
    /**
     * Get the page object code
     * 
     * @return The page object code
     */
    public String getPageObjectCode() {
        return pageObjectCode;
    }
    
    /**
     * Set the page object code
     * 
     * @param pageObjectCode The page object code
     */
    public void setPageObjectCode(String pageObjectCode) {
        this.pageObjectCode = pageObjectCode;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
} 