package com.cstestforge.recorder.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.ExceptionWebSocketHandlerDecorator;
import org.springframework.web.socket.handler.LoggingWebSocketHandlerDecorator;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.util.Map;
import java.util.UUID;
import java.security.Principal;

/**
 * Unified WebSocket configuration for the recorder system.
 * This replaces both RecorderWebSocketConfig and WebSocketConfig to provide a
 * consistent WebSocket setup with robust error handling and fallbacks.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable a simple memory-based message broker for subscription endpoints
        registry.enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(new ConcurrentTaskScheduler()) // Add explicit task scheduler
                .setHeartbeatValue(new long[] {10000, 10000}); // 10 second heartbeat interval

        // Application destination prefixes for client-to-server messages
        registry.setApplicationDestinationPrefixes("/app");

        // User destination prefix for user-specific messages
        registry.setUserDestinationPrefix("/user");

        logger.info("WebSocket message broker configured with heartbeat and task scheduler");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register primary WebSocket endpoint with enhanced error handling
        registry.addEndpoint("/ws-recorder")
                .setAllowedOriginPatterns("*") // Allow connections from any origin
                .setHandshakeHandler(createHandshakeHandler())
                .addInterceptors(createSessionInterceptor());

        // Also register SockJS fallback for browsers that don't support WebSocket
        registry.addEndpoint("/ws-recorder")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(createHandshakeHandler())
                .addInterceptors(createSessionInterceptor())
                .withSockJS()
                .setClientLibraryUrl("/webjars/sockjs-client/1.5.1/sockjs.min.js")
                .setSessionCookieNeeded(true)
                .setDisconnectDelay(30 * 1000);

        // Add standard /ws endpoint for backward compatibility
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(createHandshakeHandler())
                .withSockJS()
                .setClientLibraryUrl("/webjars/sockjs-client/1.5.1/sockjs.min.js")
                .setSessionCookieNeeded(true)
                .setDisconnectDelay(30 * 1000);

        logger.info("WebSocket STOMP endpoints registered with enhanced error handling");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // Configure WebSocket transport parameters for better performance
        registration.setMessageSizeLimit(1024 * 1024) // Increase message size limit to 1MB
                .setSendBufferSizeLimit(2 * 1024 * 1024) // Increase buffer size to 2MB
                .setSendTimeLimit(30 * 1000) // Set send timeout to 30 seconds
                .setTimeToFirstMessage(60 * 1000); // Allow 60 seconds for the first message

        logger.info("WebSocket transport configured with enhanced limits");
    }

    /**
     * Create a session tracking interceptor
     */
    private HandshakeInterceptor createSessionInterceptor() {
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request,
                                           ServerHttpResponse response,
                                           WebSocketHandler wsHandler,
                                           Map<String, Object> attributes) throws Exception {
                String path = request.getURI().getPath();
                String origin = request.getHeaders().getOrigin();
                logger.debug("WebSocket handshake initiated from {} for path: {}", origin, path);

                // Add connection metadata
                attributes.put("connectionTime", System.currentTimeMillis());
                attributes.put("origin", origin);
                attributes.put("remoteAddress", request.getRemoteAddress());

                // Generate a connection ID for tracing
                attributes.put("connectionId", UUID.randomUUID().toString());

                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request,
                                       ServerHttpResponse response,
                                       WebSocketHandler wsHandler,
                                       Exception exception) {
                if (exception != null) {
                    logger.error("WebSocket handshake failed: {}", exception.getMessage(), exception);
                } else {
                    // Add CORS headers to response to ensure cross-origin compatibility
                    response.getHeaders().add("Access-Control-Allow-Origin", "*");
                    response.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                    response.getHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
                    response.getHeaders().add("Access-Control-Max-Age", "3600");

                    logger.debug("WebSocket handshake completed successfully");
                }
            }
        };
    }

    /**
     * Create an enhanced handshake handler with better error handling
     */
    private DefaultHandshakeHandler createHandshakeHandler() {
        return new CustomHandshakeHandler();
    }

    /**
     * Decorate WebSocket handlers with better error handling and logging
     */
    @Bean
    public WebSocketHandler decoratedWebSocketHandler(WebSocketHandler webSocketHandler) {
        // Add logging and exception handling decorators
        return new ExceptionWebSocketHandlerDecorator(
                new LoggingWebSocketHandlerDecorator(webSocketHandler));
    }

    /**
     * Custom handshake handler that supports anonymous principals
     */
    private static class CustomHandshakeHandler extends DefaultHandshakeHandler {
        @Override
        protected Principal determineUser(ServerHttpRequest request,
                                          WebSocketHandler wsHandler,
                                          Map<String, Object> attributes) {
            // Get principal from the request if it exists
            Principal principal = request.getPrincipal();

            // If no principal exists, create an anonymous one
            if (principal == null) {
                String remoteAddress = request.getRemoteAddress().toString();
                String connectionId = (String) attributes.get("connectionId");
                principal = new AnonymousPrincipal(connectionId, remoteAddress);
                logger.debug("Created anonymous principal for WebSocket client: {}");
            }

            return principal;
        }

        /**
         * Simple principal implementation for anonymous users
         */
        private static class AnonymousPrincipal implements Principal {
            private final String name;
            private final String remoteAddress;

            public AnonymousPrincipal(String connectionId, String remoteAddress) {
                this.name = "anonymous-" + connectionId;
                this.remoteAddress = remoteAddress;
            }

            @Override
            public String getName() {
                return name;
            }

            public String getRemoteAddress() {
                return remoteAddress;
            }
        }
    }
}