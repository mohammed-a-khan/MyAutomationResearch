package com.cstestforge.recorder.controller;

import com.cstestforge.recorder.browser.BrowserManager;
import com.cstestforge.recorder.model.*;
import com.cstestforge.recorder.model.config.LoopConfig;
import com.cstestforge.recorder.model.events.LoopEvent;
import com.cstestforge.recorder.service.RecorderService;
import com.cstestforge.recorder.websocket.RecorderWebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.*;

/**
 * Controller for handling recording-related endpoints
 */
@RestController
@RequestMapping("/api/recorder")
public class RecorderController {
    private static final Logger logger = LoggerFactory.getLogger(RecorderController.class);
    
    @Autowired
    private RecorderService recorderService;
    
    @Autowired
    private BrowserManager browserManager;
    
    @Autowired
    private RecorderWebSocketService webSocketService;
    
    /**
     * Start a recording session
     *
     * @param request The recording request parameters
     * @return Recording session details and status
     */
    @PostMapping("/start")
    public ResponseEntity<RecordingResponse> startRecording(@RequestBody RecordingRequest request) {
        try {
            // Log the entire request
            logger.info("Received recording request - projectId: {}, browserType: {}, framework: {}, url: {}",
                request.getProjectId(), request.getBrowserType(), request.getFramework(), request.getUrl());
            
            // Create a recording session
            String projectId = request.getProjectId();
            
            if (StringUtils.hasLength(projectId) == false) {
                return ResponseEntity.badRequest().body(
                    new RecordingResponse(null, "Project ID is required", false)
                );
            }
            
            // Generate a session ID for this recording
            UUID sessionId = UUID.randomUUID();
            
            // Get the browser type
            String browserType = request.getBrowserType();
            if (StringUtils.hasLength(browserType) == false) {
                browserType = "chrome"; // Default to Chrome if not specified
            }
            
            // Log the selected browser
            logger.info("Selected browser for recording: {}", browserType);
            
            // Get the framework
            String framework = request.getFramework();
            if (StringUtils.hasLength(framework) == false) {
                framework = "selenium_java_testng"; // Default framework
            }
            
            // Log the selected framework
            logger.info("Selected framework for recording: {}", framework);
            
            // Create a recording configuration
            RecordingConfig config = new RecordingConfig();
            config.setBrowserType(browserType);
            
            // Properly handle the URL
            String baseUrl = request.getUrl();
            if (StringUtils.hasLength(baseUrl)) {
                // Handle about:blank as a special case
                if ("about:blank".equals(baseUrl)) {
                    logger.info("Using special URL: about:blank");
                    config.setBaseUrl(baseUrl);
                } else {
                    // Ensure URL has a protocol
                    if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
                        baseUrl = "https://" + baseUrl;
                        logger.info("Added https:// prefix to URL: {}", baseUrl);
                    }
                    
                    // Validate URL format
                    try {
                        new URL(baseUrl);
                        config.setBaseUrl(baseUrl);
                        logger.info("Using base URL for navigation: {}", baseUrl);
                    } catch (MalformedURLException e) {
                        logger.error("Invalid URL format: {}", baseUrl);
                        return ResponseEntity.badRequest().body(
                            new RecordingResponse(null, "Invalid URL format: " + baseUrl, false)
                        );
                    }
                }
            } else {
                logger.warn("No URL provided, will use blank page");
                config.setBaseUrl("about:blank");
            }
            
            // Set environment variables for the recording
            Map<String, String> env = new HashMap<>();
            env.put("framework", framework);
            config.setEnvironmentVariables(env);
            
            // Set default timeout (30 seconds)
            config.setCommandTimeoutSeconds(30);
            config.setCaptureNetwork(true);
            
            // Start the browser
            boolean success = browserManager.startBrowser(sessionId, config);
            
            if (success) {
                // Create a record in the database for this session
                RecordingSession session = new RecordingSession();
                session.setId(sessionId);
                session.setProjectId(projectId);
                session.setBrowser(browserType);
                session.setFramework(framework);
                session.setBaseUrl(baseUrl);
                session.setStartTime(new Date());
                session.setStatus(RecordingStatus.ACTIVE);
                
                recorderService.saveSession(session);
                
                logger.info("Recording session created: {} for project {}", sessionId, projectId);
                
                // Start event collection for this session
                recorderService.startEventCollection(sessionId);
                
                // Return session information
                RecordingResponse response = new RecordingResponse(
                    sessionId.toString(), 
                    "Recording started successfully", 
                    true
                );
                
                logger.info("Browser started successfully for recording session: {}", sessionId);
                return ResponseEntity.ok(response);
            } else {
                logger.error("Failed to start browser for recording session");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RecordingResponse(null, "Failed to start browser", false));
            }
        } catch (Exception e) {
            logger.error("Error starting recording session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new RecordingResponse(null, "Error: " + e.getMessage(), false));
        }
    }
    
    /**
     * Create a new recording session
     *
     * @param requestBody Map containing session details
     * @return ResponseEntity with the created recording session
     */
    @PostMapping("/sessions")
    public ResponseEntity<RecordingSession> createSession(@RequestBody Map<String, Object> requestBody) {
        String name = (String) requestBody.get("name");
        String projectId = (String) requestBody.get("projectId");
        RecordingConfig config = (RecordingConfig) requestBody.get("config");
        String createdBy = (String) requestBody.get("createdBy");
        
        RecordingSession session = recorderService.createSession(name, projectId, config, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }
    
    /**
     * Get a recording session by ID
     *
     * @param sessionId The session ID
     * @return ResponseEntity with the recording session
     */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<RecordingSession> getSession(@PathVariable UUID sessionId) {
        RecordingSession session = recorderService.getSession(sessionId);
        
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(session);
    }
    
    /**
     * Get all recording sessions for a project
     *
     * @param projectId The project ID
     * @return ResponseEntity with the list of recording sessions
     */
    @GetMapping("/sessions/project/{projectId}")
    public ResponseEntity<List<RecordingSession>> getSessionsByProject(@PathVariable String projectId) {
        List<RecordingSession> sessions = recorderService.getSessionsByProject(projectId);
        return ResponseEntity.ok(sessions);
    }
    
    /**
     * Get all active recording sessions
     *
     * @return ResponseEntity with the list of active recording sessions
     */
    @GetMapping("/sessions/active")
    public ResponseEntity<List<RecordingSession>> getActiveSessions() {
        List<RecordingSession> sessions = recorderService.getActiveSessions();
        return ResponseEntity.ok(sessions);
    }
    
    /**
     * Add an event to a recording session
     *
     * @param sessionId The session ID
     * @param event The event to add
     * @return ResponseEntity with success/failure status
     */
    @PostMapping("/sessions/{sessionId}/events")
    public ResponseEntity<?> addEvent(@PathVariable UUID sessionId, @RequestBody RecordedEvent event) {
        boolean success = recorderService.addEvent(sessionId, event);
        
        if (success) {
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } else {
            return ResponseEntity.badRequest().body("Failed to add event. Session may be inactive or not found.");
        }
    }
    
    /**
     * Add an event to a recording session by session key
     *
     * @param sessionKey The session key
     * @param event The event to add
     * @return ResponseEntity with success/failure status
     */
    @PostMapping("/sessions/key/{sessionKey}/events")
    public ResponseEntity<?> addEventByKey(@PathVariable String sessionKey, @RequestBody RecordedEvent event) {
        boolean success = recorderService.addEventByKey(sessionKey, event);
        
        if (success) {
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } else {
            return ResponseEntity.badRequest().body("Failed to add event. Session may not exist.");
        }
    }
    
    /**
     * Update the status of a recording session
     *
     * @param sessionId The session ID
     * @param requestBody Map containing the new status
     * @return ResponseEntity with the updated recording session
     */
    @PatchMapping("/sessions/{sessionId}/status")
    public ResponseEntity<RecordingSession> updateSessionStatus(
            @PathVariable UUID sessionId,
            @RequestBody Map<String, String> requestBody) {
        
        String statusStr = requestBody.get("status");
        RecordingStatus status;
        
        try {
            status = RecordingStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        
        RecordingSession updatedSession = recorderService.updateSessionStatus(sessionId, status);
        
        if (updatedSession == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(updatedSession);
    }
    
    /**
     * Delete a recording session
     *
     * @param sessionId The session ID
     * @return ResponseEntity with success/failure status
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> deleteSession(@PathVariable UUID sessionId) {
        boolean success = recorderService.deleteSession(sessionId);
        
        if (success) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Create a loop event in a recording session
     *
     * @param requestBody Map containing loop details and parent event ID
     * @return ResponseEntity with the created loop event
     */
    @PostMapping("/loop")
    public ResponseEntity<LoopEvent> createLoop(@RequestBody Map<String, Object> requestBody) {
        String parentEventId = (String) requestBody.get("parentEventId");
        LoopConfig loopConfig = (LoopConfig) requestBody.get("loop");
        
        if (parentEventId == null || loopConfig == null) {
            return ResponseEntity.badRequest().build();
        }
        
        LoopEvent loopEvent = recorderService.createLoopEvent(UUID.fromString(parentEventId), loopConfig);
        
        if (loopEvent == null) {
            return ResponseEntity.badRequest().body(null);
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(loopEvent);
    }
    
    /**
     * Update a loop configuration for an existing event
     *
     * @param eventId The event ID
     * @param loopConfig The updated loop configuration
     * @return ResponseEntity with the updated loop event
     */
    @PutMapping("/event/{eventId}/loop")
    public ResponseEntity<LoopEvent> updateLoop(
            @PathVariable UUID eventId,
            @RequestBody LoopConfig loopConfig) {
        
        LoopEvent updatedEvent = recorderService.updateLoopEvent(eventId, loopConfig);
        
        if (updatedEvent == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(updatedEvent);
    }
    
    /**
     * Add an event to a loop
     *
     * @param loopEventId The loop event ID
     * @param event The event to add to the loop
     * @return ResponseEntity with success status
     */
    @PostMapping("/loop/{loopEventId}/events")
    public ResponseEntity<?> addEventToLoop(
            @PathVariable UUID loopEventId,
            @RequestBody RecordedEvent event) {
        
        boolean success = recorderService.addEventToLoop(loopEventId, event);
        
        if (success) {
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } else {
            return ResponseEntity.badRequest()
                    .body("Failed to add event to loop. Loop may not exist or be in an invalid state.");
        }
    }
    
    /**
     * Remove an event from a loop
     *
     * @param loopEventId The loop event ID
     * @param eventId The ID of the event to remove
     * @return ResponseEntity with success status
     */
    @DeleteMapping("/loop/{loopEventId}/events/{eventId}")
    public ResponseEntity<?> removeEventFromLoop(
            @PathVariable UUID loopEventId,
            @PathVariable UUID eventId) {
        
        boolean success = recorderService.removeEventFromLoop(loopEventId, eventId);
        
        if (success) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get all events in a loop
     *
     * @param loopEventId The loop event ID
     * @return ResponseEntity with the list of events in the loop
     */
    @GetMapping("/loop/{loopEventId}/events")
    public ResponseEntity<List<RecordedEvent>> getLoopEvents(@PathVariable UUID loopEventId) {
        List<RecordedEvent> events = recorderService.getLoopEvents(loopEventId);
        
        if (events == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(events);
    }

    /**
     * Stop a recording session
     *
     * @param requestBody Map containing session details
     * @return ResponseEntity with the updated recording session
     */
    @PostMapping("/stop")
    public ResponseEntity<RecordingSession> stopRecording(@RequestBody Map<String, Object> requestBody) {
        try {
            String sessionParam = (String) requestBody.getOrDefault("sessionId", "");
            if (StringUtils.hasLength(sessionParam) == false) {
                return ResponseEntity.badRequest().build();
            }
            
            UUID sessionId;
            try {
                sessionId = UUID.fromString(sessionParam);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid session ID format: {}", sessionParam);
                return ResponseEntity.badRequest().build();
            }
            
            // First, stop the browser to ensure it's closed
            logger.info("Stopping browser for recording session: {}", sessionId);
            browserManager.stopBrowser(sessionId);
            
            // Update the session status in the database
            RecordingSession session = recorderService.updateSessionStatus(sessionId, RecordingStatus.COMPLETED);
            
            if (session != null) {
                logger.info("Recording session {} stopped successfully", sessionId);
                
                // Notify clients via WebSocket that recording has stopped
                webSocketService.notifySessionStatusChanged(sessionId, RecordingStatus.COMPLETED);
                
                return ResponseEntity.ok(session);
            } else {
                logger.error("Failed to update session {} status to COMPLETED", sessionId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            logger.error("Error stopping recording session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Check the status of a recording session
     *
     * @param sessionId The session ID
     * @return ResponseEntity with the recording session status and connection info
     */
    @GetMapping("/status/{sessionId}")
    public ResponseEntity<Map<String, Object>> getRecorderStatus(@PathVariable UUID sessionId) {
        RecordingSession session = recorderService.getSession(sessionId);
        
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        
        boolean isBrowserActive = browserManager.isSessionActive(sessionId);
        
        Map<String, Object> status = new HashMap<>();
        status.put("session", session);
        status.put("browserActive", isBrowserActive);
        status.put("websocketStatus", "UNKNOWN"); // Can be updated with real WebSocket status if needed
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Debug endpoint for WebSocket connections
     *
     * @return Response with application WebSocket information
     */
    @GetMapping("/debug/websocket")
    public ResponseEntity<Map<String, Object>> debugWebSocket() {
        Map<String, Object> info = new HashMap<>();
        
        // Add WebSocket connection info
        info.put("websocketEndpoint", "/ws-recorder");
        info.put("sockJsEnabled", true);
        info.put("allowedOrigins", "http://localhost:8080, http://localhost:3000, *");
        info.put("topicPrefix", "/topic");
        info.put("appPrefix", "/app");
        info.put("userPrefix", "/user");
        info.put("serverTime", System.currentTimeMillis());
        info.put("activeWebSocketSessions", getActiveWebSocketSessionsCount());
        info.put("activeBrowsers", browserManager.getActiveBrowsers().size());
        info.put("serverInfo", getServerInfo());
        
        return ResponseEntity.ok(info);
    }
    
    /**
     * Get the number of active WebSocket sessions
     * 
     * @return Map with active WebSocket sessions count
     */
    private int getActiveWebSocketSessionsCount() {
        try {
            // This is a simplified implementation
            // In a real implementation, you would get this from a WebSocket sessions registry
            return browserManager.getActiveBrowsers().size();
        } catch (Exception e) {
            logger.error("Error getting active WebSocket sessions", e);
            return -1;
        }
    }
    
    /**
     * Get basic server information
     * 
     * @return Map with server information
     */
    private Map<String, Object> getServerInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("os", System.getProperty("os.name"));
        info.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        info.put("freeMemory", Runtime.getRuntime().freeMemory());
        info.put("maxMemory", Runtime.getRuntime().maxMemory());
        return info;
    }
    
    /**
     * Handle events from the recorder script in the browser
     *
     * @param sessionId The session ID
     * @param event The event from the browser
     * @return ResponseEntity with success/failure status
     */
    @PostMapping("/events/{sessionId}")
    public ResponseEntity<?> handleRecorderEvent(
            @PathVariable UUID sessionId,
            @RequestBody Map<String, Object> event) {
        
        String eventType = (String) event.get("type");
        if (eventType == null) {
            return ResponseEntity.badRequest().body("Missing event type");
        }
        
        logger.info("Received event from recorder: {} for session {}", eventType, sessionId);
        
        // Handle different event types
        switch (eventType) {
            case "RECORDER_CONTROL":
                String action = (String) event.get("action");
                if ("PAUSE".equals(action)) {
                    recorderService.updateSessionStatus(sessionId, RecordingStatus.PAUSED);
                } else if ("RESUME".equals(action)) {
                    recorderService.updateSessionStatus(sessionId, RecordingStatus.ACTIVE);
                } else if ("STOP".equals(action)) {
                    browserManager.stopBrowser(sessionId);
                    recorderService.updateSessionStatus(sessionId, RecordingStatus.COMPLETED);
                }
                break;
                
            case "NAVIGATION":
            case "CLICK":
            case "DOUBLE_CLICK":
            case "RIGHT_CLICK":
            case "INPUT":
            case "CHANGE":
            case "SELECT":
            case "HOVER":
            case "FOCUS":
            case "BLUR":
            case "FORM_SUBMIT":
                // Convert to RecordedEvent and add to session
                RecordedEvent recordedEvent = convertToRecordedEvent(sessionId, event);
                recorderService.addEvent(sessionId, recordedEvent);
                break;
                
            case "INIT":
                // Update session with initial page info
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("initialUrl", event.get("url"));
                metadata.put("userAgent", event.get("userAgent"));
                metadata.put("initialTitle", event.get("title"));
                recorderService.updateSessionMetadata(sessionId, metadata);
                break;
                
            case "SCREENSHOT":
                // Handle screenshot data
                String screenshotData = (String) event.get("data");
                if (screenshotData != null && !screenshotData.isEmpty()) {
                    recorderService.saveScreenshot(sessionId, screenshotData);
                }
                break;
        }
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Convert a raw event map to a RecordedEvent
     */
    private RecordedEvent convertToRecordedEvent(UUID sessionId, Map<String, Object> eventMap) {
        // Create the event with the correct type
        RecordedEventType eventType;
        try {
            eventType = RecordedEventType.valueOf((String) eventMap.get("type"));
        } catch (IllegalArgumentException e) {
            // Default to CUSTOM if the type doesn't match
            eventType = RecordedEventType.CUSTOM;
        }
        
        RecordedEvent event = new RecordedEvent(eventType);
        event.setUrl((String) eventMap.get("url"));
        
        // Extract common fields
        if (eventMap.containsKey("value")) {
            event.setValue((String) eventMap.get("value"));
        }
        
        // Set the element info
        @SuppressWarnings("unchecked")
        Map<String, Object> elementInfoMap = (Map<String, Object>) eventMap.get("elementInfo");
        if (elementInfoMap != null) {
            ElementInfo elementInfo = new ElementInfo();
            
            if (elementInfoMap.containsKey("tagName")) {
                elementInfo.setTagName((String) elementInfoMap.get("tagName"));
            }
            
            if (elementInfoMap.containsKey("id")) {
                elementInfo.setId((String) elementInfoMap.get("id"));
            }
            
            if (elementInfoMap.containsKey("cssSelector")) {
                elementInfo.setCssSelector((String) elementInfoMap.get("cssSelector"));
            }
            
            if (elementInfoMap.containsKey("xpath")) {
                elementInfo.setXpath((String) elementInfoMap.get("xpath"));
            }
            
            if (elementInfoMap.containsKey("text")) {
                elementInfo.setText((String) elementInfoMap.get("text"));
            }
            
            if (elementInfoMap.containsKey("value")) {
                elementInfo.setValue((String) elementInfoMap.get("value"));
            }
            
            if (elementInfoMap.containsKey("href")) {
                elementInfo.setHref((String) elementInfoMap.get("href"));
            }
            
            @SuppressWarnings("unchecked")
            Map<String, String> attributes = (Map<String, String>) elementInfoMap.get("attributes");
            if (attributes != null) {
                elementInfo.setAttributes(attributes);
            }
            
            event.setElementInfo(elementInfo);
        }
        
        // Set event-specific data in metadata
        Map<String, Object> metadata = new HashMap<>();
        if (eventMap.containsKey("ctrlKey")) metadata.put("ctrlKey", eventMap.get("ctrlKey"));
        if (eventMap.containsKey("altKey")) metadata.put("altKey", eventMap.get("altKey"));
        if (eventMap.containsKey("shiftKey")) metadata.put("shiftKey", eventMap.get("shiftKey"));
        if (eventMap.containsKey("metaKey")) metadata.put("metaKey", eventMap.get("metaKey"));
        if (eventMap.containsKey("button")) metadata.put("button", eventMap.get("button"));
        if (eventMap.containsKey("formData")) metadata.put("formData", eventMap.get("formData"));
        event.setMetadata(metadata);
        
        return event;
    }
} 