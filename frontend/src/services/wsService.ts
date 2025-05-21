/**
 * WebSocket service for real-time event streaming
 * Using pure native WebSockets without third-party libraries
 */
import { logError } from '../utils/errorHandling';
// TypeScript definitions and imports (no SockJS)

// Event types
export enum WsMessageType {
  EVENT_RECORDED = 'EVENT_RECORDED',
  SESSION_STATUS = 'SESSION_STATUS',
  ERROR = 'ERROR',
  CONNECTION_STATUS = 'CONNECTION_STATUS',
  ELEMENT_HIGHLIGHTED = 'ELEMENT_HIGHLIGHTED'
}

// Message interface
export interface WsMessage<T = any> {
  type: WsMessageType;
  payload: T;
  timestamp: number;
}

// Connection status
export enum ConnectionStatus {
  CONNECTING = 'CONNECTING',
  CONNECTED = 'CONNECTED',
  DISCONNECTED = 'DISCONNECTED',
  ERROR = 'ERROR'
}

// WebSocket service class
class WebSocketService {
  private socket: WebSocket | null = null;
  private messageListeners: Map<string, Set<(message: any) => void>> = new Map();
  private statusListeners: Set<(status: ConnectionStatus) => void> = new Set();
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectTimeout: ReturnType<typeof setTimeout> | null = null;
  private sessionId: string | null = null;
  private status: ConnectionStatus = ConnectionStatus.DISCONNECTED;
  private keepAliveInterval: ReturnType<typeof setInterval> | null = null;
  private wsUrl: string | null = null;
  private reconnecting = false;
  private lastMessageTime = 0;
  private connectionTimeout: ReturnType<typeof setTimeout> | null = null;
  private healthCheckInterval: ReturnType<typeof setInterval> | null = null;
  private healthCheckTimeoutMs = 30000; // 30 seconds timeout for health check
  private maxReconnectDelay = 30000; // Maximum reconnect delay of 30 seconds
  private connectionMonitorActive = false;

