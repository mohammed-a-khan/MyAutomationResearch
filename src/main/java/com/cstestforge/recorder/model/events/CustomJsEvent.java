package com.cstestforge.recorder.model.events;

import com.cstestforge.recorder.model.ElementInfo;
import com.cstestforge.recorder.model.RecordedEvent;
import com.cstestforge.recorder.model.RecordedEventType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Event representing custom JavaScript execution in the recording.
 * Allows for executing arbitrary JavaScript code with variable access.
 */
@JsonTypeName("CUSTOM_JS")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomJsEvent extends RecordedEvent {
    
    private String script;
    private boolean async;
    private Integer timeout;
    private List<String> returnVariables;
    private boolean runInIsolation;
    private String description;
    private boolean useElementContext;
    private ElementInfo contextElement;
    
    /**
     * Default constructor for serialization
     */
    public CustomJsEvent() {
        super(RecordedEventType.CUSTOM_JS);
        this.returnVariables = new ArrayList<>();
        this.async = false;
        this.timeout = 30000;
        this.runInIsolation = false;
        this.useElementContext = false;
    }
    
    /**
     * Constructor with necessary JavaScript information
     *
     * @param url Current URL where the script will run
     * @param script JavaScript code to execute
     * @param description Human-readable description of what the script does
     */
    public CustomJsEvent(String url, String script, String description) {
        super(RecordedEventType.CUSTOM_JS);
        setUrl(url);
        this.script = script;
        this.description = description;
        this.returnVariables = new ArrayList<>();
        this.async = false;
        this.timeout = 30000;
        this.runInIsolation = false;
        this.useElementContext = false;
    }
    
    /**
     * Get the JavaScript code
     *
     * @return The script code
     */
    public String getScript() {
        return script;
    }
    
    /**
     * Set the JavaScript code
     *
     * @param script The script code
     */
    public void setScript(String script) {
        this.script = script;
    }
    
    /**
     * Check if the script should run asynchronously
     *
     * @return True if async, false if synchronous
     */
    public boolean isAsync() {
        return async;
    }
    
    /**
     * Set whether the script should run asynchronously
     *
     * @param async True for async, false for synchronous
     */
    public void setAsync(boolean async) {
        this.async = async;
    }
    
    /**
     * Get the script execution timeout in milliseconds
     *
     * @return The timeout value
     */
    public Integer getTimeout() {
        return timeout;
    }
    
    /**
     * Set the script execution timeout in milliseconds
     *
     * @param timeout The timeout value
     */
    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }
    
    /**
     * Get the variables to extract from the script result
     *
     * @return List of variable names
     */
    public List<String> getReturnVariables() {
        return returnVariables;
    }
    
    /**
     * Set the variables to extract from the script result
     *
     * @param returnVariables List of variable names
     */
    public void setReturnVariables(List<String> returnVariables) {
        this.returnVariables = returnVariables != null ? returnVariables : new ArrayList<>();
    }
    
    /**
     * Add a return variable name
     *
     * @param variableName Variable name to add
     */
    public void addReturnVariable(String variableName) {
        if (this.returnVariables == null) {
            this.returnVariables = new ArrayList<>();
        }
        this.returnVariables.add(variableName);
    }
    
    /**
     * Check if the script runs in isolation (separate scope)
     *
     * @return True if isolated, false otherwise
     */
    public boolean isRunInIsolation() {
        return runInIsolation;
    }
    
    /**
     * Set whether the script runs in isolation (separate scope)
     *
     * @param runInIsolation True for isolation, false otherwise
     */
    public void setRunInIsolation(boolean runInIsolation) {
        this.runInIsolation = runInIsolation;
    }
    
    /**
     * Get the human-readable description
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Set the human-readable description
     *
     * @param description The description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Check if the script uses an element as context
     *
     * @return True if using element context, false otherwise
     */
    public boolean isUseElementContext() {
        return useElementContext;
    }
    
    /**
     * Set whether the script uses an element as context
     *
     * @param useElementContext True to use element context, false otherwise
     */
    public void setUseElementContext(boolean useElementContext) {
        this.useElementContext = useElementContext;
    }
    
    /**
     * Get the context element
     *
     * @return The context element
     */
    public ElementInfo getContextElement() {
        return contextElement;
    }
    
    /**
     * Set the context element
     *
     * @param contextElement The context element
     */
    public void setContextElement(ElementInfo contextElement) {
        this.contextElement = contextElement;
        if (contextElement != null) {
            this.useElementContext = true;
        }
    }
    
    /**
     * Get the target element for this event
     *
     * @return The target element (context element if using element context, null otherwise)
     */
    public ElementInfo getTargetElement() {
        return useElementContext ? contextElement : null;
    }
    
    /**
     * Validate the custom JavaScript event data
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() throws IllegalArgumentException {
        if (script == null || script.trim().isEmpty()) {
            throw new IllegalArgumentException("JavaScript code is required");
        }
        
        if (timeout != null && timeout <= 0) {
            throw new IllegalArgumentException("Timeout must be a positive number");
        }
        
        if (useElementContext && contextElement == null) {
            throw new IllegalArgumentException("Context element is required when using element context");
        }
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
        StringBuilder desc = new StringBuilder("Execute custom JavaScript");
        
        if (description != null && !description.trim().isEmpty()) {
            desc.append(": ").append(description);
        }
        
        if (useElementContext && contextElement != null) {
            desc.append(" on element ").append(contextElement.getBestSelector());
        }
        
        if (!returnVariables.isEmpty()) {
            desc.append(", returning ");
            desc.append(String.join(", ", returnVariables));
        }
        
        if (async) {
            desc.append(" (async)");
        }
        
        return desc.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CustomJsEvent that = (CustomJsEvent) o;
        return async == that.async &&
               runInIsolation == that.runInIsolation &&
               useElementContext == that.useElementContext &&
               Objects.equals(script, that.script) &&
               Objects.equals(timeout, that.timeout) &&
               Objects.equals(description, that.description);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), script, async, timeout, runInIsolation, description, useElementContext);
    }
} 