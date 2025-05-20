package com.cstestforge.project.model.test;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single step in a test case.
 */
public class TestStep {

    private String id;
    private String name;
    private String description;
    private int order;
    private TestStepType type;
    private Map<String, Object> parameters;
    private boolean disabled;
    private Map<String, Object> metadata;
    
    /**
     * Default constructor
     */
    public TestStep() {
        this.parameters = new HashMap<>();
        this.metadata = new HashMap<>();
    }
    
    /**
     * Constructor with basic properties
     * 
     * @param name Name of the step
     * @param type Type of the step
     * @param order Execution order
     */
    public TestStep(String name, TestStepType type, int order) {
        this();
        this.name = name;
        this.type = type;
        this.order = order;
    }
    
    /**
     * Get the unique identifier for this step
     * 
     * @return Step ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Set the unique identifier for this step
     * 
     * @param id Step ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get the name of this step
     * 
     * @return Step name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the name of this step
     * 
     * @param name Step name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get the description of this step
     * 
     * @return Step description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Set the description of this step
     * 
     * @param description Step description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Get the execution order of this step
     * 
     * @return Order number
     */
    public int getOrder() {
        return order;
    }
    
    /**
     * Set the execution order of this step
     * 
     * @param order Order number
     */
    public void setOrder(int order) {
        this.order = order;
    }
    
    /**
     * Get the type of this step
     * 
     * @return Step type
     */
    public TestStepType getType() {
        return type;
    }
    
    /**
     * Set the type of this step
     * 
     * @param type Step type
     */
    public void setType(TestStepType type) {
        this.type = type;
    }
    
    /**
     * Get the parameters for this step
     * 
     * @return Map of parameters
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    /**
     * Set the parameters for this step
     * 
     * @param parameters Map of parameters
     */
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
    
    /**
     * Check if this step is disabled
     * 
     * @return True if disabled
     */
    public boolean isDisabled() {
        return disabled;
    }
    
    /**
     * Set whether this step is disabled
     * 
     * @param disabled True to disable the step
     */
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
    
    /**
     * Get the metadata for this step
     * 
     * @return Map of metadata
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    /**
     * Set the metadata for this step
     * 
     * @param metadata Map of metadata
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    /**
     * Add a parameter to this step
     * 
     * @param key Parameter name
     * @param value Parameter value
     * @return This step instance for method chaining
     */
    public TestStep addParameter(String key, Object value) {
        if (this.parameters == null) {
            this.parameters = new HashMap<>();
        }
        this.parameters.put(key, value);
        return this;
    }
    
    /**
     * Add metadata to this step
     * 
     * @param key Metadata key
     * @param value Metadata value
     * @return This step instance for method chaining
     */
    public TestStep addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }
} 