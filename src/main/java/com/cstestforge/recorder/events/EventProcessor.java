package com.cstestforge.recorder.events;

import com.cstestforge.recorder.model.ElementInfo;
import com.cstestforge.recorder.model.RecordedEvent;
import com.cstestforge.recorder.model.RecordedEventType;
import com.cstestforge.recorder.model.RecordingSession;
import com.cstestforge.recorder.websocket.RecorderWebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.time.Instant;

/**
 * Processes browser events for recording sessions.
 * Handles event validation, deduplication, and enhancement.
 */
@Component
public class EventProcessor {
    private static final Logger logger = LoggerFactory.getLogger(EventProcessor.class);
    
    @Autowired
    private RecorderWebSocketService webSocketService;
    
    // Store recent events to detect duplicates
    private final Map<UUID, Queue<RecordedEvent>> recentEvents = new ConcurrentHashMap<>();
    
    // Maximum number of recent events to keep per session
    private static final int MAX_RECENT_EVENTS = 50;
    
    // Minimum time between similar events in milliseconds
    private static final long EVENT_DEBOUNCE_MS = 500;
    
    /**
     * Process a new event from the browser
     *
     * @param sessionId The session ID
     * @param event The event to process
     * @return The processed event or null if the event was filtered out
     */
    public RecordedEvent processEvent(UUID sessionId, RecordedEvent event) {
        if (sessionId == null || event == null) {
            return null;
        }
        
        // Validate the event
        if (!validateEvent(event)) {
            logger.debug("Event validation failed for session {}: {}", sessionId, event);
            return null;
        }
        
        // Check for duplicate or similar events
        if (isDuplicateEvent(sessionId, event)) {
            logger.debug("Duplicate event detected for session {}: {}", sessionId, event);
            return null;
        }
        
        // Enhance the event with additional information
        enhanceEvent(event);
        
        // Add to recent events for deduplication
        addToRecentEvents(sessionId, event);
        
        logger.debug("Processed event for session {}: {}", sessionId, event.getType());
        return event;
    }
    
