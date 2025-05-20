package com.cstestforge.recorder.model.events;

import com.cstestforge.recorder.model.ElementInfo;
import com.cstestforge.recorder.model.RecordedEvent;
import com.cstestforge.recorder.model.RecordedEventType;
import com.cstestforge.recorder.model.config.ConditionConfig;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Event representing a conditional branching structure in the recording.
 * Supports if-then-else logic with complex conditions.
 */
@JsonTypeName("CONDITIONAL")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConditionalEvent extends RecordedEvent {
    
    private ConditionConfig conditionConfig;
    private List<RecordedEvent> thenEvents;
    private List<RecordedEvent> elseEvents;
    
    /**
     * Default constructor for serialization
     */
    public ConditionalEvent() {
        super(RecordedEventType.CONDITIONAL);
        this.thenEvents = new ArrayList<>();
        this.elseEvents = new ArrayList<>();
    }
    
    /**
     * Constructor with necessary conditional information
     *
     * @param url Current URL where the condition is defined
     * @param conditionConfig Condition configuration details
     */
    public ConditionalEvent(String url, ConditionConfig conditionConfig) {
        super(RecordedEventType.CONDITIONAL);
        setUrl(url);
        this.conditionConfig = conditionConfig;
        this.thenEvents = new ArrayList<>();
        this.elseEvents = new ArrayList<>();
    }
    
    /**
     * Get the condition configuration
     *
     * @return Condition configuration
     */
    public ConditionConfig getConditionConfig() {
        return conditionConfig;
    }
    
    /**
     * Set the condition configuration
     *
     * @param conditionConfig Condition configuration
     */
    public void setConditionConfig(ConditionConfig conditionConfig) {
        this.conditionConfig = conditionConfig;
    }
    
    /**
     * Get the events to execute if the condition is true
     *
     * @return List of events for the "then" branch
     */
    public List<RecordedEvent> getThenEvents() {
        return thenEvents;
    }
    
    /**
     * Set the events to execute if the condition is true
     *
     * @param thenEvents List of events for the "then" branch
     */
    public void setThenEvents(List<RecordedEvent> thenEvents) {
        this.thenEvents = thenEvents != null ? thenEvents : new ArrayList<>();
    }
    
    /**
     * Get the events to execute if the condition is false
     *
     * @return List of events for the "else" branch
     */
    public List<RecordedEvent> getElseEvents() {
        return elseEvents;
    }
    
    /**
     * Set the events to execute if the condition is false
     *
     * @param elseEvents List of events for the "else" branch
     */
    public void setElseEvents(List<RecordedEvent> elseEvents) {
        this.elseEvents = elseEvents != null ? elseEvents : new ArrayList<>();
    }
    
    /**
     * Add an event to the "then" branch
     *
     * @param event Event to add
     */
    public void addThenEvent(RecordedEvent event) {
        if (this.thenEvents == null) {
            this.thenEvents = new ArrayList<>();
        }
        this.thenEvents.add(event);
    }
    
    /**
     * Remove an event from the "then" branch
     *
     * @param event Event to remove
     * @return True if the event was removed
     */
    public boolean removeThenEvent(RecordedEvent event) {
        if (this.thenEvents == null) {
            return false;
        }
        return this.thenEvents.remove(event);
    }
    
    /**
     * Add an event to the "else" branch
     *
     * @param event Event to add
     */
    public void addElseEvent(RecordedEvent event) {
        if (this.elseEvents == null) {
            this.elseEvents = new ArrayList<>();
        }
        this.elseEvents.add(event);
    }
    
    /**
     * Remove an event from the "else" branch
     *
     * @param event Event to remove
     * @return True if the event was removed
     */
    public boolean removeElseEvent(RecordedEvent event) {
        if (this.elseEvents == null) {
            return false;
        }
        return this.elseEvents.remove(event);
    }
    
    /**
     * Get the target element for this event
     *
     * @return The target element (null for conditional events)
     */
    public ElementInfo getTargetElement() {
        // Conditional events don't have a direct target element
        return null;
    }
    
    /**
     * Validate the conditional event data
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() throws IllegalArgumentException {
        if (conditionConfig == null) {
            throw new IllegalArgumentException("Condition configuration is required");
        }
        
        // We don't validate nested events here to avoid circular validation
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
        if (conditionConfig == null) {
            return "Invalid condition";
        }
        
        StringBuilder description = new StringBuilder("If ");
        description.append(conditionConfig.getDescription());
        
        int thenCount = thenEvents != null ? thenEvents.size() : 0;
        int elseCount = elseEvents != null ? elseEvents.size() : 0;
        
        description.append(" then execute ").append(thenCount).append(" step(s)");
        
        if (elseCount > 0) {
            description.append(" else execute ").append(elseCount).append(" step(s)");
        }
        
        return description.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ConditionalEvent that = (ConditionalEvent) o;
        return Objects.equals(conditionConfig, that.conditionConfig);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), conditionConfig);
    }
} 