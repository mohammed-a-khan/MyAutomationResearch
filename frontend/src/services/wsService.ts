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

  /**
   * Initialize a WebSocket connection
   * @param sessionId Recording session ID
   * @returns Promise resolving when connection is established
   */
  public connect(sessionId: string): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        this.disconnect();
      }

      this.sessionId = sessionId;
      this.setStatus(ConnectionStatus.CONNECTING);

      // Create the WebSocket URL
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const host = window.location.host;
      
      // Use the application's context path - must match server.servlet.context-path in application.properties
      const contextPath = '/cstestforge';
      
      // Create the WebSocket URL with the context path prefix - use native WebSocket, not SockJS
      const wsUrl = `${protocol}//${host}${contextPath}/ws-recorder`;
      
      console.debug('Connecting to WebSocket server', {
        url: wsUrl,
        sessionId: sessionId,
        protocol: protocol,
        host: host,
        browserUrl: window.location.href
      });

      try {
        // Create native WebSocket connection - no third-party dependencies
        this.socket = new WebSocket(wsUrl);
        
        this.socket.onopen = () => {
          this.setStatus(ConnectionStatus.CONNECTED);
          this.reconnectAttempts = 0;
          this.setupKeepAlive();
          
          console.debug('WebSocket connected successfully', {
            sessionId: this.sessionId,
            url: wsUrl,
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
            const message: WsMessage = JSON.parse(event.data);
            this.handleMessage(message);
          } catch (error) {
            logError(error, 'WebSocket Message Parse');
          }
        };
        
        this.socket.onclose = (event: CloseEvent) => {
          this.setStatus(ConnectionStatus.DISCONNECTED);
          this.clearKeepAlive();
          
          console.debug('WebSocket connection closed', {
            code: event.code,
            reason: event.reason,
            wasClean: event.wasClean,
            sessionId: this.sessionId
          });
          
          if (event.code !== 1000) { // 1000 is normal closure
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
          reject(error);
        };
      } catch (error) {
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
    if (this.socket) {
      try {
        // SockJS and native WebSocket have the same close method
        this.socket.close(1000, 'Client initiated disconnect');
      } catch (error) {
        console.error('Error disconnecting WebSocket:', error);
      } finally {
        this.socket = null;
        this.setStatus(ConnectionStatus.DISCONNECTED);
        this.clearKeepAlive();
      }
    }

    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
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
   * Handle incoming WebSocket messages
   * @param message Received message
   */
  private handleMessage(message: WsMessage): void {
    const { type, payload } = message;
    
    const listeners = this.messageListeners.get(type);
    if (listeners) {
      listeners.forEach(listener => {
        try {
          listener(payload);
        } catch (error) {
          logError(error, `WebSocket Listener for ${type}`);
        }
      });
    }
  }

  /**
   * Set and notify connection status change
   * @param newStatus New connection status
   */
  private setStatus(newStatus: ConnectionStatus): void {
    this.status = newStatus;
    this.statusListeners.forEach(listener => {
      try {
        listener(newStatus);
      } catch (error) {
        logError(error, 'WebSocket Status Listener');
      }
    });
  }

  /**
   * Attempt to reconnect after connection failure
   */
  private attemptReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      logError('Maximum reconnection attempts reached', 'WebSocket');
      return;
    }

    this.reconnectAttempts++;
    const delay = Math.pow(2, this.reconnectAttempts) * 1000; // Exponential backoff
    
    this.reconnectTimeout = setTimeout(() => {
      if (this.sessionId) {
        this.connect(this.sessionId).catch(() => {
          // Error is already logged in connect method
        });
      }
    }, delay);
  }

  /**
   * Set up keep-alive ping to prevent connection timeout
   */
  private setupKeepAlive(): void {
    this.clearKeepAlive();
    
    this.keepAliveInterval = setInterval(() => {
      if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        this.sendMessage('ping', { timestamp: Date.now() }).catch(() => {
          // Error is already logged in sendMessage method
        });
      }
    }, 30000); // 30 seconds ping
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
}

// Export singleton instance
const wsService = new WebSocketService();
export default wsService; 