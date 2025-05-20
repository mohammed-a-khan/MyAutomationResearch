package com.cstestforge.codegen.model.event;

/**
 * Represents an event to capture a value from an element or variable
 */
public class CaptureEvent extends Event {
    public enum CaptureType {
        TEXT,
        ATTRIBUTE,
        VALUE,
        PROPERTY,
        COMPUTED_STYLE,
        SCREENSHOT,
        VARIABLE
    }
    
    private CaptureType captureType;
    private String targetSelector; // Element selector
    private String targetVariable; // Variable to capture from
    private String targetAttribute; // For attribute capture
    private String targetProperty; // For property capture
    private String resultVariable; // Variable to store result in
    private String expression; // Optional expression to evaluate
    
    public CaptureEvent() {
        super("capture", "Capture");
    }
    
    public CaptureEvent(CaptureType captureType, String targetSelector, String resultVariable) {
        this();
        this.captureType = captureType;
        this.targetSelector = targetSelector;
        this.resultVariable = resultVariable;
    }
    
    public CaptureEvent(CaptureType captureType, String targetSelector, String targetAttribute, String resultVariable) {
        this(captureType, targetSelector, resultVariable);
        this.targetAttribute = targetAttribute;
    }
    
    // Getters and Setters
    public CaptureType getCaptureType() {
        return captureType;
    }

    public void setCaptureType(CaptureType captureType) {
        this.captureType = captureType;
    }

    public String getTargetSelector() {
        return targetSelector;
    }

    public void setTargetSelector(String targetSelector) {
        this.targetSelector = targetSelector;
    }

    public String getTargetVariable() {
        return targetVariable;
    }

    public void setTargetVariable(String targetVariable) {
        this.targetVariable = targetVariable;
    }

    public String getTargetAttribute() {
        return targetAttribute;
    }

    public void setTargetAttribute(String targetAttribute) {
        this.targetAttribute = targetAttribute;
    }

    public String getTargetProperty() {
        return targetProperty;
    }

    public void setTargetProperty(String targetProperty) {
        this.targetProperty = targetProperty;
    }

    public String getResultVariable() {
        return resultVariable;
    }

    public void setResultVariable(String resultVariable) {
        this.resultVariable = resultVariable;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    @Override
    public String toCode(String language, String framework, int indentLevel) {
        // This will be implemented by the code generation service
        return null;
    }
} 