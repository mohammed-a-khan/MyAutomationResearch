package com.cstestforge.recorder.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * WebSocket configuration for recorder module.
 * Using pure native WebSockets without SockJS for direct browser communication.
 */
@Configuration
@EnableWebSocketMessageBroker
public class RecorderWebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(RecorderWebSocketConfig.class);
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Set prefix for messages that are bound for methods annotated with @MessageMapping
        registry.setApplicationDestinationPrefixes("/app");
        
        // Set prefix for messages that are bound for the broker (subscribe)
        registry.enableSimpleBroker("/topic", "/queue");
        
        // Set prefix for user-specific messages
        registry.setUserDestinationPrefix("/user");
        
        logger.info("Configured WebSocket message broker with topics: /topic, /queue");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the "/ws-recorder" endpoint, using native WebSockets without SockJS
        registry.addEndpoint("/ws-recorder")
                .setAllowedOrigins("*") // Allow connections from any origin
                .setAllowedOriginPatterns("*"); // Allow any pattern
        
        logger.info("Registered STOMP endpoint at /ws-recorder with full cross-origin support");
    }
    
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // Configure WebSocket transport settings
        registration.setMessageSizeLimit(65536) // Message size limit in bytes (64KB)
                .setSendBufferSizeLimit(512 * 1024) // Buffer size limit for outgoing messages (512KB)
                .setSendTimeLimit(20000); // Time limit for sending messages in milliseconds (20s)
        
        logger.info("Configured WebSocket transport with message size limit: 64KB");
    }
} 