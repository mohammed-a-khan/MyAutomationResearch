package com.cstestforge.recorder.service;

import com.cstestforge.recorder.model.*;
import com.cstestforge.recorder.model.config.LoopConfig;
import com.cstestforge.recorder.model.events.LoopEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing recording sessions and events.
 */
@Service
public class RecorderService {
    private static final Logger logger = LoggerFactory.getLogger(RecorderService.class);
    
    // In-memory store for active recording sessions
    private final Map<UUID, RecordingSession> activeSessions = new ConcurrentHashMap<>();
    
    // In-memory map of session keys to session IDs for authentication
    private final Map<String, UUID> sessionKeyMap = new ConcurrentHashMap<>();
    
    // In-memory store for loop events for easier access
    private final Map<UUID, com.cstestforge.recorder.model.events.LoopEvent> loopEvents = new ConcurrentHashMap<>();
    
    /**
     * Create a new recording session
     *
     * @param name The session name
     * @param projectId The project ID
     * @param config The recording configuration
     * @param createdBy The user creating the session
     * @return The created recording session
     */
    public RecordingSession createSession(String name, String projectId, RecordingConfig config, String createdBy) {
        RecordingSession session = new RecordingSession(name, projectId);
        
        if (config != null) {
            session.setConfig(config);
        }
        
        session.setCreatedBy(createdBy);
        session.setStartTime(Instant.now());
        
        // Store the session
        activeSessions.put(session.getId(), session);
        sessionKeyMap.put(session.getSessionKey(), session.getId());
        
        return session;
    }
    
    /**
     * Get a recording session by ID
     *
     * @param sessionId The session ID
     * @return The recording session, or null if not found
     */
    public RecordingSession getSession(UUID sessionId) {
        return activeSessions.get(sessionId);
    }
    
    /**
     * Get a recording session by session key
     *
     * @param sessionKey The session key
     * @return The recording session, or null if not found
     */
    public RecordingSession getSessionByKey(String sessionKey) {
        UUID sessionId = sessionKeyMap.get(sessionKey);
        return sessionId != null ? activeSessions.get(sessionId) : null;
    }
    
    /**
     * Add an event to a recording session
     *
     * @param sessionId The session ID
     * @param event The event to add
     * @return True if the event was added successfully
     */
    public boolean addEvent(UUID sessionId, RecordedEvent event) {
        RecordingSession session = activeSessions.get(sessionId);
        
        if (session == null || session.getStatus() != RecordingStatus.ACTIVE) {
            return false;
        }
        
        return session.addEvent(event);
    }
    
    /**
     * Add an event to a recording session by session key
     *
     * @param sessionKey The session key
     * @param event The event to add
     * @return True if the event was added successfully
     */
    public boolean addEventByKey(String sessionKey, RecordedEvent event) {
        RecordingSession session = getSessionByKey(sessionKey);
        
        if (session == null) {
            return false;
        }
        
        return session.addEvent(event);
    }
    
