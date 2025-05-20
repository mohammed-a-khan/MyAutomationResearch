package com.cstestforge.project.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a testing environment for a project.
 */
public class Environment {
    private String id;
    private String name;
    private String url;
    private String description;
    private boolean isDefault;
    private List<EnvironmentVariable> variables;

    public Environment() {
        this.id = UUID.randomUUID().toString();
        this.variables = new ArrayList<>();
    }

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public List<EnvironmentVariable> getVariables() {
        return variables;
    }

    public void setVariables(List<EnvironmentVariable> variables) {
        this.variables = variables;
    }

    /**
     * Adds a new variable to the environment
     * 
     * @param variable The variable to add
     */
    public void addVariable(EnvironmentVariable variable) {
        this.variables.add(variable);
    }

    /**
     * Removes a variable from the environment
     * 
     * @param variableId ID of the variable to remove
     * @return true if variable was removed, false if not found
     */
    public boolean removeVariable(String variableId) {
        return this.variables.removeIf(v -> v.getId().equals(variableId));
    }

    /**
     * Finds a variable by ID
     * 
     * @param variableId ID of the variable to find
     * @return The variable or null if not found
     */
    public EnvironmentVariable findVariable(String variableId) {
        return this.variables.stream()
                .filter(v -> v.getId().equals(variableId))
                .findFirst()
                .orElse(null);
    }
} 