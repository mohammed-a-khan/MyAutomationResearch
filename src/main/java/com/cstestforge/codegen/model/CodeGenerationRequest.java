package com.cstestforge.codegen.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a request to generate code with specific configuration options
 */
public class CodeGenerationRequest {
    private String sourceId; // Recording session ID or test ID
    private String projectId;
    private String framework; // selenium-java, playwright-typescript
    private String language; // java, typescript
    private String templateId; // Optional, if null use default
    private String testClassName; // Optional class name override
    private String testMethodName; // Optional method name override
    private boolean includeComments; // Whether to include comments in generated code
    private boolean formatCode; // Whether to format the generated code
    private boolean includeImports; // Whether to include import statements
    private Map<String, Object> customOptions; // Framework-specific options
    
    public CodeGenerationRequest() {
        this.includeComments = true;
        this.formatCode = true;
        this.includeImports = true;
        this.customOptions = new HashMap<>();
    }
    
    public CodeGenerationRequest(String sourceId, String projectId, String framework, String language) {
        this();
        this.sourceId = sourceId;
        this.projectId = projectId;
        this.framework = framework;
        this.language = language;
    }
    
    // Getters and Setters
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

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getTestClassName() {
        return testClassName;
    }

    public void setTestClassName(String testClassName) {
        this.testClassName = testClassName;
    }

    public String getTestMethodName() {
        return testMethodName;
    }

    public void setTestMethodName(String testMethodName) {
        this.testMethodName = testMethodName;
    }

    public boolean isIncludeComments() {
        return includeComments;
    }

    public void setIncludeComments(boolean includeComments) {
        this.includeComments = includeComments;
    }

    public boolean isFormatCode() {
        return formatCode;
    }

    public void setFormatCode(boolean formatCode) {
        this.formatCode = formatCode;
    }

    public boolean isIncludeImports() {
        return includeImports;
    }

    public void setIncludeImports(boolean includeImports) {
        this.includeImports = includeImports;
    }

    public Map<String, Object> getCustomOptions() {
        return customOptions;
    }

    public void setCustomOptions(Map<String, Object> customOptions) {
        this.customOptions = customOptions;
    }
    
    public void addCustomOption(String key, Object value) {
        this.customOptions.put(key, value);
    }
} 