package com.cstestforge.recorder.browser;

import com.cstestforge.recorder.model.BrowserType;
import com.cstestforge.recorder.model.RecordingConfig;
import com.cstestforge.recorder.websocket.RecorderWebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manages browser lifecycle and script injection for the recording sessions.
 */
@Component
public class BrowserManager {
    private static final Logger logger = LoggerFactory.getLogger(BrowserManager.class);
    
    @Autowired
    private RecorderWebSocketService webSocketService;
    
    private final Map<UUID, BrowserInstance> activeBrowsers = new ConcurrentHashMap<>();
    
    // Path to the recorder script
    private static final String RECORDER_SCRIPT_PATH = "src/main/resources/static/js/recorder-script.js";
    
    // Cache the script content to avoid reading from disk repeatedly
    private String recorderScriptCache;
    
    /**
     * Start a browser session for recording
     *
     * @param sessionId The recording session ID
     * @param config The recording configuration
     * @return true if browser started successfully, false otherwise
     */
    public boolean startBrowser(UUID sessionId, RecordingConfig config) {
        if (sessionId == null || config == null) {
            logger.error("Cannot start browser: sessionId or config is null");
            return false;
        }
        
        if (activeBrowsers.containsKey(sessionId)) {
            logger.warn("Browser session already exists for sessionId: {}", sessionId);
            return true;
        }
        
        BrowserType browserType = BrowserType.fromString(config.getBrowserType());
        BrowserInstance browser = createBrowserInstance(sessionId, browserType, config);
        
        boolean started = browser.start(config.getCommandTimeoutSeconds(), TimeUnit.SECONDS);
        if (started) {
            activeBrowsers.put(sessionId, browser);
            
            // Navigate to the base URL if provided
            boolean navigationSuccess = true;
            String baseUrl = config.getBaseUrl();
            if (baseUrl != null && !baseUrl.isEmpty()) {
                // Special handling for about:blank - don't modify this URL
                if ("about:blank".equals(baseUrl)) {
                    logger.info("Navigating to special URL: {}", baseUrl);
                    navigationSuccess = browser.navigate(baseUrl);
                } else {
                    navigationSuccess = navigateTo(sessionId, baseUrl);
                }
                
                if (!navigationSuccess) {
                    logger.warn("Failed to navigate to initial URL: {}", baseUrl);
                    // Continue anyway - not a critical failure
                }
            }
            
            // Inject the recorder script
            boolean injected = injectRecorderScript(sessionId);
            
            if (!injected) {
                logger.error("Failed to inject recorder script for session: {}", sessionId);
                // Still consider browser started, we'll retry script injection later
            } else {
                logger.info("Recorder script injected successfully");
            }
            
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Stop a browser session
     *
     * @param sessionId The recording session ID
     */
    public void stopBrowser(UUID sessionId) {
        if (sessionId == null) {
            return;
        }
        
        BrowserInstance browser = activeBrowsers.remove(sessionId);
        if (browser != null) {
            browser.stop();
            logger.info("Browser stopped for session: {}", sessionId);
        }
    }
    
    /**
     * Navigate to a URL in the browser
     *
     * @param sessionId The recording session ID
     * @param url The URL to navigate to
     * @return true if navigation succeeded, false otherwise
     */
    public boolean navigateTo(UUID sessionId, String url) {
        BrowserInstance browser = getBrowserInstance(sessionId);
        if (browser == null || url == null || url.isEmpty()) {
            return false;
        }
        
        try {
            logger.info("Navigating to URL: {}", url);
            boolean success = browser.navigate(url);
            
            if (success) {
                // Wait briefly for page to load before injecting script
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // Re-inject the recorder script after navigation
                boolean injected = injectRecorderScript(sessionId);
                
                if (!injected) {
                    logger.warn("Failed to re-inject recorder script after navigation to: {}", url);
                }
                
                return true;
            } else {
                logger.error("Navigation failed for session {} to URL: {}", sessionId, url);
                return false;
            }
        } catch (Exception e) {
            logger.error("Navigation failed for session {}: {}", sessionId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Inject the recorder script into the browser
     *
     * @param sessionId The recording session ID
     * @return true if script injection succeeded, false otherwise
     */
    public boolean injectRecorderScript(UUID sessionId) {
        BrowserInstance browser = getBrowserInstance(sessionId);
        if (browser == null) {
            logger.error("Cannot inject recorder script - no browser found for session: {}", sessionId);
            return false;
        }
        
        try {
            String script = getRecorderScript();
            if (script == null || script.isEmpty()) {
                logger.error("Recorder script is empty or could not be loaded");
                return false;
            }
            
            logger.debug("Loaded recorder script with length: {} characters", script.length());
            
            // Modify the script with the session ID and WebSocket URL
            script = script.replace("__SESSION_ID__", sessionId.toString());
            
            // Build the WebSocket URL based on the server's configuration
            // This ensures the recorder script connects back to our application server, not the target website
            // Dynamic way: Get the origin URL for our server (could be configurable in a properties file)
            // For now, we'll determine it dynamically based on system properties or environment variables
            String serverHost = System.getProperty("cstestforge.server.host", "localhost");
            String serverPort = System.getProperty("cstestforge.server.port", "8080");
            
            // Get the server's context path from properties (default to /cstestforge)
            String contextPath = System.getProperty("server.servlet.context-path", "/cstestforge");
            // Ensure it starts with a slash
            if (!contextPath.startsWith("/")) {
                contextPath = "/" + contextPath;
            }
            
            // Note: Instead of forcing protocol, we'll send a neutral URL
            // The recorder script will determine whether to use ws:// or wss://
            // Based on the protocol of the page and connection success
            
            // We only send hostname and path, protocol will be determined by script
            // Format is: hostname:port/path
            String wsUrl = String.format("%s:%s%s/ws-recorder", 
                               serverHost, serverPort, contextPath);
            
            logger.debug("WebSocket base URL (protocol will be determined by client): {}", wsUrl);
            script = script.replace("__WS_URL__", wsUrl);
            
            // Add a script to monitor navigation and auto re-inject
            script += "\n" + 
                      "// Check if script needs re-injection every 2 seconds\n" +
                      "setInterval(function() {\n" +
                      "  if (window.__csRecorderActive && !document.getElementById('cs-recorder-toolbar')) {\n" +
                      "    console.debug('CSTestForge: Re-initializing recorder UI');\n" +
                      "    if (typeof createRecordingIndicator === 'function') {\n" +
                      "      createRecordingIndicator();\n" +
                      "    }\n" +
                      "  }\n" +
                      "}, 2000);\n";
            
            // Execute the script in the browser
            logger.debug("Injecting recorder script for session: {}", sessionId);
            logger.debug("Current URL for script injection: {}", browser.getCurrentUrl());
            
            try {
                // Wrap script in try-catch to better report errors
                String wrappedScript = 
                    "try { " +
                    script +
                    "return true; " +
                    "} catch(e) { console.error('CSTestForge script error:', e); return e.toString(); }";
                
                Object result = browser.injectScript(wrappedScript);
                
                if (result != null) {
                    if (result instanceof String && ((String)result).contains("Error")) {
                        logger.error("Failed to inject recorder script - error: {}", result);
                        return false;
                    }
                    
                    logger.info("Recorder script injected successfully for session: {}", sessionId);
                    
                    // Setup a timer to periodically check if script needs re-injection
                    setupScriptReinjectionCheck(sessionId);
                    
                    return true;
                } else {
                    logger.error("Failed to inject recorder script - null result returned from browser");
                    return false;
                }
            } catch (Exception e) {
                logger.error("Exception during script injection: {}", e.getMessage(), e);
                return false;
            }
        } catch (Exception e) {
            logger.error("Failed to inject recorder script for session {}: {}", sessionId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Set up a periodic check to re-inject the script if needed
     * 
     * @param sessionId The recording session ID
     */
    private void setupScriptReinjectionCheck(UUID sessionId) {
        // Create a daemon thread to periodically check if script needs re-injection
        Thread checkThread = new Thread(() -> {
            BrowserInstance browser = getBrowserInstance(sessionId);
            if (browser == null) {
                return;
            }
            
            int checkCount = 0;
            while (browser.isRunning() && checkCount < 30) { // Run for up to 5 minutes (30 * 10 seconds)
                try {
                    Thread.sleep(10000); // Check every 10 seconds
                    checkCount++;
                    
                    // Check if __csRecorderActive exists but UI indicator is missing
                    Object result = browser.executeScript(
                        "return window.__csRecorderActive === true && " +
                        "document.getElementById('cs-recorder-toolbar') === null;"
                    );
                    
                    if (Boolean.TRUE.equals(result)) {
                        logger.info("Detected missing recorder UI for session {}, re-injecting", sessionId);
                        injectRecorderScript(sessionId);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // Ignore errors during check
                }
            }
        });
        
        checkThread.setDaemon(true);
        checkThread.setName("recorder-script-check-" + sessionId);
        checkThread.start();
    }
    
    /**
     * Capture a screenshot from the browser
     *
     * @param sessionId The recording session ID
     * @return screenshot as byte array, or null if it failed
     */
    public byte[] captureScreenshot(UUID sessionId) {
        BrowserInstance browser = getBrowserInstance(sessionId);
        if (browser == null) {
            return null;
        }
        
        try {
            return browser.captureScreenshot();
        } catch (Exception e) {
            logger.error("Failed to capture screenshot for session {}: {}", sessionId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Get the current URL of the browser
     *
     * @param sessionId The recording session ID
     * @return The current URL or null if not available
     */
    public String getCurrentUrl(UUID sessionId) {
        BrowserInstance browser = getBrowserInstance(sessionId);
        return browser != null ? browser.getCurrentUrl() : null;
    }
    
    /**
     * Execute a JavaScript script in the browser
     *
     * @param sessionId The recording session ID
     * @param script The script to execute
     * @return The result of the script execution
     */
    public Object executeScript(UUID sessionId, String script) {
        BrowserInstance browser = getBrowserInstance(sessionId);
        if (browser == null || script == null) {
            return null;
        }
        
        try {
            return browser.executeScript(script, new Object[0]);
        } catch (Exception e) {
            logger.error("Failed to execute script for session {}: {}", sessionId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Wait for a condition in the browser
     *
     * @param sessionId The recording session ID
     * @param conditionScript JavaScript condition that returns true when condition is met
     * @param timeoutMs Timeout in milliseconds
     * @return true if condition was met, false if timeout or error
     */
    public boolean waitForCondition(UUID sessionId, String conditionScript, long timeoutMs) {
        BrowserInstance browser = getBrowserInstance(sessionId);
        if (browser == null || conditionScript == null) {
            return false;
        }
        
        try {
            return browser.waitForCondition(conditionScript, timeoutMs);
        } catch (Exception e) {
            logger.error("Failed to wait for condition for session {}: {}", sessionId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if a browser session is active
     *
     * @param sessionId The recording session ID
     * @return true if the browser session is active
     */
    public boolean isSessionActive(UUID sessionId) {
        return sessionId != null && activeBrowsers.containsKey(sessionId);
    }
    
    /**
     * Get all active browser sessions
     *
     * @return Map of session IDs to browser instances
     */
    public Map<UUID, BrowserInstance> getActiveBrowsers() {
        return Map.copyOf(activeBrowsers);
    }
    
    /**
     * Creates a browser instance based on the browser type
     *
     * @param sessionId The session ID
     * @param browserType The browser type
     * @param config The recording configuration
     * @return A BrowserInstance implementation
     */
    private BrowserInstance createBrowserInstance(UUID sessionId, BrowserType browserType, RecordingConfig config) {
        switch (browserType) {
            case CHROME:
                return new ChromeBrowserInstance(sessionId, browserType, config);
            case FIREFOX:
                return new FirefoxBrowserInstance(sessionId, browserType, config);
            case EDGE:
                return new EdgeBrowserInstance(sessionId, browserType, config);
            case SAFARI:
                return new SafariBrowserInstance(sessionId, browserType, config);
            case CHROME_PLAYWRIGHT:
                return new PlaywrightBrowserInstance(sessionId, browserType, config);
            case FIREFOX_PLAYWRIGHT:
                return new PlaywrightBrowserInstance(sessionId, browserType, config);
            case WEBKIT_PLAYWRIGHT:
                return new PlaywrightBrowserInstance(sessionId, browserType, config);
            case MSEDGE_PLAYWRIGHT:
                return new PlaywrightBrowserInstance(sessionId, browserType, config);
            default:
                return new ChromeBrowserInstance(sessionId, browserType, config);
        }
    }
    
    /**
     * Get the browser instance for a session
     *
     * @param sessionId The session ID
     * @return The browser instance or null if not found
     */
    private BrowserInstance getBrowserInstance(UUID sessionId) {
        if (sessionId == null) {
            return null;
        }
        
        BrowserInstance browser = activeBrowsers.get(sessionId);
        if (browser == null) {
            logger.warn("No browser instance found for session: {}", sessionId);
        }
        return browser;
    }
    
    /**
     * Check if the application is running over a secure connection (HTTPS)
     * Since this is simplistic, this always returns false for now
     * In a production environment, this would check server configuration
     *
     * @return true if running on HTTPS, false otherwise
     */
    private boolean isSecureConnection() {
        // For now, return false as most dev environments run on HTTP
        // In production, this could check the server configuration or environment variables
        return false;
    }
    
    /**
     * Get the recorder script content from file or cache
     *
     * @return The recorder script content
     * @throws IOException If the script file can't be read
     */
    private String getRecorderScript() throws IOException {
        if (recorderScriptCache == null) {
            Path scriptPath = Paths.get(RECORDER_SCRIPT_PATH);
            recorderScriptCache = Files.readString(scriptPath);
        }
        return recorderScriptCache;
    }
    
    /**
     * Stop all browser instances during application shutdown
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down all browser instances...");
        for (Map.Entry<UUID, BrowserInstance> entry : activeBrowsers.entrySet()) {
            try {
                entry.getValue().stop();
            } catch (Exception e) {
                logger.error("Error stopping browser for session {}: {}", entry.getKey(), e.getMessage());
            }
        }
        activeBrowsers.clear();
    }
} 