    /**
     * Process a batch of events
     *
     * @param sessionId The session ID
     * @param events List of events to process
     * @return List of processed events
     */
    public List<RecordedEvent> processEvents(UUID sessionId, List<RecordedEvent> events) {
        if (sessionId == null || events == null || events.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<RecordedEvent> processedEvents = new ArrayList<>();
        for (RecordedEvent event : events) {
            RecordedEvent processedEvent = processEvent(sessionId, event);
            if (processedEvent != null) {
                processedEvents.add(processedEvent);
            }
        }
        
        return processedEvents;
    }
    
    /**
     * Clear event history for a session
     *
     * @param sessionId The session ID
     */
    public void clearSessionEvents(UUID sessionId) {
        if (sessionId != null) {
            recentEvents.remove(sessionId);
        }
    }
    
    /**
     * Validate that an event has all required fields
     *
     * @param event The event to validate
     * @return true if the event is valid
     */
    private boolean validateEvent(RecordedEvent event) {
        // Check required fields based on event type
        if (event.getType() == null) {
            return false;
        }
        
        // For element interaction events, element info is required
        if (isElementInteractionEvent(event.getType()) && event.getElementInfo() == null) {
            return false;
        }
        
        // For input events, value is required
        if (event.getType() == RecordedEventType.INPUT && event.getValue() == null) {
            return false;
        }
        
        // For navigation events, URL is required
        if (event.getType() == RecordedEventType.NAVIGATION && event.getUrl() == null) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if an event is a duplicate or too similar to recent events
     *
     * @param sessionId The session ID
     * @param event The event to check
     * @return true if the event is a duplicate
     */
    private boolean isDuplicateEvent(UUID sessionId, RecordedEvent event) {
        Queue<RecordedEvent> sessionEvents = recentEvents.get(sessionId);
        if (sessionEvents == null || sessionEvents.isEmpty()) {
            return false;
        }
        
        // Check for exact duplicates or similar events within the debounce window
        for (RecordedEvent recentEvent : sessionEvents) {
            // Skip if event types don't match
            if (recentEvent.getType() != event.getType()) {
                continue;
            }
            
            // For element interaction events, check if it's the same element
            if (isElementInteractionEvent(event.getType()) && 
                    isSameElement(recentEvent.getElementInfo(), event.getElementInfo())) {
                
                // Check if the event is within the debounce window
                long timeDiff = event.getTimestamp().toEpochMilli() - recentEvent.getTimestamp().toEpochMilli();
                if (timeDiff < EVENT_DEBOUNCE_MS) {
                    return true;
                }
            }
            
            // For navigation events, check if it's the same URL
            if (event.getType() == RecordedEventType.NAVIGATION && 
                    Objects.equals(recentEvent.getUrl(), event.getUrl())) {
                
                // Check if the event is within the debounce window
                long timeDiff = event.getTimestamp().toEpochMilli() - recentEvent.getTimestamp().toEpochMilli();
                if (timeDiff < EVENT_DEBOUNCE_MS) {
                    return true;
                }
            }
            
            // For input events, check if it's the same element and similar value
            if (event.getType() == RecordedEventType.INPUT && 
                    isSameElement(recentEvent.getElementInfo(), event.getElementInfo())) {
                
                // Check if the event is within the debounce window
                long timeDiff = event.getTimestamp().toEpochMilli() - recentEvent.getTimestamp().toEpochMilli();
                if (timeDiff < EVENT_DEBOUNCE_MS) {
                    // For input events, we also check if the value is similar
                    // This is to avoid recording every keystroke
                    String prevValue = recentEvent.getValue();
                    String newValue = event.getValue();
                    
                    // If the new value is just one character longer, it's likely a keystroke
                    if (prevValue != null && newValue != null && 
                            (newValue.startsWith(prevValue) || prevValue.startsWith(newValue)) &&
                            Math.abs(newValue.length() - prevValue.length()) <= 1) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Enhance an event with additional information
     *
     * @param event The event to enhance
     */
    private void enhanceEvent(RecordedEvent event) {
        // Generate a unique ID if not present
        if (event.getId() == null) {
            event.setId(UUID.randomUUID());
        }
        
        // Set timestamp if not present
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }
        
        // Enhance element information if present
        if (event.getElementInfo() != null) {
            enhanceElementInfo(event.getElementInfo());
        }
    }
    
    /**
     * Enhance element information with additional data
     *
     * @param elementInfo The element info to enhance
     */
    private void enhanceElementInfo(ElementInfo elementInfo) {
        // Generate smart locators if not present
        if (elementInfo.getXpath() == null) {
            generateXPath(elementInfo);
        }
        
        // Generate a friendly name if not present
        if (elementInfo.getFriendlyName() == null || elementInfo.getFriendlyName().isEmpty()) {
            generateFriendlyName(elementInfo);
        }
    }
    
    /**
     * Generate XPath for an element using available attributes
     *
     * @param elementInfo The element info
     */
    private void generateXPath(ElementInfo elementInfo) {
        StringBuilder xpath = new StringBuilder();
        String tagName = elementInfo.getTagName() != null ? elementInfo.getTagName().toLowerCase() : "*";
        
        // Strategy 1: ID-based XPath (most reliable)
        if (elementInfo.getId() != null && !elementInfo.getId().isEmpty()) {
            xpath.append("//*[@id='").append(elementInfo.getId()).append("']");
        }
        // Strategy 2: Name attribute 
        else if (elementInfo.getName() != null && !elementInfo.getName().isEmpty()) {
            xpath.append("//").append(tagName).append("[@name='").append(elementInfo.getName()).append("']");
        }
        // Strategy 3: Use combination of attributes
        else {
            xpath.append("//").append(tagName);
            
            Map<String, String> attributes = elementInfo.getAttributes();
            if (attributes != null && !attributes.isEmpty()) {
                boolean hasAttr = false;
                
                // Add class attribute if available (very common)
                if (attributes.containsKey("class") && !attributes.get("class").isEmpty()) {
                    String[] classes = attributes.get("class").split("\\s+");
                    // Use the first class that's not too generic (if available)
                    for (String cls : classes) {
                        if (cls.length() > 2 && !cls.matches("^(row|col|container|wrapper|box|item|btn|active|disabled)$")) {
                            xpath.append("[@class='").append(attributes.get("class")).append("']");
                            hasAttr = true;
                            break;
                        }
                    }
                    
                    // If no good class found, use contains for the first class
                    if (!hasAttr && classes.length > 0) {
                        xpath.append("[contains(@class, '").append(classes[0]).append("')]");
                        hasAttr = true;
                    }
                }
                
                // Add other useful attributes
                String[] usefulAttrs = {"type", "role", "aria-label", "title", "data-testid", "placeholder"};
                for (String attr : usefulAttrs) {
                    if (attributes.containsKey(attr) && !attributes.get(attr).isEmpty()) {
                        xpath.append("[@").append(attr).append("='").append(attributes.get(attr)).append("']");
                        hasAttr = true;
                        break;
                    }
                }
                
                // If element has text, use that
                if (!hasAttr && elementInfo.getText() != null && !elementInfo.getText().isEmpty()) {
                    // Truncate text if too long and escape quotes
                    String text = elementInfo.getText();
                    if (text.length() > 50) {
                        text = text.substring(0, 50);
                    }
                    // Escape any single quotes in the text
                    text = text.replace("'", "\\'");
                    
                    xpath.append("[text()='").append(text).append("']");
                }
            }
        }
        
        elementInfo.setXpath(xpath.toString());
        
        // Generate a CSS selector backup if not already present
        if (elementInfo.getCssSelector() == null || elementInfo.getCssSelector().isEmpty()) {
            generateCssSelector(elementInfo);
        }
    }
    
    /**
     * Generate a CSS selector for the element
     * 
     * @param elementInfo The element info
     */
    private void generateCssSelector(ElementInfo elementInfo) {
        StringBuilder css = new StringBuilder();
        String tagName = elementInfo.getTagName() != null ? elementInfo.getTagName().toLowerCase() : "*";
        
        // Strategy 1: ID-based selector
        if (elementInfo.getId() != null && !elementInfo.getId().isEmpty()) {
            css.append("#").append(elementInfo.getId());
        }
        // Strategy 2: Name attribute
        else if (elementInfo.getName() != null && !elementInfo.getName().isEmpty()) {
            css.append(tagName).append("[name='").append(elementInfo.getName()).append("']");
        }
        // Strategy 3: Class-based selector
        else {
            css.append(tagName);
            
            Map<String, String> attributes = elementInfo.getAttributes();
            if (attributes != null && !attributes.isEmpty()) {
                // Add classes
                if (attributes.containsKey("class") && !attributes.get("class").isEmpty()) {
                    String[] classes = attributes.get("class").split("\\s+");
                    // Add up to two classes for specificity but avoid overly generic ones
                    int classCount = 0;
                    for (String cls : classes) {
                        if (cls.length() > 2 && !cls.matches("^(row|col|container|wrapper|box|item|btn|active|disabled)$")) {
                            css.append(".").append(cls);
                            classCount++;
                            if (classCount >= 2) break;
                        }
                    }
                }
                
                // Add other attributes if no classes were added
                if (!css.toString().contains(".")) {
                    String[] usefulAttrs = {"type", "role", "data-testid", "placeholder"};
                    for (String attr : usefulAttrs) {
                        if (attributes.containsKey(attr) && !attributes.get(attr).isEmpty()) {
                            css.append("[").append(attr).append("='").append(attributes.get(attr)).append("']");
                            break;
                        }
                    }
                }
            }
        }
        
        elementInfo.setCssSelector(css.toString());
    }
    
    /**
     * Generate a friendly name for an element
     *
     * @param elementInfo The element info
     */
    private void generateFriendlyName(ElementInfo elementInfo) {
        String name = "";
        
        // Use text content if available
        if (elementInfo.getText() != null && !elementInfo.getText().isEmpty()) {
            name = elementInfo.getText();
            if (name.length() > 20) {
                name = name.substring(0, 17) + "...";
            }
        } 
        // Use placeholder text for inputs
        else if (elementInfo.getPlaceholder() != null && !elementInfo.getPlaceholder().isEmpty()) {
            name = elementInfo.getPlaceholder() + " input";
        }
        // Use element type and attributes
        else {
            String type = elementInfo.getTagName() != null ? elementInfo.getTagName().toLowerCase() : "element";
            
            if ("input".equals(type)) {
                String inputType = elementInfo.getAttributes().get("type");
                if (inputType != null) {
                    type = inputType + " input";
                }
            } else if ("button".equals(type)) {
                name = "button";
            } else if ("a".equals(type)) {
                name = "link";
            } else if ("select".equals(type)) {
                name = "dropdown";
            } else if ("textarea".equals(type)) {
                name = "text area";
            }
            
            // Add ID or name if available
            if (elementInfo.getId() != null && !elementInfo.getId().isEmpty()) {
                name = type + " #" + elementInfo.getId();
            } else if (elementInfo.getName() != null && !elementInfo.getName().isEmpty()) {
                name = type + " named " + elementInfo.getName();
            } else {
                name = type;
            }
        }
        
        elementInfo.setFriendlyName(name);
    }
    
    /**
     * Check if two elements are the same based on their identifiers
     *
     * @param element1 First element
     * @param element2 Second element
     * @return true if the elements are the same
     */
    private boolean isSameElement(ElementInfo element1, ElementInfo element2) {
        if (element1 == null || element2 == null) {
            return false;
        }
        
        // Check ID
        if (element1.getId() != null && element2.getId() != null &&
                !element1.getId().isEmpty() && element1.getId().equals(element2.getId())) {
            return true;
        }
        
        // Check CSS selector
        if (element1.getCssSelector() != null && element2.getCssSelector() != null &&
                !element1.getCssSelector().isEmpty() && element1.getCssSelector().equals(element2.getCssSelector())) {
            return true;
        }
        
        // Check XPath
        if (element1.getXpath() != null && element2.getXpath() != null &&
                !element1.getXpath().isEmpty() && element1.getXpath().equals(element2.getXpath())) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if an event type is an element interaction
     *
     * @param type The event type
     * @return true if the event is an element interaction
     */
    private boolean isElementInteractionEvent(RecordedEventType type) {
        return type == RecordedEventType.CLICK ||
               type == RecordedEventType.DBLCLICK ||
               type == RecordedEventType.INPUT ||
               type == RecordedEventType.CHANGE ||
               type == RecordedEventType.FOCUS ||
               type == RecordedEventType.BLUR ||
               type == RecordedEventType.KEYDOWN ||
               type == RecordedEventType.KEYUP ||
               type == RecordedEventType.MOUSEDOWN ||
               type == RecordedEventType.MOUSEUP ||
               type == RecordedEventType.MOUSEOVER ||
               type == RecordedEventType.MOUSEOUT ||
               type == RecordedEventType.DRAG ||
               type == RecordedEventType.DROP;
    }
    
    /**
     * Add an event to the recent events queue for a session
     *
     * @param sessionId The session ID
     * @param event The event to add
     */
    private void addToRecentEvents(UUID sessionId, RecordedEvent event) {
        Queue<RecordedEvent> sessionEvents = recentEvents.computeIfAbsent(sessionId, 
                k -> new LinkedList<>());
        
        // Add the event to the queue
        sessionEvents.add(event);
        
        // Trim the queue if it exceeds the maximum size
        while (sessionEvents.size() > MAX_RECENT_EVENTS) {
            sessionEvents.poll();
        }
    }
} 