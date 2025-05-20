package com.cstestforge.testing.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing a test step
 */
public class TestStep {
    private String id;
    private int sequence;
    private StepType type;
    private String target;
    private String value;
    private Map<String, String> attributes;
    
    /**
     * Default constructor
     */
    public TestStep() {
        this.attributes = new HashMap<>();
    }
    
    /**
     * Constructor with essential fields
     * 
     * @param sequence Step sequence number
     * @param type Type of step
     * @param target Target element selector
     * @param value Value for the step action
     */
    public TestStep(int sequence, StepType type, String target, String value) {
        this.sequence = sequence;
        this.type = type;
        this.target = target;
        this.value = value;
        this.attributes = new HashMap<>();
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public StepType getType() {
        return type;
    }

    public void setType(StepType type) {
        this.type = type;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }
    
    /**
     * Add an attribute to the step
     * 
     * @param key Attribute key
     * @param value Attribute value
     */
    public void addAttribute(String key, String value) {
        attributes.put(key, value);
    }
    
    /**
     * Enumeration of step types
     */
    public enum StepType {
        NAVIGATE,
        CLICK,
        TYPE,
        SELECT,
        ASSERT,
        WAIT,
        SCREENSHOT,
        CUSTOM
    }
} 