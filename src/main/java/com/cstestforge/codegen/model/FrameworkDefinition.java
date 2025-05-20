package com.cstestforge.codegen.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a supported test automation framework definition
 */
public class FrameworkDefinition {
    private String id; // e.g., selenium-java, playwright-typescript
    private String name; // e.g., "Selenium Java", "Playwright TypeScript"
    private String language; // e.g., java, typescript
    private String version;
    private String basePackage; // e.g., com.cstestforge.framework.selenium
    private List<String> supportedTestTypes; // e.g., unit, integration, functional, UI
    private Map<String, Object> capabilities; // Framework-specific capabilities
    private List<String> dependencies; // List of required dependencies
    
    public FrameworkDefinition() {
        this.supportedTestTypes = new ArrayList<>();
        this.capabilities = new HashMap<>();
        this.dependencies = new ArrayList<>();
    }
    
    public FrameworkDefinition(String id, String name, String language, String version) {
        this();
        this.id = id;
        this.name = name;
        this.language = language;
        this.version = version;
    }
    
    // Getters and Setters
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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public List<String> getSupportedTestTypes() {
        return supportedTestTypes;
    }

    public void setSupportedTestTypes(List<String> supportedTestTypes) {
        this.supportedTestTypes = supportedTestTypes;
    }
    
    public void addSupportedTestType(String testType) {
        this.supportedTestTypes.add(testType);
    }

    public Map<String, Object> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Map<String, Object> capabilities) {
        this.capabilities = capabilities;
    }
    
    public void addCapability(String key, Object value) {
        this.capabilities.put(key, value);
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }
    
    public void addDependency(String dependency) {
        this.dependencies.add(dependency);
    }
} 