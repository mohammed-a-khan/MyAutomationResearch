package com.cstestforge.codegen.model.event;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an if/else conditional event in a test
 */
public class ConditionalEvent extends Event {
    private String condition;
    private List<Event> thenEvents;
    private List<Event> elseEvents;
    private boolean hasElse;
    
    public ConditionalEvent() {
        super("conditional", "Conditional");
        this.thenEvents = new ArrayList<>();
        this.elseEvents = new ArrayList<>();
        this.hasElse = false;
    }
    
    public ConditionalEvent(String condition, List<Event> thenEvents) {
        this();
        this.condition = condition;
        this.thenEvents = thenEvents;
    }
    
    public ConditionalEvent(String condition, List<Event> thenEvents, List<Event> elseEvents) {
        this(condition, thenEvents);
        this.elseEvents = elseEvents;
        this.hasElse = true;
    }
    
    // Getters and Setters
    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public List<Event> getThenEvents() {
        return thenEvents;
    }

    public void setThenEvents(List<Event> thenEvents) {
        this.thenEvents = thenEvents;
    }
    
    public void addThenEvent(Event event) {
        this.thenEvents.add(event);
    }

    public List<Event> getElseEvents() {
        return elseEvents;
    }

    public void setElseEvents(List<Event> elseEvents) {
        this.elseEvents = elseEvents;
        this.hasElse = (elseEvents != null && !elseEvents.isEmpty());
    }
    
    public void addElseEvent(Event event) {
        this.elseEvents.add(event);
        this.hasElse = true;
    }

    public boolean isHasElse() {
        return hasElse;
    }

    public void setHasElse(boolean hasElse) {
        this.hasElse = hasElse;
    }

    @Override
    public String toCode(String language, String framework, int indentLevel) {
        // This will be implemented by the code generation service
        return null;
    }
} 