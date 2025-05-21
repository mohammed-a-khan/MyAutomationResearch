/**
 * CSTestForge Event Recorder Script - Enhanced Version
 * Cross-browser compatibility with CSP-friendly implementation
 * and robust error handling
 */

'use strict';

(function() {
    // Immediately check if we're already running to avoid duplicates
    if (window.__csRecorderActive === true) {
        console.debug('CSTestForge: Recorder already active, skipping');
        return;
    }

    // Mark that we're active to prevent duplicate initialization
    window.__csRecorderActive = true;

    // Save session ID for future reference
    const sessionId = "__SESSION_ID__";

    // Store basic information in the global scope
    window.__csRecorder = {
        version: '1.3',
        sessionId: sessionId,
        status: 'recording',
        startTime: new Date().toISOString(),
        location: window.location.href,
        errors: [],
        reconnectAttempts: 0,
        maxReconnectAttempts: 5,
        connectionStatus: 'connecting'
    };

    console.debug('CSTestForge: Starting recorder on', window.location.href);

    // Create a UI indicator that doesn't rely on external resources
    function createUI() {
        try {
            // Remove any existing indicator first
            const existingIndicator = document.getElementById('cs-recorder-indicator');
            if (existingIndicator) {
                existingIndicator.remove();
            }

            // Create container element
            const ui = document.createElement('div');
            ui.id = 'cs-recorder-indicator';
            ui.style.cssText = `
                position: fixed;
                top: 10px;
                left: 50%;
                transform: translateX(-50%);
                background-color: #333;
                color: white;
                border-radius: 4px;
                padding: 8px 12px;
                font-family: Arial, sans-serif;
                z-index: 2147483647;
                box-shadow: 0 2px 10px rgba(0,0,0,0.3);
                display: flex;
                align-items: center;
                gap: 8px;
                font-size: 14px;
                user-select: none;
                transition: background-color 0.3s ease;
            `;

            // Create recording indicator
            const dot = document.createElement('div');
            dot.id = 'cs-recorder-dot';
            dot.style.cssText = `
                width: 10px;
                height: 10px;
                background-color: red;
                border-radius: 50%;
                transition: opacity 0.4s ease;
            `;

            // Create text label
            const text = document.createElement('span');
            text.id = 'cs-recorder-text';
            text.textContent = 'CSTestForge Recording';

            // Create connection status indicator
            const statusIndicator = document.createElement('span');
            statusIndicator.id = 'cs-recorder-status';
            statusIndicator.style.cssText = `
                display: inline-block;
                font-size: 10px;
                margin-left: 5px;
                padding: 2px 5px;
                border-radius: 3px;
                background-color: #666;
            `;
            statusIndicator.textContent = 'connecting...';

            // Assemble UI
            ui.appendChild(dot);
            ui.appendChild(text);
            ui.appendChild(statusIndicator);

            // Add blinking animation
            let blinking = true;
            const blinkInterval = setInterval(function() {
                if (!blinking) return;
                dot.style.opacity = dot.style.opacity === '0.4' ? '1' : '0.4';
            }, 800);

            // Store reference to clear interval when needed
            window.__csRecorder.blinkInterval = blinkInterval;

            // Add toolbar buttons
            const buttonsContainer = document.createElement('div');
            buttonsContainer.style.cssText = `
                display: flex;
                gap: 5px;
                margin-left: 10px;
            `;

            // Pause/Resume button
            const pauseButton = document.createElement('button');
            pauseButton.textContent = '⏸️';
            pauseButton.title = 'Pause Recording';
            pauseButton.style.cssText = `
                background: none;
                border: none;
                color: white;
                cursor: pointer;
                font-size: 14px;
                padding: 0 5px;
            `;
            pauseButton.onclick = function(e) {
                e.stopPropagation();
                if (window.__csRecorder.status === 'recording') {
                    pauseRecording();
                    pauseButton.textContent = '▶️';
                    pauseButton.title = 'Resume Recording';
                    ui.style.backgroundColor = '#666';
                    blinking = false;
                    dot.style.opacity = '0.4';
                } else {
                    resumeRecording();
                    pauseButton.textContent = '⏸️';
                    pauseButton.title = 'Pause Recording';
                    ui.style.backgroundColor = '#333';
                    blinking = true;
                }
            };

            // Stop button
            const stopButton = document.createElement('button');
            stopButton.textContent = '⏹️';
            stopButton.title = 'Stop Recording';
            stopButton.style.cssText = `
                background: none;
                border: none;
                color: white;
                cursor: pointer;
                font-size: 14px;
                padding: 0 5px;
            `;
            stopButton.onclick = function(e) {
                e.stopPropagation();
                stopRecording();
            };

            buttonsContainer.appendChild(pauseButton);
            buttonsContainer.appendChild(stopButton);
            ui.appendChild(buttonsContainer);

            // Append to document
            if (document.body) {
                document.body.appendChild(ui);
                console.debug('CSTestForge: Indicator created');
            } else {
                console.debug('CSTestForge: Body not available, will add indicator when ready');

                // Wait for document body
                function checkBodyAndAdd() {
                    if (document.body) {
                        document.body.appendChild(ui);
                        console.debug('CSTestForge: Indicator added to body');
                    } else {
                        setTimeout(checkBodyAndAdd, 100);
                    }
                }

                setTimeout(checkBodyAndAdd, 100);
            }

            return true;
        } catch (e) {
            console.error('CSTestForge: Failed to create indicator:', e);
            logError('Failed to create UI indicator', e);
            return false;
        }
    }

    // Helper function to update the connection status indicator
    function updateConnectionStatus(status) {
        window.__csRecorder.connectionStatus = status;

        const statusIndicator = document.getElementById('cs-recorder-status');
        if (!statusIndicator) return;

        switch(status) {
            case 'connected':
                statusIndicator.textContent = 'connected';
                statusIndicator.style.backgroundColor = '#2a2';
                break;
            case 'disconnected':
                statusIndicator.textContent = 'disconnected';
                statusIndicator.style.backgroundColor = '#a22';
                break;
            case 'connecting':
                statusIndicator.textContent = 'connecting...';
                statusIndicator.style.backgroundColor = '#666';
                break;
            case 'http-fallback':
                statusIndicator.textContent = 'using http';
                statusIndicator.style.backgroundColor = '#a82';
                break;
            default:
                statusIndicator.textContent = status;
                statusIndicator.style.backgroundColor = '#666';
        }
    }

    // Log errors to the console and store them
    function logError(message, error) {
        console.error('CSTestForge Error:', message, error);
        window.__csRecorder.errors.push({
            message: message,
            error: error ? error.toString() : null,
            time: new Date().toISOString()
        });

        // Try to send error to server
        try {
            const errorEvent = {
                type: 'ERROR',
                url: window.location.href,
                timestamp: new Date().toISOString(),
                message: message,
                error: error ? error.toString() : null,
                stack: error && error.stack ? error.stack : null
            };

            // Use HTTP fallback for error reporting to ensure delivery
            sendEventViaHttp(errorEvent);
        } catch (e) {
            console.error('CSTestForge: Failed to report error to server:', e);
        }
    }

    // Connect to WebSocket
    let websocket = null;
    let wsUrl = "__WS_URL__";

    function connectWebSocket() {
        try {
            if (window.__csRecorder.reconnectAttempts >= window.__csRecorder.maxReconnectAttempts) {
                console.debug('CSTestForge: Max reconnect attempts reached, falling back to HTTP mode');
                window.__csRecorderUsingHttpFallback = true;
                updateConnectionStatus('http-fallback');
                return;
            }

            // If we already have an open connection, don't create another
            if (websocket && websocket.readyState === WebSocket.OPEN) {
                console.debug('CSTestForge: WebSocket already connected');
                return;
            }

            // Close existing connection if any
            if (websocket) {
                try {
                    websocket.close();
                } catch (e) {
                    // Ignore close errors
                }
            }

            // Determine if we should use secure WebSocket based on the page protocol
            let finalWsUrl = wsUrl;
            if (!finalWsUrl.startsWith('ws:') && !finalWsUrl.startsWith('wss:')) {
                const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
                finalWsUrl = protocol + '//' + wsUrl;
            }

            console.debug('CSTestForge: Connecting to WebSocket at ' + finalWsUrl);

            try {
                websocket = new WebSocket(finalWsUrl);
            } catch (e) {
                console.error('CSTestForge: Error creating WebSocket:', e);
                websocket = null;
                tryHttpFallback();
                return;
            }

            // Add connection timeout handling
            let wsConnectionTimeout = setTimeout(function() {
                if (websocket && websocket.readyState !== WebSocket.OPEN) {
                    console.error('CSTestForge: WebSocket connection timeout');
                    logError('WebSocket connection timeout', null);

                    // Try reconnecting
                    window.__csRecorder.reconnectAttempts++;

                    // If we're using secure protocol but timeout, try falling back to non-secure
                    if (window.location.protocol === 'https:' && finalWsUrl.startsWith('wss:')) {
                        // Try with ws:// instead
                        finalWsUrl = finalWsUrl.replace('wss:', 'ws:');
                        console.debug('CSTestForge: Trying fallback to WS protocol');

                        setTimeout(connectWebSocket, 1000);
                    } else {
                        // Fall back to HTTP if we're already using non-secure or if this is a subsequent attempt
                        tryHttpFallback();
                    }
                }
            }, 5000);

            websocket.onopen = function(event) {
                console.debug('CSTestForge: WebSocket connection established');
                clearTimeout(wsConnectionTimeout);

                // Reset reconnect attempts on successful connection
                window.__csRecorder.reconnectAttempts = 0;

                // Send an init message
                const initEvent = {
                    type: 'INIT',
                    sessionId: sessionId,
                    url: window.location.href,
                    title: document.title,
                    timestamp: new Date().toISOString(),
                    userAgent: navigator.userAgent,
                    platform: navigator.platform,
                    viewport: {
                        width: window.innerWidth,
                        height: window.innerHeight
                    },
                    doctype: document.doctype ? document.doctype.name : null
                };

                sendEvent(initEvent);
                updateConnectionStatus('connected');

                // Start heartbeat to keep connection alive
                startHeartbeat();
            };

            websocket.onmessage = function(event) {
                try {
                    const message = JSON.parse(event.data);
                    console.debug('CSTestForge: Received message', message);

                    // Handle different message types
                    switch (message.type) {
                        case 'COMMAND':
                            handleCommand(message);
                            break;
                        case 'PING':
                            // Respond to ping with a pong
                            sendEvent({
                                type: 'PONG',
                                timestamp: new Date().toISOString()
                            });
                            break;
                    }
                } catch (e) {
                    console.error('CSTestForge: Error processing WebSocket message:', e);
                    logError('Error processing WebSocket message', e);
                }
            };

            websocket.onerror = function(error) {
                // Clear the connection timeout
                clearTimeout(wsConnectionTimeout);

                // Check if this error might be due to unsupported WebSocket in the browser
                console.error('CSTestForge: WebSocket error:', error);
                logError('WebSocket error', error);
                updateConnectionStatus('disconnected');

                // Try to reconnect with fallback strategy
                if (window.__csRecorder.reconnectAttempts < window.__csRecorder.maxReconnectAttempts) {
                    window.__csRecorder.reconnectAttempts++;

                    if (window.location.protocol === 'https:' && finalWsUrl.startsWith('wss:')) {
                        // Try non-secure first if we're on HTTPS
                        console.debug('CSTestForge: Trying non-secure WebSocket fallback');
                        setTimeout(function() {
                            connectWebSocket();
                        }, 2000);
                    } else {
                        // Otherwise fall back to HTTP immediately
                        tryHttpFallback();
                    }
                } else {
                    tryHttpFallback();
                }
            };

            websocket.onclose = function(event) {
                // Clear connection timeout
                clearTimeout(wsConnectionTimeout);

                console.debug('CSTestForge: WebSocket connection closed, code:', event.code, 'reason:', event.reason);
                updateConnectionStatus('disconnected');

                // Stop the heartbeat
                stopHeartbeat();

                if (!window.__csRecorderUsingHttpFallback) {
                    // Try to reconnect if not explicitly closed (code 1000)
                    if (event.code !== 1000) {
                        window.__csRecorder.reconnectAttempts++;
                        setTimeout(connectWebSocket, 2000);
                    }
                }
            };

        } catch (e) {
            console.error('CSTestForge: Failed to connect to WebSocket:', e);
            logError('Failed to connect to WebSocket', e);

            // Fall back to HTTP communication
            tryHttpFallback();
        }
    }

    // Add HTTP fallback mechanism
    function tryHttpFallback() {
        console.warn('CSTestForge: Using HTTP fallback for communication');
        window.__csRecorderUsingHttpFallback = true;
        updateConnectionStatus('http-fallback');

        // Send an initialization event via HTTP to let the server know we're using HTTP
        sendEventViaHttp({
            type: 'INIT',
            sessionId: sessionId,
            url: window.location.href,
            title: document.title,
            timestamp: new Date().toISOString(),
            userAgent: navigator.userAgent,
            platform: navigator.platform,
            viewport: {
                width: window.innerWidth,
                height: window.innerHeight
            },
            usingHttpFallback: true
        });
    }

    // Heartbeat to keep WebSocket connection alive
    let heartbeatInterval = null;

    function startHeartbeat() {
        // Send a heartbeat every 30 seconds to keep the connection alive
        heartbeatInterval = setInterval(function() {
            if (websocket && websocket.readyState === WebSocket.OPEN) {
                sendEvent({
                    type: 'HEARTBEAT',
                    timestamp: new Date().toISOString()
                });
            }
        }, 30000);
    }

    function stopHeartbeat() {
        if (heartbeatInterval) {
            clearInterval(heartbeatInterval);
            heartbeatInterval = null;
        }
    }

    // Handle commands from the server
    function handleCommand(command) {
        switch (command.action) {
            case 'PAUSE':
                pauseRecording();
                break;
            case 'RESUME':
                resumeRecording();
                break;
            case 'STOP':
                stopRecording();
                break;
            case 'CAPTURE_SCREENSHOT':
                captureScreenshot();
                break;
            case 'STATUS':
                // Send status information back to server
                sendEvent({
                    type: 'STATUS',
                    status: window.__csRecorder.status,
                    url: window.location.href,
                    title: document.title,
                    timestamp: new Date().toISOString(),
                    errors: window.__csRecorder.errors
                });
                break;
        }
    }

    // Pause recording
    function pauseRecording() {
        window.__csRecorder.status = 'paused';
        console.debug('CSTestForge: Recording paused');

        // Notify server
        sendEvent({
            type: 'RECORDER_CONTROL',
            action: 'PAUSE',
            timestamp: new Date().toISOString()
        });
    }

    // Resume recording
    function resumeRecording() {
        window.__csRecorder.status = 'recording';
        console.debug('CSTestForge: Recording resumed');

        // Notify server
        sendEvent({
            type: 'RECORDER_CONTROL',
            action: 'RESUME',
            timestamp: new Date().toISOString()
        });
    }

    // Stop recording
    function stopRecording() {
        window.__csRecorder.status = 'stopped';
        console.debug('CSTestForge: Recording stopped');

        // Notify server
        sendEvent({
            type: 'RECORDER_CONTROL',
            action: 'STOP',
            timestamp: new Date().toISOString()
        });

        // Clean up
        if (window.__csRecorder.blinkInterval) {
            clearInterval(window.__csRecorder.blinkInterval);
        }

        // Close WebSocket
        if (websocket) {
            websocket.close(1000, 'Recording stopped');
        }

        // Update UI
        const indicator = document.getElementById('cs-recorder-indicator');
        if (indicator) {
            indicator.style.backgroundColor = '#666';
            const dot = document.getElementById('cs-recorder-dot');
            if (dot) {
                dot.style.opacity = '0.4';
            }
            const text = document.getElementById('cs-recorder-text');
            if (text) {
                text.textContent = 'Recording Stopped';
            }

            // Remove after delay
            setTimeout(function() {
                if (indicator.parentNode) {
                    indicator.parentNode.removeChild(indicator);
                }
            }, 3000);
        }

        // Set inactive
        window.__csRecorderActive = false;
    }

    // Send event to the server
    function sendEvent(eventData) {
        // If we're using HTTP fallback mode, skip WebSocket attempt
        if (window.__csRecorderUsingHttpFallback === true) {
            sendEventViaHttp(eventData);
            return true;
        }

        // Add session ID to the event data
        eventData.sessionId = sessionId;

        // Send via WebSocket if connected
        let sent = false;
        if (websocket && websocket.readyState === WebSocket.OPEN) {
            try {
                websocket.send(JSON.stringify(eventData));
                sent = true;
            } catch (e) {
                console.error('CSTestForge: Failed to send event via WebSocket:', e);
                logError('Failed to send event via WebSocket', e);
            }
        }

        // If WebSocket sending failed, try REST API as fallback
        if (!sent) {
            return sendEventViaHttp(eventData);
        }
        return sent;
    }

    // Send event via HTTP API as fallback
    function sendEventViaHttp(eventData) {
        try {
            // Extract server host and context path from wsUrl
            // wsUrl format is typically: hostname:port/contextPath/ws-recorder
            const wsUrlParts = wsUrl.split('/');
            const hostPortPart = wsUrlParts[0]; // hostname:port

            // Reconstruct the context path
            let contextPath = '';
            if (wsUrlParts.length > 2) {
                const contextPathParts = wsUrlParts.slice(1, wsUrlParts.length - 1);
                contextPath = '/' + contextPathParts.join('/');
            }

            // Use the page protocol for API calls to avoid mixed content issues
            const protocol = window.location.protocol;
            const apiUrl = `${protocol}//${hostPortPart}${contextPath}/api/recorder/events/${sessionId}`;

            console.debug('CSTestForge: Sending event via HTTP API to:', apiUrl, eventData);

            // Add session ID to the event data
            eventData.sessionId = sessionId;

            // Use fetch with retry mechanism for better reliability
            fetch(apiUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(eventData)
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`HTTP status ${response.status}: ${response.statusText}`);
                    }
                    return response.json();
                })
                .then(data => {
                    console.debug('CSTestForge: HTTP API response:', data);
                })
                .catch(error => {
                    console.error('CSTestForge: HTTP API error:', error);

                    // Store failed events for potential retry
                    if (!window.__csRecorderFailedEvents) {
                        window.__csRecorderFailedEvents = [];
                    }

                    if (window.__csRecorderFailedEvents.length < 50) { // Limit stored failed events
                        window.__csRecorderFailedEvents.push({
                            event: eventData,
                            timestamp: new Date().toISOString()
                        });
                    }

                    logError('Failed to send event via HTTP API', error);
                });

            return true;
        } catch (e) {
            console.error('CSTestForge: Error sending event via HTTP:', e);
            logError('Error sending event via HTTP', e);
            return false;
        }
    }

    // Retry failed events periodically
    function setupFailedEventRetry() {
        // Check for failed events every minute
        setInterval(function() {
            if (window.__csRecorderFailedEvents && window.__csRecorderFailedEvents.length > 0) {
                console.debug('CSTestForge: Retrying failed events, count:', window.__csRecorderFailedEvents.length);

                // Take up to 5 events to retry
                const eventsToRetry = window.__csRecorderFailedEvents.splice(0, 5);

                eventsToRetry.forEach(item => {
                    // Only retry events less than 5 minutes old
                    const ageMs = new Date().getTime() - item.timestamp;
                    if (ageMs < 5 * 60 * 1000) {
                        console.debug('CSTestForge: Retrying event:', item.event);
                        sendEventViaHttp(item.event);
                    } else {
                        console.debug('CSTestForge: Discarding old event:', item.event);
                    }
                });
            }
        }, 60000);
    }

    // Get CSS selector for an element
    function getCssSelector(element) {
        if (!element) return null;

        // Try to build a unique CSS selector for the element
        let selector = '';

        // Use ID if available
        if (element.id) {
            return '#' + element.id;
        }

        // Use classes if available
        if (element.className) {
            const classNames = element.className.split(/\s+/).filter(name => name);
            if (classNames.length > 0) {
                selector = element.tagName.toLowerCase() + '.' + classNames.join('.');
                // Check if this selector is unique
                if (document.querySelectorAll(selector).length === 1) {
                    return selector;
                }
            }
        }

        // Use tag name and attributes
        selector = element.tagName.toLowerCase();

        // Add name attribute if available
        if (element.name) {
            selector += '[name="' + element.name + '"]';
            // Check if this selector is unique
            if (document.querySelectorAll(selector).length === 1) {
                return selector;
            }
        }

        // Add position in parent
        let parent = element.parentNode;
        if (parent && parent.tagName) {
            const siblings = Array.from(parent.children);
            const index = siblings.indexOf(element) + 1;

            selector += ':nth-child(' + index + ')';

            // Check if parent has ID
            if (parent.id) {
                return '#' + parent.id + ' > ' + selector;
            }

            // Add parent tag
            selector = parent.tagName.toLowerCase() + ' > ' + selector;
        }

        return selector;
    }

    // Get XPath for an element
    function getXPath(element) {
        if (!element) return null;

        // Check if element has ID
        if (element.id) {
            return '//*[@id="' + element.id + '"]';
        }

        // Get XPath by traversing the DOM tree
        let parts = [];
        while (element && element.nodeType === Node.ELEMENT_NODE) {
            let part = element.tagName.toLowerCase();

            // Add unique identifier
            if (element.id) {
                part += '[@id="' + element.id + '"]';
                parts.unshift(part);
                break;
            } else {
                // Add index among siblings of same type
                let siblings = Array.from(element.parentNode.children).filter(e => e.tagName === element.tagName);
                if (siblings.length > 1) {
                    let index = siblings.indexOf(element) + 1;
                    part += '[' + index + ']';
                }
            }

            parts.unshift(part);
            element = element.parentNode;
        }

        return '/' + parts.join('/');
    }

    // Get element information
    function getElementInfo(element) {
        if (!element) return null;

        const info = {
            tagName: element.tagName.toLowerCase(),
            id: element.id || null,
            className: element.className || null,
            name: element.name || null,
            type: element.type || null,
            value: element.value || null,
            cssSelector: getCssSelector(element),
            xpath: getXPath(element),
            text: element.textContent ? element.textContent.trim().substring(0, 100) : null,
            attributes: {}
        };

        // Add all attributes
        for (let i = 0; i < element.attributes.length; i++) {
            const attr = element.attributes[i];
            info.attributes[attr.name] = attr.value;
        }

        // Add specific attributes for common elements
        if (element.tagName.toLowerCase() === 'a') {
            info.href = element.href || null;
        } else if (element.tagName.toLowerCase() === 'img') {
            info.src = element.src || null;
            info.alt = element.alt || null;
        } else if (element.tagName.toLowerCase() === 'input') {
            info.placeholder = element.placeholder || null;
        }

        return info;
    }

    // Capture a screenshot
    function captureScreenshot() {
        try {
            // Send event indicating screenshot will be taken
            sendEvent({
                type: 'SCREENSHOT_START',
                url: window.location.href,
                title: document.title,
                timestamp: new Date().toISOString()
            });

            // For browsers that support OffscreenCanvas, capture screenshot data
            // This is experimental and may not work in all browsers
            if (window.html2canvas) {
                html2canvas(document.documentElement).then(function(canvas) {
                    try {
                        const screenshotData = canvas.toDataURL('image/png');

                        // Send screenshot data
                        sendEvent({
                            type: 'SCREENSHOT',
                            url: window.location.href,
                            title: document.title,
                            timestamp: new Date().toISOString(),
                            data: screenshotData
                        });
                    } catch (e) {
                        console.error('CSTestForge: Failed to process screenshot data:', e);
                        logError('Failed to process screenshot data', e);
                    }
                }).catch(function(error) {
                    console.error('CSTestForge: Failed to capture screenshot:', error);
                    logError('Failed to capture screenshot with html2canvas', error);
                });
            } else {
                // Otherwise, let the server know to take a screenshot
                sendEvent({
                    type: 'SCREENSHOT_REQUEST',
                    url: window.location.href,
                    title: document.title,
                    timestamp: new Date().toISOString()
                });
            }
        } catch (e) {
            console.error('CSTestForge: Failed to capture screenshot:', e);
            logError('Failed to capture screenshot', e);
        }
    }

    // Set up event listeners
    function setupEventListeners() {
        try {
            // Track clicks
            document.addEventListener('click', function(event) {
                if (window.__csRecorder.status !== 'recording') return;

                const target = event.target;

                // Create click event
                const clickEvent = {
                    type: 'CLICK',
                    url: window.location.href,
                    title: document.title,
                    timestamp: event.timeStamp || new Date().getTime(),
                    elementInfo: getElementInfo(target),
                    ctrlKey: event.ctrlKey,
                    altKey: event.altKey,
                    shiftKey: event.shiftKey,
                    metaKey: event.metaKey,
                    button: event.button
                };

                sendEvent(clickEvent);
            }, true);

            // Track double clicks
            document.addEventListener('dblclick', function(event) {
                if (window.__csRecorder.status !== 'recording') return;

                const target = event.target;

                // Create double click event
                const dblClickEvent = {
                    type: 'DOUBLE_CLICK',
                    url: window.location.href,
                    title: document.title,
                    timestamp: event.timeStamp || new Date().getTime(),
                    elementInfo: getElementInfo(target)
                };

                sendEvent(dblClickEvent);
            }, true);

            // Track right clicks
            document.addEventListener('contextmenu', function(event) {
                if (window.__csRecorder.status !== 'recording') return;

                const target = event.target;

                // Create right click event
                const rightClickEvent = {
                    type: 'RIGHT_CLICK',
                    url: window.location.href,
                    title: document.title,
                    timestamp: event.timeStamp || new Date().getTime(),
                    elementInfo: getElementInfo(target)
                };

                sendEvent(rightClickEvent);
            }, true);

            // Track input changes (debounced)
            let inputTimeout = null;
            let lastInputValue = new WeakMap();

            document.addEventListener('input', function(event) {
                if (window.__csRecorder.status !== 'recording') return;

                const target = event.target;

                // Skip if not a valid input element
                if (!target || !['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName)) return;

                // Clear existing timeout
                if (inputTimeout) {
                    clearTimeout(inputTimeout);
                }

                // Get the previous value
                const previousValue = lastInputValue.get(target) || '';

                // Store current value
                lastInputValue.set(target, target.value);

                // Set a timeout to send the input event (debounce)
                inputTimeout = setTimeout(function() {
                    // Create input event
                    const inputEvent = {
                        type: 'INPUT',
                        url: window.location.href,
                        title: document.title,
                        timestamp: new Date().toISOString(),
                        elementInfo: getElementInfo(target),
                        value: target.value,
                        previousValue: previousValue
                    };

                    // For password fields, mask the value
                    if (target.type === 'password') {
                        inputEvent.value = '********';
                        inputEvent.masked = true;
                    }

                    sendEvent(inputEvent);
                }, 500);
            }, true);

            // Track form submissions
            document.addEventListener('submit', function(event) {
                if (window.__csRecorder.status !== 'recording') return;

                const target = event.target;

                // Create form submit event
                const submitEvent = {
                    type: 'FORM_SUBMIT',
                    url: window.location.href,
                    title: document.title,
                    timestamp: event.timeStamp || new Date().getTime(),
                    elementInfo: getElementInfo(target)
                };

                // Collect form data (excluding passwords)
                const formData = {};
                if (target && target.elements) {
                    for (let i = 0; i < target.elements.length; i++) {
                        const element = target.elements[i];
                        if (element.name && element.value !== undefined) {
                            if (element.type === 'password') {
                                formData[element.name] = '********';
                            } else {
                                formData[element.name] = element.value;
                            }
                        }
                    }
                }

                submitEvent.formData = formData;

                sendEvent(submitEvent);
            }, true);

            // Track navigation events
            const originalPushState = window.history.pushState;
            const originalReplaceState = window.history.replaceState;

            window.history.pushState = function() {
                // Call original function
                originalPushState.apply(this, arguments);

                // Create navigation event
                if (window.__csRecorder.status === 'recording') {
                    const navEvent = {
                        type: 'NAVIGATION',
                        url: window.location.href,
                        title: document.title,
                        timestamp: new Date().toISOString(),
                        method: 'pushState',
                        target: arguments[2] // URL argument
                    };

                    sendEvent(navEvent);
                }

                // Check if we need to reinject the recorder script
                setTimeout(checkRecorderStatus, 500);
            };

            window.history.replaceState = function() {
                // Call original function
                originalReplaceState.apply(this, arguments);

                // Create navigation event
                if (window.__csRecorder.status === 'recording') {
                    const navEvent = {
                        type: 'NAVIGATION',
                        url: window.location.href,
                        title: document.title,
                        timestamp: new Date().toISOString(),
                        method: 'replaceState',
                        target: arguments[2] // URL argument
                    };

                    sendEvent(navEvent);
                }

                // Check if we need to reinject the recorder script
                setTimeout(checkRecorderStatus, 500);
            };

            // Track popstate events (browser back/forward)
            window.addEventListener('popstate', function(event) {
                if (window.__csRecorder.status !== 'recording') return;

                const navEvent = {
                    type: 'NAVIGATION',
                    url: window.location.href,
                    title: document.title,
                    timestamp: event.timeStamp || new Date().getTime(),
                    method: 'popstate'
                };

                sendEvent(navEvent);

                // Check if we need to reinject the recorder script
                setTimeout(checkRecorderStatus, 500);
            });

            // Handle page unload
            window.addEventListener('beforeunload', function(event) {
                if (window.__csRecorder.status !== 'recording') return;

                const unloadEvent = {
                    type: 'UNLOAD',
                    url: window.location.href,
                    title: document.title,
                    timestamp: new Date().toISOString()
                };

                // Use synchronous XHR for unload event to ensure it's sent
                try {
                    sendEvent(unloadEvent);
                } catch (e) {
                    // Ignore errors on unload
                }
            });

            console.debug('CSTestForge: Event listeners set up successfully');
        } catch (e) {
            console.error('CSTestForge: Failed to set up event listeners:', e);
            logError('Failed to set up event listeners', e);
        }
    }

    // Function to check recorder status and handle SPA navigation
    function checkRecorderStatus() {
        // Check if recorder is still active
        if (!window.__csRecorderActive) {
            console.debug('CSTestForge: Recorder not active after navigation, reinitializing');

            // Try to recover previous session information
            try {
                const storedSessionId = sessionStorage.getItem('__csRecorderSessionId');
                const storedWsUrl = sessionStorage.getItem('__csRecorderWsUrl');

                if (storedSessionId && storedWsUrl) {
                    // Reinitialize recorder
                    window.__csRecorderActive = true;

                    // Recreate UI
                    createUI();

                    // Reconnect WebSocket
                    connectWebSocket();

                    // Set up event listeners
                    setupEventListeners();

                    console.debug('CSTestForge: Recorder reinitialized successfully');
                }
            } catch (e) {
                console.error('CSTestForge: Failed to reinitialize recorder:', e);
                logError('Failed to reinitialize recorder after navigation', e);
            }
        }
    }

    // Initialize the recorder
    try {
        console.debug('CSTestForge: Creating visual indicator');
        createUI();

        console.debug('CSTestForge: Setting up WebSocket connection');
        connectWebSocket();

        console.debug('CSTestForge: Setting up event listeners');
        setupEventListeners();

        // Set up failed event retry
        setupFailedEventRetry();

        // Store session data in sessionStorage for persistence across navigation
        try {
            sessionStorage.setItem('__csRecorderSessionId', sessionId);
            sessionStorage.setItem('__csRecorderWsUrl', wsUrl);
        } catch (e) {
            console.error('CSTestForge: Failed to store recorder data in sessionStorage:', e);
            logError('Failed to store recorder data in sessionStorage', e);
        }

        // Set up an interval to check if we need to reinitialize
        setInterval(checkRecorderStatus, 5000);

        console.debug('CSTestForge: Recording started successfully');
    } catch (e) {
        console.error('CSTestForge: Error during recorder initialization:', e);
        logError('Error during recorder initialization', e);
    }
})();

