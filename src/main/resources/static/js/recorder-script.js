/**
 * CSTestForge Event Recorder Script
 * Custom WebSocket implementation with no third-party dependencies
 */

(function() {
    'use strict';
    
    // Configuration variables injected by server
    const sessionId = "__SESSION_ID__";
    const wsUrl = "__WS_URL__";
    
    // Private state
    let websocket = null;
    let isConnecting = false;
    let reconnectAttempts = 0;
    let events = [];
    let isRecording = true;
    let isPaused = false;
    let lastEvent = null;
    let recordingStartTime = Date.now();
    
    console.debug('CSTestForge: Recorder script loading...');
    
    // Set a flag to prevent multiple initializations on the same page
    if (window.__csRecorderActive) {
        console.debug('CSTestForge: Recorder already active on this page');
        return;
    }
    window.__csRecorderActive = true;
    
    // Create a visual recording indicator
    function createRecordingIndicator() {
        if (document.getElementById('cs-recorder-toolbar')) {
            return; // Already exists
        }
        
        // Create the toolbar container
        const toolbar = document.createElement('div');
        toolbar.id = 'cs-recorder-toolbar';
        toolbar.style.cssText = `
            position: fixed;
            top: 10px;
            left: 50%;
            transform: translateX(-50%);
            background-color: #333;
            color: white;
            border-radius: 4px;
            padding: 8px 12px;
            font-family: Arial, sans-serif;
            font-size: 14px;
            z-index: 999999;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
            display: flex;
            align-items: center;
            gap: 12px;
        `;
        
        // Create the status indicator dot
        const indicator = document.createElement('div');
        indicator.className = 'cs-status-indicator';
        indicator.style.cssText = `
            background-color: rgba(255, 165, 0, 0.7);
            border-radius: 50%;
            width: 12px;
            height: 12px;
            display: inline-block;
        `;
        
        // Create status text
        const statusText = document.createElement('span');
        statusText.className = 'cs-status-text';
        statusText.textContent = 'Initializing...';
        
        // Create buttons container
        const buttonsContainer = document.createElement('div');
        buttonsContainer.className = 'cs-buttons-container';
        buttonsContainer.style.cssText = `
            display: flex;
            gap: 8px;
        `;
        
        // Create event counter
        const eventsCounter = document.createElement('span');
        eventsCounter.className = 'cs-events-counter';
        eventsCounter.textContent = '0 events';
        eventsCounter.style.cssText = `
            margin-right: 10px;
            font-size: 12px;
            color: #aaa;
        `;
        
        // Helper function to create buttons
        function createButton(text, clickHandler) {
            const button = document.createElement('button');
            button.textContent = text;
            button.style.cssText = `
                background-color: #444;
                border: none;
                color: white;
                padding: 4px 8px;
                border-radius: 3px;
                cursor: pointer;
                font-size: 12px;
                min-width: 60px;
                transition: background-color 0.2s;
            `;
            button.addEventListener('mouseenter', () => {
                button.style.backgroundColor = '#555';
            });
            button.addEventListener('mouseleave', () => {
                button.style.backgroundColor = '#444';
            });
            button.addEventListener('click', clickHandler);
            return button;
        }
        
        // Create control buttons
        const pauseButton = createButton(isPaused ? 'Resume' : 'Pause', togglePause);
        pauseButton.className = 'cs-pause-button';
        
        const stopButton = createButton('Stop', stopRecording);
        stopButton.className = 'cs-stop-button';
        
        const clearButton = createButton('Clear', clearEvents);
        clearButton.className = 'cs-clear-button';
        
        // Add buttons to container
        buttonsContainer.appendChild(eventsCounter);
        buttonsContainer.appendChild(pauseButton);
        buttonsContainer.appendChild(stopButton);
        buttonsContainer.appendChild(clearButton);
        
        // Add elements to toolbar
        toolbar.appendChild(indicator);
        toolbar.appendChild(statusText);
        toolbar.appendChild(buttonsContainer);
        
        // Add toolbar to page
        document.body.appendChild(toolbar);
        
        console.debug('CSTestForge: Recorder toolbar created');
    }
    
    // Update connection status in UI
    function updateConnectionStatus(status) {
        const statusElem = document.querySelector('.cs-status-text');
        const indicatorElem = document.querySelector('.cs-status-indicator');
        
        if (!statusElem || !indicatorElem) {
            return;
        }
        
        if (status === 'connected') {
            statusElem.textContent = isRecording ? (isPaused ? 'Paused' : 'Recording') : 'Connected';
            indicatorElem.style.backgroundColor = isRecording ? 
                (isPaused ? 'rgba(255, 165, 0, 0.7)' : 'rgba(0, 255, 0, 0.7)') : 
                'rgba(0, 170, 255, 0.7)';
        } else if (status === 'connecting') {
            statusElem.textContent = 'Connecting...';
            indicatorElem.style.backgroundColor = 'rgba(255, 165, 0, 0.7)';
        } else {
            statusElem.textContent = 'Disconnected';
            indicatorElem.style.backgroundColor = 'rgba(255, 0, 0, 0.7)';
        }
    }
    
    // Update event counter
    function updateEventsCounter() {
        const counterElem = document.querySelector('.cs-events-counter');
        if (counterElem) {
            counterElem.textContent = `${events.length} event${events.length !== 1 ? 's' : ''}`;
        }
    }
    
    // Toggle pause/resume recording
    function togglePause() {
        isPaused = !isPaused;
        
        const pauseButton = document.querySelector('.cs-pause-button');
        if (pauseButton) {
            pauseButton.textContent = isPaused ? 'Resume' : 'Pause';
        }
        
        updateConnectionStatus('connected');
        
        // Send pause/resume status to server
        sendEvent({
            type: isPaused ? 'PAUSE' : 'RESUME',
            timestamp: Date.now()
        });
        
        console.debug(`CSTestForge: Recording ${isPaused ? 'paused' : 'resumed'}`);
    }
    
    // Stop recording
    function stopRecording() {
        if (!isRecording) {
            return;
        }
        
        isRecording = false;
        updateConnectionStatus('connected');
        
        // Show stopping indicator in the UI
        const statusElem = document.querySelector('.cs-status-text');
        const stopButton = document.querySelector('.cs-stop-button');
        if (statusElem) {
            statusElem.textContent = 'Stopping...';
        }
        if (stopButton) {
            stopButton.disabled = true;
            stopButton.textContent = 'Stopping...';
            stopButton.style.backgroundColor = '#666';
        }
        
        // Send stop event through WebSocket
        sendEvent({
            type: 'STOP',
            timestamp: Date.now()
        });
        
        // Also send HTTP request to ensure the browser is closed
        try {
            // Extract server origin from wsUrl
            const wsUrlParts = wsUrl.split('/');
            const serverHost = wsUrlParts[0];
            // Use HTTP protocol, not the target page's protocol
            const apiUrl = `http://${serverHost}/cstestforge/api/recorder/stop`;
            
            // Make an HTTP POST request to the stop endpoint
            const xhr = new XMLHttpRequest();
            xhr.open('POST', apiUrl, true);
            xhr.setRequestHeader('Content-Type', 'application/json');
            xhr.onreadystatechange = function() {
                if (xhr.readyState === 4) {
                    if (xhr.status === 200) {
                        console.debug('CSTestForge: Successfully stopped recording session on server');
                    } else {
                        console.error('CSTestForge: Failed to stop recording session on server, status:', xhr.status, 'URL:', apiUrl);
                    }
                }
            };
            xhr.send(JSON.stringify({
                sessionId: sessionId
            }));
            console.debug('CSTestForge: Stop request sent to:', apiUrl);
        } catch (error) {
            console.error('CSTestForge: Error sending stop request:', error);
        }
        
        console.debug('CSTestForge: Recording stopped and browser closing request sent');
    }
    
    // Clear recorded events
    function clearEvents() {
        events = [];
        updateEventsCounter();
        
        // Send clear event to server
        sendEvent({
            type: 'CLEAR',
            timestamp: Date.now()
        });
        
        console.debug('CSTestForge: Events cleared');
    }
    
    // Connect to WebSocket server using pure native WebSockets
    function connectWebSocket() {
        if (isConnecting) {
            return;
        }
        
        isConnecting = true;
        updateConnectionStatus('connecting');
        
        try {
            // Log connection information for debugging
            console.debug('CSTestForge: Connecting to WebSocket, base URL:', wsUrl);
            console.debug('CSTestForge: Session ID is', sessionId);
            console.debug('CSTestForge: Current page protocol is', window.location.protocol);
            
            // Clean up any existing connection
            if (websocket) {
                try {
                    websocket.close();
                } catch (e) {
                    // Ignore errors when closing
                }
                websocket = null;
            }
            
            // The wsUrl from server has format hostname:port/path (no protocol)
            // We need to add the appropriate protocol (ws:// or wss://)
            let finalWsUrl;
            
            // Choose protocol based on reconnectAttempts
            // On first try with HTTPS pages, try wss://
            // On subsequent attempts or for HTTP pages, use ws://
            if (window.location.protocol === 'https:' && reconnectAttempts === 0) {
                finalWsUrl = `wss://${wsUrl}`;
                console.debug('CSTestForge: Trying secure WebSocket URL first:', finalWsUrl);
            } else {
                finalWsUrl = `ws://${wsUrl}`;
                
                if (window.location.protocol === 'https:') {
                    console.warn('CSTestForge: Using fallback insecure WebSocket on secure page - mixed content warnings may appear');
                }
                
                console.debug('CSTestForge: Using WebSocket URL:', finalWsUrl);
            }
            
            // Create native WebSocket connection - no third-party dependencies
            console.debug('CSTestForge: Creating WebSocket connection to:', finalWsUrl);
            websocket = new WebSocket(finalWsUrl);
            
            websocket.onopen = function() {
                console.debug('CSTestForge: WebSocket connected successfully');
                updateConnectionStatus('connected');
                reconnectAttempts = 0;
                isConnecting = false;
                
                // Send a session start message
                sendEvent({
                    type: 'SESSION_START',
                    url: window.location.href,
                    title: document.title,
                    userAgent: navigator.userAgent,
                    viewport: {
                        width: window.innerWidth,
                        height: window.innerHeight
                    },
                    timestamp: Date.now()
                });
            };
            
            websocket.onmessage = function(event) {
                try {
                    const message = JSON.parse(event.data);
                    console.debug('CSTestForge: Received message', message);
                    
                    handleServerMessage(message);
                } catch (error) {
                    console.error('CSTestForge: Error parsing WebSocket message:', error);
                }
            };
            
            websocket.onclose = function(event) {
                console.debug('CSTestForge: WebSocket disconnected, code:', event.code, 'reason:', event.reason);
                updateConnectionStatus('disconnected');
                isConnecting = false;
                
                // Attempt to reconnect
                if (reconnectAttempts < 5) {
                    reconnectAttempts++;
                    setTimeout(connectWebSocket, 3000);
                }
            };
            
            websocket.onerror = function(error) {
                console.error('CSTestForge: WebSocket error:', error);
                updateConnectionStatus('disconnected');
                isConnecting = false;
                
                // If this is the first attempt with wss:// and it failed,
                // try again with ws:// immediately
                if (window.location.protocol === 'https:' && finalWsUrl.startsWith('wss:') && reconnectAttempts === 0) {
                    console.debug('CSTestForge: WSS connection failed, trying WS fallback immediately');
                    reconnectAttempts++;
                    setTimeout(connectWebSocket, 100);
                }
                // Otherwise use standard reconnect logic with backoff
                else if (reconnectAttempts < 5) {
                    reconnectAttempts++;
                    const backoffTime = reconnectAttempts * 2000; // Increasing backoff
                    console.debug(`CSTestForge: Will attempt reconnection in ${backoffTime/1000} seconds (attempt ${reconnectAttempts})`);
                    setTimeout(connectWebSocket, backoffTime);
                } else {
                    console.error('CSTestForge: Maximum reconnection attempts reached, giving up');
                    
                    // Update UI to show connection failure
                    const statusElem = document.querySelector('.cs-status-text');
                    if (statusElem) {
                        statusElem.textContent = 'Connection Failed';
                    }
                }
            };
        } catch (error) {
            console.error('CSTestForge: Failed to connect to WebSocket:', error);
            updateConnectionStatus('disconnected');
            isConnecting = false;
            
            // Try again later
            if (reconnectAttempts < 5) {
                reconnectAttempts++;
                setTimeout(connectWebSocket, 3000);
            }
        }
    }
    
    // Handle messages from the server
    function handleServerMessage(message) {
        const { type, payload } = message;
        
        switch (type) {
            case 'PAUSE':
                isPaused = true;
                updateConnectionStatus('connected');
                break;
                
            case 'RESUME':
                isPaused = false;
                updateConnectionStatus('connected');
                break;
                
            case 'STOP':
                isRecording = false;
                updateConnectionStatus('connected');
                break;
                
            case 'CLEAR':
                events = [];
                updateEventsCounter();
                break;
                
            case 'PING':
                // Send pong response
                sendEvent({
                    type: 'PONG',
                    timestamp: Date.now()
                });
                break;
                
            default:
                console.debug('CSTestForge: Unknown message type:', type);
        }
    }
    
    // Send event to the server
    function sendEvent(eventData) {
        if (!websocket || websocket.readyState !== WebSocket.OPEN) {
            console.debug('CSTestForge: Cannot send event - WebSocket not connected');
            return false;
        }
        
        try {
            const message = {
                type: 'EVENT',
                sessionId: sessionId,
                timestamp: Date.now(),
                payload: eventData
            };
            
            websocket.send(JSON.stringify(message));
            return true;
        } catch (error) {
            console.error('CSTestForge: Error sending event:', error);
            return false;
        }
    }
    
    // Add an event to the local queue and send it
    function recordEvent(eventType, eventData) {
        if (!isRecording || isPaused) {
            return;
        }
        
        const event = {
            type: eventType,
            timestamp: Date.now(),
            url: window.location.href,
            ...eventData
        };
        
        // Store event locally
        events.push(event);
        lastEvent = event;
        updateEventsCounter();
        
        // Try to send via WebSocket first
        const sent = sendEvent(event);
        
        // If WebSocket sending failed, try REST API as fallback
        if (!sent) {
            try {
                // Extract server origin from wsUrl (always use http/https for REST calls)
                const wsUrlParts = wsUrl.split('/');
                const serverHost = wsUrlParts[0];
                // Use HTTP protocol, not the target page's protocol
                const apiUrl = `http://${serverHost}/cstestforge/api/recorder/events/${sessionId}`;
                
                const xhr = new XMLHttpRequest();
                xhr.open('POST', apiUrl, true);
                xhr.setRequestHeader('Content-Type', 'application/json');
                xhr.send(JSON.stringify({
                    type: event.type,
                    timestamp: event.timestamp,
                    sessionId: sessionId,
                    url: event.url,
                    data: event
                }));
                console.debug('CSTestForge: Event sent via REST API fallback to:', apiUrl);
            } catch (error) {
                console.error('CSTestForge: Failed to send event via REST API:', error);
            }
        }
        
        console.debug('CSTestForge: Recorded event', event);
    }
    
    // Capture mouse events
    function captureMouseEvents() {
        document.addEventListener('click', function(e) {
            if (!isRecording || isPaused) return;
            
            let target = e.target;
            let selector = buildSelector(target);
            
            recordEvent('CLICK', {
                selector: selector,
                x: e.clientX,
                y: e.clientY,
                element: {
                    tagName: target.tagName,
                    id: target.id,
                    className: target.className,
                    type: target.type,
                    value: target.value,
                    href: target.href,
                    text: target.textContent ? target.textContent.substring(0, 50) : ''
                }
            });
        }, true);
        
        document.addEventListener('dblclick', function(e) {
            if (!isRecording || isPaused) return;
            
            let target = e.target;
            let selector = buildSelector(target);
            
            recordEvent('DBLCLICK', {
                selector: selector,
                x: e.clientX,
                y: e.clientY,
                element: {
                    tagName: target.tagName,
                    id: target.id,
                    className: target.className
                }
            });
        }, true);
    }
    
    // Capture keyboard events
    function captureKeyboardEvents() {
        document.addEventListener('keydown', function(e) {
            if (!isRecording || isPaused) return;
            
            // Only record certain key events
            if (e.target && (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA')) {
                let target = e.target;
                let selector = buildSelector(target);
                
                // Don't record sensitive input values
                const isSensitive = target.type === 'password' ||
                    target.getAttribute('autocomplete') === 'cc-number' || 
                    target.getAttribute('autocomplete') === 'cc-csc';
                
                if (e.key === 'Enter') {
                    recordEvent('SUBMIT', {
                        selector: selector,
                        element: {
                            tagName: target.tagName,
                            id: target.id,
                            className: target.className,
                            type: target.type
                        }
                    });
                }
                
                // Skip individual key logging for sensitive fields
                if (isSensitive) {
                    return;
                }
                
                // Log special keys or key combinations
                if (e.ctrlKey || e.altKey || e.metaKey || 
                    ['Tab', 'Enter', 'Escape', 'ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].includes(e.key)) {
                    
                    recordEvent('KEY', {
                        key: e.key,
                        ctrl: e.ctrlKey,
                        alt: e.altKey,
                        shift: e.shiftKey,
                        meta: e.metaKey,
                        selector: selector
                    });
                }
            }
        }, true);
    }
    
    // Capture form events
    function captureFormEvents() {
        // Input value changes
        document.addEventListener('change', function(e) {
            if (!isRecording || isPaused) return;
            
            let target = e.target;
            
            if (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.tagName === 'SELECT')) {
                let selector = buildSelector(target);
                let value = target.value;
                
                // Mask sensitive data
                if (target.type === 'password') {
                    value = '*'.repeat(value.length);
                }
                
                // Skip recording values for credit card fields
                if (target.getAttribute('autocomplete') === 'cc-number' || 
                    target.getAttribute('autocomplete') === 'cc-csc') {
                    value = '[REDACTED]';
                }
                
                recordEvent('INPUT', {
                    selector: selector,
                    value: value,
                    element: {
                        tagName: target.tagName,
                        id: target.id,
                        className: target.className,
                        type: target.type,
                        name: target.name
                    }
                });
            }
        }, true);
        
        // Form submissions
        document.addEventListener('submit', function(e) {
            if (!isRecording || isPaused) return;
            
            let form = e.target;
            let selector = buildSelector(form);
            
            recordEvent('FORM_SUBMIT', {
                selector: selector,
                action: form.action,
                method: form.method,
                element: {
                    id: form.id,
                    className: form.className,
                    name: form.name
                }
            });
        }, true);
    }
    
    // Capture navigation events
    function captureNavigationEvents() {
        // History navigation
        window.addEventListener('popstate', function() {
            if (!isRecording || isPaused) return;
            
            recordEvent('NAVIGATION', {
                type: 'popstate',
                url: window.location.href,
                title: document.title
            });
        });
        
        // Monitor URL changes
        let lastUrl = window.location.href;
        
        // Check for URL changes periodically
        setInterval(function() {
            if (!isRecording || isPaused) return;
            
            const currentUrl = window.location.href;
            if (currentUrl !== lastUrl) {
                recordEvent('NAVIGATION', {
                    type: 'urlchange',
                    from: lastUrl,
                    to: currentUrl,
                    title: document.title
                });
                
                lastUrl = currentUrl;
            }
        }, 1000);
    }
    
    // Build a CSS selector to uniquely identify an element
    function buildSelector(element) {
        if (!element) return '';
        if (element === document || element === document.documentElement || element === document.body) {
            return element.nodeName.toLowerCase();
        }
        
        // Use ID if available
        if (element.id) {
            return '#' + element.id;
        }
        
        // Try data-testid or similar attributes
        const testIdAttr = element.getAttribute('data-testid') || 
                          element.getAttribute('data-test-id') || 
                          element.getAttribute('data-cy');
        if (testIdAttr) {
            return `[data-testid="${testIdAttr}"]`;
        }
        
        // Use classes if available
        if (element.className && typeof element.className === 'string' && element.className.trim()) {
            const classes = element.className.trim().split(/\s+/);
            if (classes.length > 0) {
                return element.nodeName.toLowerCase() + '.' + classes.join('.');
            }
        }
        
        // Use position relative to parent
        const parent = element.parentNode;
        if (parent && parent !== document && parent !== document.documentElement) {
            const parentSelector = buildSelector(parent);
            const siblings = Array.from(parent.children);
            const index = siblings.indexOf(element);
            
            return `${parentSelector} > ${element.nodeName.toLowerCase()}:nth-child(${index + 1})`;
        }
        
        // Fallback to just the node name
        return element.nodeName.toLowerCase();
    }
    
    // Initialize
    function initialize() {
        console.debug('CSTestForge: Initializing recorder...');
        
        // Create recording indicator
        createRecordingIndicator();
        
        // Connect to WebSocket server
        connectWebSocket();
        
        // Set up event listeners
        captureMouseEvents();
        captureKeyboardEvents();
        captureFormEvents();
        captureNavigationEvents();
        
        // Send navigation event
        recordEvent('PAGE_LOAD', {
            url: window.location.href,
            title: document.title,
            referrer: document.referrer,
            userAgent: navigator.userAgent,
            viewport: {
                width: window.innerWidth,
                height: window.innerHeight
            }
        });
        
        // Log important variables
        console.debug('CSTestForge: Initialization complete');
    }
    
    // Initialize on load
    if (document.readyState === 'complete') {
        initialize();
    } else {
        window.addEventListener('load', initialize);
    }
})(); 