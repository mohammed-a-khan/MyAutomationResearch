package com.cstestforge.recorder.websocket;

import com.cstestforge.recorder.model.RecordedEvent;
import com.cstestforge.recorder.model.RecordedEventType;
import com.cstestforge.recorder.model.RecordingSession;
import com.cstestforge.recorder.model.RecordingStatus;
import com.cstestforge.recorder.service.RecorderService;
import com.cstestforge.recorder.events.EventProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced WebSocket handler for recorder events with improved error handling.
 */
@Controller
public class RecorderWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(RecorderWebSocketHandler.class);

    @Autowired
    private RecorderService recorderService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private EventProcessor eventProcessor;

    /**
     * Handle event recording messages with improved error handling and event processing
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

        if (sessionKey == null || event == null) {
            sendErrorMessage(sessionKey, "Invalid event data: missing session key or event");
            return;
        }

        try {
            // Log the event type and session
            logger.debug("Received WebSocket event for session {}: type={}", sessionKey, event.getType());

            // Get the session
            RecordingSession session = recorderService.getSessionByKey(sessionKey);

            if (session == null) {
                sendErrorMessage(sessionKey, "Recording session not found: " + sessionKey);
                return;
            }

            // Process the event to ensure quality and avoid duplicates
            RecordedEvent processedEvent = eventProcessor.processEvent(session.getId(), event);

            if (processedEvent == null) {
                // Event was filtered out - send acknowledgment anyway to keep client happy
                sendEventAcknowledgment(sessionKey, event.getId(), false, "Event filtered out");
                return;
            }

            // Add the event to the session
            boolean success = recorderService.addEventByKey(sessionKey, processedEvent);

            if (success) {
                // Send success acknowledgment back to the client
                sendEventAcknowledgment(sessionKey, processedEvent.getId(), true, "Event recorded successfully");

                // Also send to any subscribers monitoring this session
                messagingTemplate.convertAndSend(
                        "/topic/recorder/sessions/" + session.getId() + "/events",
                        processedEvent
                );
            } else {
                sendErrorMessage(sessionKey, "Failed to record event. Session may be inactive.");
            }
        } catch (Exception e) {
            logger.error("Error processing WebSocket event for session {}: {}", sessionKey, e.getMessage(), e);
            sendErrorMessage(sessionKey, "Error processing event: " + e.getMessage());
        }
    }

    /**
     * Handle status update messages with improved handling
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

        if (sessionKey == null || status == null) {
            sendErrorMessage(sessionKey, "Invalid status update: missing session key or status");
            return;
        }

        try {
            RecordingSession session = recorderService.getSessionByKey(sessionKey);

            if (session == null) {
                sendErrorMessage(sessionKey, "Recording session not found: " + sessionKey);
                return;
            }

            try {
                RecordingSession updatedSession = recorderService.updateSessionStatus(
                        session.getId(),
                        RecordingStatus.valueOf(status)
                );

                if (updatedSession != null) {
                    // Send success acknowledgment back to the client
                    sendStatusUpdateResponse(sessionKey, updatedSession.getStatus(), true,
                            "Status updated successfully");

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
        } catch (Exception e) {
            logger.error("Error updating status for session {}: {}", sessionKey, e.getMessage(), e);
            sendErrorMessage(sessionKey, "Error updating status: " + e.getMessage());
        }
    }

    /**
     * Handle heartbeat messages from clients
     *
     * @param sessionKey The session key
     * @param headerAccessor The message headers
     */
    @MessageMapping("/recorder/{sessionKey}/heartbeat")
    public void handleHeartbeat(
            @DestinationVariable String sessionKey,
            SimpMessageHeaderAccessor headerAccessor) {

        if (sessionKey == null) {
            return;
        }

        try {
            RecordingSession session = recorderService.getSessionByKey(sessionKey);

            if (session == null) {
                return;
            }

            // Send heartbeat response
            Map<String, Object> response = new HashMap<>();
            response.put("type", "HEARTBEAT_RESPONSE");
            response.put("timestamp", System.currentTimeMillis());
            response.put("sessionId", session.getId().toString());
            response.put("status", session.getStatus().toString());

            messagingTemplate.convertAndSend(
                    "/topic/recorder/" + sessionKey + "/heartbeat",
                    response
            );
        } catch (Exception e) {
            logger.warn("Error handling heartbeat for session {}: {}", sessionKey, e.getMessage());
        }
    }

    /**
     * Handle browser window/tab closure detection
     *
     * @param sessionKey The session key
     * @param headerAccessor The message headers
     */
    @MessageMapping("/recorder/{sessionKey}/close")
    public void handleBrowserClose(
            @DestinationVariable String sessionKey,
            SimpMessageHeaderAccessor headerAccessor) {

        if (sessionKey == null) {
            return;
        }

        try {
            RecordingSession session = recorderService.getSessionByKey(sessionKey);

            if (session == null) {
                return;
            }

            // Log the browser closure
            logger.info("Browser close detected for session: {}", sessionKey);

            // Create a special browser close event
            RecordedEvent closeEvent = new RecordedEvent();
            closeEvent.setId(UUID.randomUUID());
            closeEvent.setType(RecordedEventType.RECORDER_STATUS); // Using appropriate event type
            closeEvent.setUrl(session.getLastEvent() != null ? session.getLastEvent().getUrl() : "");
            closeEvent.setTimestamp(java.time.Instant.now());
            closeEvent.addMetadata("action", "BROWSER_CLOSE");
            closeEvent.addMetadata("description", "Browser window closed");

            recorderService.addEventByKey(sessionKey, closeEvent);

            // Notify subscribers about the browser close
            messagingTemplate.convertAndSend(
                    "/topic/recorder/sessions/" + session.getId() + "/events",
                    closeEvent
            );
        } catch (Exception e) {
            logger.error("Error handling browser close for session {}: {}", sessionKey, e.getMessage());
        }
    }

    /**
     * Send an error message back to the client
     *
     * @param sessionKey The session key
     * @param errorMessage The error message
     */
    private void sendErrorMessage(String sessionKey, String errorMessage) {
        if (sessionKey == null) {
            logger.warn("Cannot send error message: session key is null");
            return;
        }

        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", errorMessage);
            response.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend(
                    "/topic/recorder/" + sessionKey + "/error",
                    response
            );

            logger.debug("Sent error message to session {}: {}", sessionKey, errorMessage);
        } catch (Exception e) {
            logger.error("Failed to send error message to client: {}", e.getMessage());
        }
    }

    /**
     * Send event acknowledgment
     *
     * @param sessionKey The session key
     * @param eventId The event ID
     * @param success Whether the operation was successful
     * @param message Additional message
     */
    private void sendEventAcknowledgment(String sessionKey, UUID eventId, boolean success, String message) {
        if (sessionKey == null) {
            return;
        }

        try {
            Map<String, Object> response = new HashMap<>();
            response.put("eventId", eventId != null ? eventId.toString() : UUID.randomUUID().toString());
            response.put("success", success);
            response.put("message", message);
            response.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend(
                    "/topic/recorder/" + sessionKey + "/events",
                    response
            );
        } catch (Exception e) {
            logger.error("Failed to send event acknowledgment: {}", e.getMessage());
        }
    }

    /**
     * Send status update response
     *
     * @param sessionKey The session key
     * @param status The status
     * @param success Whether the operation was successful
     * @param message Additional message
     */
    private void sendStatusUpdateResponse(String sessionKey, RecordingStatus status,
                                          boolean success, String message) {
        if (sessionKey == null) {
            return;
        }

        try {
            Map<String, Object> response = new HashMap<>();
            response.put("status", status.toString());
            response.put("success", success);
            response.put("message", message);
            response.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend(
                    "/topic/recorder/" + sessionKey + "/status",
                    response
            );
        } catch (Exception e) {
            logger.error("Failed to send status update response: {}", e.getMessage());
        }
    }
}