// Add support for SPA (Single Page Application) navigation detection
(function() {
    // Store last URL for change detection
    let lastUrl = window.location.href;

    // Check URL changes periodically
    setInterval(function() {
        if (window.location.href !== lastUrl) {
            console.debug('CSTestForge: SPA navigation detected - from ' + lastUrl + ' to ' + window.location.href);
            lastUrl = window.location.href;

            // Create navigation event
            if (window.__csRecorder && window.__csRecorder.status === 'recording') {
                const navEvent = {
                    type: 'NAVIGATION',
                    url: window.location.href,
                    title: document.title,
                    timestamp: new Date().toISOString(),
                    method: 'spa_navigation'
                };

                // Try to send the event
                if (typeof sendEvent === 'function') {
                    sendEvent(navEvent);
                } else if (window.__csRecorderUsingHttpFallback && typeof sendEventViaHttp === 'function') {
                    sendEventViaHttp(navEvent);
                }
            }

            // Check if recorder is still active
            // If not, try to reinitialize it
            if (!window.__csRecorderActive && sessionStorage.getItem('__csRecorderSessionId')) {
                // Wait a moment for the DOM to settle
                setTimeout(function() {
                    if (typeof checkRecorderStatus === 'function') {
                        checkRecorderStatus();
                    } else {
                        // If checkRecorderStatus is not available, try to reload the page
                        console.debug('CSTestForge: Reloading page after navigation to reinitialize recorder');
                        window.location.reload();
                    }
                }, 1000);
            }
        }
    }, 1000);
})();