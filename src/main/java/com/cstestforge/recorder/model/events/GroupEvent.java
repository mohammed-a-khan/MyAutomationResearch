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
 * Event representing a grouping structure in the recording.
 * Groups related events together for better organization.
 */
@JsonTypeName("GROUP")
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupEvent extends RecordedEvent {
    
    private String groupName;
    private String description;
    private List<RecordedEvent> groupEvents;
    private boolean collapsed;
    private String color;
    private String icon;
    
    /**
     * Default constructor for serialization
     */
    public GroupEvent() {
        super(RecordedEventType.GROUP);
        this.groupEvents = new ArrayList<>();
        this.collapsed = false;
    }
    
    /**
     * Constructor with necessary group information
     *
     * @param url Current URL where the group is defined
     * @param groupName Name of the group
     * @param description Description of the group
     */
    public GroupEvent(String url, String groupName, String description) {
        super(RecordedEventType.GROUP);
        setUrl(url);
        this.groupName = groupName;
        this.description = description;
        this.groupEvents = new ArrayList<>();
        this.collapsed = false;
    }
    
    /**
     * Get the group name
     *
     * @return The group name
     */
    public String getGroupName() {
        return groupName;
    }
    
    /**
     * Set the group name
     *
     * @param groupName The group name
     */
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    
    /**
     * Get the group description
     *
     * @return The group description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Set the group description
     *
     * @param description The group description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Get the events in the group
     *
     * @return List of events in the group
     */
    public List<RecordedEvent> getGroupEvents() {
        return groupEvents;
    }
    
    /**
     * Set the events in the group
     *
     * @param groupEvents List of events in the group
     */
    public void setGroupEvents(List<RecordedEvent> groupEvents) {
        this.groupEvents = groupEvents != null ? groupEvents : new ArrayList<>();
    }
    
    /**
     * Add an event to the group
     *
     * @param event Event to add
     */
    public void addGroupEvent(RecordedEvent event) {
        if (this.groupEvents == null) {
            this.groupEvents = new ArrayList<>();
        }
        this.groupEvents.add(event);
    }
    
    /**
     * Remove an event from the group
     *
     * @param event Event to remove
     * @return True if the event was removed
     */
    public boolean removeGroupEvent(RecordedEvent event) {
        if (this.groupEvents == null) {
            return false;
        }
        return this.groupEvents.remove(event);
    }
    
    /**
     * Check if the group is collapsed in the UI
     *
     * @return True if collapsed, false if expanded
     */
    public boolean isCollapsed() {
        return collapsed;
    }
    
    /**
     * Set whether the group is collapsed in the UI
     *
     * @param collapsed True to collapse, false to expand
     */
    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }
    
    /**
     * Get the group color for UI display
     *
     * @return The color value (CSS-compatible)
     */
    public String getColor() {
        return color;
    }
    
    /**
     * Set the group color for UI display
     *
     * @param color The color value (CSS-compatible)
     */
    public void setColor(String color) {
        this.color = color;
    }
    
    /**
     * Get the icon name for UI display
     *
     * @return The icon name
     */
    public String getIcon() {
        return icon;
    }
    
    /**
     * Set the icon name for UI display
     *
     * @param icon The icon name
     */
    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    /**
     * Get the target element for this event
     *
     * @return The target element (null for group events)
     */
    public ElementInfo getTargetElement() {
        // Group events don't have a direct target element
        return null;
    }
    
    /**
     * Validate the group event data
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() throws IllegalArgumentException {
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new IllegalArgumentException("Group name is required");
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
        StringBuilder description = new StringBuilder("Group: ");
        description.append(groupName != null ? groupName : "Unnamed group");
        
        int count = groupEvents != null ? groupEvents.size() : 0;
        description.append(" (").append(count).append(" step(s))");
        
        if (this.description != null && !this.description.trim().isEmpty()) {
            description.append(" - ").append(this.description);
        }
        
        return description.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        GroupEvent that = (GroupEvent) o;
        return collapsed == that.collapsed &&
               Objects.equals(groupName, that.groupName) &&
               Objects.equals(description, that.description) &&
               Objects.equals(color, that.color) &&
               Objects.equals(icon, that.icon);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), groupName, description, collapsed, color, icon);
    }
} 