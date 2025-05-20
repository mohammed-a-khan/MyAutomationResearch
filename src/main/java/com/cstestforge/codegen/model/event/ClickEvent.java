package com.cstestforge.codegen.model.event;

/**
 * Represents a click action event
 */
public class ClickEvent extends ActionEvent {
    
    public ClickEvent() {
        super();
        setActionType(ActionType.CLICK);
        setName("Click");
    }
    
    public ClickEvent(String targetSelector) {
        this();
        setTargetSelector(targetSelector);
    }
    
    @Override
    public String toCode(String language, String framework, int indentLevel) {
        // Implementation will be provided by the code generator
        return null;
    }
} 