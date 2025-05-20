package com.cstestforge.recorder.model.config;

import com.cstestforge.recorder.model.ElementInfo;

import java.util.Objects;
import java.util.UUID;

/**
 * Configuration for capturing data from elements or responses into variables.
 * Supports different capture sources and methods.
 */
public class CaptureConfig {
    
    private String id;
    private String variableName;
    private CaptureSource source;
    private ElementInfo targetElement;
    private String property;
    private String selector;
    private String expression;
    private CaptureMethod method;
    private String format;
    private String defaultValue;
    private boolean isGlobal;
    private String transform;
    
    /**
     * Default constructor
     */
    public CaptureConfig() {
        this.id = UUID.randomUUID().toString();
        this.source = CaptureSource.ELEMENT;
        this.method = CaptureMethod.PROPERTY;
        this.isGlobal = false;
        this.property = "textContent";
    }
    
    /**
     * Constructor for element property capture
     *
     * @param variableName The variable name to store the captured value
     * @param targetElement The target element
     * @param property The element property to capture
     */
    public CaptureConfig(String variableName, ElementInfo targetElement, String property) {
        this();
        this.variableName = variableName;
        this.targetElement = targetElement;
        this.property = property;
        this.source = CaptureSource.ELEMENT;
        this.method = CaptureMethod.PROPERTY;
    }
    
    /**
     * Constructor for response capture
     *
     * @param variableName The variable name to store the captured value
     * @param expression The JSON path or XPath expression
     * @param method The capture method (JSON_PATH or XPATH)
     */
    public CaptureConfig(String variableName, String expression, CaptureMethod method) {
        this();
        this.variableName = variableName;
        this.expression = expression;
        this.method = method;
        this.source = CaptureSource.RESPONSE;
    }
    
    /**
     * Get the capture ID
     *
     * @return The capture ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Set the capture ID
     *
     * @param id The capture ID
     */
    public void setId(String id) {
        this.id = id;
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
     * Get the capture source
     *
     * @return The capture source
     */
    public CaptureSource getSource() {
        return source;
    }
    
    /**
     * Set the capture source
     *
     * @param source The capture source
     */
    public void setSource(CaptureSource source) {
        this.source = source;
    }
    
    /**
     * Get the target element
     *
     * @return The target element
     */
    public ElementInfo getTargetElement() {
        return targetElement;
    }
    
    /**
     * Set the target element
     *
     * @param targetElement The target element
     */
    public void setTargetElement(ElementInfo targetElement) {
        this.targetElement = targetElement;
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
     * Get the CSS selector
     *
     * @return The CSS selector
     */
    public String getSelector() {
        return selector;
    }
    
    /**
     * Set the CSS selector
     *
     * @param selector The CSS selector
     */
    public void setSelector(String selector) {
        this.selector = selector;
    }
    
    /**
     * Get the JSON path or XPath expression
     *
     * @return The expression
     */
    public String getExpression() {
        return expression;
    }
    
    /**
     * Set the JSON path or XPath expression
     *
     * @param expression The expression
     */
    public void setExpression(String expression) {
        this.expression = expression;
    }
    
    /**
     * Get the capture method
     *
     * @return The capture method
     */
    public CaptureMethod getMethod() {
        return method;
    }
    
    /**
     * Set the capture method
     *
     * @param method The capture method
     */
    public void setMethod(CaptureMethod method) {
        this.method = method;
    }
    
    /**
     * Get the format string
     *
     * @return The format string
     */
    public String getFormat() {
        return format;
    }
    
    /**
     * Set the format string
     *
     * @param format The format string
     */
    public void setFormat(String format) {
        this.format = format;
    }
    
    /**
     * Get the default value
     *
     * @return The default value
     */
    public String getDefaultValue() {
        return defaultValue;
    }
    
    /**
     * Set the default value
     *
     * @param defaultValue The default value
     */
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    /**
     * Check if the variable is global
     *
     * @return True if global, false if local
     */
    public boolean isGlobal() {
        return isGlobal;
    }
    
    /**
     * Set whether the variable is global
     *
     * @param global True if global, false if local
     */
    public void setGlobal(boolean global) {
        isGlobal = global;
    }
    
    /**
     * Get the transform JavaScript code
     *
     * @return The transform code
     */
    public String getTransform() {
        return transform;
    }
    
    /**
     * Set the transform JavaScript code
     *
     * @param transform The transform code
     */
    public void setTransform(String transform) {
        this.transform = transform;
    }
    
    /**
     * Validate the capture configuration
     *
     * @throws IllegalArgumentException if the configuration is invalid
     */
    public void validate() throws IllegalArgumentException {
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new IllegalArgumentException("Variable name is required");
        }
        
        if (source == null) {
            throw new IllegalArgumentException("Capture source is required");
        }
        
        if (method == null) {
            throw new IllegalArgumentException("Capture method is required");
        }
        
        switch (source) {
            case ELEMENT:
                if (method == CaptureMethod.PROPERTY) {
                    if (property == null || property.trim().isEmpty()) {
                        throw new IllegalArgumentException("Property is required for element property capture");
                    }
                }
                
                if (targetElement == null && (selector == null || selector.trim().isEmpty())) {
                    throw new IllegalArgumentException("Either target element or selector is required for element capture");
                }
                break;
                
            case RESPONSE:
                if (method == CaptureMethod.JSON_PATH || method == CaptureMethod.XPATH) {
                    if (expression == null || expression.trim().isEmpty()) {
                        throw new IllegalArgumentException("Expression is required for response capture");
                    }
                }
                break;
                
            case JAVASCRIPT:
                if (expression == null || expression.trim().isEmpty()) {
                    throw new IllegalArgumentException("JavaScript expression is required for JavaScript capture");
                }
                break;
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CaptureConfig that = (CaptureConfig) o;
        return isGlobal == that.isGlobal &&
               Objects.equals(id, that.id) &&
               Objects.equals(variableName, that.variableName) &&
               source == that.source &&
               method == that.method;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, variableName, source, method, isGlobal);
    }
    
    /**
     * Source of the captured data
     */
    public enum CaptureSource {
        ELEMENT,    // Capture from a DOM element
        RESPONSE,   // Capture from an API response or network response
        JAVASCRIPT, // Capture using JavaScript execution
        URL,        // Capture from the current URL
        COOKIE,     // Capture from a cookie
        STORAGE     // Capture from local or session storage
    }
    
    /**
     * Method used to capture data
     */
    public enum CaptureMethod {
        PROPERTY,   // Capture an element property
        ATTRIBUTE,  // Capture an element attribute
        INNER_TEXT, // Capture the inner text
        INNER_HTML, // Capture the inner HTML
        TEXT_CONTENT, // Capture the textContent
        JSON_PATH,  // Capture using JSON path
        XPATH,      // Capture using XPath
        REGEX,      // Capture using regular expression
        JAVASCRIPT  // Capture using JavaScript execution
    }
} 