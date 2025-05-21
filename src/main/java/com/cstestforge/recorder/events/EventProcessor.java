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
import java.time.Duration;
import java.time.Instant;

/**
 * Processes browser events for recording sessions.
 * Enhanced with better event filtering, deduplication, and handling.
 */
@Component
public class EventProcessor {
    private static final Logger logger = LoggerFactory.getLogger(EventProcessor.class);

    @Autowired
    private RecorderWebSocketService webSocketService;

    // Store recent events to detect duplicates with improved structure
    private final Map<UUID, Deque<ProcessedEvent>> recentEvents = new ConcurrentHashMap<>();

    // Maximum number of recent events to keep per session
    private static final int MAX_RECENT_EVENTS = 100;

    // Minimum time between similar events in milliseconds
    private static final long CLICK_DEBOUNCE_MS = 300;
    private static final long INPUT_DEBOUNCE_MS = 500;
    private static final long NAVIGATION_DEBOUNCE_MS = 1000;

    // Inner class to store processed events with additional metadata
    private static class ProcessedEvent {
        final RecordedEvent event;
        final long processTime;
        final String fingerprint;

        ProcessedEvent(RecordedEvent event) {
            this.event = event;
            this.processTime = System.currentTimeMillis();
            this.fingerprint = generateFingerprint(event);
        }

        private static String generateFingerprint(RecordedEvent event) {
            StringBuilder fp = new StringBuilder();
            fp.append(event.getType()).append(":");

            // Add type-specific fingerprinting
            switch (event.getType()) {
                case CLICK:
                case DOUBLE_CLICK:
                case RIGHT_CLICK:
                    if (event.getElementInfo() != null) {
                        fp.append("element[");
                        if (event.getElementInfo().getId() != null) {
                            fp.append("id=").append(event.getElementInfo().getId());
                        } else if (event.getElementInfo().getXpath() != null) {
                            fp.append("xpath=").append(event.getElementInfo().getXpath());
                        } else if (event.getElementInfo().getCssSelector() != null) {
                            fp.append("css=").append(event.getElementInfo().getCssSelector());
                        } else {
                            fp.append("tag=").append(event.getElementInfo().getTagName());
                        }
                        fp.append("]");
                    }
                    break;

                case NAVIGATION:
                    fp.append("url[").append(event.getUrl()).append("]");
                    break;

                case INPUT:
                    if (event.getElementInfo() != null) {
                        fp.append("element[");
                        if (event.getElementInfo().getId() != null) {
                            fp.append("id=").append(event.getElementInfo().getId());
                        } else if (event.getElementInfo().getName() != null) {
                            fp.append("name=").append(event.getElementInfo().getName());
                        } else {
                            fp.append("tag=").append(event.getElementInfo().getTagName());
                        }
                        fp.append("]");
                    }
                    break;

                default:
                    // For other event types, just use the type as the fingerprint
                    break;
            }

            return fp.toString();
        }
    }

    /**
     * Process a new event from the browser with improved filtering
     *
     * @param sessionId The session ID
     * @param event The event to process
     * @return The processed event or null if the event was filtered out
     */
    public RecordedEvent processEvent(UUID sessionId, RecordedEvent event) {
        if (sessionId == null || event == null) {
            return null;
        }

        // Handle "HEARTBEAT" type events - convert to RECORDER_STATUS
        if (event.getType() == null && "HEARTBEAT".equals(event.getMetadata().get("originalType"))) {
            event.setType(RecordedEventType.RECORDER_STATUS);
        }

        // Set default values for critical fields if missing
        normalizeEvent(event);

        // Validate the event
        if (!validateEvent(event)) {
            logger.debug("Event validation failed for session {}: {}", sessionId, event);
            return null;
        }

        // Check for duplicate or similar events
        if (isDuplicateEvent(sessionId, event)) {
            logger.debug("Duplicate event filtered for session {}: {}", sessionId, event.getType());
            return null;
        }

        // Enhance the event with additional information
        enhanceEvent(event);

        // Add to recent events for deduplication
        addToRecentEvents(sessionId, event);

        // Notify connected clients via WebSocket
        if (webSocketService != null) {
            try {
                webSocketService.notifyEventAdded(sessionId, event);
            } catch (Exception e) {
                logger.warn("Failed to notify clients about new event: {}", e.getMessage());
            }
        }

        logger.debug("Processed event for session {}: {}", sessionId, event.getType());
        return event;
    }

    /**
     * Sets default values for critical fields if missing
     *
     * @param event The event to normalize
     */
    private void normalizeEvent(RecordedEvent event) {
        // Set ID if missing
        if (event.getId() == null) {
            event.setId(UUID.randomUUID());
        }

        // Set timestamp if missing
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }

