package com.cstestforge.codegen.model.event;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a logical grouping of related events
 */
public class GroupEvent extends Event {
    private List<Event> events;
    private String groupType; // Optional categorization (e.g., "setup", "teardown", "test")
    
    public GroupEvent() {
        super("group", "Group");
        this.events = new ArrayList<>();
    }
    
    public GroupEvent(String name, List<Event> events) {
        super("group", name);
        this.events = events;
    }
    
    public GroupEvent(String name, String groupType, List<Event> events) {
        this(name, events);
        this.groupType = groupType;
    }
    
    // Getters and Setters
    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }
    
    public void addEvent(Event event) {
        this.events.add(event);
    }

    public String getGroupType() {
        return groupType;
    }

    public void setGroupType(String groupType) {
        this.groupType = groupType;
    }

    @Override
    public String toCode(String language, String framework, int indentLevel) {
        // This will be implemented by the code generation service
        return null;
    }
} 