package com.cstestforge.export.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines the structure template for exported projects
 */
public class ProjectTemplate {
    private String name;
    private String framework;
    private String language;
    private String buildTool;
    private Map<String, String> configFiles;
    private List<String> directories;
    private Map<String, String> dependencies;
    
    /**
     * Default constructor
     */
    public ProjectTemplate() {
        this.configFiles = new HashMap<>();
        this.directories = new ArrayList<>();
        this.dependencies = new HashMap<>();
    }
    
    /**
     * Constructor with basic information
     * 
     * @param name Template name
     * @param framework Target framework
     * @param language Target language
     * @param buildTool Build tool to use
     */
    public ProjectTemplate(String name, String framework, String language, String buildTool) {
        this();
        this.name = name;
        this.framework = framework;
        this.language = language;
        this.buildTool = buildTool;
    }
    
    /**
     * Add a config file to the template
     * 
     * @param path Path to the config file within the project
     * @param templateContent Template content
     * @return This ProjectTemplate for chaining
     */
    public ProjectTemplate addConfigFile(String path, String templateContent) {
        configFiles.put(path, templateContent);
        return this;
    }
    
    /**
     * Add a directory to the template structure
     * 
     * @param path Directory path within the project
     * @return This ProjectTemplate for chaining
     */
    public ProjectTemplate addDirectory(String path) {
        directories.add(path);
        return this;
    }
    
    /**
     * Add a dependency to the template
     * 
     * @param name Dependency name
     * @param version Version specification
     * @return This ProjectTemplate for chaining
     */
    public ProjectTemplate addDependency(String name, String version) {
        dependencies.put(name, version);
        return this;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Map<String, String> getConfigFiles() {
        return configFiles;
    }

    public void setConfigFiles(Map<String, String> configFiles) {
        this.configFiles = configFiles;
    }

    public List<String> getDirectories() {
        return directories;
    }

    public void setDirectories(List<String> directories) {
        this.directories = directories;
    }

    public Map<String, String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Map<String, String> dependencies) {
        this.dependencies = dependencies;
    }
    
    /**
     * Get the main source directory based on language and framework
     * 
     * @return Path to main source directory
     */
    public String getMainSourceDirectory() {
        if ("java".equalsIgnoreCase(language)) {
            return "src/main/java";
        } else if ("typescript".equalsIgnoreCase(language)) {
            return "src/main/typescript";
        } else {
            return "src/main";
        }
    }
    
    /**
     * Get the test source directory based on language and framework
     * 
     * @return Path to test source directory
     */
    public String getTestSourceDirectory() {
        if ("java".equalsIgnoreCase(language)) {
            return "src/test/java";
        } else if ("typescript".equalsIgnoreCase(language)) {
            return "src/test";
        } else {
            return "src/test";
        }
    }
    
    /**
     * Get the resources directory
     * 
     * @param isTest Whether to get test resources
     * @return Path to resources directory
     */
    public String getResourcesDirectory(boolean isTest) {
        if (isTest) {
            return "src/test/resources";
        } else {
            return "src/main/resources";
        }
    }
} 