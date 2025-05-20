package com.cstestforge.recorder.websocket;

import com.cstestforge.recorder.model.RecordedEvent;
import com.cstestforge.recorder.model.RecordingSession;
import com.cstestforge.recorder.model.RecordingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for real-time WebSocket communication with clients.
 * Handles notifications about recorder events and status updates.
 */
@Service
public class RecorderWebSocketService {
    
    private static final Logger logger = LoggerFactory.getLogger(RecorderWebSocketService.class);
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    // Map of session IDs to user principals
    private final Map<UUID, String> sessionSubscriptions = new ConcurrentHashMap<>();
    
    /**
     * Send a notification about a new recording session
     *
     * @param session The new session
     */
    public void notifySessionCreated(RecordingSession session) {
        if (session == null) {
            return;
        }
        
        Map<String, Object> message = new HashMap<>();
        message.put("type", "SESSION_CREATED");
        message.put("payload", session);
        
        // Broadcast to all subscribers of the project topic
        String destination = "/topic/project/" + session.getProjectId();
        sendMessage(destination, message);
        
        logger.debug("Sent session created notification for session {}", session.getId());
    }
    
    /**
     * Send a notification about a session status change
     *
     * @param sessionId The session ID
     * @param status The new status
     */
    public void notifySessionStatusChanged(UUID sessionId, RecordingStatus status) {
        if (sessionId == null || status == null) {
            return;
        }
        
        Map<String, Object> message = new HashMap<>();
        message.put("type", "SESSION_STATUS_CHANGED");
        message.put("sessionId", sessionId.toString());
        message.put("status", status.toString());
        
        // Send to the session-specific topic
        String destination = "/topic/session/" + sessionId;
        sendMessage(destination, message);
        
        logger.debug("Sent status changed notification for session {} to status {}", sessionId, status);
    }
    
    /**
     * Send a notification about a new event added to a session
     *
     * @param sessionId The session ID
     * @param event The new event
     */
    public void notifyEventAdded(UUID sessionId, RecordedEvent event) {
        if (sessionId == null || event == null) {
            return;
        }
        
        Map<String, Object> message = new HashMap<>();
        message.put("type", "EVENT_ADDED");
        message.put("sessionId", sessionId.toString());
        message.put("event", event);
        
        // Send to the session-specific topic
        String destination = "/topic/session/" + sessionId;
        sendMessage(destination, message);
        
        logger.debug("Sent event added notification for session {} event {}", sessionId, event.getId());
    }
    
    /**
     * Send a notification about an event update
     *
     * @param sessionId The session ID
     * @param event The updated event
     */
    public void notifyEventUpdated(UUID sessionId, RecordedEvent event) {
        if (sessionId == null || event == null) {
            return;
        }
        
        Map<String, Object> message = new HashMap<>();
        message.put("type", "EVENT_UPDATED");
        message.put("sessionId", sessionId.toString());
        message.put("event", event);
        
        // Send to the session-specific topic
        String destination = "/topic/session/" + sessionId;
        sendMessage(destination, message);
        
        logger.debug("Sent event updated notification for session {} event {}", sessionId, event.getId());
    }
    
    /**
     * Send a notification about an event being deleted
     *
     * @param sessionId The session ID
     * @param eventId The event ID
     */
    public void notifyEventDeleted(UUID sessionId, UUID eventId) {
        if (sessionId == null || eventId == null) {
            return;
        }
        
        Map<String, Object> message = new HashMap<>();
        message.put("type", "EVENT_DELETED");
        message.put("sessionId", sessionId.toString());
        message.put("eventId", eventId.toString());
        
        // Send to the session-specific topic
        String destination = "/topic/session/" + sessionId;
        sendMessage(destination, message);
        
        logger.debug("Sent event deleted notification for session {} event {}", sessionId, eventId);
    }
    
    /**
     * Send a notification about an error in a session
     *
     * @param sessionId The session ID
     * @param errorCode The error code
     * @param errorMessage The error message
     */
    public void notifySessionError(UUID sessionId, String errorCode, String errorMessage) {
        if (sessionId == null) {
            return;
        }
        
        Map<String, Object> message = new HashMap<>();
        message.put("type", "SESSION_ERROR");
        message.put("sessionId", sessionId.toString());
        message.put("errorCode", errorCode);
        message.put("errorMessage", errorMessage);
        
        // Send to the session-specific topic
        String destination = "/topic/session/" + sessionId;
        sendMessage(destination, message);
        
        logger.debug("Sent error notification for session {}: {}", sessionId, errorMessage);
    }
    
    /**
     * Get the WebSocket host information for connections
     * 
     * @return The host string to use for WebSocket connections
     */
    public String getWebSocketHost() {
        // By default, use window.location.host from the browser
        // This allows the WebSocket to connect to the same host as the UI
        return "${window.location.host}";
    }
    
    /**
     * Register a subscriber for a session
     *
     * @param sessionId The session ID
     * @param username The username
     */
    public void registerSessionSubscriber(UUID sessionId, String username) {
        if (sessionId != null && username != null) {
            sessionSubscriptions.put(sessionId, username);
            logger.debug("Registered subscriber {} for session {}", username, sessionId);
        }
    }
    
    /**
     * Unregister a user as a subscriber from a session
     *
     * @param sessionId The session ID
     */
    public void unregisterSessionSubscriber(UUID sessionId) {
        if (sessionId == null) {
            return;
        }
        
        String username = sessionSubscriptions.remove(sessionId);
        if (username != null) {
            logger.debug("User {} unregistered as subscriber for session {}", username, sessionId);
        }
    }
    
    /**
     * Get the username of a session subscriber
     *
     * @param sessionId The session ID
     * @return The username, or null if not found
     */
    public String getSessionSubscriber(UUID sessionId) {
        if (sessionId == null) {
            return null;
        }
        
        return sessionSubscriptions.get(sessionId);
    }
    
    /**
     * Send a message to a topic
     *
     * @param destination The destination topic
     * @param message The message
     */
    private void sendMessage(String destination, Object message) {
        try {
            logger.debug("Sending WebSocket message to {}: {}", destination, message);
            messagingTemplate.convertAndSend(destination, message);
            logger.debug("Successfully sent WebSocket message to {}", destination);
        } catch (Exception e) {
            logger.error("Failed to send WebSocket message to {}: {}", destination, e.getMessage(), e);
        }
    }
    
    /**
     * Get the count of active session subscriptions
     * 
     * @return The number of active subscriptions
     */
    public int getActiveSubscriptionsCount() {
        return sessionSubscriptions.size();
    }
    
    /**
     * Utility method to log all active WebSocket subscriptions
     */
    public void logActiveSubscriptions() {
        logger.info("Current WebSocket subscriptions count: {}", sessionSubscriptions.size());
        sessionSubscriptions.forEach((sessionId, username) -> 
            logger.info("Session: {}, Subscriber: {}", sessionId, username)
        );
    }
} 