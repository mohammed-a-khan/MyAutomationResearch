package com.cstestforge.recorder.websocket;

import com.cstestforge.recorder.model.RecordedEvent;
import com.cstestforge.recorder.model.RecordingSession;
import com.cstestforge.recorder.service.RecorderService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.UUID;

/**
 * Controller for handling WebSocket messages related to recording.
 */
@Controller
public class RecorderWebSocketHandler {
    
    @Autowired
    private RecorderService recorderService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * Handle event recording messages
     *
     * @param sessionKey The session key
     * @param event The event to record
     * @param headerAccessor The message headers
     */
    @MessageMapping("/recorder/{sessionKey}/event")
    public void handleRecordEvent(
            @DestinationVariable String sessionKey,
            @Payload RecordedEvent event,
            SimpMessageHeaderAccessor headerAccessor) {
        
        RecordingSession session = recorderService.getSessionByKey(sessionKey);
        
        if (session == null) {
            sendErrorMessage(sessionKey, "Recording session not found");
            return;
        }
        
        boolean success = recorderService.addEventByKey(sessionKey, event);
        
        if (success) {
            // Send success acknowledgment back to the client
            messagingTemplate.convertAndSend(
                    "/topic/recorder/" + sessionKey + "/events",
                    new EventAcknowledgment(event.getId(), true, "Event recorded successfully")
            );
            
            // Also send to any subscribers monitoring this session
            messagingTemplate.convertAndSend(
                    "/topic/recorder/sessions/" + session.getId() + "/events",
                    event
            );
        } else {
            sendErrorMessage(sessionKey, "Failed to record event. Session may be inactive.");
        }
    }
    
    /**
     * Handle status update messages
     *
     * @param sessionKey The session key
     * @param status The new status (ACTIVE, PAUSED, COMPLETED, ERROR)
     * @param headerAccessor The message headers
     */
    @MessageMapping("/recorder/{sessionKey}/status")
    public void handleStatusUpdate(
            @DestinationVariable String sessionKey,
            @Payload String status,
            SimpMessageHeaderAccessor headerAccessor) {
        
        RecordingSession session = recorderService.getSessionByKey(sessionKey);
        
        if (session == null) {
            sendErrorMessage(sessionKey, "Recording session not found");
            return;
        }
        
        try {
            RecordingSession updatedSession = recorderService.updateSessionStatus(
                    session.getId(),
                    Enum.valueOf(com.cstestforge.recorder.model.RecordingStatus.class, status)
            );
            
            if (updatedSession != null) {
                // Send success acknowledgment back to the client
                messagingTemplate.convertAndSend(
                        "/topic/recorder/" + sessionKey + "/status",
                        new StatusUpdateResponse(updatedSession.getStatus(), true, "Status updated successfully")
                );
                
                // Also send to any subscribers monitoring this session
                messagingTemplate.convertAndSend(
                        "/topic/recorder/sessions/" + session.getId() + "/status",
                        updatedSession.getStatus()
                );
            } else {
                sendErrorMessage(sessionKey, "Failed to update session status.");
            }
        } catch (IllegalArgumentException e) {
            sendErrorMessage(sessionKey, "Invalid status: " + status);
        }
    }
    
    /**
     * Send an error message back to the client
     *
     * @param sessionKey The session key
     * @param errorMessage The error message
     */
    private void sendErrorMessage(String sessionKey, String errorMessage) {
        messagingTemplate.convertAndSend(
                "/topic/recorder/" + sessionKey + "/error",
                new ErrorMessage(errorMessage)
        );
    }
    
    /**
     * Event acknowledgment response
     */
    private static class EventAcknowledgment {
        private UUID eventId;
        private boolean success;
        private String message;
        
        public EventAcknowledgment(UUID eventId, boolean success, String message) {
            this.eventId = eventId;
            this.success = success;
            this.message = message;
        }
        
        public UUID getEventId() {
            return eventId;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    /**
     * Status update response
     */
    private static class StatusUpdateResponse {
        private com.cstestforge.recorder.model.RecordingStatus status;
        private boolean success;
        private String message;
        
        public StatusUpdateResponse(com.cstestforge.recorder.model.RecordingStatus status, boolean success, String message) {
            this.status = status;
            this.success = success;
            this.message = message;
        }
        
        public com.cstestforge.recorder.model.RecordingStatus getStatus() {
            return status;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    /**
     * Error message response
     */
    private static class ErrorMessage {
        private String error;
        
        public ErrorMessage(String error) {
            this.error = error;
        }
        
        public String getError() {
            return error;
        }
    }
}