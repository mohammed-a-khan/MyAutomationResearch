package com.cstestforge.codegen.model.event;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a basic UI action event like click, input, etc.
 */
public class ActionEvent extends Event {
    public enum ActionType {
        CLICK,
        DOUBLE_CLICK,
        RIGHT_CLICK,
        HOVER,
        TYPE,
        CLEAR,
        SELECT,
        CHECK,
        UNCHECK,
        SUBMIT,
        SCROLL,
        DRAG_AND_DROP,
        FOCUS,
        BLUR,
        PRESS_KEY,
        WAIT,
        NAVIGATE,
        REFRESH,
        BACK,
        FORWARD,
        CLOSE,
        UPLOAD_FILE,
        EXECUTE_SCRIPT,
        SWITCH_FRAME,
        SWITCH_WINDOW,
        CUSTOM
    }
    
    private ActionType actionType;
    private String targetSelector;
    private String targetId;
    private String targetName;
    private String value; // For input, select, etc.
    private String customAction; // For custom actions
    private Map<String, Object> actionParams; // Additional parameters
    
    public ActionEvent() {
        super("action", "Action");
        this.actionParams = new HashMap<>();
    }
    
    public ActionEvent(ActionType actionType, String targetSelector) {
        this();
        this.actionType = actionType;
        this.targetSelector = targetSelector;
    }
    
    public ActionEvent(ActionType actionType, String targetSelector, String value) {
        this(actionType, targetSelector);
        this.value = value;
    }
    
    // Getters and Setters
    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public String getTargetSelector() {
        return targetSelector;
    }

    public void setTargetSelector(String targetSelector) {
        this.targetSelector = targetSelector;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getCustomAction() {
        return customAction;
    }

    public void setCustomAction(String customAction) {
        this.customAction = customAction;
    }

    public Map<String, Object> getActionParams() {
        return actionParams;
    }

    public void setActionParams(Map<String, Object> actionParams) {
        this.actionParams = actionParams;
    }
    
    public void addActionParam(String key, Object value) {
        this.actionParams.put(key, value);
    }
    
    public Object getActionParam(String key) {
        return this.actionParams.get(key);
    }

    @Override
    public String toCode(String language, String framework, int indentLevel) {
        // This will be implemented by the code generation service
        return null;
    }
} 