        // Set type-specific default values
        switch (event.getType()) {
            case NAVIGATION:
                // Ensure URL is set for navigation events
                if (event.getUrl() == null || event.getUrl().isEmpty()) {
                    event.setUrl("about:blank");
                }
                break;

            case CLICK:
            case DOUBLE_CLICK:
            case RIGHT_CLICK:
                // Ensure element info is present for click events
                if (event.getElementInfo() == null) {
                    ElementInfo elementInfo = new ElementInfo();
                    elementInfo.setTagName("unknown");
                    event.setElementInfo(elementInfo);
                }
                break;

            case INPUT:
                // Ensure value is set for input events
                if (event.getValue() == null) {
                    event.setValue("");
                }
                break;

            default:
                // No specific normalization for other event types
                break;
        }
    }

    /**
     * Process a batch of events with improved efficiency
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
     * Validate that an event has all required fields with improved logic
     *
     * @param event The event to validate
     * @return true if the event is valid
     */
    private boolean validateEvent(RecordedEvent event) {
        // Check for minimum requirements for all events
        if (event.getType() == null) {
            logger.debug("Event rejected: missing type");
            return false;
        }

        // Validate based on event type
        switch (event.getType()) {
            case CLICK:
            case DOUBLE_CLICK:
            case RIGHT_CLICK:
                // For click events, we need element info or at least URL context
                if (event.getElementInfo() == null && (event.getUrl() == null || event.getUrl().isEmpty())) {
                    logger.debug("Click event rejected: missing element info and URL context");
                    return false;
                }
                break;

            case NAVIGATION:
                // For navigation events, URL is required
                if (event.getUrl() == null || event.getUrl().isEmpty()) {
                    logger.debug("Navigation event rejected: missing URL");
                    return false;
                }
                break;

            case INPUT:
                // For input events, element info and value are important
                if (event.getElementInfo() == null) {
                    logger.debug("Input event rejected: missing element info");
                    return false;
                }
                break;

            case WAIT:
            case VERIFY:
            case SCREENSHOT:
            case LOOP:
                // These events have less strict validation requirements
                break;

            default:
                // For unknown event types, log a warning but still accept
                logger.warn("Unknown event type: {}", event.getType());
                break;
        }

        return true;
    }

    /**
     * Check if an event is a duplicate or too similar to recent events with improved algorithm
     *
     * @param sessionId The session ID
     * @param event The event to check
     * @return true if the event is a duplicate
     */
    private boolean isDuplicateEvent(UUID sessionId, RecordedEvent event) {
        Deque<ProcessedEvent> sessionEvents = recentEvents.get(sessionId);
        if (sessionEvents == null || sessionEvents.isEmpty()) {
            return false;
        }

        // Create fingerprint for fast comparison
        String eventFingerprint = ProcessedEvent.generateFingerprint(event);
        long currentTime = System.currentTimeMillis();

        // Get debounce time based on event type
        long debounceTime;
        switch (event.getType()) {
            case CLICK:
            case DOUBLE_CLICK:
            case RIGHT_CLICK:
                debounceTime = CLICK_DEBOUNCE_MS;
                break;
            case INPUT:
                debounceTime = INPUT_DEBOUNCE_MS;
                break;
            case NAVIGATION:
                debounceTime = NAVIGATION_DEBOUNCE_MS;
                break;
            default:
                debounceTime = 100; // Low debounce for other event types
        }

        // Check against recent events
        for (ProcessedEvent recentEvent : sessionEvents) {
            // Skip if event types don't match
            if (recentEvent.event.getType() != event.getType()) {
                continue;
            }

            // Check time difference
            long timeDiff = currentTime - recentEvent.processTime;
            if (timeDiff > debounceTime) {
                // Event is outside the debounce window
                continue;
            }

            // For events within the debounce window, check fingerprint
            if (eventFingerprint.equals(recentEvent.fingerprint)) {
                return true;
            }

            // Special handling for input events to detect typing
            if (event.getType() == RecordedEventType.INPUT) {
                String prevValue = recentEvent.event.getValue();
                String newValue = event.getValue();

                // Check if this is likely consecutive typing (value appending)
                if (prevValue != null && newValue != null &&
                        (newValue.startsWith(prevValue) || prevValue.startsWith(newValue)) &&
                        Math.abs(newValue.length() - prevValue.length()) <= 2) {
                    // This looks like incremental typing, filter it out
                    return true;
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
        // Generate selectors if not present
        if (event.getElementInfo() != null) {
            enhanceElementInfo(event.getElementInfo());
        }

        // Add additional context based on event type
        switch (event.getType()) {
            case NAVIGATION:
                // Add page title if missing
                if (event.getTitle() == null || event.getTitle().isEmpty()) {
                    event.setTitle("Unknown Page");
                }
                break;

            case CLICK:
            case DOUBLE_CLICK:
            case RIGHT_CLICK:
                // Add descriptive text for click targets
                if (event.getElementInfo() != null &&
                        (event.getDescription() == null || event.getDescription().isEmpty())) {
                    ElementInfo element = event.getElementInfo();
                    String targetDesc = element.getFriendlyName();
                    if (targetDesc == null || targetDesc.isEmpty()) {
                        targetDesc = element.getTagName();
                        if (element.getId() != null && !element.getId().isEmpty()) {
                            targetDesc += " with ID '" + element.getId() + "'";
                        } else if (element.getText() != null && !element.getText().isEmpty()) {
                            String text = element.getText();
                            if (text.length() > 20) {
                                text = text.substring(0, 17) + "...";
                            }
                            targetDesc += " with text '" + text + "'";
                        }
                    }

                    String clickType = event.getType() == RecordedEventType.DOUBLE_CLICK ? "Double-click on " :
                            event.getType() == RecordedEventType.RIGHT_CLICK ? "Right-click on " :
                                    "Click on ";

                    event.setDescription(clickType + targetDesc);
                }
                break;

            case INPUT:
                // Add descriptive text for input events
                if (event.getElementInfo() != null &&
                        (event.getDescription() == null || event.getDescription().isEmpty())) {
                    ElementInfo element = event.getElementInfo();
                    String inputDesc = element.getFriendlyName();
                    if (inputDesc == null || inputDesc.isEmpty()) {
                        inputDesc = element.getTagName();
                        if (element.getId() != null && !element.getId().isEmpty()) {
                            inputDesc += " with ID '" + element.getId() + "'";
                        } else if (element.getName() != null && !element.getName().isEmpty()) {
                            inputDesc += " with name '" + element.getName() + "'";
                        }
                    }

                    String value = event.getValue();
                    if (value != null) {
                        if (element.getType() != null &&
                                ("password".equalsIgnoreCase(element.getType()) ||
                                        element.getType().toLowerCase().contains("password"))) {
                            value = "********";
                        } else if (value.length() > 20) {
                            value = value.substring(0, 17) + "...";
                        }
                    } else {
                        value = "";
                    }

                    event.setDescription("Enter '" + value + "' into " + inputDesc);
                }
                break;

            default:
                // No specific enhancements for other event types
                break;
        }
    }

    /**
     * Enhance element information with additional data
     *
     * @param elementInfo The element info to enhance
     */
    private void enhanceElementInfo(ElementInfo elementInfo) {
        // Generate selectors if not present
        if (elementInfo.getXpath() == null || elementInfo.getXpath().isEmpty()) {
            generateXPath(elementInfo);
        }

        if (elementInfo.getCssSelector() == null || elementInfo.getCssSelector().isEmpty()) {
            generateCssSelector(elementInfo);
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
                            xpath.append("[contains(@class, '").append(cls).append("')]");
                            hasAttr = true;
                            break;
                        }
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
    }

    /**
     * Generate a CSS selector for the element with improved algorithm
     *
     * @param elementInfo The element info
     */
    private void generateCssSelector(ElementInfo elementInfo) {
        StringBuilder css = new StringBuilder();
        String tagName = elementInfo.getTagName() != null ? elementInfo.getTagName().toLowerCase() : "*";

        // Strategy 1: ID-based selector (most reliable)
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

            // Add nth-child if we still have just a tag selector
            if (css.toString().equals(tagName)) {
                css.append(":nth-of-type(1)");
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

        // Use text content if available and not too long
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
                String inputType = elementInfo.getType();
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
     * Add an event to the recent events queue for a session with improved memory management
     *
     * @param sessionId The session ID
     * @param event The event to add
     */
    private void addToRecentEvents(UUID sessionId, RecordedEvent event) {
        Deque<ProcessedEvent> sessionEvents = recentEvents.computeIfAbsent(sessionId,
                k -> new LinkedList<>());

        // Add the event to the queue
        sessionEvents.addFirst(new ProcessedEvent(event));

        // Trim the queue if it exceeds the maximum size
        while (sessionEvents.size() > MAX_RECENT_EVENTS) {
            sessionEvents.removeLast();
        }
    }

    /**
     * Clean up old session data for memory management
     */
    public void cleanupOldSessions() {
        long now = System.currentTimeMillis();
        long maxAge = TimeUnit.HOURS.toMillis(1); // Remove sessions older than 1 hour

        recentEvents.entrySet().removeIf(entry -> {
            if (entry.getValue().isEmpty()) {
                return true;
            }

            ProcessedEvent oldest = entry.getValue().getLast();
            return (now - oldest.processTime) > maxAge;
        });
    }
}