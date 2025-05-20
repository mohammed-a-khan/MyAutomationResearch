package com.cstestforge.recorder.controller;

import com.cstestforge.recorder.model.ElementInfo;
import com.cstestforge.recorder.model.ElementLocation;
import com.cstestforge.recorder.model.ElementSize;
import com.cstestforge.recorder.model.RecordedEvent;
import com.cstestforge.recorder.model.RecordedEventType;
import com.cstestforge.recorder.model.RecordingSession;
import com.cstestforge.recorder.model.RecordingStatus;
import com.cstestforge.recorder.model.events.ClickEvent;
import com.cstestforge.recorder.model.events.InputEvent;
import com.cstestforge.recorder.model.events.NavigationEvent;
import com.cstestforge.recorder.service.RecorderService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for handling extension webhook events.
 */
@RestController
@RequestMapping("/api/recorder/hooks")
public class RecorderWebhookController {
    
    private static final Logger LOGGER = Logger.getLogger(RecorderWebhookController.class.getName());
    
    @Autowired
    private RecorderService recorderService;
    
    /**
     * Process an event from the browser extension
     *
     * @param sessionKey The session key
     * @param eventData The event data
     * @return ResponseEntity with success/failure response
     */
    @PostMapping("/{sessionKey}/event")
    public ResponseEntity<?> processEvent(
            @PathVariable String sessionKey,
            @RequestBody Map<String, Object> eventData) {
        
        LOGGER.log(Level.INFO, "Received event for session: {0}", sessionKey);
        
        RecordingSession session = recorderService.getSessionByKey(sessionKey);
        
        if (session == null) {
            LOGGER.log(Level.WARNING, "Session not found: {0}", sessionKey);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Recording session not found"));
        }
        
        try {
            // Convert the generic event data to a specific event type
            RecordedEvent event = convertToSpecificEvent(eventData);
            
            if (event == null) {
                LOGGER.log(Level.WARNING, "Invalid event data for session: {0}", sessionKey);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Invalid event data"));
            }
            
            boolean success = recorderService.addEventByKey(sessionKey, event);
            
            if (success) {
                LOGGER.log(Level.INFO, "Event recorded successfully for session: {0}", sessionKey);
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Event recorded successfully");
                response.put("eventId", event.getId().toString());
                
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                LOGGER.log(Level.WARNING, "Failed to add event for session: {0}", sessionKey);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Failed to record event. Session may be inactive."));
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing event for session: " + sessionKey, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error processing event: " + e.getMessage()));
        }
    }
    
    /**
     * Update session status
     *
     * @param sessionKey The session key
     * @param statusData The status update data
     * @return ResponseEntity with success/failure response
     */
    @PostMapping("/{sessionKey}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable String sessionKey,
            @RequestBody Map<String, String> statusData) {
        
        LOGGER.log(Level.INFO, "Updating status for session: {0}", sessionKey);
        
        RecordingSession session = recorderService.getSessionByKey(sessionKey);
        
        if (session == null) {
            LOGGER.log(Level.WARNING, "Session not found for status update: {0}", sessionKey);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Recording session not found"));
        }
        
        String statusStr = statusData.get("status");
        
        try {
            RecordingStatus status = RecordingStatus.valueOf(statusStr);
            RecordingSession updatedSession = recorderService.updateSessionStatus(session.getId(), status);
            
            if (updatedSession != null) {
                LOGGER.log(Level.INFO, "Status updated for session {0}: {1}", 
                        new Object[]{sessionKey, status});
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Status updated successfully");
                response.put("status", updatedSession.getStatus().toString());
                
                return ResponseEntity.ok(response);
            } else {
                LOGGER.log(Level.WARNING, "Failed to update status for session: {0}", sessionKey);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Failed to update session status"));
            }
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid status value: {0} for session: {1}", 
                    new Object[]{statusStr, sessionKey});
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Invalid status: " + statusStr));
        }
    }
    
    /**
     * Process browser information
     *
     * @param sessionKey The session key
     * @param browserInfo The browser information
     * @return ResponseEntity with success/failure response
     */
    @PostMapping("/{sessionKey}/browser-info")
    public ResponseEntity<?> processBrowserInfo(
            @PathVariable String sessionKey,
            @RequestBody Map<String, String> browserInfo) {
        
        LOGGER.log(Level.INFO, "Processing browser info for session: {0}", sessionKey);
        
        RecordingSession session = recorderService.getSessionByKey(sessionKey);
        
        if (session == null) {
            LOGGER.log(Level.WARNING, "Session not found for browser info update: {0}", sessionKey);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Recording session not found"));
        }
        
        session.setBrowserInfo(browserInfo.get("browser"));
        session.setOperatingSystem(browserInfo.get("os"));
        session.setUserAgent(browserInfo.get("userAgent"));
        
        LOGGER.log(Level.INFO, "Browser info updated for session: {0}", sessionKey);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Browser information updated successfully");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get session information
     *
     * @param sessionKey The session key
     * @return ResponseEntity with the session information
     */
    @GetMapping("/{sessionKey}/info")
    public ResponseEntity<?> getSessionInfo(@PathVariable String sessionKey) {
        LOGGER.log(Level.INFO, "Getting session info for: {0}", sessionKey);
        
        RecordingSession session = recorderService.getSessionByKey(sessionKey);
        
        if (session == null) {
            LOGGER.log(Level.WARNING, "Session not found for info request: {0}", sessionKey);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Recording session not found"));
        }
        
        Map<String, Object> sessionInfo = new HashMap<>();
        sessionInfo.put("id", session.getId().toString());
        sessionInfo.put("name", session.getName());
        sessionInfo.put("status", session.getStatus().toString());
        sessionInfo.put("eventCount", session.getEvents().size());
        sessionInfo.put("startTime", session.getStartTime().toString());
        sessionInfo.put("config", session.getConfig());
        
        return ResponseEntity.ok(sessionInfo);
    }
    
    /**
     * Convert generic event data to a specific event type
     *
     * @param eventData The generic event data
     * @return The specific event instance
     */
    private RecordedEvent convertToSpecificEvent(Map<String, Object> eventData) {
        String type = (String) eventData.get("type");
        
        if (type == null) {
            LOGGER.log(Level.WARNING, "Event data missing type field");
            return null;
        }
        
        try {
            // Convert type string to enum
            RecordedEventType eventType = RecordedEventType.valueOf(type);
            
            // Create specific event based on type
            switch (eventType) {
                case CLICK:
                case DOUBLE_CLICK:
                case RIGHT_CLICK:
                    return createClickEvent(eventData, eventType);
                    
                case NAVIGATION:
                    return createNavigationEvent(eventData);
                    
                case INPUT:
                    return createInputEvent(eventData);
                    
                case HOVER:
                    // In production, implement hover events
                    LOGGER.log(Level.INFO, "Hover events not yet implemented");
                    return null;
                    
                case SCROLL:
                    // In production, implement scroll events
                    LOGGER.log(Level.INFO, "Scroll events not yet implemented");
                    return null;
                    
                // Additional event types would be handled here
                    
                default:
                    LOGGER.log(Level.WARNING, "Unsupported event type: {0}", eventType);
                    return null;
            }
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid event type: {0}", type);
            return null;
        }
    }
    
    /**
     * Create a click event from event data
     *
     * @param eventData The event data
     * @param eventType The event type
     * @return The click event
     */
    private ClickEvent createClickEvent(Map<String, Object> eventData, RecordedEventType eventType) {
        ClickEvent event = new ClickEvent();
        
        // Set common properties
        setCommonEventProperties(event, eventData);
        
        // Set specific properties
        if (eventData.containsKey("isDoubleClick")) {
            event.setDoubleClick((Boolean) eventData.get("isDoubleClick"));
        } else if (eventType == RecordedEventType.DOUBLE_CLICK) {
            event.setDoubleClick(true);
        }
        
        if (eventData.containsKey("isRightClick")) {
            event.setRightClick((Boolean) eventData.get("isRightClick"));
        } else if (eventType == RecordedEventType.RIGHT_CLICK) {
            event.setRightClick(true);
        }
        
        if (eventData.containsKey("isMiddleClick")) {
            event.setMiddleClick((Boolean) eventData.get("isMiddleClick"));
        }
        
        if (eventData.containsKey("ctrlKey")) {
            event.setCtrlKey((Boolean) eventData.get("ctrlKey"));
        }
        
        if (eventData.containsKey("shiftKey")) {
            event.setShiftKey((Boolean) eventData.get("shiftKey"));
        }
        
        if (eventData.containsKey("altKey")) {
            event.setAltKey((Boolean) eventData.get("altKey"));
        }
        
        if (eventData.containsKey("metaKey")) {
            event.setMetaKey((Boolean) eventData.get("metaKey"));
        }
        
        // Set target element if present
        if (eventData.containsKey("targetElement")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> elementData = (Map<String, Object>) eventData.get("targetElement");
            ElementInfo elementInfo = createElementInfo(elementData);
            event.setTargetElement(elementInfo);
        }
        
        // Validate that the event has the required data
        if (!event.isValid()) {
            LOGGER.log(Level.WARNING, "Created click event is invalid");
            return null;
        }
        
        return event;
    }
    
    /**
     * Create a navigation event from event data
     *
     * @param eventData The event data
     * @return The navigation event
     */
    private NavigationEvent createNavigationEvent(Map<String, Object> eventData) {
        NavigationEvent event = new NavigationEvent();
        
        // Set common properties
        setCommonEventProperties(event, eventData);
        
        // Set specific properties
        if (eventData.containsKey("sourceUrl")) {
            event.setSourceUrl((String) eventData.get("sourceUrl"));
        }
        
        if (eventData.containsKey("targetUrl")) {
            event.setTargetUrl((String) eventData.get("targetUrl"));
        } else if (eventData.containsKey("url")) {
            // Fall back to the common URL field if targetUrl is not specified
            event.setTargetUrl((String) eventData.get("url"));
        }
        
        if (eventData.containsKey("isRedirect")) {
            event.setRedirect((Boolean) eventData.get("isRedirect"));
        }
        
        if (eventData.containsKey("isBackNavigation")) {
            event.setBackNavigation((Boolean) eventData.get("isBackNavigation"));
        }
        
        if (eventData.containsKey("isForwardNavigation")) {
            event.setForwardNavigation((Boolean) eventData.get("isForwardNavigation"));
        }
        
        if (eventData.containsKey("isRefresh")) {
            event.setRefresh((Boolean) eventData.get("isRefresh"));
        }
        
        if (eventData.containsKey("loadTimeMs")) {
            if (eventData.get("loadTimeMs") instanceof Number) {
                event.setLoadTimeMs(((Number) eventData.get("loadTimeMs")).longValue());
            }
        }
        
        if (eventData.containsKey("trigger") && eventData.get("trigger") instanceof String) {
            try {
                String triggerStr = (String) eventData.get("trigger");
                NavigationEvent.NavigationTrigger trigger = NavigationEvent.NavigationTrigger.valueOf(triggerStr);
                event.setTrigger(trigger);
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.WARNING, "Invalid navigation trigger: {0}", eventData.get("trigger"));
            }
        }
        
        // Validate that the event has the required data
        if (!event.isValid()) {
            LOGGER.log(Level.WARNING, "Created navigation event is invalid");
            return null;
        }
        
        return event;
    }
    
    /**
     * Create an input event from event data
     *
     * @param eventData The event data
     * @return The input event
     */
    private InputEvent createInputEvent(Map<String, Object> eventData) {
        InputEvent event = new InputEvent();
        
        // Set common properties
        setCommonEventProperties(event, eventData);
        
        // Set specific properties
        if (eventData.containsKey("inputValue")) {
            event.setInputValue((String) eventData.get("inputValue"));
        }
        
        if (eventData.containsKey("previousValue")) {
            event.setPreviousValue((String) eventData.get("previousValue"));
        }
        
        if (eventData.containsKey("isClearFirst")) {
            event.setClearFirst((Boolean) eventData.get("isClearFirst"));
        }
        
        if (eventData.containsKey("isPasswordField")) {
            event.setPasswordField((Boolean) eventData.get("isPasswordField"));
        }
        
        if (eventData.containsKey("isMasked")) {
            event.setMasked((Boolean) eventData.get("isMasked"));
        }
        
        if (eventData.containsKey("isFilePicker")) {
            event.setFilePicker((Boolean) eventData.get("isFilePicker"));
        }
        
        if (eventData.containsKey("selectedFiles") && eventData.get("selectedFiles") instanceof Object[]) {
            Object[] files = (Object[]) eventData.get("selectedFiles");
            String[] fileNames = new String[files.length];
            
            for (int i = 0; i < files.length; i++) {
                fileNames[i] = String.valueOf(files[i]);
            }
            
            event.setSelectedFiles(fileNames);
        }
        
        if (eventData.containsKey("inputType") && eventData.get("inputType") instanceof String) {
            try {
                String inputTypeStr = (String) eventData.get("inputType");
                InputEvent.InputType inputType = InputEvent.InputType.valueOf(inputTypeStr);
                event.setInputType(inputType);
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.WARNING, "Invalid input type: {0}", eventData.get("inputType"));
            }
        }
        
        // Set target element if present
        if (eventData.containsKey("targetElement")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> elementData = (Map<String, Object>) eventData.get("targetElement");
            ElementInfo elementInfo = createElementInfo(elementData);
            event.setTargetElement(elementInfo);
            
            // If this is a password field, mark it as such
            if (elementInfo != null && "password".equalsIgnoreCase(elementInfo.getType())) {
                event.setPasswordField(true);
                event.setMasked(true);
            }
        }
        
        // Validate that the event has the required data
        if (!event.isValid()) {
            LOGGER.log(Level.WARNING, "Created input event is invalid");
            return null;
        }
        
        return event;
    }
    
    /**
     * Create an ElementInfo object from element data
     *
     * @param elementData The element data map
     * @return The ElementInfo object
     */
    private ElementInfo createElementInfo(Map<String, Object> elementData) {
        if (elementData == null) {
            return null;
        }
        
        ElementInfo elementInfo = new ElementInfo();
        
        // Set basic properties
        if (elementData.containsKey("id")) {
            elementInfo.setId((String) elementData.get("id"));
        }
        
        if (elementData.containsKey("tagName")) {
            elementInfo.setTagName((String) elementData.get("tagName"));
        }
        
        if (elementData.containsKey("className")) {
            elementInfo.setClassName((String) elementData.get("className"));
        }
        
        if (elementData.containsKey("name")) {
            elementInfo.setName((String) elementData.get("name"));
        }
        
        if (elementData.containsKey("text")) {
            elementInfo.setText((String) elementData.get("text"));
        }
        
        if (elementData.containsKey("href")) {
            elementInfo.setHref((String) elementData.get("href"));
        }
        
        if (elementData.containsKey("src")) {
            elementInfo.setSrc((String) elementData.get("src"));
        }
        
        if (elementData.containsKey("alt")) {
            elementInfo.setAlt((String) elementData.get("alt"));
        }
        
        if (elementData.containsKey("title")) {
            elementInfo.setTitle((String) elementData.get("title"));
        }
        
        if (elementData.containsKey("placeholder")) {
            elementInfo.setPlaceholder((String) elementData.get("placeholder"));
        }
        
        if (elementData.containsKey("value")) {
            elementInfo.setValue((String) elementData.get("value"));
        }
        
        if (elementData.containsKey("type")) {
            elementInfo.setType((String) elementData.get("type"));
        }
        
        // Set selectors
        if (elementData.containsKey("selector")) {
            elementInfo.setSelector((String) elementData.get("selector"));
        }
        
        if (elementData.containsKey("xpath")) {
            elementInfo.setXpath((String) elementData.get("xpath"));
        }
        
        if (elementData.containsKey("cssSelector")) {
            elementInfo.setCssSelector((String) elementData.get("cssSelector"));
        }
        
        // Set state properties
        if (elementData.containsKey("isVisible")) {
            elementInfo.setVisible((Boolean) elementData.get("isVisible"));
        }
        
        if (elementData.containsKey("isEnabled")) {
            elementInfo.setEnabled((Boolean) elementData.get("isEnabled"));
        }
        
        if (elementData.containsKey("isSelected")) {
            elementInfo.setSelected((Boolean) elementData.get("isSelected"));
        }
        
        if (elementData.containsKey("isRequired")) {
            elementInfo.setRequired((Boolean) elementData.get("isRequired"));
        }
        
        // Set location
        if (elementData.containsKey("location") && elementData.get("location") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> locationData = (Map<String, Object>) elementData.get("location");
            ElementLocation location = new ElementLocation();
            
            if (locationData.containsKey("x") && locationData.get("x") instanceof Number) {
                location.setX(((Number) locationData.get("x")).intValue());
            }
            
            if (locationData.containsKey("y") && locationData.get("y") instanceof Number) {
                location.setY(((Number) locationData.get("y")).intValue());
            }
            
            elementInfo.setLocation(location);
        }
        
        // Set size
        if (elementData.containsKey("size") && elementData.get("size") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> sizeData = (Map<String, Object>) elementData.get("size");
            ElementSize size = new ElementSize();
            
            if (sizeData.containsKey("width") && sizeData.get("width") instanceof Number) {
                size.setWidth(((Number) sizeData.get("width")).intValue());
            }
            
            if (sizeData.containsKey("height") && sizeData.get("height") instanceof Number) {
                size.setHeight(((Number) sizeData.get("height")).intValue());
            }
            
            elementInfo.setSize(size);
        }
        
        // Set attributes
        if (elementData.containsKey("attributes") && elementData.get("attributes") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> attributes = (Map<String, String>) elementData.get("attributes");
            
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                elementInfo.addAttribute(entry.getKey(), entry.getValue());
            }
        }
        
        return elementInfo;
    }
    
    /**
     * Set common properties for all event types
     *
     * @param event The event to set properties on
     * @param eventData The event data
     */
    private void setCommonEventProperties(RecordedEvent event, Map<String, Object> eventData) {
        if (eventData.containsKey("url")) {
            event.setUrl((String) eventData.get("url"));
        }
        
        if (eventData.containsKey("title")) {
            event.setTitle((String) eventData.get("title"));
        }
        
        if (eventData.containsKey("timestamp") && eventData.get("timestamp") instanceof Number) {
            long timestamp = ((Number) eventData.get("timestamp")).longValue();
            event.setTimestamp(Instant.ofEpochMilli(timestamp));
        }
        
        if (eventData.containsKey("viewport") && eventData.get("viewport") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> viewportData = (Map<String, Object>) eventData.get("viewport");
            
            if (viewportData.containsKey("width") && viewportData.get("width") instanceof Number && 
                viewportData.containsKey("height") && viewportData.get("height") instanceof Number) {
                
                int width = ((Number) viewportData.get("width")).intValue();
                int height = ((Number) viewportData.get("height")).intValue();
                
                event.getViewport().setWidth(width);
                event.getViewport().setHeight(height);
            }
        }
    }
    
    /**
     * Create an error response
     *
     * @param message The error message
     * @return The error response map
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return response;
    }
} 