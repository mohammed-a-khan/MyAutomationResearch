package com.cstestforge.recorder.websocket;

/**
 * Represents a structured message for WebSocket communication.
 * @param <T> The type of the payload
 */
public class WebSocketMessage<T> {
    
    private String type;
    private T payload;
    
    /**
     * Default constructor required for serialization
     */
    public WebSocketMessage() {
    }
    
    /**
     * Create a new WebSocket message
     *
     * @param type The message type
     * @param payload The message payload
     */
    public WebSocketMessage(String type, T payload) {
        this.type = type;
        this.payload = payload;
    }
    
    /**
     * Get the message type
     *
     * @return The message type
     */
    public String getType() {
        return type;
    }
    
    /**
     * Set the message type
     *
     * @param type The message type
     */
    public void setType(String type) {
        this.type = type;
    }
    
    /**
     * Get the message payload
     *
     * @return The message payload
     */
    public T getPayload() {
        return payload;
    }
    
    /**
     * Set the message payload
     *
     * @param payload The message payload
     */
    public void setPayload(T payload) {
        this.payload = payload;
    }
    
    /**
     * Create a new WebSocket message with the given type and payload
     *
     * @param <T> The payload type
     * @param type The message type
     * @param payload The message payload
     * @return A new WebSocketMessage instance
     */
    public static <T> WebSocketMessage<T> of(String type, T payload) {
        return new WebSocketMessage<>(type, payload);
    }
} 