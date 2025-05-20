package com.cstestforge.project.model;

import java.util.UUID;

/**
 * Represents a variable within an environment.
 */
public class EnvironmentVariable {
    private String id;
    private String name;
    private String value;
    private boolean isSecret;

    public EnvironmentVariable() {
        this.id = UUID.randomUUID().toString();
    }

    public EnvironmentVariable(String name, String value, boolean isSecret) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.value = value;
        this.isSecret = isSecret;
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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isSecret() {
        return isSecret;
    }

    public void setSecret(boolean isSecret) {
        this.isSecret = isSecret;
    }
} 