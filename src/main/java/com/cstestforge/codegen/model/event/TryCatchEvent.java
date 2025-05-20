package com.cstestforge.codegen.model.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a try/catch/finally exception handling block
 */
public class TryCatchEvent extends Event {
    private List<Event> tryEvents;
    private Map<String, List<Event>> catchBlocks; // Exception type -> catch events
    private List<Event> finallyEvents;
    private boolean hasFinally;
    
    public TryCatchEvent() {
        super("try_catch", "Try Catch");
        this.tryEvents = new ArrayList<>();
        this.catchBlocks = new HashMap<>();
        this.finallyEvents = new ArrayList<>();
        this.hasFinally = false;
    }
    
    public TryCatchEvent(List<Event> tryEvents) {
        this();
        this.tryEvents = tryEvents;
    }
    
    public TryCatchEvent(List<Event> tryEvents, Map<String, List<Event>> catchBlocks) {
        this(tryEvents);
        this.catchBlocks = catchBlocks;
    }
    
    public TryCatchEvent(List<Event> tryEvents, Map<String, List<Event>> catchBlocks, List<Event> finallyEvents) {
        this(tryEvents, catchBlocks);
        this.finallyEvents = finallyEvents;
        this.hasFinally = true;
    }
    
    // Getters and Setters
    public List<Event> getTryEvents() {
        return tryEvents;
    }

    public void setTryEvents(List<Event> tryEvents) {
        this.tryEvents = tryEvents;
    }
    
    public void addTryEvent(Event event) {
        this.tryEvents.add(event);
    }

    public Map<String, List<Event>> getCatchBlocks() {
        return catchBlocks;
    }

    public void setCatchBlocks(Map<String, List<Event>> catchBlocks) {
        this.catchBlocks = catchBlocks;
    }
    
    public void addCatchBlock(String exceptionType, List<Event> events) {
        this.catchBlocks.put(exceptionType, events);
    }
    
    public void addEventToCatchBlock(String exceptionType, Event event) {
        if (!this.catchBlocks.containsKey(exceptionType)) {
            this.catchBlocks.put(exceptionType, new ArrayList<>());
        }
        this.catchBlocks.get(exceptionType).add(event);
    }

    public List<Event> getFinallyEvents() {
        return finallyEvents;
    }

    public void setFinallyEvents(List<Event> finallyEvents) {
        this.finallyEvents = finallyEvents;
        this.hasFinally = (finallyEvents != null && !finallyEvents.isEmpty());
    }
    
    public void addFinallyEvent(Event event) {
        this.finallyEvents.add(event);
        this.hasFinally = true;
    }

    public boolean isHasFinally() {
        return hasFinally;
    }

    public void setHasFinally(boolean hasFinally) {
        this.hasFinally = hasFinally;
    }

    @Override
    public String toCode(String language, String framework, int indentLevel) {
        // This will be implemented by the code generation service
        return null;
    }
} 