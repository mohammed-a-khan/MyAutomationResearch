package com.cstestforge.recorder.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents an event recorded during a test recording session
 */
public class RecordedEvent {
    private UUID id;
    private RecordedEventType type;
    private String url;
    private String value;
    private ElementInfo elementInfo;
    private Instant timestamp;
    private Map<String, Object> metadata = new HashMap<>();
    private String title;
    private Viewport viewport;
    private String description; // Added missing field

    public RecordedEvent() {
        this.id = UUID.randomUUID();
        this.timestamp = Instant.now();
        this.viewport = new Viewport();
    }

    public RecordedEvent(RecordedEventType type) {
        this();
        this.type = type;
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public RecordedEventType getType() {
        return type;
    }

    public void setType(RecordedEventType type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public ElementInfo getElementInfo() {
        return elementInfo;
    }

    public void setElementInfo(ElementInfo elementInfo) {
        this.elementInfo = elementInfo;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

//    /**
//     * Convert LocalDateTime to Instant for backward compatibility
//     *
//     * @param localDateTime The LocalDateTime to convert
//     */
//    public void setTimestamp(LocalDateTime localDateTime) {
//        this.timestamp = localDateTime.toInstant(ZoneOffset.UTC);
//    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Viewport getViewport() {
        return viewport;
    }

    public void setViewport(Viewport viewport) {
        this.viewport = viewport != null ? viewport : new Viewport();
    }

    /**
     * Get the description of this event
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the description of this event
     *
     * @param description The description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get timestamp as epoch milliseconds
     *
     * @return timestamp in epoch milliseconds
     */
    public long toEpochMilli() {
        return timestamp.toEpochMilli();
    }

    /**
     * Checks if this event has valid data
     *
     * @return true if the event has valid data
     */
    public boolean isValid() {
        return type != null;
    }

    /**
     * Returns a human-readable description of this event
     *
     * @return A string describing this event
     */
    public String toHumanReadableDescription() {
        // Use the description field if it's set
        if (description != null && !description.isEmpty()) {
            return description;
        }

        StringBuilder desc = new StringBuilder();
        desc.append(type != null ? type.toString() : "Unknown event");

        if (url != null && !url.isEmpty()) {
            desc.append(" on ").append(url);
        }

        if (value != null && !value.isEmpty()) {
            desc.append(" with value '").append(value).append("'");
        }

        return desc.toString();
    }

    /**
     * Viewport information for a recorded event
     */
    public static class Viewport {
        private int width;
        private int height;

        public Viewport() {
            this.width = 0;
            this.height = 0;
        }

        public Viewport(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }
    }
}