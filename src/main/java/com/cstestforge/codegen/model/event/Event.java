package com.cstestforge.codegen.model.event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for all test events
 */
public abstract class Event {
    private String id;
    private String type;
    private String name;
    private String description;
    private Map<String, Object> metadata;
    
    public Event() {
        this.id = UUID.randomUUID().toString();
        this.metadata = new HashMap<>();
    }
    
    public Event(String type, String name) {
        this();
        this.type = type;
        this.name = name;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
    
    /**
     * Method to be implemented by subclasses to convert the event to code
     * 
     * @param language the programming language to generate code for (e.g., java, typescript)
     * @param framework the testing framework to use (e.g., selenium-java, playwright-typescript)
     * @param indentLevel the level of indentation to use
     * @return the generated code as a string
     */
    public abstract String toCode(String language, String framework, int indentLevel);
} 