package com.cstestforge.codegen.model.event;

/**
 * Represents a navigation action event
 */
public class NavigationEvent extends ActionEvent {
    
    private String url;
    
    public NavigationEvent() {
        super();
        setActionType(ActionType.NAVIGATE);
        setName("Navigation");
    }
    
    public NavigationEvent(String url) {
        this();
        this.url = url;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    @Override
    public String toCode(String language, String framework, int indentLevel) {
        // Implementation will be provided by the code generator
        return null;
    }
} 