package com.cstestforge.recorder.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * Custom WebSocket handshake handler that allows any origin and creates anonymous principals
 * for connecting clients that don't have authentication.
 */
public class CustomHandshakeHandler extends DefaultHandshakeHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomHandshakeHandler.class);
    
    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        // Get principal from the request if it exists
        Principal principal = request.getPrincipal();
        
        // If no principal exists, create an anonymous one
        if (principal == null) {
            String remoteAddress = request.getRemoteAddress().toString();
            principal = new AnonymousPrincipal(remoteAddress);
            logger.debug("Created anonymous principal for WebSocket client: {}", remoteAddress);
        }
        
        return principal;
    }
    
    /**
     * Simple principal implementation for anonymous users
     */
    private static class AnonymousPrincipal implements Principal {
        private final String name;
        
        public AnonymousPrincipal(String remoteAddress) {
            // Create a unique name using the remote address and a random UUID
            this.name = "anonymous-" + remoteAddress + "-" + UUID.randomUUID();
        }
        
        @Override
        public String getName() {
            return name;
        }
    }
} 