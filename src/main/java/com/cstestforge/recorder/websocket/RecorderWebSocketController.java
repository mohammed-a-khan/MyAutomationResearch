package com.cstestforge.recorder.websocket;

import com.cstestforge.recorder.model.RecordedEvent;
import com.cstestforge.recorder.model.RecordingSession;
import com.cstestforge.recorder.service.RecorderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * WebSocket controller for handling real-time recorder updates.
 */
@Controller
public class RecorderWebSocketController {
    
    private static final Logger logger = LoggerFactory.getLogger(RecorderWebSocketController.class);
    
    @Autowired
    private RecorderService recorderService;
    
    @Autowired
    private RecorderWebSocketService webSocketService;
    
    /**
     * Handle subscription to a recording session
     *
     * @param sessionId The session ID
     * @param headerAccessor The message headers
     * @return Session state information
     */
    @SubscribeMapping("/session/{sessionId}")
    public WebSocketMessage<Map<String, Object>> subscribeToSession(
            @DestinationVariable UUID sessionId,
            SimpMessageHeaderAccessor headerAccessor) {
        
        String username = headerAccessor.getUser() != null ? 
                headerAccessor.getUser().getName() : "anonymous";
        
        logger.debug("User {} subscribed to session {}", username, sessionId);
        
        // Register the subscriber
        webSocketService.registerSessionSubscriber(sessionId, username);
        
        // Get session information
        RecordingSession session = recorderService.getSession(sessionId);
        
        Map<String, Object> payload = new HashMap<>();
        if (session != null) {
            payload.put("session", session);
            payload.put("status", session.getStatus());
            payload.put("eventCount", session.getEvents().size());
            payload.put("hasActiveSubscriber", true);
        } else {
            payload.put("error", "Session not found");
        }
        
        return WebSocketMessage.of("SESSION_STATE", payload);
    }
    
    /**
     * Handle recording session commands
     *
     * @param sessionId The session ID
     * @param message The message with command
     * @param headerAccessor The message headers
     * @return Command result
     */
    @MessageMapping("/session/{sessionId}/command")
    @SendTo("/topic/session/{sessionId}")
    public WebSocketMessage<?> handleSessionCommand(
            @DestinationVariable UUID sessionId,
            @Payload WebSocketMessage<Map<String, Object>> message,
            SimpMessageHeaderAccessor headerAccessor) {
        
        String username = headerAccessor.getUser() != null ? 
                headerAccessor.getUser().getName() : "anonymous";
        
        String command = message.getType();
        Map<String, Object> payload = message.getPayload();
        
        logger.debug("Received command {} for session {} from user {}", command, sessionId, username);
        
        // Process the command
        Map<String, Object> result = new HashMap<>();
        result.put("command", command);
        
        switch (command) {
            case "PING":
                result.put("status", "OK");
                result.put("time", System.currentTimeMillis());
                break;
                
            case "ADD_EVENT":
                try {
                    RecordedEvent event = (RecordedEvent) payload.get("event");
                    boolean success = recorderService.addEvent(sessionId, event);
                    result.put("success", success);
                    result.put("eventId", event.getId().toString());
                } catch (Exception e) {
                    logger.error("Error adding event to session {}", sessionId, e);
                    result.put("success", false);
                    result.put("error", e.getMessage());
                }
                break;
                
            case "UPDATE_EVENT":
                try {
                    RecordedEvent event = (RecordedEvent) payload.get("event");
                    UUID eventId = event.getId();
                    result.put("success", true);
                    result.put("eventId", eventId.toString());
                    // The service will handle notifying clients about the update
                } catch (Exception e) {
                    logger.error("Error updating event in session {}", sessionId, e);
                    result.put("success", false);
                    result.put("error", e.getMessage());
                }
                break;
                
            case "DELETE_EVENT":
                try {
                    UUID eventId = UUID.fromString((String) payload.get("eventId"));
                    // The service will handle notifying clients about the deletion
                    result.put("success", true);
                    result.put("eventId", eventId.toString());
                } catch (Exception e) {
                    logger.error("Error deleting event from session {}", sessionId, e);
                    result.put("success", false);
                    result.put("error", e.getMessage());
                }
                break;
                
            case "REORDER_EVENT":
                try {
                    UUID eventId = UUID.fromString((String) payload.get("eventId"));
                    int newIndex = (int) payload.get("newIndex");
                    // The service will handle notifying clients about the reorder
                    result.put("success", true);
                    result.put("eventId", eventId.toString());
                    result.put("newIndex", newIndex);
                } catch (Exception e) {
                    logger.error("Error reordering event in session {}", sessionId, e);
                    result.put("success", false);
                    result.put("error", e.getMessage());
                }
                break;
                
            default:
                logger.warn("Unknown command {} for session {}", command, sessionId);
                result.put("status", "ERROR");
                result.put("error", "Unknown command: " + command);
        }
        
        return WebSocketMessage.of(command + "_RESULT", result);
    }
} 