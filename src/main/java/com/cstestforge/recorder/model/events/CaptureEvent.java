package com.cstestforge.recorder.model.events;

import com.cstestforge.recorder.model.ElementInfo;
import com.cstestforge.recorder.model.RecordedEvent;
import com.cstestforge.recorder.model.RecordedEventType;
import com.cstestforge.recorder.model.config.CaptureConfig;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Objects;

/**
 * Event representing a variable capture operation.
 * Captures data from elements, responses, or other sources into variables.
 */
@JsonTypeName("CAPTURE")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaptureEvent extends RecordedEvent {
    
    private CaptureConfig captureConfig;
    
    /**
     * Default constructor for serialization
     */
    public CaptureEvent() {
        super(RecordedEventType.CAPTURE);
        this.captureConfig = new CaptureConfig();
    }
    
    /**
     * Constructor with necessary capture information
     *
     * @param url Current URL where the capture is defined
     * @param captureConfig Capture configuration details
     */
    public CaptureEvent(String url, CaptureConfig captureConfig) {
        super(RecordedEventType.CAPTURE);
        setUrl(url);
        this.captureConfig = captureConfig;
    }
    
    /**
     * Get the capture configuration
     *
     * @return Capture configuration
     */
    public CaptureConfig getCaptureConfig() {
        return captureConfig;
    }
    
    /**
     * Set the capture configuration
     *
     * @param captureConfig Capture configuration
     */
    public void setCaptureConfig(CaptureConfig captureConfig) {
        this.captureConfig = captureConfig;
    }
    
    /**
     * Get the target element for this event
     *
     * @return The target element if this is an element capture, null otherwise
     */
    public ElementInfo getTargetElement() {
        if (captureConfig != null && captureConfig.getSource() == CaptureConfig.CaptureSource.ELEMENT) {
            return captureConfig.getTargetElement();
        }
        return null;
    }
    
    /**
     * Set the target element for this event
     *
     * @param element The target element
     */
    public void setTargetElement(ElementInfo element) {
        if (captureConfig == null) {
            this.captureConfig = new CaptureConfig();
        }
        captureConfig.setTargetElement(element);
        captureConfig.setSource(CaptureConfig.CaptureSource.ELEMENT);
    }
    
    /**
     * Validate the capture event data
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() throws IllegalArgumentException {
        if (captureConfig == null) {
            throw new IllegalArgumentException("Capture configuration is required");
        }
        
        captureConfig.validate();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid() {
        try {
            validate();
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toHumanReadableDescription() {
        if (captureConfig == null) {
            return "Invalid capture";
        }
        
        StringBuilder description = new StringBuilder("Capture ");
        
        switch (captureConfig.getSource()) {
            case ELEMENT:
                description.append("element ");
                if (captureConfig.getTargetElement() != null) {
                    description.append(captureConfig.getTargetElement().getBestSelector());
                } else {
                    description.append(captureConfig.getSelector() != null ? 
                        captureConfig.getSelector() : "unknown");
                }
                
                if (captureConfig.getMethod() == CaptureConfig.CaptureMethod.PROPERTY) {
                    description.append(".")
                        .append(captureConfig.getProperty() != null ? 
                            captureConfig.getProperty() : "textContent");
                } else if (captureConfig.getMethod() == CaptureConfig.CaptureMethod.ATTRIBUTE) {
                    description.append(" attribute '")
                        .append(captureConfig.getProperty() != null ? 
                            captureConfig.getProperty() : "")
                        .append("'");
                }
                break;
                
            case RESPONSE:
                description.append("response data using ");
                if (captureConfig.getMethod() == CaptureConfig.CaptureMethod.JSON_PATH) {
                    description.append("JSONPath expression '")
                        .append(captureConfig.getExpression() != null ? 
                            captureConfig.getExpression() : "")
                        .append("'");
                } else if (captureConfig.getMethod() == CaptureConfig.CaptureMethod.XPATH) {
                    description.append("XPath expression '")
                        .append(captureConfig.getExpression() != null ? 
                            captureConfig.getExpression() : "")
                        .append("'");
                }
                break;
                
            case JAVASCRIPT:
                description.append("JavaScript result from '")
                    .append(captureConfig.getExpression() != null ? 
                        captureConfig.getExpression() : "")
                    .append("'");
                break;
                
            case URL:
                description.append("current URL");
                if (captureConfig.getMethod() == CaptureConfig.CaptureMethod.REGEX) {
                    description.append(" using regex '")
                        .append(captureConfig.getExpression() != null ? 
                            captureConfig.getExpression() : "")
                        .append("'");
                }
                break;
                
            case COOKIE:
                description.append("cookie '")
                    .append(captureConfig.getProperty() != null ? 
                        captureConfig.getProperty() : "")
                    .append("'");
                break;
                
            case STORAGE:
                description.append("storage item '")
                    .append(captureConfig.getProperty() != null ? 
                        captureConfig.getProperty() : "")
                    .append("'");
                break;
                
            default:
                description.append("unknown source");
        }
        
        description.append(" into variable ")
            .append(captureConfig.isGlobal() ? "global." : "")
            .append(captureConfig.getVariableName() != null ? 
                captureConfig.getVariableName() : "undefined");
        
        return description.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CaptureEvent that = (CaptureEvent) o;
        return Objects.equals(captureConfig, that.captureConfig);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), captureConfig);
    }
} 