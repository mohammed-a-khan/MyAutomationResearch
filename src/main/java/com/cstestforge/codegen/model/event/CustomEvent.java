package com.cstestforge.codegen.model.event;

/**
 * Represents a custom event
 */
public class CustomEvent extends Event {
    
    private String code;
    private String language;
    
    public CustomEvent() {
        super("custom", "Custom Action");
    }
    
    public CustomEvent(String name, String code) {
        super("custom", name);
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    @Override
    public String toCode(String language, String framework, int indentLevel) {
        // For a custom event, we typically just return the custom code
        // with proper indentation
        StringBuilder sb = new StringBuilder();
        String indent = " ".repeat(indentLevel * 2);
        
        // Add a comment to indicate this is a custom action
        sb.append(indent).append("// Custom action: ").append(getName()).append("\n");
        
        // If code is provided, add it with proper indentation
        if (this.code != null && !this.code.isEmpty()) {
            for (String line : this.code.split("\n")) {
                sb.append(indent).append(line).append("\n");
            }
        }
        
        return sb.toString();
    }
} 