  /**
   * Initialize a WebSocket connection
   * @param sessionId Recording session ID
   * @returns Promise resolving when connection is established
   */
  public connect(sessionId: string): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        console.debug('WebSocket already connected, disconnecting first');
        this.disconnect();
      }

      this.sessionId = sessionId;
      this.setStatus(ConnectionStatus.CONNECTING);

      // Clear any previous reconnection timeout
      if (this.reconnectTimeout) {
        clearTimeout(this.reconnectTimeout);
        this.reconnectTimeout = null;
      }

      // Create the WebSocket URL
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const host = window.location.host;
      
      // Use the application's context path - must match server.servlet.context-path in application.properties
      const contextPath = '/cstestforge';
      
      // Create the WebSocket URL with the context path prefix - use native WebSocket, not SockJS
      this.wsUrl = `${protocol}//${host}${contextPath}/ws-recorder`;
      
      console.debug('Connecting to WebSocket server', {
        url: this.wsUrl,
        sessionId: sessionId,
        protocol: protocol,
        host: host,
        browserUrl: window.location.href
      });

      // Store the URL in sessionStorage for reconnection purposes
      try {
        sessionStorage.setItem('__csRecorderWsUrl', this.wsUrl);
        sessionStorage.setItem('__csRecorderSessionId', sessionId);
      } catch (e) {
        console.warn('Failed to store WebSocket connection details in sessionStorage', e);
      }

      try {
        // Create native WebSocket connection - no third-party dependencies
        this.socket = new WebSocket(this.wsUrl);
        
        // Set connection timeout
        this.connectionTimeout = setTimeout(() => {
          if (this.status !== ConnectionStatus.CONNECTED) {
            console.warn('WebSocket connection timeout after 10 seconds');
            this.setStatus(ConnectionStatus.ERROR);
            
            if (this.socket) {
              // Force close the socket and trigger reconnection
              try {
                this.socket.close();
              } catch (e) {
                // Ignore
              }
              this.socket = null;
            }
            
            this.attemptReconnect();
            reject(new Error('WebSocket connection timeout'));
          }
        }, 10000);
        
        this.socket.onopen = () => {
          this.setStatus(ConnectionStatus.CONNECTED);
          this.reconnectAttempts = 0;
          this.reconnecting = false;
          this.lastMessageTime = Date.now();
          
          // Clear connection timeout
          if (this.connectionTimeout) {
            clearTimeout(this.connectionTimeout);
            this.connectionTimeout = null;
          }
          
          // Start connection monitoring
          this.startConnectionMonitoring();
          
          console.debug('WebSocket connected successfully', {
            sessionId: this.sessionId,
            url: this.wsUrl,
            timestamp: new Date().toISOString()
          });
          
          // Subscribe to session-specific topics
          this.sendMessage('SUBSCRIBE', { 
            destination: `/topic/session/${sessionId}`,
            sessionId: sessionId
          }).catch(err => {
            console.warn('Failed to subscribe to session topic', err);
          });
          
          resolve();
        };
        
        this.socket.onmessage = (event: MessageEvent) => {
          try {
            this.lastMessageTime = Date.now();
            const message: WsMessage = JSON.parse(event.data);
            this.handleMessage(message);
          } catch (error) {
            logError(error, 'WebSocket Message Parse');
          }
        };
        
        this.socket.onclose = (event: CloseEvent) => {
          // Clear connection timeout if it exists
          if (this.connectionTimeout) {
            clearTimeout(this.connectionTimeout);
            this.connectionTimeout = null;
          }
          
          // Clear connection monitoring
          this.clearConnectionMonitoring();
          
          this.setStatus(ConnectionStatus.DISCONNECTED);
          
          console.debug('WebSocket connection closed', {
            code: event.code,
            reason: event.reason,
            wasClean: event.wasClean,
            sessionId: this.sessionId,
            reconnecting: this.reconnecting
          });
          
          if (!this.reconnecting && event.code !== 1000) { // 1000 is normal closure
            this.attemptReconnect();
          }
        };
        
        this.socket.onerror = (error) => {
          this.setStatus(ConnectionStatus.ERROR);
          console.error('WebSocket error', {
            sessionId: this.sessionId,
            error: error
          });
          logError('WebSocket connection error', 'WebSocket');
          
          // Don't reject the promise if we're reconnecting
          if (!this.reconnecting) {
            reject(error);
          }
        };
      } catch (error) {
        // Clear connection timeout if it exists
        if (this.connectionTimeout) {
          clearTimeout(this.connectionTimeout);
          this.connectionTimeout = null;
        }
        
        this.setStatus(ConnectionStatus.ERROR);
        console.error('Failed to create WebSocket connection', error);
        logError(error, 'WebSocket Connection');
        reject(error);
      }
    });
  }

  /**
   * Close the WebSocket connection
   */
  public disconnect(): void {
    // Stop connection monitoring
    this.clearConnectionMonitoring();
    
    // Clear the keep-alive
    this.clearKeepAlive();
    
    if (this.socket) {
      try {
        this.socket.close(1000, 'Client initiated disconnect');
      } catch (error) {
        console.error('Error disconnecting WebSocket:', error);
      } finally {
        this.socket = null;
        this.setStatus(ConnectionStatus.DISCONNECTED);
      }
    }

    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }
    
    if (this.connectionTimeout) {
      clearTimeout(this.connectionTimeout);
      this.connectionTimeout = null;
    }
  }

  /**
   * Send a message through the WebSocket
   * @param type Message type
   * @param payload Message data
   * @returns Promise resolving when message is sent
   */
  public sendMessage<T = any>(type: string, payload: T): Promise<void> {
    return new Promise((resolve, reject) => {
      if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
        // If WebSocket is not open, try reconnecting once
        if (this.sessionId && !this.reconnecting && this.status !== ConnectionStatus.CONNECTING) {
          console.debug('WebSocket not connected, attempting reconnection before sending message');
          this.reconnecting = true;
          this.connect(this.sessionId)
            .then(() => this.sendMessage(type, payload))
            .then(resolve)
            .catch(reject);
          return;
        }
        
        reject(new Error('WebSocket is not connected'));
        return;
      }

      try {
        const message: WsMessage<T> = {
          type: type as WsMessageType,
          payload,
          timestamp: Date.now()
        };

        this.socket.send(JSON.stringify(message));
        resolve();
      } catch (error) {
        logError(error, 'WebSocket Send');
        reject(error);
      }
    });
  }

  /**
   * Subscribe to a specific message type
   * @param type Message type to listen for
   * @param listener Callback function
   * @returns Unsubscribe function
   */
  public subscribe<T = any>(type: WsMessageType, listener: (payload: T) => void): () => void {
    if (!this.messageListeners.has(type)) {
      this.messageListeners.set(type, new Set());
    }

    const listeners = this.messageListeners.get(type)!;
    listeners.add(listener);

    // Return unsubscribe function
    return () => {
      const listenerSet = this.messageListeners.get(type);
      if (listenerSet) {
        listenerSet.delete(listener);
      }
    };
  }

  /**
   * Subscribe to connection status changes
   * @param listener Status change callback
   * @returns Unsubscribe function
   */
  public subscribeToStatus(listener: (status: ConnectionStatus) => void): () => void {
    this.statusListeners.add(listener);
    
    // Immediately notify with current status
    listener(this.status);
    
    return () => {
      this.statusListeners.delete(listener);
    };
  }

  /**
   * Get current connection status
   * @returns Current status
   */
  public getStatus(): ConnectionStatus {
    return this.status;
  }

  /**
   * Check if connection is healthy
   * @returns True if connection is healthy
   */
  public isConnectionHealthy(): boolean {
    if (this.status !== ConnectionStatus.CONNECTED) {
      return false;
    }
    
    // Check if we received a message in the last 30 seconds
    const now = Date.now();
    return now - this.lastMessageTime < 30000;
  }

  /**
   * Handle incoming WebSocket messages
   * @param message Received message
   */
  private handleMessage(message: WsMessage): void {
    const { type, payload } = message;
    
    // Update last message time
    this.lastMessageTime = Date.now();
    
    const listeners = this.messageListeners.get(type);
    if (listeners) {
      listeners.forEach(listener => {
        try {
          listener(payload);
        } catch (error) {
          logError(error, 'WebSocket Message Listener');
        }
      });
    }
  }

  /**
   * Set the connection status and notify listeners
   * @param newStatus New connection status
   */
  private setStatus(newStatus: ConnectionStatus): void {
    if (this.status !== newStatus) {
      this.status = newStatus;
      
      // Notify all status listeners
      this.statusListeners.forEach(listener => {
        try {
          listener(newStatus);
        } catch (error) {
          console.error('Error in status listener:', error);
        }
      });
    }
  }

  /**
   * Attempt to reconnect the WebSocket
   */
  private attemptReconnect(): void {
    if (this.reconnecting || !this.sessionId) {
      return;
    }
    
    // Reset reconnect attempts if we've reached the max
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.log('Resetting reconnect attempts after reaching max');
      this.reconnectAttempts = 0;
    }
    
    this.reconnecting = true;
    this.reconnectAttempts++;
    
    // Exponential backoff with maximum cap
    const delay = Math.min(
      1000 * Math.pow(1.5, this.reconnectAttempts - 1), 
      this.maxReconnectDelay
    );
    
    console.debug(`Attempting WebSocket reconnect ${this.reconnectAttempts}/${this.maxReconnectAttempts} in ${delay}ms`);
    
    this.reconnectTimeout = setTimeout(() => {
      if (this.sessionId) {
        console.debug(`Reconnecting WebSocket to session ${this.sessionId}...`);
        this.connect(this.sessionId)
          .then(() => {
            console.debug('WebSocket reconnected successfully');
            this.reconnecting = false;
            // Reset reconnect attempts on success
            this.reconnectAttempts = 0;
            
            // Send a ping to verify connection is working
            this.sendMessage('PING', { timestamp: Date.now(), reconnected: true })
              .catch(err => console.warn('Failed to send ping after reconnection', err));
          })
          .catch(error => {
            console.error('WebSocket reconnection failed:', error);
            this.reconnecting = false;
            // Try again with backoff if we haven't reached max attempts
            if (this.reconnectAttempts < this.maxReconnectAttempts) {
              this.attemptReconnect();
            } else {
              console.error('Maximum reconnect attempts reached');
              // Reset for future attempts
              this.reconnectAttempts = 0;
            }
          });
      } else {
        this.reconnecting = false;
      }
    }, delay);
  }

  /**
   * Setup keep-alive mechanism
   */
  private setupKeepAlive(): void {
    this.clearKeepAlive();
    
    // Send a ping message every 20 seconds to keep the connection alive
    this.keepAliveInterval = setInterval(() => {
      if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        this.sendMessage('PING', { timestamp: Date.now() })
          .catch(err => console.warn('Failed to send ping message', err));
      }
      
      // Check if we need to reconnect due to inactivity
      if (this.status === ConnectionStatus.CONNECTED && !this.isConnectionHealthy()) {
        console.warn('WebSocket connection appears unhealthy, attempting reconnection');
        this.disconnect();
        if (this.sessionId) {
          this.attemptReconnect();
        }
      }
    }, 20000);
  }

  /**
   * Clear keep-alive interval
   */
  private clearKeepAlive(): void {
    if (this.keepAliveInterval) {
      clearInterval(this.keepAliveInterval);
      this.keepAliveInterval = null;
    }
  }

  /**
   * Start monitoring the connection health
   */
  private startConnectionMonitoring() {
    if (this.connectionMonitorActive) {
      return; // Already monitoring
    }
    
    this.connectionMonitorActive = true;
    console.debug('Starting WebSocket connection monitoring');
    
    // Clear any existing intervals
    this.clearConnectionMonitoring();
    
    // Start health check interval
    this.healthCheckInterval = setInterval(() => {
      if (this.status === ConnectionStatus.CONNECTED) {
        const now = Date.now();
        const timeSinceLastMessage = now - this.lastMessageTime;
        
        if (timeSinceLastMessage > this.healthCheckTimeoutMs) {
          console.warn(`No WebSocket activity for ${Math.round(timeSinceLastMessage / 1000)}s, connection may be stale`);
          
          // Test connection with a ping
          this.sendMessage('PING', { timestamp: Date.now() })
            .catch(() => {
              console.error('Connection test failed, reconnecting WebSocket');
              this.disconnect();
              if (this.sessionId) {
                this.attemptReconnect();
              }
            });
        }
      }
    }, 15000); // Check every 15 seconds
  }

  /**
   * Stop monitoring the connection health
   */
  private clearConnectionMonitoring() {
    if (this.healthCheckInterval) {
      clearInterval(this.healthCheckInterval);
      this.healthCheckInterval = null;
    }
    
    this.connectionMonitorActive = false;
  }
}

// Create and export a singleton instance
const wsService = new WebSocketService();
export default wsService; 