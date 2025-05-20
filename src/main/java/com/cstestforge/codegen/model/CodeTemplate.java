package com.cstestforge.codegen.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a code template for generating code for a specific framework and language
 */
public class CodeTemplate {
    private String id;
    private String name;
    private String framework; // selenium-java, playwright-typescript
    private String language; // java, typescript
    private String content;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Default constructor
    public CodeTemplate() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Constructor with basic fields
    public CodeTemplate(String name, String framework, String language, String content, String description) {
        this();
        this.name = name;
        this.framework = framework;
        this.language = language;
        this.content = content;
        this.description = description;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * Updates the updatedAt timestamp to current time
     */
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
} 