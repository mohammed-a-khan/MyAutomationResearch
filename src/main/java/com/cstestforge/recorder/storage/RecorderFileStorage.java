package com.cstestforge.recorder.storage;

import com.cstestforge.recorder.model.RecordedEvent;
import com.cstestforge.recorder.model.RecordingSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Storage service for persisting recording sessions to the file system.
 * Handles JSON serialization, screenshot storage, and indexing.
 */
@Component
public class RecorderFileStorage {
    
    private static final Logger logger = LoggerFactory.getLogger(RecorderFileStorage.class);
    
    @Value("${cstestforge.storage.base-path:./data}")
    private String basePath;
    
    private final Map<UUID, ReadWriteLock> sessionLocks = new ConcurrentHashMap<>();
    private final ReadWriteLock indexLock = new ReentrantReadWriteLock();
    private final ObjectMapper objectMapper;
    
    public RecorderFileStorage() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }
    
    @PostConstruct
    public void init() {
        try {
            // Ensure base directories exist
            createDirectoryIfNotExists(getRecordingsDirectory());
            createDirectoryIfNotExists(getScreenshotsDirectory());
            
            // Ensure index file exists
            Path indexPath = Paths.get(getRecordingsDirectory(), "_index.json");
            if (!Files.exists(indexPath)) {
                Files.write(indexPath, "{\"sessions\": []}".getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            logger.error("Failed to initialize storage directories", e);
        }
    }
    
    /**
     * Save a recording session to storage
     *
     * @param session The session to save
     * @throws IOException If the session cannot be saved
     */
    public void saveSession(RecordingSession session) throws IOException {
        if (session == null || session.getId() == null) {
            throw new IllegalArgumentException("Session cannot be null and must have an ID");
        }
        
        ReadWriteLock lock = getSessionLock(session.getId());
        Lock writeLock = lock.writeLock();
        
        try {
            writeLock.lock();
            
            // Ensure session directory exists
            String sessionDirPath = getSessionDirectory(session.getId());
            createDirectoryIfNotExists(sessionDirPath);
            
            // Save session data
            String sessionFilePath = getSessionFilePath(session.getId());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(sessionFilePath), session);
            
            // Update index
            updateIndex(session);
            
        } finally {
            writeLock.unlock();
        }
    }
    
    /**
     * Load a recording session from storage
     *
     * @param sessionId The session ID
     * @return The loaded session, or null if not found
     * @throws IOException If the session cannot be loaded
     */
    public RecordingSession loadSession(UUID sessionId) throws IOException {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID cannot be null");
        }
        
        ReadWriteLock lock = getSessionLock(sessionId);
        Lock readLock = lock.readLock();
        
        try {
            readLock.lock();
            
            String sessionFilePath = getSessionFilePath(sessionId);
            File sessionFile = new File(sessionFilePath);
            
            if (!sessionFile.exists()) {
                return null;
            }
            
            return objectMapper.readValue(sessionFile, RecordingSession.class);
        } finally {
            readLock.unlock();
        }
    }
    
    /**
     * Load all recording sessions from storage
     *
     * @return List of all sessions
     * @throws IOException If the sessions cannot be loaded
     */
    public List<RecordingSession> loadAllSessions() throws IOException {
        Lock readLock = indexLock.readLock();
        
        try {
            readLock.lock();
            
            Path indexPath = Paths.get(getRecordingsDirectory(), "_index.json");
            Map<String, List<Map<String, Object>>> index = objectMapper.readValue(indexPath.toFile(), Map.class);
            
            List<Map<String, Object>> sessions = index.get("sessions");
            List<RecordingSession> result = new ArrayList<>();
            
            if (sessions != null) {
                for (Map<String, Object> entry : sessions) {
                    String id = (String) entry.get("id");
                    if (id != null) {
                        try {
                            RecordingSession session = loadSession(UUID.fromString(id));
                            if (session != null) {
                                result.add(session);
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to load session {}", id, e);
                        }
                    }
                }
            }
            
            return result;
        } finally {
            readLock.unlock();
        }
    }
    
    /**
     * Delete a recording session from storage
     *
     * @param sessionId The session ID
     * @throws IOException If the session cannot be deleted
     */
    public void deleteSession(UUID sessionId) throws IOException {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID cannot be null");
        }
        
        ReadWriteLock lock = getSessionLock(sessionId);
        Lock writeLock = lock.writeLock();
        
        try {
            writeLock.lock();
            
            // Remove from index first
            removeFromIndex(sessionId);
            
            // Delete session directory
            String sessionDirPath = getSessionDirectory(sessionId);
            File sessionDir = new File(sessionDirPath);
            
            if (sessionDir.exists()) {
                deleteDirectory(sessionDir);
            }
            
        } finally {
            writeLock.unlock();
            
            // Clean up lock
            sessionLocks.remove(sessionId);
        }
    }
    
    /**
     * Save a screenshot for an event
     *
     * @param sessionId The session ID
     * @param eventId The event ID
     * @param screenshot Base64-encoded screenshot data
     * @throws IOException If the screenshot cannot be saved
     */
    public void saveScreenshot(UUID sessionId, UUID eventId, String screenshot) throws IOException {
        if (sessionId == null || eventId == null) {
            throw new IllegalArgumentException("Session ID and event ID cannot be null");
        }
        
        if (screenshot == null || screenshot.isEmpty()) {
            throw new IllegalArgumentException("Screenshot data cannot be null or empty");
        }
        
        ReadWriteLock lock = getSessionLock(sessionId);
        Lock writeLock = lock.writeLock();
        
        try {
            writeLock.lock();
            
            // Ensure screenshots directory exists
            String screenshotsDirPath = getSessionScreenshotsDirectory(sessionId);
            createDirectoryIfNotExists(screenshotsDirPath);
            
            // Save screenshot
            String screenshotPath = getEventScreenshotPath(sessionId, eventId);
            
            // If the screenshot is Base64-encoded, decode it
            if (screenshot.startsWith("data:image")) {
                String base64Data = screenshot.substring(screenshot.indexOf(",") + 1);
                byte[] decodedData = Base64.getDecoder().decode(base64Data);
                Files.write(Paths.get(screenshotPath), decodedData);
            } else {
                byte[] decodedData = Base64.getDecoder().decode(screenshot);
                Files.write(Paths.get(screenshotPath), decodedData);
            }
            
        } finally {
            writeLock.unlock();
        }
    }
    
    /**
     * Load a screenshot for an event
     *
     * @param sessionId The session ID
     * @param eventId The event ID
     * @return The screenshot data as a Base64-encoded string, or null if not found
     * @throws IOException If the screenshot cannot be loaded
     */
    public String loadScreenshot(UUID sessionId, UUID eventId) throws IOException {
        if (sessionId == null || eventId == null) {
            throw new IllegalArgumentException("Session ID and event ID cannot be null");
        }
        
        ReadWriteLock lock = getSessionLock(sessionId);
        Lock readLock = lock.readLock();
        
        try {
            readLock.lock();
            
            String screenshotPath = getEventScreenshotPath(sessionId, eventId);
            File screenshotFile = new File(screenshotPath);
            
            if (!screenshotFile.exists()) {
                return null;
            }
            
            byte[] fileData = Files.readAllBytes(screenshotFile.toPath());
            return Base64.getEncoder().encodeToString(fileData);
            
        } finally {
            readLock.unlock();
        }
    }
    
    /**
     * Update the session index
     *
     * @param session The session to update in the index
     * @throws IOException If the index cannot be updated
     */
    private void updateIndex(RecordingSession session) throws IOException {
        Lock writeLock = indexLock.writeLock();
        
        try {
            writeLock.lock();
            
            Path indexPath = Paths.get(getRecordingsDirectory(), "_index.json");
            Map<String, List<Map<String, Object>>> index;
            
            if (Files.exists(indexPath)) {
                index = objectMapper.readValue(indexPath.toFile(), Map.class);
            } else {
                index = new HashMap<>();
                index.put("sessions", new ArrayList<>());
            }
            
            List<Map<String, Object>> sessions = index.get("sessions");
            
            // Remove existing entry if present
            sessions.removeIf(entry -> session.getId().toString().equals(entry.get("id")));
            
            // Add/update entry
            Map<String, Object> sessionEntry = new HashMap<>();
            sessionEntry.put("id", session.getId().toString());
            sessionEntry.put("name", session.getName());
            sessionEntry.put("projectId", session.getProjectId());
            sessionEntry.put("createdBy", session.getCreatedBy());
            sessionEntry.put("createTime", session.getStartTime().toString());
            sessionEntry.put("modifiedTime", Instant.now().toString());
            sessionEntry.put("status", session.getStatus().toString());
            sessionEntry.put("eventCount", session.getEvents().size());
            
            sessions.add(sessionEntry);
            
            // Write updated index
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(indexPath.toFile(), index);
            
        } finally {
            writeLock.unlock();
        }
    }
    
    /**
     * Remove a session from the index
     *
     * @param sessionId The session ID
     * @throws IOException If the index cannot be updated
     */
    private void removeFromIndex(UUID sessionId) throws IOException {
        Lock writeLock = indexLock.writeLock();
        
        try {
            writeLock.lock();
            
            Path indexPath = Paths.get(getRecordingsDirectory(), "_index.json");
            
            if (!Files.exists(indexPath)) {
                return;
            }
            
            Map<String, List<Map<String, Object>>> index = objectMapper.readValue(indexPath.toFile(), Map.class);
            List<Map<String, Object>> sessions = index.get("sessions");
            
            if (sessions != null) {
                sessions.removeIf(entry -> sessionId.toString().equals(entry.get("id")));
                
                // Write updated index
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(indexPath.toFile(), index);
            }
            
        } finally {
            writeLock.unlock();
        }
    }
    
    /**
     * Get the session lock for thread safety
     *
     * @param sessionId The session ID
     * @return The session lock
     */
    private ReadWriteLock getSessionLock(UUID sessionId) {
        return sessionLocks.computeIfAbsent(sessionId, id -> new ReentrantReadWriteLock());
    }
    
    /**
     * Get the base recordings directory
     *
     * @return Path to recordings directory
     */
    private String getRecordingsDirectory() {
        return Paths.get(basePath, "recordings").toString();
    }
    
    /**
     * Get the base screenshots directory
     *
     * @return Path to screenshots directory
     */
    private String getScreenshotsDirectory() {
        return Paths.get(basePath, "screenshots").toString();
    }
    
    /**
     * Get the directory for a specific session
     *
     * @param sessionId The session ID
     * @return Path to session directory
     */
    private String getSessionDirectory(UUID sessionId) {
        return Paths.get(getRecordingsDirectory(), sessionId.toString()).toString();
    }
    
    /**
     * Get the screenshots directory for a specific session
     *
     * @param sessionId The session ID
     * @return Path to session screenshots directory
     */
    private String getSessionScreenshotsDirectory(UUID sessionId) {
        return Paths.get(getSessionDirectory(sessionId), "screenshots").toString();
    }
    
    /**
     * Get the path to the session JSON file
     *
     * @param sessionId The session ID
     * @return Path to session file
     */
    private String getSessionFilePath(UUID sessionId) {
        return Paths.get(getSessionDirectory(sessionId), "session.json").toString();
    }
    
    /**
     * Get the path to an event's screenshot file
     *
     * @param sessionId The session ID
     * @param eventId The event ID
     * @return Path to screenshot file
     */
    private String getEventScreenshotPath(UUID sessionId, UUID eventId) {
        return Paths.get(getSessionScreenshotsDirectory(sessionId), eventId.toString() + ".png").toString();
    }
    
    /**
     * Create a directory if it doesn't exist
     *
     * @param directoryPath Path to the directory
     * @throws IOException If the directory cannot be created
     */
    private void createDirectoryIfNotExists(String directoryPath) throws IOException {
        Path path = Paths.get(directoryPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }
    
    /**
     * Recursively delete a directory
     *
     * @param directory Directory to delete
     * @throws IOException If the directory cannot be deleted
     */
    private void deleteDirectory(File directory) throws IOException {
        File[] files = directory.listFiles();
        
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    if (!file.delete()) {
                        throw new IOException("Failed to delete file: " + file);
                    }
                }
            }
        }
        
        if (!directory.delete()) {
            throw new IOException("Failed to delete directory: " + directory);
        }
    }
} 