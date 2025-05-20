package com.cstestforge.codegen.model.event;

/**
 * Represents a wait action event
 */
public class WaitEvent extends ActionEvent {
    
    private int timeout; // in milliseconds
    private WaitType waitType;
    private String condition;
    
    public enum WaitType {
        FIXED_TIME,
        ELEMENT_VISIBLE,
        ELEMENT_INVISIBLE,
        ELEMENT_PRESENT,
        ELEMENT_NOT_PRESENT,
        ELEMENT_CLICKABLE,
        CUSTOM_CONDITION
    }
    
    public WaitEvent() {
        super();
        setActionType(ActionType.WAIT);
        setName("Wait");
        this.waitType = WaitType.FIXED_TIME;
        this.timeout = 1000; // Default 1 second
    }
    
    public WaitEvent(int timeout) {
        this();
        this.timeout = timeout;
    }
    
    public WaitEvent(WaitType waitType, String targetSelector, int timeout) {
        this();
        this.waitType = waitType;
        setTargetSelector(targetSelector);
        this.timeout = timeout;
    }
    
    public int getTimeout() {
        return timeout;
    }
    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    public WaitType getWaitType() {
        return waitType;
    }
    
    public void setWaitType(WaitType waitType) {
        this.waitType = waitType;
    }
    
    public String getCondition() {
        return condition;
    }
    
    public void setCondition(String condition) {
        this.condition = condition;
    }
    
    @Override
    public String toCode(String language, String framework, int indentLevel) {
        // Implementation will be provided by the code generator
        return null;
    }
} 