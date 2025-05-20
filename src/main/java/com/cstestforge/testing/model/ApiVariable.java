package com.cstestforge.testing.model;

/**
 * Represents a variable that can be used in API requests for dynamic values
 */
public class ApiVariable {
    
    private String id;
    private String name;
    private String value;
    private String description;
    private String source;
    private String sourceProperty;
    private boolean enabled;
    
    /**
     * Default constructor
     */
    public ApiVariable() {
        this.enabled = true;
    }
    
    /**
     * Constructor with essential fields
     * 
     * @param id Unique variable ID
     * @param name Variable name
     * @param value Variable value
     */
    public ApiVariable(String id, String name, String value) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.enabled = true;
    }
    
    /**
     * Constructor with all fields
     * 
     * @param id Unique variable ID
     * @param name Variable name
     * @param value Variable value
     * @param description Variable description
     * @param source Source of variable (static, previous_response, environment)
     * @param sourceProperty Property path when source is previous_response
     * @param enabled Whether this variable is enabled
     */
    public ApiVariable(String id, String name, String value, String description, String source, String sourceProperty, boolean enabled) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.description = description;
        this.source = source;
        this.sourceProperty = sourceProperty;
        this.enabled = enabled;
    }
    
    /**
     * Get the unique ID for this variable
     * 
     * @return Variable ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Set the unique ID for this variable
     * 
     * @param id Variable ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get the variable name
     * 
     * @return Variable name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the variable name
     * 
     * @param name Variable name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get the variable value
     * 
     * @return Variable value
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Set the variable value
     * 
     * @param value Variable value
     */
    public void setValue(String value) {
        this.value = value;
    }
    
    /**
     * Get the variable description
     * 
     * @return Variable description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Set the variable description
     * 
     * @param description Variable description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Get the variable source
     * 
     * @return Variable source (static, previous_response, environment)
     */
    public String getSource() {
        return source;
    }
    
    /**
     * Set the variable source
     * 
     * @param source Variable source
     */
    public void setSource(String source) {
        this.source = source;
    }
    
    /**
     * Get the source property path
     * 
     * @return Source property path when source is previous_response
     */
    public String getSourceProperty() {
        return sourceProperty;
    }
    
    /**
     * Set the source property path
     * 
     * @param sourceProperty Source property path
     */
    public void setSourceProperty(String sourceProperty) {
        this.sourceProperty = sourceProperty;
    }
    
    /**
     * Check if this variable is enabled
     * 
     * @return True if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Set whether this variable is enabled
     * 
     * @param enabled True to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
} 