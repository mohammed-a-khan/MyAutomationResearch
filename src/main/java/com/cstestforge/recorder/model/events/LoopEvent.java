package com.cstestforge.recorder.model.events;

import com.cstestforge.recorder.model.ElementInfo;
import com.cstestforge.recorder.model.RecordedEvent;
import com.cstestforge.recorder.model.RecordedEventType;
import com.cstestforge.recorder.model.config.LoopConfig;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Event representing a loop control structure in the recording.
 * Supports different types of loops: COUNT, WHILE, FOR_EACH, UNTIL.
 */
@JsonTypeName("LOOP")
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoopEvent extends RecordedEvent {
    
    private LoopConfig loopConfig;
    private List<RecordedEvent> nestedEvents = new ArrayList<>();
    
    /**
     * Default constructor for serialization
     */
    public LoopEvent() {
        super(RecordedEventType.LOOP);
    }
    
    /**
     * Constructor with necessary loop information
     *
     * @param url Current URL where the loop is defined
     * @param loopConfig Loop configuration details
     */
    public LoopEvent(String url, LoopConfig loopConfig) {
        super(RecordedEventType.LOOP);
        this.setUrl(url);
        this.loopConfig = loopConfig;
    }
    
    /**
     * Get the loop configuration
     *
     * @return Loop configuration
     */
    public LoopConfig getLoopConfig() {
        return loopConfig;
    }
    
    /**
     * Set the loop configuration
     *
     * @param loopConfig Loop configuration
     */
    public void setLoopConfig(LoopConfig loopConfig) {
        this.loopConfig = loopConfig;
    }
    
    /**
     * Get the events nested within this loop
     *
     * @return List of nested events
     */
    public List<RecordedEvent> getNestedEvents() {
        return nestedEvents;
    }
    
    /**
     * Set the events nested within this loop
     *
     * @param nestedEvents List of nested events
     */
    public void setNestedEvents(List<RecordedEvent> nestedEvents) {
        this.nestedEvents = nestedEvents;
    }
    
    /**
     * Add a nested event to this loop
     *
     * @param event Event to add to the loop
     */
    public void addNestedEvent(RecordedEvent event) {
        this.nestedEvents.add(event);
    }
    
    /**
     * Remove a nested event from this loop
     *
     * @param eventId ID of the event to remove from the loop
     * @return True if the event was removed
     */
    public boolean removeNestedEvent(UUID eventId) {
        return this.nestedEvents.removeIf(event -> eventId.equals(event.getId()));
    }
    
    /**
     * Generate code representation of this loop
     *
     * @return JavaScript code representing this loop
     */
    public String getCodeRepresentation() {
        if (loopConfig == null) {
            return "// Invalid loop configuration";
        }
        
        StringBuilder code = new StringBuilder();
        
        switch (loopConfig.getType()) {
            case COUNT:
                code.append(String.format("for (let %s = 0; %s < %d; %s++) {%n", 
                    loopConfig.getIterationVariable(), 
                    loopConfig.getIterationVariable(), 
                    loopConfig.getCount() != null ? loopConfig.getCount() : 0,
                    loopConfig.getIterationVariable()));
                break;
                
            case WHILE:
                code.append("while (")
                    .append(loopConfig.getCondition() != null ? 
                        generateConditionCode(loopConfig.getCondition()) : "true")
                    .append(") {\n");
                break;
                
            case UNTIL:
                code.append("do {\n");
                break;
                
            case FOR_EACH:
                code.append(String.format("for (const %s of %s) {%n", 
                    loopConfig.getIterationVariable(), 
                    generateDataSourceAccessCode()));
                break;
                
            default:
                code.append("// Unsupported loop type\n");
        }
        
        // Add nested events code
        if (nestedEvents != null) {
            for (RecordedEvent event : nestedEvents) {
                // Call toHumanReadableDescription as a placeholder for code generation
                // In a real implementation, each event type would have proper code generation
                String eventCode = event.toHumanReadableDescription();
                if (eventCode != null && !eventCode.isEmpty()) {
                    // Indent each line of the event code
                    String[] lines = eventCode.split("\n");
                    for (String line : lines) {
                        code.append("  ").append(line).append("\n");
                    }
                }
            }
        }
        
        // Special handling for UNTIL loops (need closing condition)
        if (loopConfig.getType() == LoopConfig.LoopType.UNTIL) {
            code.append("} while (!(")
                .append(loopConfig.getCondition() != null ? 
                    generateConditionCode(loopConfig.getCondition()) : "true")
                .append("));\n");
        } else {
            code.append("}\n");
        }
        
        return code.toString();
    }
    
    /**
     * Helper method to generate condition code for WHILE and UNTIL loops
     * 
     * @param condition The condition configuration
     * @return JavaScript condition code
     */
    private String generateConditionCode(Object condition) {
        // In a real implementation, this would properly translate the condition to code
        // This is a placeholder implementation
        return condition.toString();
    }
    
    /**
     * Helper method to generate data source access code for FOR_EACH loops
     * 
     * @return JavaScript data source access code
     */
    private String generateDataSourceAccessCode() {
        if (loopConfig.getDataSourceId() == null) {
            return "[]";
        }
        
        String dataSourceReference = "getDataSource('" + loopConfig.getDataSourceId() + "')";
        if (loopConfig.getDataSourcePath() != null && !loopConfig.getDataSourcePath().isEmpty()) {
            // In a real implementation, we'd generate proper JSON path access
            return dataSourceReference + ".getPath('" + loopConfig.getDataSourcePath() + "')";
        }
        return dataSourceReference;
    }
    
    /**
     * Get the target element for this event
     *
     * @return The target element (null for loop events)
     */
    public ElementInfo getTargetElement() {
        // Loop events don't have a direct target element
        return null;
    }
    
    /**
     * Validate the loop event data
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() throws IllegalArgumentException {
        if (loopConfig == null) {
            throw new IllegalArgumentException("Loop configuration is required");
        }
        
        switch (loopConfig.getType()) {
            case COUNT:
                if (loopConfig.getCount() == null) {
                    throw new IllegalArgumentException("Count is required for COUNT loop type");
                }
                if (loopConfig.getCount() < 0) {
                    throw new IllegalArgumentException("Count must be a positive number");
                }
                break;
                
            case WHILE:
            case UNTIL:
                if (loopConfig.getCondition() == null) {
                    throw new IllegalArgumentException("Condition is required for " + 
                        loopConfig.getType() + " loop type");
                }
                break;
                
            case FOR_EACH:
                if (loopConfig.getDataSourceId() == null) {
                    throw new IllegalArgumentException("Data source ID is required for FOR_EACH loop type");
                }
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported loop type: " + loopConfig.getType());
        }
        
        if (loopConfig.getIterationVariable() == null || loopConfig.getIterationVariable().isEmpty()) {
            throw new IllegalArgumentException("Iteration variable name is required");
        }
        
        if (loopConfig.getMaxIterations() != null && loopConfig.getMaxIterations() <= 0) {
            throw new IllegalArgumentException("Maximum iterations must be a positive number");
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
        if (loopConfig == null) {
            return "Invalid loop";
        }
        
        return "Loop: " + loopConfig.getDescription() + 
               " with " + (nestedEvents != null ? nestedEvents.size() : 0) + " steps";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        LoopEvent loopEvent = (LoopEvent) o;
        return Objects.equals(loopConfig, loopEvent.loopConfig);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), loopConfig);
    }
} 