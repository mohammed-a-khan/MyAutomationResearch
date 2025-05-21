package com.cstestforge.recorder.service;

import com.cstestforge.recorder.model.*;
import com.cstestforge.recorder.model.LoopConfig;
import com.cstestforge.recorder.model.events.LoopEvent;
import com.cstestforge.recorder.storage.RecorderFileStorage;
import com.cstestforge.recorder.repository.RecordingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
    private final Map<UUID, LoopEvent> loopEvents = new ConcurrentHashMap<>();

    @Autowired
    private RecorderFileStorage fileStorage;

    @Autowired(required = false)
    private RecordingRepository recordingRepository;

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

        // Store the session in memory
        activeSessions.put(session.getId(), session);
        sessionKeyMap.put(session.getSessionKey(), session.getId());

        // Persist the session
        try {
            fileStorage.saveSession(session);

            // If repository is available, also save there
            if (recordingRepository != null) {
                RecordingMetadata metadata = new RecordingMetadata(
                        session.getId().toString(),
                        projectId,
                        name);
                metadata.setStartTime(LocalDateTime.ofInstant(session.getStartTime(), ZoneId.systemDefault()));
                recordingRepository.createSession(projectId, name, "");
            }
        } catch (IOException e) {
            logger.error("Failed to persist session {}: {}", session.getId(), e.getMessage(), e);
        }

        return session;
    }

    /**
     * Get a recording session by ID
     *
     * @param sessionId The session ID
     * @return The recording session, or null if not found
     */
    public RecordingSession getSession(UUID sessionId) {
        // Try to get from memory first
        RecordingSession session = activeSessions.get(sessionId);

        // If not in memory, try to load from storage
        if (session == null) {
            try {
                session = fileStorage.loadSession(sessionId);
                if (session != null) {
                    // Add to memory cache
                    activeSessions.put(sessionId, session);
                    sessionKeyMap.put(session.getSessionKey(), sessionId);
                }
            } catch (IOException e) {
                logger.error("Failed to load session {}: {}", sessionId, e.getMessage(), e);
            }
        }

        return session;
    }

    /**
     * Get a recording session by session key
     *
     * @param sessionKey The session key
     * @return The recording session, or null if not found
     */
    public RecordingSession getSessionByKey(String sessionKey) {
        UUID sessionId = sessionKeyMap.get(sessionKey);
        return sessionId != null ? getSession(sessionId) : null;
    }

    /**
     * Add an event to a recording session
     *
     * @param sessionId The session ID
     * @param event The event to add
     * @return True if the event was added successfully
     */
    public boolean addEvent(UUID sessionId, RecordedEvent event) {
        RecordingSession session = getSession(sessionId);

        if (session == null || session.getStatus() != RecordingStatus.ACTIVE) {
            return false;
        }

        // Add event to session
        boolean added = session.addEvent(event);

        if (added) {
            // Persist session
            try {
                fileStorage.saveSession(session);

                // If repository is available, also save there
                if (recordingRepository != null) {
                    recordingRepository.saveEvent(sessionId.toString(), event);
                }
            } catch (IOException e) {
                logger.error("Failed to persist session after adding event: {}", e.getMessage(), e);
            }
        }

        return added;
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

        return addEvent(session.getId(), event);
    }

    /**
     * Update the status of a recording session
     *
     * @param sessionId The session ID
     * @param status The new status
     * @return The updated recording session, or null if not found
     */
    public RecordingSession updateSessionStatus(UUID sessionId, RecordingStatus status) {
        RecordingSession session = getSession(sessionId);

        if (session == null) {
            return null;
        }

        session.setStatus(status);

        // If the session is completed or has an error, update the end time
        if (status == RecordingStatus.COMPLETED || status == RecordingStatus.FAILED) {
            session.setEndTime(Instant.now());
        }

        // Persist the updated session
        try {
            fileStorage.saveSession(session);

            // If repository is available, also update there
            if (recordingRepository != null) {
                RecordingMetadata metadata = recordingRepository.getRecordingMetadata(sessionId.toString());
                if (metadata != null) {
                    // Store status in metadata map since there's no direct status field
                    metadata.addMetadata("status", status.name());
                    if (status == RecordingStatus.COMPLETED || status == RecordingStatus.FAILED) {
                        metadata.setEndTime(LocalDateTime.ofInstant(session.getEndTime(), ZoneId.systemDefault()));
                    }
                    recordingRepository.updateSessionMetadata(sessionId.toString(), metadata);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to persist session status update: {}", e.getMessage(), e);
        }

        return session;
    }

    /**
     * Get all sessions for a project
     *
     * @param projectId The project ID
     * @return List of sessions for the project
     */
    public List<RecordingSession> getSessionsByProject(String projectId) {
        List<RecordingSession> result = new ArrayList<>();

        try {
            // Load all sessions from storage
            List<RecordingSession> allSessions = fileStorage.loadAllSessions();

            // Filter by project ID
            for (RecordingSession session : allSessions) {
                if (projectId.equals(session.getProjectId())) {
                    result.add(session);

                    // Update memory cache
                    activeSessions.put(session.getId(), session);
                    sessionKeyMap.put(session.getSessionKey(), session.getId());
                }
            }
        } catch (IOException e) {
            logger.error("Failed to load sessions for project {}: {}", projectId, e.getMessage(), e);
        }

        return result;
    }

    /**
     * Get all active sessions
     *
     * @return List of active sessions
     */
    public List<RecordingSession> getActiveSessions() {
        List<RecordingSession> result = new ArrayList<>();

        try {
            // Load all sessions from storage
            List<RecordingSession> allSessions = fileStorage.loadAllSessions();

            // Filter by status
            for (RecordingSession session : allSessions) {
                if (session.getStatus() == RecordingStatus.ACTIVE) {
                    result.add(session);

                    // Update memory cache
                    activeSessions.put(session.getId(), session);
                    sessionKeyMap.put(session.getSessionKey(), session.getId());
                }
            }
        } catch (IOException e) {
            logger.error("Failed to load active sessions: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * Get all recording sessions
     *
     * @return List of all recording sessions
     */
    public List<RecordingSession> getAllSessions() {
        try {
            // Load all sessions from storage
            List<RecordingSession> allSessions = fileStorage.loadAllSessions();

            // Update memory cache
            for (RecordingSession session : allSessions) {
                activeSessions.put(session.getId(), session);
                sessionKeyMap.put(session.getSessionKey(), session.getId());
            }

            return allSessions;
        } catch (IOException e) {
            logger.error("Failed to load all sessions: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Delete a recording session
     *
     * @param sessionId The session ID
     * @return True if the session was deleted
     */
    public boolean deleteSession(UUID sessionId) {
        try {
            // Remove from memory
            RecordingSession session = activeSessions.remove(sessionId);
            if (session != null) {
                sessionKeyMap.remove(session.getSessionKey());
            }

            // Remove from file storage
            fileStorage.deleteSession(sessionId);

            // If repository is available, also delete there
            if (recordingRepository != null) {
                recordingRepository.deleteSession(sessionId.toString());
            }

            return true;
        } catch (IOException e) {
            logger.error("Failed to delete session {}: {}", sessionId, e.getMessage(), e);
            return false;
        }
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
        LoopEvent loopEvent = new LoopEvent(parentEvent.getUrl(), loopConfig);

        // Store in our loop events cache
        loopEvents.put(loopEvent.getId(), loopEvent);

        // Add to the session
        if (session.addEvent(loopEvent)) {
            // Persist the updated session
            try {
                fileStorage.saveSession(session);
            } catch (IOException e) {
                logger.error("Failed to persist session after adding loop event: {}", e.getMessage(), e);
            }
        }

        // Create and return the DTO
        return new com.cstestforge.recorder.model.LoopEvent(parentEvent.getUrl(), loopConfig);
    }

    /**
     * Update a loop event
     *
     * @param eventId The event ID
     * @param loopConfig The updated loop configuration
     * @return The updated loop event, or null if not found
     */
    public com.cstestforge.recorder.model.LoopEvent updateLoopEvent(UUID eventId, LoopConfig loopConfig) {
        LoopEvent loopEvent = loopEvents.get(eventId);

        if (loopEvent == null) {
            // Try to find it in sessions if not in cache
            for (RecordingSession session : activeSessions.values()) {
                RecordedEvent event = findEventById(session, eventId);
                if (event instanceof LoopEvent) {
                    loopEvent = (LoopEvent) event;
                    loopEvents.put(eventId, loopEvent);
                    break;
                }
            }

            if (loopEvent == null) {
                // Try to find it in stored sessions
                try {
                    List<RecordingSession> allSessions = fileStorage.loadAllSessions();
                    for (RecordingSession session : allSessions) {
                        RecordedEvent event = findEventById(session, eventId);
                        if (event instanceof LoopEvent) {
                            loopEvent = (LoopEvent) event;
                            loopEvents.put(eventId, loopEvent);
                            break;
                        }
                    }
                } catch (IOException e) {
                    logger.error("Failed to load sessions to find loop event: {}", e.getMessage(), e);
                }

                if (loopEvent == null) {
                    return null;
                }
            }
        }

        // Update the configuration
        loopEvent.setLoopConfig(loopConfig);

        // Update the session containing the loop event
        RecordingSession session = findSessionByEventId(eventId);
        if (session != null) {
            try {
                fileStorage.saveSession(session);
            } catch (IOException e) {
                logger.error("Failed to persist session after updating loop event: {}", e.getMessage(), e);
            }
        }

        // Create and return the DTO
        return new com.cstestforge.recorder.model.LoopEvent(loopEvent.getUrl(), loopConfig);
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

        // Update the session containing the loop event
        RecordingSession session = findSessionByEventId(loopEventId);
        if (session != null) {
            try {
                fileStorage.saveSession(session);
            } catch (IOException e) {
                logger.error("Failed to persist session after adding event to loop: {}", e.getMessage(), e);
            }
        }

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

                // Update the session containing the loop event
                RecordingSession session = findSessionByEventId(loopEventId);
                if (session != null) {
                    try {
                        fileStorage.saveSession(session);
                    } catch (IOException e) {
                        logger.error("Failed to persist session after removing event from loop: {}", e.getMessage(), e);
                    }
                }

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
            // Try to find it in sessions if not in cache
            for (RecordingSession session : activeSessions.values()) {
                RecordedEvent event = findEventById(session, loopEventId);
                if (event instanceof LoopEvent) {
                    loopEvent = (LoopEvent) event;
                    loopEvents.put(loopEventId, loopEvent);
                    break;
                }
            }

            if (loopEvent == null) {
                return null;
            }
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
        // First check active sessions in memory
        for (RecordingSession session : activeSessions.values()) {
            if (findEventById(session, eventId) != null) {
                return session;
            }
        }

        // If not found, check all sessions from storage
        try {
            List<RecordingSession> allSessions = fileStorage.loadAllSessions();
            for (RecordingSession session : allSessions) {
                if (findEventById(session, eventId) != null) {
                    // Update memory cache
                    activeSessions.put(session.getId(), session);
                    sessionKeyMap.put(session.getSessionKey(), session.getId());
                    return session;
                }
            }
        } catch (IOException e) {
            logger.error("Failed to load sessions to find event: {}", e.getMessage(), e);
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

            // Also check nested events if this is a loop event
            if (event instanceof LoopEvent) {
                LoopEvent loopEvent = (LoopEvent) event;
                for (RecordedEvent nestedEvent : loopEvent.getNestedEvents()) {
                    if (nestedEvent.getId().equals(eventId)) {
                        return nestedEvent;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Save a screenshot for an event
     *
     * @param sessionId The session ID
     * @param eventId The event ID
     * @param screenshot Base64-encoded screenshot data
     * @return True if saved successfully
     */
    public boolean saveScreenshot(UUID sessionId, UUID eventId, String screenshot) {
        try {
            fileStorage.saveScreenshot(sessionId, eventId, screenshot);
            return true;
        } catch (IOException e) {
            logger.error("Failed to save screenshot for event {}: {}", eventId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Load a screenshot for an event
     *
     * @param sessionId The session ID
     * @param eventId The event ID
     * @return Base64-encoded screenshot data, or null if not found
     */
    public String loadScreenshot(UUID sessionId, UUID eventId) {
        try {
            return fileStorage.loadScreenshot(sessionId, eventId);
        } catch (IOException e) {
            logger.error("Failed to load screenshot for event {}: {}", eventId, e.getMessage(), e);
            return null;
        }
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

        // Persist to storage
        try {
            fileStorage.saveSession(session);
        } catch (IOException e) {
            logger.error("Failed to persist session {}: {}", session.getId(), e.getMessage(), e);
        }

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