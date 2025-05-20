package com.cstestforge.codegen.model.event;

/**
 * Represents an input action event
 */
public class InputEvent extends ActionEvent {
    
    public InputEvent() {
        super();
        setActionType(ActionType.TYPE);
        setName("Input");
    }
    
    public InputEvent(String targetSelector, String value) {
        this();
        setTargetSelector(targetSelector);
        setValue(value);
    }
    
    @Override
    public String toCode(String language, String framework, int indentLevel) {
        // Implementation will be provided by the code generator
        return null;
    }
} 