package com.cstestforge.recorder.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Represents a recording session that captures browser interactions.
 */
public class RecordingSession {
    
    private UUID id;
    private String name;
    private String description;
    private String projectId;
    private RecordingConfig config;
    private RecordingStatus status;
    private Instant startTime;
    private Instant endTime;
    private String browserInfo;
    private String operatingSystem;
    private String userAgent;
    private List<RecordedEvent> events;
    private String createdBy;
    private String sessionKey;
    private java.util.Map<String, Object> metadata;
    private String browser;
    private String framework;
    private String baseUrl;
    
    /**
     * Default constructor
     */
    public RecordingSession() {
        this.id = UUID.randomUUID();
        this.startTime = Instant.now();
        this.status = RecordingStatus.ACTIVE;
        this.config = new RecordingConfig();
        this.events = new ArrayList<>();
        this.sessionKey = generateSessionKey();
        this.metadata = new java.util.HashMap<>();
    }
    
    /**
     * Constructor with name and project ID
     *
     * @param name The recording session name
     * @param projectId The project ID
     */
    public RecordingSession(String name, String projectId) {
        this();
        this.name = name;
        this.projectId = projectId;
    }
    
    /**
     * Get the session ID
     *
     * @return The session ID
     */
    public UUID getId() {
        return id;
    }
    
    /**
     * Set the session ID
     *
     * @param id The session ID
     */
    public void setId(UUID id) {
        this.id = id;
    }
    
    /**
     * Get the session name
     *
     * @return The session name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the session name
     *
     * @param name The session name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get the session description
     *
     * @return The session description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Set the session description
     *
     * @param description The session description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Get the project ID
     *
     * @return The project ID
     */
    public String getProjectId() {
        return projectId;
    }
    
    /**
     * Set the project ID
     *
     * @param projectId The project ID
     */
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
    
    /**
     * Get the recording configuration
     *
     * @return The recording configuration
     */
    public RecordingConfig getConfig() {
        return config;
    }
    
    /**
     * Set the recording configuration
     *
     * @param config The recording configuration
     */
    public void setConfig(RecordingConfig config) {
        this.config = config;
    }
    
    /**
     * Get the recording status
     *
     * @return The recording status
     */
    public RecordingStatus getStatus() {
        return status;
    }
    
    /**
     * Set the recording status
     *
     * @param status The recording status
     */
    public void setStatus(RecordingStatus status) {
        this.status = status;
        
        // If the session is completed or encountered an error, set the end time
        if (status == RecordingStatus.COMPLETED || status == RecordingStatus.FAILED) {
            this.endTime = Instant.now();
        }
    }
    
    /**
     * Get the session start time
     *
     * @return The start time
     */
    public Instant getStartTime() {
        return startTime;
    }
    
    /**
     * Set the session start time
     *
     * @param startTime The start time
     */
    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }
    
    /**
     * Get the session end time
     *
     * @return The end time
     */
    public Instant getEndTime() {
        return endTime;
    }
    
    /**
     * Set the session end time
     *
     * @param endTime The end time
     */
    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }
    
    /**
     * Get the browser information
     *
     * @return Browser information
     */
    public String getBrowserInfo() {
        return browserInfo;
    }
    
    /**
     * Set the browser information
     *
     * @param browserInfo Browser information
     */
    public void setBrowserInfo(String browserInfo) {
        this.browserInfo = browserInfo;
    }
    
    /**
     * Get the operating system information
     *
     * @return Operating system information
     */
    public String getOperatingSystem() {
        return operatingSystem;
    }
    
    /**
     * Set the operating system information
     *
     * @param operatingSystem Operating system information
     */
    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }
    
    /**
     * Get the user agent string
     *
     * @return User agent string
     */
    public String getUserAgent() {
        return userAgent;
    }
    
    /**
     * Set the user agent string
     *
     * @param userAgent User agent string
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    /**
     * Get the recorded events
     *
     * @return List of recorded events
     */
    public List<RecordedEvent> getEvents() {
        return events;
    }
    
    /**
     * Set the recorded events
     *
     * @param events List of recorded events
     */
    public void setEvents(List<RecordedEvent> events) {
        this.events = events;
    }
    
    /**
     * Get the user who created the session
     *
     * @return Creator username
     */
    public String getCreatedBy() {
        return createdBy;
    }
    
    /**
     * Set the user who created the session
     *
     * @param createdBy Creator username
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    /**
     * Get the session key used to authenticate recording clients
     *
     * @return Session key
     */
    public String getSessionKey() {
        return sessionKey;
    }
    
    /**
     * Set the session key used to authenticate recording clients
     *
     * @param sessionKey Session key
     */
    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }
    
    /**
     * Add an event to the session
     *
     * @param event The event to add
     * @return True if the event was added successfully
     */
    public boolean addEvent(RecordedEvent event) {
        if (this.status != RecordingStatus.ACTIVE) {
            return false;
        }
        
        if (this.events == null) {
            this.events = new ArrayList<>();
        }
        
        if (this.config != null && this.events.size() >= this.config.getMaxEventCount()) {
            return false;
        }
        
        return this.events.add(event);
    }
    
    /**
     * Get the duration of the recording session in milliseconds
     *
     * @return Session duration in milliseconds
     */
    public long getDurationMillis() {
        Instant end = this.endTime != null ? this.endTime : Instant.now();
        return this.startTime != null ? end.toEpochMilli() - this.startTime.toEpochMilli() : 0;
    }
    
    /**
     * Pause the recording session
     */
    public void pause() {
        this.status = RecordingStatus.PAUSED;
    }
    
    /**
     * Resume the recording session
     */
    public void resume() {
        this.status = RecordingStatus.ACTIVE;
    }
    
    /**
     * Complete the recording session
     */
    public void complete() {
        this.status = RecordingStatus.COMPLETED;
        this.endTime = Instant.now();
    }
    
    /**
     * Mark the recording session as having an error
     */
    public void error() {
        this.status = RecordingStatus.FAILED;
        this.endTime = Instant.now();
    }
    
    /**
     * Generate a unique session key for client authentication
     *
     * @return Generated session key
     */
    private String generateSessionKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * Get session metadata
     *
     * @return Session metadata
     */
    public java.util.Map<String, Object> getMetadata() {
        return metadata;
    }
    
    /**
     * Set session metadata
     *
     * @param metadata Session metadata
     */
    public void setMetadata(java.util.Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    /**
     * Get the most recently added event
     *
     * @return The last event, or null if no events exist
     */
    public RecordedEvent getLastEvent() {
        if (events.isEmpty()) {
            return null;
        }
        return events.get(events.size() - 1);
    }
    
    public String getBrowser() {
        return browser;
    }
    
    public void setBrowser(String browser) {
        this.browser = browser;
    }
    
    public String getFramework() {
        return framework;
    }
    
    public void setFramework(String framework) {
        this.framework = framework;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    // Convenience method to handle Date objects
    public void setStartTime(Date date) {
        if (date != null) {
            this.startTime = date.toInstant();
        }
    }
} 