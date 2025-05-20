package com.cstestforge.recorder.repository;

import com.cstestforge.recorder.model.RecordedEvent;
import com.cstestforge.recorder.model.RecordingMetadata;
import java.util.List;
import java.util.UUID;

/**
 * Repository for managing recorded test events
 */
public interface RecordingRepository {
    
    /**
     * Create a new recording session
     * 
     * @param projectId Project ID
     * @param name Session name
     * @param description Session description
     * @return Recording metadata with generated session ID
     */
    RecordingMetadata createSession(String projectId, String name, String description);
    
    /**
     * Get recording session metadata
     * 
     * @param sessionId Session ID
     * @return Recording metadata or null if not found
     */
    RecordingMetadata getRecordingMetadata(String sessionId);
    
    /**
     * Save a recorded event
     * 
     * @param sessionId Session ID
     * @param event Recorded event
     * @return Saved event with assigned ID
     */
    RecordedEvent saveEvent(String sessionId, RecordedEvent event);
    
    /**
     * Save multiple recorded events in batch
     * 
     * @param sessionId Session ID
     * @param events List of recorded events
     * @return List of saved events with assigned IDs
     */
    List<RecordedEvent> saveEvents(String sessionId, List<RecordedEvent> events);
    
    /**
     * Find all events for a recording session
     * 
     * @param sessionId Session ID
     * @return List of recorded events
     */
    List<RecordedEvent> findEventsBySessionId(String sessionId);
    
    /**
     * Find a specific event by ID
     * 
     * @param sessionId Session ID
     * @param eventId Event ID
     * @return Recorded event or null if not found
     */
    RecordedEvent findEventById(String sessionId, UUID eventId);
    
    /**
     * Delete a recording session and all its events
     * 
     * @param sessionId Session ID
     * @return True if deleted successfully
     */
    boolean deleteSession(String sessionId);
    
    /**
     * Update recording session metadata
     * 
     * @param sessionId Session ID
     * @param metadata Updated metadata
     * @return Updated metadata
     */
    RecordingMetadata updateSessionMetadata(String sessionId, RecordingMetadata metadata);
} 