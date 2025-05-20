package com.cstestforge.codegen.model.event;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a loop event in a test (for, while, forEach)
 */
public class LoopEvent extends Event {
    public enum LoopType {
        FOR,
        WHILE,
        FOR_EACH
    }
    
    private LoopType loopType;
    private String initialization; // For 'for' loops
    private String condition; // For 'for' and 'while' loops
    private String increment; // For 'for' loops
    private String iterableExpression; // For 'forEach' loops
    private String iteratorVariable; // For 'forEach' loops
    private List<Event> loopEvents;
    private int maxIterations; // Safety limit
    
    public LoopEvent() {
        super("loop", "Loop");
        this.loopEvents = new ArrayList<>();
        this.maxIterations = 100; // Default safety limit
    }
    
    public LoopEvent(LoopType loopType, String condition, List<Event> loopEvents) {
        this();
        this.loopType = loopType;
        this.condition = condition;
        this.loopEvents = loopEvents;
    }
    
    // Specific constructor for FOR loops
    public LoopEvent(String initialization, String condition, String increment, List<Event> loopEvents) {
        this();
        this.loopType = LoopType.FOR;
        this.initialization = initialization;
        this.condition = condition;
        this.increment = increment;
        this.loopEvents = loopEvents;
    }
    
    // Specific constructor for FOR_EACH loops
    public LoopEvent(String iterableExpression, String iteratorVariable, List<Event> loopEvents) {
        this();
        this.loopType = LoopType.FOR_EACH;
        this.iterableExpression = iterableExpression;
        this.iteratorVariable = iteratorVariable;
        this.loopEvents = loopEvents;
    }
    
    // Getters and Setters
    public LoopType getLoopType() {
        return loopType;
    }

    public void setLoopType(LoopType loopType) {
        this.loopType = loopType;
    }

    public String getInitialization() {
        return initialization;
    }

    public void setInitialization(String initialization) {
        this.initialization = initialization;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getIncrement() {
        return increment;
    }

    public void setIncrement(String increment) {
        this.increment = increment;
    }

    public String getIterableExpression() {
        return iterableExpression;
    }

    public void setIterableExpression(String iterableExpression) {
        this.iterableExpression = iterableExpression;
    }

    public String getIteratorVariable() {
        return iteratorVariable;
    }

    public void setIteratorVariable(String iteratorVariable) {
        this.iteratorVariable = iteratorVariable;
    }

    public List<Event> getLoopEvents() {
        return loopEvents;
    }

    public void setLoopEvents(List<Event> loopEvents) {
        this.loopEvents = loopEvents;
    }
    
    public void addLoopEvent(Event event) {
        this.loopEvents.add(event);
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    @Override
    public String toCode(String language, String framework, int indentLevel) {
        // This will be implemented by the code generation service
        return null;
    }
} 