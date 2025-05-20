package com.cstestforge.recorder.model.config;

import com.cstestforge.recorder.model.ElementInfo;

import java.util.Objects;

/**
 * Represents an operand in a condition.
 * Can be a literal value, variable reference, or element property.
 */
public class OperandConfig {
    
    private OperandType type;
    private String value;
    private ElementInfo elementInfo;
    private String property;
    private String variableName;
    
    /**
     * Default constructor
     */
    public OperandConfig() {
        this.type = OperandType.LITERAL;
    }
    
    /**
     * Constructor for literal values
     *
     * @param value The literal value
     */
    public OperandConfig(String value) {
        this.type = OperandType.LITERAL;
        this.value = value;
    }
    
    /**
     * Constructor for variable references
     *
     * @param variableName The variable name
     * @param isVariable Flag to indicate this is a variable
     */
    public OperandConfig(String variableName, boolean isVariable) {
        this.type = OperandType.VARIABLE;
        this.variableName = variableName;
    }
    
    /**
     * Constructor for element properties
     *
     * @param elementInfo The element info
     * @param property The element property to access
     */
    public OperandConfig(ElementInfo elementInfo, String property) {
        this.type = OperandType.ELEMENT;
        this.elementInfo = elementInfo;
        this.property = property;
    }
    
    /**
     * Get the operand type
     *
     * @return The operand type
     */
    public OperandType getType() {
        return type;
    }
    
    /**
     * Set the operand type
     *
     * @param type The operand type
     */
    public void setType(OperandType type) {
        this.type = type;
    }
    
    /**
     * Get the literal value
     *
     * @return The literal value
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Set the literal value
     *
     * @param value The literal value
     */
    public void setValue(String value) {
        this.value = value;
    }
    
    /**
     * Get the element info
     *
     * @return The element info
     */
    public ElementInfo getElementInfo() {
        return elementInfo;
    }
    
    /**
     * Set the element info
     *
     * @param elementInfo The element info
     */
    public void setElementInfo(ElementInfo elementInfo) {
        this.elementInfo = elementInfo;
    }
    
    /**
     * Get the element property
     *
     * @return The element property
     */
    public String getProperty() {
        return property;
    }
    
    /**
     * Set the element property
     *
     * @param property The element property
     */
    public void setProperty(String property) {
        this.property = property;
    }
    
    /**
     * Get the variable name
     *
     * @return The variable name
     */
    public String getVariableName() {
        return variableName;
    }
    
    /**
     * Set the variable name
     *
     * @param variableName The variable name
     */
    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }
    
    /**
     * Get a description of the operand
     *
     * @return Human-readable description of the operand
     */
    public String getDescription() {
        switch (type) {
            case LITERAL:
                return "\"" + value + "\"";
            case VARIABLE:
                return "${" + variableName + "}";
            case ELEMENT:
                if (elementInfo == null) {
                    return "Unknown element";
                }
                if (property == null || property.isEmpty()) {
                    return "Element " + elementInfo.getBestSelector();
                } else {
                    return "Element " + elementInfo.getBestSelector() + "." + property;
                }
            default:
                return "Unknown operand";
        }
    }
    
    /**
     * Create a literal operand
     *
     * @param value The literal value
     * @return A new operand config
     */
    public static OperandConfig literal(String value) {
        OperandConfig config = new OperandConfig();
        config.setType(OperandType.LITERAL);
        config.setValue(value);
        return config;
    }
    
    /**
     * Create a variable operand
     *
     * @param variableName The variable name
     * @return A new operand config
     */
    public static OperandConfig variable(String variableName) {
        OperandConfig config = new OperandConfig();
        config.setType(OperandType.VARIABLE);
        config.setVariableName(variableName);
        return config;
    }
    
    /**
     * Create an element operand
     *
     * @param elementInfo The element info
     * @param property The element property
     * @return A new operand config
     */
    public static OperandConfig element(ElementInfo elementInfo, String property) {
        OperandConfig config = new OperandConfig();
        config.setType(OperandType.ELEMENT);
        config.setElementInfo(elementInfo);
        config.setProperty(property);
        return config;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OperandConfig that = (OperandConfig) o;
        return type == that.type &&
               Objects.equals(value, that.value) &&
               Objects.equals(elementInfo, that.elementInfo) &&
               Objects.equals(property, that.property) &&
               Objects.equals(variableName, that.variableName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(type, value, elementInfo, property, variableName);
    }
    
    /**
     * Types of operands
     */
    public enum OperandType {
        LITERAL,
        VARIABLE,
        ELEMENT
    }
} 