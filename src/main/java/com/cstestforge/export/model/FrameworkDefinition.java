package com.cstestforge.export.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines a supported test automation framework
 */
public class FrameworkDefinition {
    private String id;
    private String name;
    private String description;
    private String language;
    private List<String> supportedBuildTools;
    private boolean supportsBDD;
    private Map<String, String> configOptions;
    
    /**
     * Default constructor
     */
    public FrameworkDefinition() {
        this.supportedBuildTools = new ArrayList<>();
        this.configOptions = new HashMap<>();
    }
    
    /**
     * Constructor with primary fields
     * 
     * @param id Unique framework identifier
     * @param name Display name
     * @param description Framework description
     * @param language Primary language
     */
    public FrameworkDefinition(String id, String name, String description, String language) {
        this();
        this.id = id;
        this.name = name;
        this.description = description;
        this.language = language;
    }
    
    /**
     * Add a supported build tool
     * 
     * @param buildTool Build tool name
     * @return This FrameworkDefinition for chaining
     */
    public FrameworkDefinition addBuildTool(String buildTool) {
        supportedBuildTools.add(buildTool);
        return this;
    }
    
    /**
     * Add a configuration option
     * 
     * @param key Option key
     * @param defaultValue Default value
     * @return This FrameworkDefinition for chaining
     */
    public FrameworkDefinition addConfigOption(String key, String defaultValue) {
        configOptions.put(key, defaultValue);
        return this;
    }
    
    /**
     * Static factory method for Selenium Java framework
     * 
     * @return Selenium Java FrameworkDefinition
     */
    public static FrameworkDefinition createSeleniumJava() {
        return new FrameworkDefinition("selenium-java", "Selenium Java", 
                "Selenium-based Java tests with configurable runners", "java")
                .addBuildTool("maven")
                .addBuildTool("gradle")
                .setSupportsBDD(true)
                .addConfigOption("runner", "testng")
                .addConfigOption("parallel", "false")
                .addConfigOption("suiteName", "Generated Test Suite");
    }
    
    /**
     * Static factory method for Playwright TypeScript framework
     * 
     * @return Playwright TypeScript FrameworkDefinition
     */
    public static FrameworkDefinition createPlaywrightTypeScript() {
        return new FrameworkDefinition("playwright-typescript", "Playwright TypeScript", 
                "Playwright-based TypeScript tests using modern async patterns", "typescript")
                .addBuildTool("npm")
                .setSupportsBDD(true)
                .addConfigOption("reporter", "html")
                .addConfigOption("browser", "chromium")
                .addConfigOption("headless", "true");
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<String> getSupportedBuildTools() {
        return supportedBuildTools;
    }

    public void setSupportedBuildTools(List<String> supportedBuildTools) {
        this.supportedBuildTools = supportedBuildTools;
    }

    public boolean isSupportsBDD() {
        return supportsBDD;
    }

    public FrameworkDefinition setSupportsBDD(boolean supportsBDD) {
        this.supportsBDD = supportsBDD;
        return this;
    }

    public Map<String, String> getConfigOptions() {
        return configOptions;
    }

    public void setConfigOptions(Map<String, String> configOptions) {
        this.configOptions = configOptions;
    }
} 