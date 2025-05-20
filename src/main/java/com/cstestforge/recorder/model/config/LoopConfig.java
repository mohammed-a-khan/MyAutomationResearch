package com.cstestforge.recorder.model.config;

import com.cstestforge.recorder.model.RecordedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Configuration for loop logic in recorded events.
 * Supports different types of loops: COUNT, WHILE, FOR_EACH, UNTIL.
 */
public class LoopConfig {
    
    private String id;
    private LoopType type;
    private Integer count;
    private ConditionConfig condition;
    private String dataSourceId;
    private String dataSourcePath;
    private String iterationVariable;
    private Integer maxIterations;
    private List<RecordedEvent> steps;
    
    /**
     * Default constructor
     */
    public LoopConfig() {
        this.id = UUID.randomUUID().toString();
        this.type = LoopType.COUNT;
        this.count = 5;
        this.iterationVariable = "i";
        this.maxIterations = 100;
        this.steps = new ArrayList<>();
    }
    
    /**
     * Constructor for a count loop
     *
     * @param count Number of iterations
     * @param iterationVariable Variable name for iteration
     */
    public LoopConfig(Integer count, String iterationVariable) {
        this();
        this.type = LoopType.COUNT;
        this.count = count;
        this.iterationVariable = iterationVariable;
    }
    
    /**
     * Constructor for a conditional loop
     *
     * @param type Loop type (WHILE or UNTIL)
     * @param condition The condition for the loop
     * @param iterationVariable Variable name for iteration
     */
    public LoopConfig(LoopType type, ConditionConfig condition, String iterationVariable) {
        this();
        this.type = type;
        this.condition = condition;
        this.iterationVariable = iterationVariable;
    }
    
    /**
     * Constructor for a for-each loop
     *
     * @param dataSourceId ID of the data source
     * @param dataSourcePath Path within data source (JSON path)
     * @param iterationVariable Variable name for current value
     */
    public LoopConfig(String dataSourceId, String dataSourcePath, String iterationVariable) {
        this();
        this.type = LoopType.FOR_EACH;
        this.dataSourceId = dataSourceId;
        this.dataSourcePath = dataSourcePath;
        this.iterationVariable = iterationVariable;
    }
    
    /**
     * Get the loop ID
     *
     * @return The loop ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Set the loop ID
     *
     * @param id The loop ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get the loop type
     *
     * @return The loop type
     */
    public LoopType getType() {
        return type;
    }
    
    /**
     * Set the loop type
     *
     * @param type The loop type
     */
    public void setType(LoopType type) {
        this.type = type;
    }
    
    /**
     * Get the iteration count
     *
     * @return The iteration count
     */
    public Integer getCount() {
        return count;
    }
    
    /**
     * Set the iteration count
     *
     * @param count The iteration count
     */
    public void setCount(Integer count) {
        this.count = count;
    }
    
    /**
     * Get the loop condition
     *
     * @return The loop condition
     */
    public ConditionConfig getCondition() {
        return condition;
    }
    
    /**
     * Set the loop condition
     *
     * @param condition The loop condition
     */
    public void setCondition(ConditionConfig condition) {
        this.condition = condition;
    }
    
    /**
     * Get the data source ID
     *
     * @return The data source ID
     */
    public String getDataSourceId() {
        return dataSourceId;
    }
    
    /**
     * Set the data source ID
     *
     * @param dataSourceId The data source ID
     */
    public void setDataSourceId(String dataSourceId) {
        this.dataSourceId = dataSourceId;
    }
    
    /**
     * Get the data source path
     *
     * @return The data source path
     */
    public String getDataSourcePath() {
        return dataSourcePath;
    }
    
    /**
     * Set the data source path
     *
     * @param dataSourcePath The data source path
     */
    public void setDataSourcePath(String dataSourcePath) {
        this.dataSourcePath = dataSourcePath;
    }
    
    /**
     * Get the iteration variable name
     *
     * @return The iteration variable name
     */
    public String getIterationVariable() {
        return iterationVariable;
    }
    
    /**
     * Set the iteration variable name
     *
     * @param iterationVariable The iteration variable name
     */
    public void setIterationVariable(String iterationVariable) {
        this.iterationVariable = iterationVariable;
    }
    
    /**
     * Get the maximum allowed iterations
     *
     * @return The maximum allowed iterations
     */
    public Integer getMaxIterations() {
        return maxIterations;
    }
    
    /**
     * Set the maximum allowed iterations
     *
     * @param maxIterations The maximum allowed iterations
     */
    public void setMaxIterations(Integer maxIterations) {
        this.maxIterations = maxIterations;
    }
    
    /**
     * Get the steps to execute in each iteration
     *
     * @return List of steps to execute
     */
    public List<RecordedEvent> getSteps() {
        return steps;
    }
    
    /**
     * Set the steps to execute in each iteration
     *
     * @param steps List of steps to execute
     */
    public void setSteps(List<RecordedEvent> steps) {
        this.steps = steps != null ? steps : new ArrayList<>();
    }
    
    /**
     * Add a step to the loop
     *
     * @param event The event to add
     */
    public void addStep(RecordedEvent event) {
        if (this.steps == null) {
            this.steps = new ArrayList<>();
        }
        this.steps.add(event);
    }
    
    /**
     * Remove a step from the loop
     *
     * @param event The event to remove
     * @return True if the event was removed
     */
    public boolean removeStep(RecordedEvent event) {
        if (this.steps == null) {
            return false;
        }
        return this.steps.remove(event);
    }
    
    /**
     * Get a human-readable description of the loop
     *
     * @return Description of the loop
     */
    public String getDescription() {
        StringBuilder description = new StringBuilder();
        
        switch (type) {
            case COUNT:
                description.append("Repeat ")
                    .append(count != null ? count : "0")
                    .append(" times with ")
                    .append(iterationVariable)
                    .append(" as counter");
                break;
                
            case WHILE:
                description.append("Repeat while ");
                if (condition != null) {
                    description.append(condition.getDescription());
                } else {
                    description.append("condition is true");
                }
                break;
                
            case UNTIL:
                description.append("Repeat until ");
                if (condition != null) {
                    description.append(condition.getDescription());
                } else {
                    description.append("condition is true");
                }
                break;
                
            case FOR_EACH:
                description.append("For each item in data source ")
                    .append(dataSourceId != null ? dataSourceId : "unknown");
                if (dataSourcePath != null && !dataSourcePath.isEmpty()) {
                    description.append(" at path ").append(dataSourcePath);
                }
                description.append(" as ").append(iterationVariable);
                break;
                
            default:
                description.append("Unknown loop type");
        }
        
        if (maxIterations != null) {
            description.append(" (max ").append(maxIterations).append(" iterations)");
        }
        
        return description.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoopConfig that = (LoopConfig) o;
        return Objects.equals(id, that.id) &&
                type == that.type &&
                Objects.equals(count, that.count) &&
                Objects.equals(condition, that.condition) &&
                Objects.equals(dataSourceId, that.dataSourceId) &&
                Objects.equals(dataSourcePath, that.dataSourcePath) &&
                Objects.equals(iterationVariable, that.iterationVariable) &&
                Objects.equals(maxIterations, that.maxIterations);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, type, count, condition, dataSourceId, dataSourcePath, 
                iterationVariable, maxIterations);
    }
    
    /**
     * Types of loops
     */
    public enum LoopType {
        COUNT,       // Fixed number of iterations
        WHILE,       // Loop while condition is true
        FOR_EACH,    // Iterate over a data source
        UNTIL        // Loop until condition is true
    }
} 