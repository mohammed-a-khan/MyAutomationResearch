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
 * Event representing a try-catch-finally structure in the recording.
 * Provides error handling for test steps.
 */
@JsonTypeName("TRY_CATCH")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TryCatchEvent extends RecordedEvent {
    
    private List<RecordedEvent> tryEvents;
    private List<RecordedEvent> catchEvents;
    private List<RecordedEvent> finallyEvents;
    private String errorVariableName;
    private List<String> catchErrorTypes;
    private boolean continueOnError;
    private boolean logError;
    
    /**
     * Default constructor for serialization
     */
    public TryCatchEvent() {
        super(RecordedEventType.TRY_CATCH);
        this.tryEvents = new ArrayList<>();
        this.catchEvents = new ArrayList<>();
        this.finallyEvents = new ArrayList<>();
        this.catchErrorTypes = new ArrayList<>();
        this.errorVariableName = "error";
        this.continueOnError = false;
        this.logError = true;
    }
    
    /**
     * Constructor with necessary try-catch information
     *
     * @param url Current URL where the try-catch is defined
     * @param errorVariableName Name of the variable to store error information
     */
    public TryCatchEvent(String url, String errorVariableName) {
        super(RecordedEventType.TRY_CATCH);
        setUrl(url);
        this.errorVariableName = errorVariableName;
        this.tryEvents = new ArrayList<>();
        this.catchEvents = new ArrayList<>();
        this.finallyEvents = new ArrayList<>();
        this.catchErrorTypes = new ArrayList<>();
        this.continueOnError = false;
        this.logError = true;
    }
    
    /**
     * Get the events in the try block
     *
     * @return List of events in the try block
     */
    public List<RecordedEvent> getTryEvents() {
        return tryEvents;
    }
    
    /**
     * Set the events in the try block
     *
     * @param tryEvents List of events in the try block
     */
    public void setTryEvents(List<RecordedEvent> tryEvents) {
        this.tryEvents = tryEvents != null ? tryEvents : new ArrayList<>();
    }
    
    /**
     * Get the events in the catch block
     *
     * @return List of events in the catch block
     */
    public List<RecordedEvent> getCatchEvents() {
        return catchEvents;
    }
    
    /**
     * Set the events in the catch block
     *
     * @param catchEvents List of events in the catch block
     */
    public void setCatchEvents(List<RecordedEvent> catchEvents) {
        this.catchEvents = catchEvents != null ? catchEvents : new ArrayList<>();
    }
    
    /**
     * Get the events in the finally block
     *
     * @return List of events in the finally block
     */
    public List<RecordedEvent> getFinallyEvents() {
        return finallyEvents;
    }
    
    /**
     * Set the events in the finally block
     *
     * @param finallyEvents List of events in the finally block
     */
    public void setFinallyEvents(List<RecordedEvent> finallyEvents) {
        this.finallyEvents = finallyEvents != null ? finallyEvents : new ArrayList<>();
    }
    
    /**
     * Add an event to the try block
     *
     * @param event Event to add
     */
    public void addTryEvent(RecordedEvent event) {
        if (this.tryEvents == null) {
            this.tryEvents = new ArrayList<>();
        }
        this.tryEvents.add(event);
    }
    
    /**
     * Remove an event from the try block
     *
     * @param event Event to remove
     * @return True if the event was removed
     */
    public boolean removeTryEvent(RecordedEvent event) {
        if (this.tryEvents == null) {
            return false;
        }
        return this.tryEvents.remove(event);
    }
    
    /**
     * Add an event to the catch block
     *
     * @param event Event to add
     */
    public void addCatchEvent(RecordedEvent event) {
        if (this.catchEvents == null) {
            this.catchEvents = new ArrayList<>();
        }
        this.catchEvents.add(event);
    }
    
    /**
     * Remove an event from the catch block
     *
     * @param event Event to remove
     * @return True if the event was removed
     */
    public boolean removeCatchEvent(RecordedEvent event) {
        if (this.catchEvents == null) {
            return false;
        }
        return this.catchEvents.remove(event);
    }
    
    /**
     * Add an event to the finally block
     *
     * @param event Event to add
     */
    public void addFinallyEvent(RecordedEvent event) {
        if (this.finallyEvents == null) {
            this.finallyEvents = new ArrayList<>();
        }
        this.finallyEvents.add(event);
    }
    
    /**
     * Remove an event from the finally block
     *
     * @param event Event to remove
     * @return True if the event was removed
     */
    public boolean removeFinallyEvent(RecordedEvent event) {
        if (this.finallyEvents == null) {
            return false;
        }
        return this.finallyEvents.remove(event);
    }
    
    /**
     * Get the error variable name
     *
     * @return Name of the variable that will hold error information
     */
    public String getErrorVariableName() {
        return errorVariableName;
    }
    
    /**
     * Set the error variable name
     *
     * @param errorVariableName Name of the variable that will hold error information
     */
    public void setErrorVariableName(String errorVariableName) {
        this.errorVariableName = errorVariableName;
    }
    
    /**
     * Get the error types to catch
     *
     * @return List of error type names to catch
     */
    public List<String> getCatchErrorTypes() {
        return catchErrorTypes;
    }
    
    /**
     * Set the error types to catch
     *
     * @param catchErrorTypes List of error type names to catch
     */
    public void setCatchErrorTypes(List<String> catchErrorTypes) {
        this.catchErrorTypes = catchErrorTypes != null ? catchErrorTypes : new ArrayList<>();
    }
    
    /**
     * Add an error type to catch
     *
     * @param errorType Error type to catch
     */
    public void addCatchErrorType(String errorType) {
        if (this.catchErrorTypes == null) {
            this.catchErrorTypes = new ArrayList<>();
        }
        this.catchErrorTypes.add(errorType);
    }
    
    /**
     * Check if execution should continue after an error
     *
     * @return True if execution continues after error, false otherwise
     */
    public boolean isContinueOnError() {
        return continueOnError;
    }
    
    /**
     * Set whether execution should continue after an error
     *
     * @param continueOnError True to continue after error, false to stop
     */
    public void setContinueOnError(boolean continueOnError) {
        this.continueOnError = continueOnError;
    }
    
    /**
     * Check if errors should be logged
     *
     * @return True if errors are logged, false otherwise
     */
    public boolean isLogError() {
        return logError;
    }
    
    /**
     * Set whether errors should be logged
     *
     * @param logError True to log errors, false otherwise
     */
    public void setLogError(boolean logError) {
        this.logError = logError;
    }
    
    /**
     * Get the target element for this event
     *
     * @return The target element (null for try-catch events)
     */
    public ElementInfo getTargetElement() {
        // Try-catch events don't have a direct target element
        return null;
    }
    
    /**
     * Validate the try-catch event data
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() throws IllegalArgumentException {
        if (tryEvents == null || tryEvents.isEmpty()) {
            throw new IllegalArgumentException("Try block must contain at least one event");
        }
        
        if (errorVariableName == null || errorVariableName.trim().isEmpty()) {
            throw new IllegalArgumentException("Error variable name is required");
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
        int tryCount = tryEvents != null ? tryEvents.size() : 0;
        int catchCount = catchEvents != null ? catchEvents.size() : 0;
        int finallyCount = finallyEvents != null ? finallyEvents.size() : 0;
        
        StringBuilder description = new StringBuilder("Try-catch block: Try ");
        description.append(tryCount).append(" step(s)");
        
        if (catchCount > 0) {
            description.append(", catch ").append(catchCount).append(" step(s)");
        }
        
        if (finallyCount > 0) {
            description.append(", finally ").append(finallyCount).append(" step(s)");
        }
        
        if (!catchErrorTypes.isEmpty()) {
            description.append(" (catching ");
            description.append(String.join(", ", catchErrorTypes));
            description.append(")");
        }
        
        return description.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TryCatchEvent that = (TryCatchEvent) o;
        return continueOnError == that.continueOnError &&
               logError == that.logError &&
               Objects.equals(errorVariableName, that.errorVariableName) &&
               Objects.equals(catchErrorTypes, that.catchErrorTypes);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), errorVariableName, catchErrorTypes, continueOnError, logError);
    }
} 