    /**
     * Update the status of a recording session
     *
     * @param sessionId The session ID
     * @param status The new status
     * @return The updated recording session, or null if not found
     */
    public RecordingSession updateSessionStatus(UUID sessionId, RecordingStatus status) {
        RecordingSession session = activeSessions.get(sessionId);
        
        if (session == null) {
            return null;
        }
        
        session.setStatus(status);
        
        // If the session is completed or has an error, we can remove it from the active sessions
        if (status == RecordingStatus.COMPLETED || status == RecordingStatus.FAILED) {
            session.setEndTime(Instant.now());
            
            // Remove from active sessions after a delay
            // In a real application, this would be persisted to a database
            // and eventually removed from memory
            Timer timer = new Timer(true);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    activeSessions.remove(session.getId());
                    sessionKeyMap.remove(session.getSessionKey());
                }
            }, 30 * 60 * 1000); // Keep session in memory for 30 minutes after completion
        }
        
        return session;
    }
    
    /**
     * Get all recording sessions for a project
     *
     * @param projectId The project ID
     * @return List of recording sessions for the project
     */
    public List<RecordingSession> getSessionsByProject(String projectId) {
        List<RecordingSession> result = new ArrayList<>();
        
        for (RecordingSession session : activeSessions.values()) {
            if (Objects.equals(session.getProjectId(), projectId)) {
                result.add(session);
            }
        }
        
        return result;
    }
    
    /**
     * Get all active recording sessions
     *
     * @return List of active recording sessions
     */
    public List<RecordingSession> getActiveSessions() {
        List<RecordingSession> result = new ArrayList<>();
        
        for (RecordingSession session : activeSessions.values()) {
            if (session.getStatus() == RecordingStatus.ACTIVE) {
                result.add(session);
            }
        }
        
        return result;
    }
    
    /**
     * Delete a recording session
     *
     * @param sessionId The session ID
     * @return True if the session was deleted
     */
    public boolean deleteSession(UUID sessionId) {
        RecordingSession session = activeSessions.remove(sessionId);
        
        if (session != null) {
            sessionKeyMap.remove(session.getSessionKey());
            return true;
        }
        
        return false;
    }
    
    /**
     * Create a loop event in a recording session
     *
     * @param parentEventId ID of the parent event
     * @param loopConfig The loop configuration
     * @return The created loop event
     */
    public com.cstestforge.recorder.model.LoopEvent createLoopEvent(UUID parentEventId, LoopConfig loopConfig) {
        // Find the session containing the parent event
        RecordingSession session = findSessionByEventId(parentEventId);
        
        if (session == null) {
            return null;
        }
        
        RecordedEvent parentEvent = findEventById(session, parentEventId);
        if (parentEvent == null) {
            return null;
        }
        
        // Create the loop event
        com.cstestforge.recorder.model.events.LoopEvent loopEvent = new com.cstestforge.recorder.model.events.LoopEvent(parentEvent.getUrl(), loopConfig);
        
        // Store in our loop events cache
        loopEvents.put(loopEvent.getId(), loopEvent);
        
        // Add to the session
        session.addEvent(loopEvent);
        
        return new com.cstestforge.recorder.model.LoopEvent(parentEvent.getUrl(), new com.cstestforge.recorder.model.LoopConfig());
    }
    
    /**
     * Update a loop event configuration
     *
     * @param eventId The loop event ID
     * @param loopConfig The updated loop configuration
     * @return The updated loop event
     */
    public com.cstestforge.recorder.model.LoopEvent updateLoopEvent(UUID eventId, LoopConfig loopConfig) {
        com.cstestforge.recorder.model.events.LoopEvent loopEvent = loopEvents.get(eventId);
        
        if (loopEvent == null) {
            // Try to find it in sessions if not in cache
            for (RecordingSession session : activeSessions.values()) {
                RecordedEvent event = findEventById(session, eventId);
                if (event != null && event.getType() == RecordedEventType.LOOP) {
                    loopEvent = (com.cstestforge.recorder.model.events.LoopEvent) event;
                    loopEvents.put(eventId, loopEvent);
                    break;
                }
            }
            
            if (loopEvent == null) {
                return null;
            }
        }
        
        // Update the configuration
        loopEvent.setLoopConfig(loopConfig);
        
        return new com.cstestforge.recorder.model.LoopEvent();
    }
    
    /**
     * Add an event to a loop
     *
     * @param loopEventId The loop event ID
     * @param event The event to add
     * @return True if the event was added successfully
     */
    public boolean addEventToLoop(UUID loopEventId, RecordedEvent event) {
        LoopEvent loopEvent = loopEvents.get(loopEventId);
        
        if (loopEvent == null) {
            return false;
        }
        
        // Add the event to the loop's nested events
        loopEvent.addNestedEvent(event);
        
        return true;
    }
    
    /**
     * Remove an event from a loop
     *
     * @param loopEventId The loop event ID
     * @param eventId The ID of the event to remove
     * @return True if the event was removed
     */
    public boolean removeEventFromLoop(UUID loopEventId, UUID eventId) {
        LoopEvent loopEvent = loopEvents.get(loopEventId);
        
        if (loopEvent == null) {
            return false;
        }
        
        // Find and remove the event
        for (Iterator<RecordedEvent> it = loopEvent.getNestedEvents().iterator(); it.hasNext();) {
            RecordedEvent nestedEvent = it.next();
            if (nestedEvent.getId().equals(eventId)) {
                it.remove();
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get all events in a loop
     *
     * @param loopEventId The loop event ID
     * @return List of events in the loop, or null if the loop wasn't found
     */
    public List<RecordedEvent> getLoopEvents(UUID loopEventId) {
        LoopEvent loopEvent = loopEvents.get(loopEventId);
        
        if (loopEvent == null) {
            return null;
        }
        
        return loopEvent.getNestedEvents();
    }
    
    /**
     * Find the session containing a specific event
     *
     * @param eventId The event ID to find
     * @return The session containing the event, or null if not found
     */
    private RecordingSession findSessionByEventId(UUID eventId) {
        for (RecordingSession session : activeSessions.values()) {
            if (findEventById(session, eventId) != null) {
                return session;
            }
        }
        return null;
    }
    
    /**
     * Find an event within a session
     *
     * @param session The session to search
     * @param eventId The event ID to find
     * @return The found event, or null if not found
     */
    private RecordedEvent findEventById(RecordingSession session, UUID eventId) {
        for (RecordedEvent event : session.getEvents()) {
            if (event.getId().equals(eventId)) {
                return event;
            }
        }
        return null;
    }
    
    /**
     * Update metadata for a recording session
     *
     * @param sessionId The session ID
     * @param metadata The metadata to update
     * @return The updated recording session, or null if not found
     */
    public RecordingSession updateSessionMetadata(UUID sessionId, Map<String, Object> metadata) {
        RecordingSession session = activeSessions.get(sessionId);
        
        if (session == null) {
            return null;
        }
        
        if (session.getMetadata() == null) {
            session.setMetadata(new HashMap<>());
        }
        
        session.getMetadata().putAll(metadata);
        return session;
    }
    
    /**
     * Save a screenshot for a recording session
     *
     * @param sessionId The session ID
     * @param screenshotData Base64 encoded screenshot data
     * @return True if the screenshot was saved successfully
     */
    public boolean saveScreenshot(UUID sessionId, String screenshotData) {
        RecordingSession session = activeSessions.get(sessionId);
        
        if (session == null) {
            return false;
        }
        
        // Create a screenshot event
        RecordedEvent screenshotEvent = new RecordedEvent(RecordedEventType.SCREENSHOT);
        screenshotEvent.setUrl(session.getLastEvent() != null ? session.getLastEvent().getUrl() : "");
        
        // Store the screenshot data in the event metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("screenshotData", screenshotData);
        screenshotEvent.setMetadata(metadata);
        
        return session.addEvent(screenshotEvent);
    }
    
    /**
     * Save a recording session
     *
     * @param session The session to save
     * @return The saved session
     */
    public RecordingSession saveSession(RecordingSession session) {
        if (session.getId() == null) {
            session.setId(UUID.randomUUID());
        }
        
        activeSessions.put(session.getId(), session);
        sessionKeyMap.put(session.getSessionKey(), session.getId());
        logger.info("Saved recording session: {}", session.getId());
        
        return session;
    }
    
    /**
     * Start event collection for a session
     *
     * @param sessionId The session ID to start collecting events for
     */
    public void startEventCollection(UUID sessionId) {
        // Ensure the session exists
        RecordingSession session = getSession(sessionId);
        if (session != null) {
            // Initialize events list if needed
            if (session.getEvents() == null) {
                session.setEvents(new ArrayList<>());
            }
            
            logger.info("Started event collection for session: {}", sessionId);
        } else {
            logger.warn("Cannot start event collection - session not found: {}", sessionId);
        }
    }
} 