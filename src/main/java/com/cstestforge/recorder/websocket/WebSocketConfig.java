package com.cstestforge.recorder.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * WebSocket configuration for real-time communication between server and browser.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure the message broker for WebSocket communication.
     * 
     * @param config The message broker registry
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple message broker for sending messages to the client
        config.enableSimpleBroker("/topic", "/queue");
        
        // Set the application destination prefix for client-to-server messages
        config.setApplicationDestinationPrefixes("/app");
        
        // Set the user destination prefix for user-specific messages
        config.setUserDestinationPrefix("/user");
    }

    /**
     * Register STOMP endpoints for WebSocket communication.
     * 
     * @param registry The STOMP endpoint registry
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the "/ws" endpoint, enabling SockJS fallback options
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS()
            .setClientLibraryUrl("/webjars/sockjs-client/1.5.1/sockjs.min.js")
            .setSessionCookieNeeded(true)
            .setDisconnectDelay(30 * 1000);
    }

    /**
     * Configure WebSocket transport options.
     * 
     * @param registration The WebSocket transport registration
     */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // Set message size limit (10MB)
        registration.setMessageSizeLimit(10 * 1024 * 1024);
        
        // Set send buffer size limit (5MB)
        registration.setSendBufferSizeLimit(5 * 1024 * 1024);
        
        // Set send time limit (20 seconds)
        registration.setSendTimeLimit(20 * 1000);
    }
} 