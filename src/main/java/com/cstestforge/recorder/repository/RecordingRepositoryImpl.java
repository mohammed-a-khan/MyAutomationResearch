package com.cstestforge.recorder.repository;

import com.cstestforge.project.storage.FileStorageService;
import com.cstestforge.recorder.model.RecordedEvent;
import com.cstestforge.recorder.model.RecordingMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of the RecordingRepository interface
 */
@Repository
public class RecordingRepositoryImpl implements RecordingRepository {

    private static final String RECORDINGS_PATH = "recordings";
    private static final String METADATA_FILE = "metadata.json";
    private static final String EVENTS_FILE = "events.json";
    
    private final FileStorageService storageService;
    
    @Autowired
    public RecordingRepositoryImpl(FileStorageService storageService) {
        this.storageService = storageService;
        
        // Ensure recordings directory exists
        storageService.createDirectory(RECORDINGS_PATH);
    }

    @Override
    public RecordingMetadata createSession(String projectId, String name, String description) {
        String sessionId = UUID.randomUUID().toString();
        RecordingMetadata metadata = new RecordingMetadata(sessionId, projectId, name);
        metadata.setDescription(description);
        metadata.setStartTime(LocalDateTime.now());
        
        // Create directory for session
        String sessionPath = getSessionPath(sessionId);
        storageService.createDirectory(sessionPath);
        
        // Save metadata
        saveMetadata(sessionId, metadata);
        
        return metadata;
    }

    @Override
    public RecordingMetadata getRecordingMetadata(String sessionId) {
        String metadataPath = getMetadataPath(sessionId);
        if (!storageService.fileExists(metadataPath)) {
            return null;
        }
        
        return storageService.readFromJson(metadataPath, RecordingMetadata.class);
    }

    @Override
    public RecordedEvent saveEvent(String sessionId, RecordedEvent event) {
        // Get current events
        List<RecordedEvent> events = findEventsBySessionId(sessionId);
        
        // Add new event
        events.add(event);
        
        // Save all events
        String eventsPath = getEventsPath(sessionId);
        storageService.saveToJson(eventsPath, events);
        
        // Update metadata event count
        updateEventCount(sessionId, events.size());
        
        return event;
    }

    @Override
    public List<RecordedEvent> saveEvents(String sessionId, List<RecordedEvent> newEvents) {
        // Get current events
        List<RecordedEvent> allEvents = findEventsBySessionId(sessionId);
        
        // Add new events
        allEvents.addAll(newEvents);
        
        // Save all events
        String eventsPath = getEventsPath(sessionId);
        storageService.saveToJson(eventsPath, allEvents);
        
        // Update metadata event count
        updateEventCount(sessionId, allEvents.size());
        
        return newEvents;
    }

    @Override
    public List<RecordedEvent> findEventsBySessionId(String sessionId) {
        String eventsPath = getEventsPath(sessionId);
        if (!storageService.fileExists(eventsPath)) {
            return new ArrayList<>();
        }
        
        List<RecordedEvent> events = storageService.readListFromJson(eventsPath, RecordedEvent.class);
        return events != null ? events : new ArrayList<>();
    }

    @Override
    public RecordedEvent findEventById(String sessionId, UUID eventId) {
        List<RecordedEvent> events = findEventsBySessionId(sessionId);
        
        return events.stream()
                .filter(e -> e.getId().equals(eventId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean deleteSession(String sessionId) {
        String sessionPath = getSessionPath(sessionId);
        return storageService.deleteDirectory(sessionPath);
    }

    @Override
    public RecordingMetadata updateSessionMetadata(String sessionId, RecordingMetadata metadata) {
        saveMetadata(sessionId, metadata);
        return metadata;
    }
    
    /**
     * Save metadata to storage
     * 
     * @param sessionId Session ID
     * @param metadata Metadata to save
     */
    private void saveMetadata(String sessionId, RecordingMetadata metadata) {
        String metadataPath = getMetadataPath(sessionId);
        storageService.saveToJson(metadataPath, metadata);
    }
    
    /**
     * Update event count in metadata
     * 
     * @param sessionId Session ID
     * @param count New event count
     */
    private void updateEventCount(String sessionId, int count) {
        RecordingMetadata metadata = getRecordingMetadata(sessionId);
        if (metadata != null) {
            metadata.setEventCount(count);
            saveMetadata(sessionId, metadata);
        }
    }
    
    /**
     * Get the path to the session directory
     * 
     * @param sessionId Session ID
     * @return Path to session directory
     */
    private String getSessionPath(String sessionId) {
        return RECORDINGS_PATH + "/" + sessionId;
    }
    
    /**
     * Get the path to the metadata file
     * 
     * @param sessionId Session ID
     * @return Path to metadata file
     */
    private String getMetadataPath(String sessionId) {
        return getSessionPath(sessionId) + "/" + METADATA_FILE;
    }
    
    /**
     * Get the path to the events file
     * 
     * @param sessionId Session ID
     * @return Path to events file
     */
    private String getEventsPath(String sessionId) {
        return getSessionPath(sessionId) + "/" + EVENTS_FILE;
    }
} 