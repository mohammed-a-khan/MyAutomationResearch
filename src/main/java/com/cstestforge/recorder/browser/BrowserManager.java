package com.cstestforge.recorder.browser;

import com.cstestforge.recorder.model.*;
import com.cstestforge.recorder.service.RecorderService;
import com.cstestforge.recorder.websocket.RecorderWebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages browser lifecycle and script injection for the recording sessions.
 * Enhanced with more robust script injection, domain change detection, and browser reconnection.
 */
@Component
public class BrowserManager {
    private static final Logger logger = LoggerFactory.getLogger(BrowserManager.class);

    @Autowired
    private RecorderWebSocketService webSocketService;

    @Autowired
    private RecorderService recorderService;

    private final Map<UUID, BrowserInstance> activeBrowsers = new ConcurrentHashMap<>();

    // Path to the recorder script - update this path if needed
    private static final String RECORDER_SCRIPT_PATH = "src/main/resources/static/js/recorder-script.js";

    // Maximum retries for script injection
    private static final int MAX_INJECTION_RETRIES = 3;

    // Timeout for browser operations in seconds
    private static final int BROWSER_OPERATION_TIMEOUT = 60;

    // Cache the script content to avoid reading from disk repeatedly
    private String recorderScriptCache;

    // Server configuration for WebSocket connections
    private String serverHost = "localhost"; // Default value
    private String serverPort = "8080";      // Default value
    private String contextPath = "/cstestforge"; // Default value

    /**
     * Constructor to initialize server configuration
     */
    public BrowserManager() {
        // Try to get server host and port from system properties
        // First check for explicit server address settings
        this.serverHost = System.getProperty("server.address");
        if (this.serverHost == null || this.serverHost.isEmpty()) {
            // Fall back to server.host if server.address is not set
            this.serverHost = System.getProperty("server.host");

            // Fall back to localhost if neither is set
            if (this.serverHost == null || this.serverHost.isEmpty()) {
                this.serverHost = "localhost";
            }
        }

        // Get server port
        String configuredPort = System.getProperty("server.port");
        if (configuredPort != null && !configuredPort.isEmpty()) {
            this.serverPort = configuredPort;
        } else {
            // Default to 8080 if not specified
            this.serverPort = "8080";
        }

        // Get context path
        String configuredContextPath = System.getProperty("server.servlet.context-path");
        if (configuredContextPath != null && !configuredContextPath.isEmpty()) {
            this.contextPath = configuredContextPath;
        } else {
            // Default to /cstestforge if not specified
            this.contextPath = "/cstestforge";
        }

        logger.info("Initialized BrowserManager with server host: {}, port: {}, context path: {}",
                serverHost, serverPort, contextPath);

        // Try to detect LAN IP address for better connection from external sites to localhost
        try {
            String lanIp = detectLanIpAddress();
            if (lanIp != null && !lanIp.equals("127.0.0.1")) {
                logger.info("Detected LAN IP address: {}. This may be more reachable than localhost", lanIp);
                if ("localhost".equals(serverHost)) {
                    logger.info("Consider using this IP address instead of localhost in server.address property");
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to detect LAN IP address: {}", e.getMessage());
        }
    }

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

        logger.info("Starting browser of type: {} for session: {}", browserType, sessionId);

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

            // Wait for page to fully load
            try {
                Thread.sleep(3000); // 3000ms for better loading
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Apply CSP bypasses for Chrome browser
            if (browser instanceof ChromeBrowserInstance) {
                ChromeBrowserInstance chromeBrowser = (ChromeBrowserInstance) browser;
                try {
                    // Apply various CSP bypasses
                    chromeBrowser.applyCSPBypass();
                    chromeBrowser.disableCSP();
                    chromeBrowser.fixCORSIssues(serverHost, serverPort);
                } catch (Exception e) {
                    logger.warn("Error applying CSP bypasses: {}", e.getMessage());
                    // Continue anyway - not a critical failure
                }
            }

            // Inject recorder script with robust strategy
            boolean injected = injectRecorderScriptWithRetry(sessionId, MAX_INJECTION_RETRIES);

            if (injected) {
                logger.info("Recorder script successfully injected for session: {}", sessionId);
            } else {
                logger.error("Failed to inject recorder script for session: {}", sessionId);
                // We'll continue anyway and try reinjection later
            }

            // Setup periodic script verification and reinjection
            setupRecorderHealthCheck(sessionId);

            // Setup mutation observer to track DOM changes and ensure recorder stays active
            setupMutationObserver(sessionId);

            // Setup domain change detection
            setupDomainChangeDetection(sessionId);

            // Setup browser close detection
            setupBrowserCloseListener(sessionId);

            return true;
        } else {
            logger.error("Failed to start browser of type: {} for session: {}", browserType, sessionId);
            return false;
        }
    }

    /**
     * Setup domain change detection similar to the reference implementation
     *
     * @param sessionId The session ID
     */
    private void setupDomainChangeDetection(UUID sessionId) {
        BrowserInstance browser = getBrowserInstance(sessionId);
        if (browser == null) return;

        try {
            // Extract initial domain
            String url = browser.getCurrentUrl();
            String currentDomain = extractDomain(url);

            // Store current domain in JavaScript context
            browser.executeScript(
                    "window.__csCurrentDomain = '" + currentDomain + "';" +
                            "console.log('Current domain set to: ' + window.__csCurrentDomain);"
            );

            // Setup a script to monitor URL changes and detect domain changes
            String domainChangeScript =
                    "if (!window.__csDomainChangeDetection) {\n" +
                            "  window.__csDomainChangeDetection = true;\n" +
                            "  window.__csLastUrl = window.location.href;\n" +
                            "  window.__csLastDomain = window.__csCurrentDomain || '';\n" +
                            "  \n" +
                            "  function extractDomain(url) {\n" +
                            "    try {\n" +
                            "      if (!url) return '';\n" +
                            "      if (url === 'about:blank') return 'about:blank';\n" +
                            "      \n" +
                            "      let domain;\n" +
                            "      // Remove protocol\n" +
                            "      if (url.indexOf('://') > -1) {\n" +
                            "        domain = url.split('://')[1];\n" +
                            "      } else {\n" +
                            "        domain = url;\n" +
                            "      }\n" +
                            "      \n" +
                            "      // Remove path\n" +
                            "      if (domain.indexOf('/') > -1) {\n" +
                            "        domain = domain.split('/')[0];\n" +
                            "      }\n" +
                            "      \n" +
                            "      return domain;\n" +
                            "    } catch(e) {\n" +
                            "      console.error('Error extracting domain:', e);\n" +
                            "      return '';\n" +
                            "    }\n" +
                            "  }\n" +
                            "  \n" +
                            "  // Check for domain changes periodically\n" +
                            "  setInterval(function() {\n" +
                            "    const currentUrl = window.location.href;\n" +
                            "    if (currentUrl !== window.__csLastUrl) {\n" +
                            "      const newDomain = extractDomain(currentUrl);\n" +
                            "      window.__csLastUrl = currentUrl;\n" +
                            "      \n" +
                            "      if (newDomain !== window.__csLastDomain) {\n" +
                            "        console.log('Domain changed from ' + window.__csLastDomain + ' to ' + newDomain);\n" +
                            "        window.__csLastDomain = newDomain;\n" +
                            "        window.__csCurrentDomain = newDomain;\n" +
                            "        \n" +
                            "        // Create and dispatch custom event\n" +
                            "        const event = new CustomEvent('cs_domain_changed', {\n" +
                            "          detail: { newDomain: newDomain, oldDomain: window.__csLastDomain }\n" +
                            "        });\n" +
                            "        window.dispatchEvent(event);\n" +
                            "        \n" +
                            "        // Check if recorder is still active\n" +
                            "        if (!window.__csRecorderActive) {\n" +
                            "          console.log('Recorder not active after domain change, requesting reinjection');\n" +
                            "          const reinjectionEvent = new CustomEvent('cs_recorder_needed');\n" +
                            "          window.dispatchEvent(reinjectionEvent);\n" +
                            "        }\n" +
                            "      }\n" +
                            "    }\n" +
                            "  }, 1000);\n" +
                            "  \n" +
                            "  // Listen for navigation events that might change the domain\n" +
                            "  window.addEventListener('popstate', function() {\n" +
                            "    setTimeout(function() {\n" +
                            "      const currentUrl = window.location.href;\n" +
                            "      const newDomain = extractDomain(currentUrl);\n" +
                            "      \n" +
                            "      if (newDomain !== window.__csLastDomain) {\n" +
                            "        console.log('Domain changed (popstate) from ' + window.__csLastDomain + ' to ' + newDomain);\n" +
                            "        window.__csLastDomain = newDomain;\n" +
                            "        window.__csCurrentDomain = newDomain;\n" +
                            "        \n" +
                            "        // Create and dispatch custom event\n" +
                            "        const event = new CustomEvent('cs_domain_changed', {\n" +
                            "          detail: { newDomain: newDomain, oldDomain: window.__csLastDomain }\n" +
                            "        });\n" +
                            "        window.dispatchEvent(event);\n" +
                            "      }\n" +
                            "    }, 100);\n" +
                            "  });\n" +
                            "  \n" +
                            "  // Handle domain change events\n" +
                            "  window.addEventListener('cs_domain_changed', function(e) {\n" +
                            "    console.log('Domain change event received:', e.detail);\n" +
                            "    \n" +
                            "    // Check if recorder is still active\n" +
                            "    setTimeout(function() {\n" +
                            "      if (!window.__csRecorderActive) {\n" +
                            "        console.log('Recorder not active after domain change event, requesting reinjection');\n" +
                            "        const reinjectionEvent = new CustomEvent('cs_recorder_needed');\n" +
                            "        window.dispatchEvent(reinjectionEvent);\n" +
                            "      }\n" +
                            "    }, 500);\n" +
                            "  });\n" +
                            "  \n" +
                            "  console.log('Domain change detection setup complete');\n" +
                            "}\n";

            browser.executeScript(domainChangeScript);

            // Add event listener for domain change events to trigger script reinjection
            String listenerScript =
                    "window.addEventListener('cs_domain_changed', function() {\n" +
                            "  // This will be handled by the Java code through separate checks\n" +
                            "});\n" +
                            "\n" +
                            "window.addEventListener('cs_recorder_needed', function() {\n" +
                            "  // This will be handled by the Java code through separate checks\n" +
                            "});\n";

            browser.executeScript(listenerScript);

            logger.info("Domain change detection setup for session: {}", sessionId);

            // Schedule periodic domain change checks
            scheduleDomainChangeChecks(sessionId);

        } catch (Exception e) {
            logger.error("Error setting up domain change detection for session {}: {}",
                    sessionId, e.getMessage());
        }
    }

    /**
     * Schedule periodic domain change checks
     *
     * @param sessionId The session ID
     */
    private void scheduleDomainChangeChecks(UUID sessionId) {
        Thread checkThread = new Thread(() -> {
            int checkCount = 0;

            while (isSessionActive(sessionId) && checkCount < 600) { // Run for up to 10 minutes
                try {
                    Thread.sleep(1000); // Check every second
                    checkCount++;

                    if (checkCount % 10 == 0) { // Every 10 seconds, perform a full check
                        checkForDomainChange(sessionId);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.warn("Error in domain change check for session {}: {}",
                            sessionId, e.getMessage());
                }
            }

            logger.info("Domain change checker completed for session: {}", sessionId);
        });

        checkThread.setDaemon(true);
        checkThread.setName("domain-change-check-" + sessionId);
        checkThread.start();
    }

    /**
     * Check for domain change or if the recorder needs to be reinjected
     * Similar to the reference implementation's checkForDomainChange()
     *
     * @param sessionId The session ID
     */
    public void checkForDomainChange(UUID sessionId) {
        BrowserInstance browser = getBrowserInstance(sessionId);
        if (browser == null) return;

        try {
            // First check if recorder is still active
            Boolean isRecorderActive = false;

            try {
                isRecorderActive = (Boolean) browser.executeScript(
                        "return typeof window.__csRecorderActive !== 'undefined' && window.__csRecorderActive === true;"
                );
            } catch (Exception e) {
                logger.debug("Could not check recorder status: {}", e.getMessage());
            }

            // If recorder is not active, reinject regardless of domain
            if (Boolean.FALSE.equals(isRecorderActive)) {
                logger.info("Recorder not active on current page, reinjecting");
                injectRecorderScriptWithRetry(sessionId, MAX_INJECTION_RETRIES);
                return;
            }

            // Get current domain from browser
            String currentDomain = "";
            String newDomain = "";

            try {
                currentDomain = (String) browser.executeScript("return window.__csCurrentDomain || '';");
                String url = browser.getCurrentUrl();
                newDomain = extractDomain(url);

                // Update the domain in browser
                browser.executeScript("window.__csCurrentDomain = '" + newDomain + "';");
            } catch (Exception e) {
                logger.warn("Could not get domain information: {}", e.getMessage());
                return;
            }

            // Check if domain has changed
            if (!newDomain.equals(currentDomain) && !newDomain.isEmpty() && !currentDomain.isEmpty()) {
                logger.info("Domain changed from {} to {}, reinjecting recorder", currentDomain, newDomain);
                injectRecorderScriptWithRetry(sessionId, MAX_INJECTION_RETRIES);
            }

        } catch (Exception e) {
            logger.warn("Could not check for domain change: {}", e.getMessage());
        }
    }

    /**
     * Extract domain from URL for comparison
     */
    private String extractDomain(String url) {
        try {
            if (url == null || url.isEmpty()) return "";
            if (url.equals("about:blank")) return "about:blank";

            String domain = url;
            // Remove protocol
            if (domain.contains("://")) {
                domain = domain.split("://")[1];
            }
            // Remove path and query
            if (domain.contains("/")) {
                domain = domain.split("/")[0];
            }
            return domain;
        } catch (Exception e) {
            logger.warn("Error extracting domain from URL {}: {}", url, e.getMessage());
            return url; // Return original if parsing fails
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

        BrowserInstance browser = activeBrowsers.get(sessionId);
        if (browser != null) {
            try {
                // First, try to execute a cleanup script
                try {
                    browser.executeScript(
                            "if (window.__csRecorderActive) {\n" +
                                    "  window.__csRecorderActive = false;\n" +
                                    "  window.__csRecorderPaused = true;\n" +
                                    "  window.__csRecorder = null;\n" +
                                    "  const indicator = document.getElementById('cs-recorder-indicator');\n" +
                                    "  if (indicator && indicator.parentNode) indicator.parentNode.removeChild(indicator);\n" +
                                    "  console.debug('CSTestForge: Recorder cleanup complete');\n" +
                                    "}");
                } catch (Exception e) {
                    logger.warn("Error executing cleanup script: {}", e.getMessage());
                }

                // Then stop the browser
                browser.stop();
                logger.info("Browser stopped for session: {}", sessionId);
            } catch (Exception e) {
                logger.error("Error stopping browser for session {}: {}", sessionId, e.getMessage());
            } finally {
                // Always remove from active browsers
                activeBrowsers.remove(sessionId);
            }
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
            logger.info("Navigating to URL: {} for session: {}", url, sessionId);

            // Get current domain before navigation for comparison
            String currentDomain = "";
            try {
                currentDomain = extractDomain(browser.getCurrentUrl());
            } catch (Exception e) {
                logger.debug("Could not get current domain before navigation: {}", e.getMessage());
            }

            boolean success = browser.navigate(url);

            if (success) {
                // Wait longer for page to load before injecting script
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Get new domain after navigation
                String newDomain = "";
                try {
                    newDomain = extractDomain(browser.getCurrentUrl());
                } catch (Exception e) {
                    logger.debug("Could not get new domain after navigation: {}", e.getMessage());
                }

                // If domain has changed, we need to reinject the script
                boolean domainChanged = !currentDomain.equals(newDomain) &&
                        !currentDomain.isEmpty() &&
                        !newDomain.isEmpty();

                if (domainChanged) {
                    logger.info("Domain changed from {} to {} during navigation, reinjecting recorder",
                            currentDomain, newDomain);

                    // Update the domain in browser
                    browser.executeScript("window.__csCurrentDomain = '" + newDomain + "';");

                    // For Chrome browser, apply CSP bypasses after navigation
                    if (browser instanceof ChromeBrowserInstance) {
                        ChromeBrowserInstance chromeBrowser = (ChromeBrowserInstance) browser;
                        try {
                            chromeBrowser.applyCSPBypass();
                            chromeBrowser.disableCSP();
                            chromeBrowser.fixCORSIssues(serverHost, serverPort);
                        } catch (Exception e) {
                            logger.warn("Error applying CSP bypasses after navigation: {}", e.getMessage());
                        }
                    }

                    // Reinject the recorder script
                    injectRecorderScriptWithRetry(sessionId, MAX_INJECTION_RETRIES);
                } else {
                    // Check if recorder is still active, reinject if needed
                    Boolean isActive = false;
                    try {
                        isActive = (Boolean) browser.executeScript(
                                "return typeof window.__csRecorderActive !== 'undefined' && window.__csRecorderActive === true;"
                        );
                    } catch (Exception e) {
                        logger.debug("Could not check recorder status after navigation: {}", e.getMessage());
                    }

                    if (Boolean.FALSE.equals(isActive)) {
                        logger.info("Recorder not active after navigation, reinjecting");
                        injectRecorderScriptWithRetry(sessionId, MAX_INJECTION_RETRIES);
                    }
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
     * Injects the recorder script using multiple injection strategies with retry
     * Similar to setupScriptInjection() in the reference
     *
     * @param sessionId The session ID
     * @param maxRetries Maximum number of retries
     * @return true if successful with any strategy
     */
    public boolean injectRecorderScriptWithRetry(UUID sessionId, int maxRetries) {
        BrowserInstance browser = getBrowserInstance(sessionId);
        if (browser == null) {
            logger.error("Cannot inject recorder script - no browser found for session: {}", sessionId);
            return false;
        }

        logger.info("Attempting script injection with up to {} retries for session: {}", maxRetries, sessionId);

        // Apply CSP bypass first for better chance of success
        if (browser instanceof ChromeBrowserInstance) {
            ChromeBrowserInstance chromeBrowser = (ChromeBrowserInstance) browser;
            try {
                chromeBrowser.applyCSPBypass();
                chromeBrowser.disableCSP();
                chromeBrowser.fixCORSIssues(serverHost, serverPort);
            } catch (Exception e) {
                logger.warn("Error applying CSP bypasses before script injection: {}", e.getMessage());
            }
        }

        // Try different strategies in order with retries
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            logger.debug("Script injection attempt {}/{} for session {}", attempt + 1, maxRetries, sessionId);

            try {
                // Small delay between attempts
                if (attempt > 0) {
                    Thread.sleep(1000);
                }

                // Strategy 1: Use ChromeBrowserInstance specific injection if available
                if (browser instanceof ChromeBrowserInstance) {
                    logger.debug("Trying Chrome-specific injection...");
                    ChromeBrowserInstance chromeBrowser = (ChromeBrowserInstance) browser;

                    boolean success = chromeBrowser.injectRecorderScriptWithServerDetails(
                            sessionId.toString(), serverHost, serverPort, contextPath);

                    if (success) {
                        logger.info("Chrome-specific script injection successful on attempt {}", attempt + 1);
                        return true;
                    }
                }

                // Strategy 2: Try ultra-compatible script
                logger.debug("Trying ultra-compatible script injection...");
                boolean success = injectUltraCompatibleScript(sessionId);

                if (success) {
                    logger.info("Ultra-compatible script injection successful on attempt {}", attempt + 1);
                    return true;
                }

                // Strategy 3: Try minimal recorder script as fallback
                logger.debug("Trying minimal script injection...");
                success = injectMinimalRecorderScript(sessionId);

                if (success) {
                    logger.info("Minimal script injection successful on attempt {}", attempt + 1);
                    return true;
                }

                // Strategy 4: Try full recorder script as last resort
                logger.debug("Trying full script injection...");
                success = injectRecorderScript(sessionId);

                if (success) {
                    logger.info("Full script injection successful on attempt {}", attempt + 1);
                    return true;
                }

                // If all strategies failed but we have more retries, try fixing CSP again
                if (attempt < maxRetries - 1 && browser instanceof ChromeBrowserInstance) {
                    ChromeBrowserInstance chromeBrowser = (ChromeBrowserInstance) browser;
                    try {
                        chromeBrowser.applyCSPBypass();
                        chromeBrowser.disableCSP();
                        chromeBrowser.fixCORSIssues(serverHost, serverPort);

                        // Add more aggressive CSP bypass using script tag
                        String bypassScript =
                                "try {\n" +
                                        "  // Add meta tag to disable CSP\n" +
                                        "  const meta = document.createElement('meta');\n" +
                                        "  meta.httpEquiv = 'Content-Security-Policy';\n" +
                                        "  meta.content = \"default-src * 'unsafe-inline' 'unsafe-eval' data: blob:; " +
                                        "connect-src * 'unsafe-inline' 'unsafe-eval' data: blob:;\";\n" +
                                        "  document.head.insertBefore(meta, document.head.firstChild);\n" +
                                        "  return true;\n" +
                                        "} catch(e) {\n" +
                                        "  console.error('Error adding CSP bypass:', e);\n" +
                                        "  return false;\n" +
                                        "}";

                        browser.executeScript(bypassScript);
                    } catch (Exception e) {
                        logger.warn("Error applying additional CSP bypasses: {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                logger.error("Error during script injection attempt {}: {}", attempt + 1, e.getMessage());
                // Continue to next retry
            }
        }

        logger.error("All script injection attempts failed for session {}", sessionId);
        return false;
    }

    /**
     * Ultra-compatible script that works with strict CSP sites
     * Improved version with absolute URLs to localhost server
     */
    public boolean injectUltraCompatibleScript(UUID sessionId) {
        BrowserInstance browser = getBrowserInstance(sessionId);
        if (browser == null) {
            logger.error("Cannot inject ultra-compatible script - no browser found for session: {}", sessionId);
            return false;
        }

        try {
            // Ensure contextPath starts with /
            String ctxPath = contextPath;
            if (!ctxPath.startsWith("/")) {
                ctxPath = "/" + ctxPath;
            }

            // Use absolute URL to avoid CSP issues
            String apiUrl = "http://" + serverHost + ":" + serverPort + ctxPath + "/api/recorder/events/" + sessionId.toString();

            // Create a super-simple script with no iframes or complex communication
            String script =
                    "(function() {\n" +
                            "  if (window.__csRecorderActive === true) {\n" +
                            "    console.debug('CSTestForge: Recorder already active, skipping');\n" +
                            "    return true;\n" +
                            "  }\n" +
                            "  \n" +
                            "  // Mark as active\n" +
                            "  window.__csRecorderActive = true;\n" +
                            "  window.__csRecorderSessionId = '" + sessionId.toString() + "';\n" +
                            "  window.__csServerHost = '" + serverHost + "';\n" +
                            "  window.__csServerPort = '" + serverPort + "';\n" +
                            "  window.__csContextPath = '" + ctxPath + "';\n" +
                            "  window.__csApiEndpoint = '" + apiUrl + "';\n" +
                            "  \n" +
                            "  // Create a minimal UI indicator\n" +
                            "  const ui = document.createElement('div');\n" +
                            "  ui.id = 'cs-recorder-indicator';\n" +
                            "  ui.style.cssText = 'position:fixed;top:10px;right:10px;background:#333;color:#fff;padding:8px 12px;z-index:2147483647;border-radius:4px;font-family:Arial;box-shadow:0 2px 8px rgba(0,0,0,0.2);display:flex;align-items:center;gap:8px;';\n" +
                            "  ui.innerHTML = '<div style=\"width:10px;height:10px;background:red;border-radius:50%;\"></div>CSTestForge Recording';\n" +
                            "  \n" +
                            "  // Add pause/stop buttons\n" +
                            "  const buttonsDiv = document.createElement('div');\n" +
                            "  buttonsDiv.style.cssText = 'margin-left:10px;display:flex;gap:5px;';\n" +
                            "  \n" +
                            "  const pauseBtn = document.createElement('button');\n" +
                            "  pauseBtn.textContent = '⏸️';\n" +
                            "  pauseBtn.style.cssText = 'background:none;border:none;color:white;cursor:pointer;font-size:14px;padding:0 5px;';\n" +
                            "  pauseBtn.onclick = function(e) {\n" +
                            "    e.stopPropagation();\n" +
                            "    if (window.__csRecorder && window.__csRecorder.status === 'recording') {\n" +
                            "      window.__csRecorder.status = 'paused';\n" +
                            "      pauseBtn.textContent = '▶️';\n" +
                            "      ui.style.backgroundColor = '#666';\n" +
                            "      sendEvent({type: 'RECORDER_CONTROL', action: 'PAUSE'});\n" +
                            "    } else {\n" +
                            "      window.__csRecorder = window.__csRecorder || {};\n" +
                            "      window.__csRecorder.status = 'recording';\n" +
                            "      pauseBtn.textContent = '⏸️';\n" +
                            "      ui.style.backgroundColor = '#333';\n" +
                            "      sendEvent({type: 'RECORDER_CONTROL', action: 'RESUME'});\n" +
                            "    }\n" +
                            "  };\n" +
                            "  \n" +
                            "  const stopBtn = document.createElement('button');\n" +
                            "  stopBtn.textContent = '⏹️';\n" +
                            "  stopBtn.style.cssText = 'background:none;border:none;color:white;cursor:pointer;font-size:14px;padding:0 5px;';\n" +
                            "  stopBtn.onclick = function(e) {\n" +
                            "    e.stopPropagation();\n" +
                            "    window.__csRecorder = window.__csRecorder || {};\n" +
                            "    window.__csRecorder.status = 'stopped';\n" +
                            "    sendEvent({type: 'RECORDER_CONTROL', action: 'STOP'});\n" +
                            "    ui.style.backgroundColor = '#666';\n" +
                            "    setTimeout(function() {\n" +
                            "      if (ui.parentNode) ui.parentNode.removeChild(ui);\n" +
                            "      window.__csRecorderActive = false;\n" +
                            "    }, 3000);\n" +
                            "  };\n" +
                            "  \n" +
                            "  buttonsDiv.appendChild(pauseBtn);\n" +
                            "  buttonsDiv.appendChild(stopBtn);\n" +
                            "  ui.appendChild(buttonsDiv);\n" +
                            "  \n" +
                            "  // Append to body or wait until body is available\n" +
                            "  function appendUI() {\n" +
                            "    if (document.body) {\n" +
                            "      document.body.appendChild(ui);\n" +
                            "    } else {\n" +
                            "      setTimeout(appendUI, 100);\n" +
                            "    }\n" +
                            "  }\n" +
                            "  appendUI();\n" +
                            "  \n" +
                            "  // Initialize recorder state\n" +
                            "  window.__csRecorder = {\n" +
                            "    status: 'recording',\n" +
                            "    sessionId: '" + sessionId.toString() + "',\n" +
                            "    version: '1.0-csp-compatible',\n" +
                            "    startTime: new Date().toISOString()\n" +
                            "  };\n" +
                            "  \n" +
                            "  // Set up XHR-based communication (no iframes)\n" +
                            "  function sendEvent(eventData) {\n" +
                            "    try {\n" +
                            "      // Add required data\n" +
                            "      eventData.sessionId = '" + sessionId.toString() + "';\n" +
                            "      eventData.timestamp = eventData.timestamp || new Date().getTime();\n" +
                            "      eventData.url = window.location.href;\n" +
                            "      eventData.title = document.title;\n" +
                            "      \n" +
                            "      // Create XHR object\n" +
                            "      const xhr = new XMLHttpRequest();\n" +
                            "      xhr.open('POST', window.__csApiEndpoint, true);\n" +
                            "      xhr.setRequestHeader('Content-Type', 'application/json');\n" +
                            "      xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');\n" +
                            "      \n" +
                            "      // Add error handler\n" +
                            "      xhr.onerror = function() {\n" +
                            "        console.error('CSTestForge: Failed to send event - network error');\n" +
                            "      };\n" +
                            "      \n" +
                            "      // Send the data\n" +
                            "      xhr.send(JSON.stringify(eventData));\n" +
                            "      \n" +
                            "      return true;\n" +
                            "    } catch(e) {\n" +
                            "      console.error('CSTestForge: Failed to send event:', e);\n" +
                            "      return false;\n" +
                            "    }\n" +
                            "  }\n" +
                            "  \n" +
                            "  // Track clicks\n" +
                            "  document.addEventListener('click', function(e) {\n" +
                            "    if (window.__csRecorder && window.__csRecorder.status !== 'recording') return;\n" +
                            "    \n" +
                            "    const target = e.target;\n" +
                            "    const elementInfo = {\n" +
                            "      tagName: target.tagName.toLowerCase(),\n" +
                            "      id: target.id || null,\n" +
                            "      name: target.name || null,\n" +
                            "      className: target.className || null,\n" +
                            "      text: target.textContent ? target.textContent.trim().substring(0, 100) : null\n" +
                            "    };\n" +
                            "    \n" +
                            "    sendEvent({\n" +
                            "      type: 'CLICK',\n" +
                            "      elementInfo: elementInfo,\n" +
                            "      ctrlKey: e.ctrlKey,\n" +
                            "      altKey: e.altKey,\n" +
                            "      shiftKey: e.shiftKey\n" +
                            "    });\n" +
                            "  }, true);\n" +
                            "  \n" +
                            "  // Track form submissions\n" +
                            "  document.addEventListener('submit', function(e) {\n" +
                            "    if (window.__csRecorder && window.__csRecorder.status !== 'recording') return;\n" +
                            "    \n" +
                            "    const target = e.target;\n" +
                            "    const formData = {};\n" +
                            "    \n" +
                            "    if (target && target.elements) {\n" +
                            "      for (let i = 0; i < target.elements.length; i++) {\n" +
                            "        const element = target.elements[i];\n" +
                            "        if (element.name && element.value !== undefined) {\n" +
                            "          if (element.type === 'password') {\n" +
                            "            formData[element.name] = '********';\n" +
                            "          } else {\n" +
                            "            formData[element.name] = element.value;\n" +
                            "          }\n" +
                            "        }\n" +
                            "      }\n" +
                            "    }\n" +
                            "    \n" +
                            "    sendEvent({\n" +
                            "      type: 'FORM_SUBMIT',\n" +
                            "      elementInfo: {\n" +
                            "        tagName: target.tagName.toLowerCase(),\n" +
                            "        id: target.id || null,\n" +
                            "        name: target.name || null\n" +
                            "      },\n" +
                            "      formData: formData\n" +
                            "    });\n" +
                            "  }, true);\n" +
                            "  \n" +
                            "  // Track input changes\n" +
                            "  document.addEventListener('input', function(e) {\n" +
                            "    if (window.__csRecorder && window.__csRecorder.status !== 'recording') return;\n" +
                            "    \n" +
                            "    const target = e.target;\n" +
                            "    \n" +
                            "    // Skip if not an input element\n" +
                            "    if (!target || !['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName)) {\n" +
                            "      return;\n" +
                            "    }\n" +
                            "    \n" +
                            "    // Debounce inputs to avoid excessive events\n" +
                            "    if (target.__csInputTimeout) {\n" +
                            "      clearTimeout(target.__csInputTimeout);\n" +
                            "    }\n" +
                            "    \n" +
                            "    target.__csInputTimeout = setTimeout(function() {\n" +
                            "      const value = target.type === 'password' ? '********' : target.value;\n" +
                            "      \n" +
                            "      sendEvent({\n" +
                            "        type: 'INPUT',\n" +
                            "        value: value,\n" +
                            "        elementInfo: {\n" +
                            "          tagName: target.tagName.toLowerCase(),\n" +
                            "          id: target.id || null,\n" +
                            "          name: target.name || null,\n" +
                            "          type: target.type || null\n" +
                            "        }\n" +
                            "      });\n" +
                            "    }, 500);\n" +
                            "  }, true);\n" +
                            "  \n" +
                            "  // Send initial event\n" +
                            "  sendEvent({\n" +
                            "    type: 'INIT',\n" +
                            "    userAgent: navigator.userAgent,\n" +
                            "    viewport: {\n" +
                            "      width: window.innerWidth,\n" +
                            "      height: window.innerHeight\n" +
                            "    }\n" +
                            "  });\n" +
                            "  \n" +
                            "  console.debug('CSTestForge: CSP-compatible recorder setup complete');\n" +
                            "  return true;\n" +
                            "})();";

            // Execute the script directly
            Object result = browser.executeScript(script);

            if (result == null) {
                logger.warn("Ultra-compatible script execution returned null");
                return false;
            }

            logger.debug("Ultra-compatible script execution result: {}", result);

            // Wait a moment for the script to execute and setup
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Verify injection
            boolean verified = verifyUltraCompatibleInjection(browser);
            logger.debug("Ultra-compatible script verification result: {}", verified);
            return verified;
        } catch (Exception e) {
            logger.error("Failed to inject ultra-compatible script for session {}: {}", sessionId, e.getMessage());
            return false;
        }
    }

    /**
     * Verify ultra-compatible script was injected successfully
     */
    private boolean verifyUltraCompatibleInjection(BrowserInstance browser) {
        try {
            // Check for the presence of the __csRecorderActive flag
            Object activeFlag = browser.executeScript(
                    "return typeof window.__csRecorderActive === 'boolean' ? window.__csRecorderActive : false;");

            logger.debug("Ultra-compatible recorder active flag: {}", activeFlag);

            // Check for the presence of the UI indicator
            Object uiExists = browser.executeScript(
                    "return document.getElementById('cs-recorder-indicator') !== null;");

            logger.debug("Ultra-compatible UI indicator exists: {}", uiExists);

            // Check for the __csRecorder object
            Object recorderExists = browser.executeScript(
                    "return window.__csRecorder && typeof window.__csRecorder === 'object';");

            logger.debug("Ultra-compatible recorder object exists: {}", recorderExists);

            return Boolean.TRUE.equals(activeFlag) &&
                    (Boolean.TRUE.equals(uiExists) || Boolean.TRUE.equals(recorderExists));
        } catch (Exception e) {
            logger.error("Error verifying ultra-compatible script injection: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Inject a minimal recorder script as a fallback
     */
    public boolean injectMinimalRecorderScript(UUID sessionId) {
        BrowserInstance browser = getBrowserInstance(sessionId);
        if (browser == null) {
            logger.error("Cannot inject minimal recorder script - no browser found for session: {}", sessionId);
            return false;
        }

        try {
            // Ensure contextPath starts with /
            String ctxPath = contextPath;
            if (!ctxPath.startsWith("/")) {
                ctxPath = "/" + ctxPath;
            }

            // Use absolute URL to avoid CSP issues
            String apiUrl = "http://" + serverHost + ":" + serverPort + ctxPath + "/api/recorder/events/" + sessionId.toString();

            String minimalScript =
                    "(function() {\n" +
                            "  window.__csRecorderActive = true;\n" +
                            "  window.__csRecorderSessionId = '" + sessionId + "';\n" +
                            "  console.log('CSTestForge: Minimal recorder initialized');\n" +
                            "  \n" +
                            "  // Create minimal UI with buttons clearly visible\n" +
                            "  function safeAppendUI() {\n" +
                            "    try {\n" +
                            "      // First check if UI already exists\n" +
                            "      const existingUI = document.getElementById('cs-recorder-indicator');\n" +
                            "      if (existingUI) {\n" +
                            "        console.debug('CSTestForge: UI indicator already exists');\n" +
                            "        return; // UI already exists, don't create a duplicate\n" +
                            "      }\n" +
                            "      \n" +
                            "      // Create parent container\n" +
                            "      const ui = document.createElement('div');\n" +
                            "      ui.id = 'cs-recorder-indicator';\n" +
                            "      ui.style.cssText = 'position:fixed;top:10px;right:10px;background:#333;color:#fff;padding:8px;' +\n" +
                            "                         'z-index:2147483647;border-radius:4px;font-family:Arial;display:flex;' +\n" +
                            "                         'flex-direction:row;align-items:center;min-width:220px;box-shadow:0 2px 5px rgba(0,0,0,0.3);';\n" +
                            "      \n" +
                            "      // Create a colored dot indicator\n" +
                            "      const dot = document.createElement('div');\n" +
                            "      dot.style.cssText = 'width:10px;height:10px;background:red;border-radius:50%;margin-right:8px;';\n" +
                            "      ui.appendChild(dot);\n" +
                            "      \n" +
                            "      // Create label in a container\n" +
                            "      const labelContainer = document.createElement('div');\n" +
                            "      labelContainer.style.cssText = 'flex:1;';\n" +
                            "      const label = document.createElement('span');\n" +
                            "      label.textContent = 'CSTestForge Recording';\n" +
                            "      labelContainer.appendChild(label);\n" +
                            "      ui.appendChild(labelContainer);\n" +
                            "      \n" +
                            "      // Create buttons container\n" +
                            "      const buttonsContainer = document.createElement('div');\n" +
                            "      buttonsContainer.style.cssText = 'display:flex;gap:8px;margin-left:10px;';\n" +
                            "      \n" +
                            "      // Create stop button\n" +
                            "      const stopBtn = document.createElement('button');\n" +
                            "      stopBtn.innerHTML = '&#x23F9;'; // Stop symbol\n" +
                            "      stopBtn.title = 'Stop Recording';\n" +
                            "      stopBtn.style.cssText = 'background:#f44336;color:white;border:none;border-radius:3px;' +\n" +
                            "                               'cursor:pointer;font-size:14px;width:28px;height:28px;display:flex;' +\n" +
                            "                               'justify-content:center;align-items:center;';\n" +
                            "      stopBtn.onclick = function(e) {\n" +
                            "        e.stopPropagation();\n" +
                            "        console.log('Stop recording clicked');\n" +
                            "        window.__csRecorderActive = false;\n" +
                            "        // Send stop event\n" +
                            "        __csSendEvent({\n" +
                            "          type: 'RECORDER_CONTROL',\n" +
                            "          action: 'STOP'\n" +
                            "        });\n" +
                            "        ui.style.backgroundColor = '#666';\n" +
                            "        label.textContent = 'Recording stopped';\n" +
                            "        dot.style.backgroundColor = '#666';\n" +
                            "        setTimeout(function() {\n" +
                            "          if (ui && ui.parentNode) {\n" +
                            "            ui.parentNode.removeChild(ui);\n" +
                            "          }\n" +
                            "        }, 2000);\n" +
                            "      };\n" +
                            "      buttonsContainer.appendChild(stopBtn);\n" +
                            "      ui.appendChild(buttonsContainer);\n" +
                            "      \n" +
                            "      if (document.body) {\n" +
                            "        document.body.appendChild(ui);\n" +
                            "      } else {\n" +
                            "        setTimeout(safeAppendUI, 1000); // Try again later if body isn't ready\n" +
                            "      }\n" +
                            "    } catch(e) {\n" +
                            "      console.error('CSTestForge: Error creating UI:', e);\n" +
                            "    }\n" +
                            "  }\n" +
                            "  \n" +
                            "  // Safely create the UI\n" +
                            "  safeAppendUI();\n" +
                            "  \n" +
                            "  // Set up a safer mutation observer that doesn't try to access removed nodes\n" +
                            "  if (window.MutationObserver) {\n" +
                            "    var safeObserver = new MutationObserver(function(mutations) {\n" +
                            "      try {\n" +
                            "        // Just check if our recorder UI is still in the document\n" +
                            "        if (!document.getElementById('cs-recorder-indicator') && window.__csRecorderActive) {\n" +
                            "          console.debug('CSTestForge: UI element removed, recreating');\n" +
                            "          safeAppendUI();\n" +
                            "        }\n" +
                            "      } catch(e) {\n" +
                            "        console.error('CSTestForge: Error in mutation observer:', e);\n" +
                            "      }\n" +
                            "    });\n" +
                            "    \n" +
                            "    // Start observing - only observe the body to avoid most errors\n" +
                            "    if (document.body) {\n" +
                            "      safeObserver.observe(document.body, { childList: true, subtree: true });\n" +
                            "    } else {\n" +
                            "      // Wait for body to be available\n" +
                            "      document.addEventListener('DOMContentLoaded', function() {\n" +
                            "        safeObserver.observe(document.body, { childList: true, subtree: true });\n" +
                            "      });\n" +
                            "    }\n" +
                            "  }\n" +
                            "  \n" +
                            "  // Send events via HTTP directly with absolute URL and safer error handling\n" +
                            "  window.__csSendEvent = function(eventData) {\n" +
                            "    try {\n" +
                            "      const xhr = new XMLHttpRequest();\n" +
                            "      xhr.open('POST', '" + apiUrl + "', true);\n" +
                            "      xhr.setRequestHeader('Content-Type', 'application/json');\n" +
                            "      xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');\n" +
                            "      \n" +
                            "      // Ensure we never send HEARTBEAT events to avoid 400 errors\n" +
                            "      if (eventData.type === 'HEARTBEAT') {\n" +
                            "        eventData.type = 'RECORDER_STATUS'; // Use an existing type that's in the enum\n" +
                            "      }\n" +
                            "      \n" +
                            "      // Add session ID if not already present\n" +
                            "      eventData.sessionId = '" + sessionId + "';\n" +
                            "      // Add timestamp if not already present\n" +
                            "      if (!eventData.timestamp) {\n" +
                            "        eventData.timestamp = new Date().getTime();\n" +
                            "      }\n" +
                            "      // Add URL and title if not already present\n" +
                            "      if (!eventData.url) {\n" +
                            "        eventData.url = window.location.href;\n" +
                            "      }\n" +
                            "      if (!eventData.title) {\n" +
                            "        eventData.title = document.title;\n" +
                            "      }\n" +
                            "      \n" +
                            "      xhr.send(JSON.stringify(eventData));\n" +
                            "    } catch(e) {\n" +
                            "      console.error('Failed to send event:', e);\n" +
                            "    }\n" +
                            "  };\n" +
                            "  \n" +
                            "  // Send init event\n" +
                            "  __csSendEvent({\n" +
                            "    type: 'INIT',\n" +
                            "    url: window.location.href,\n" +
                            "    title: document.title,\n" +
                            "    userAgent: navigator.userAgent\n" +
                            "  });\n" +
                            "  \n" +
                            "  // Setup event listeners for user interactions\n" +
                            "  document.addEventListener('click', function(e) {\n" +
                            "    if (!window.__csRecorderActive) return;\n" +
                            "    \n" +
                            "    // Don't record clicks on our own UI\n" +
                            "    if (e.target.closest('#cs-recorder-indicator')) return;\n" +
                            "    \n" +
                            "    __csSendEvent({\n" +
                            "      type: 'CLICK',\n" +
                            "      elementInfo: {\n" +
                            "        tagName: e.target.tagName.toLowerCase(),\n" +
                            "        id: e.target.id || null,\n" +
                            "        className: e.target.className || null,\n" +
                            "        text: e.target.textContent ? e.target.textContent.trim().substring(0, 100) : null\n" +
                            "      },\n" +
                            "      url: window.location.href\n" +
                            "    });\n" +
                            "  }, true);\n" +
                            "  \n" +
                            "  console.debug('CSTestForge: Minimal recorder setup complete with safer handlers');\n" +
                            "  return 'Minimal recorder initialized';\n" +
                            "})();";

            Object result = browser.executeScript(minimalScript);
            logger.debug("Minimal script execution result: {}", result);

            // Wait a moment for the script to execute and setup
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Verify injection
            boolean verified = verifyMinimalScriptInjection(browser);
            logger.debug("Minimal script verification result: {}", verified);
            return verified;

        } catch (Exception e) {
            logger.error("Failed to inject minimal recorder script for session {}: {}", sessionId, e.getMessage());
            return false;
        }
    }

    /**
     * Verify that the minimal script was properly injected and initialized
     *
     * @param browser The browser instance to check
     * @return true if script is active
     */
    private boolean verifyMinimalScriptInjection(BrowserInstance browser) {
        try {
            // Check for the presence of the __csRecorderActive flag
            Object activeFlag = browser.executeScript(
                    "return typeof window.__csRecorderActive === 'boolean' ? window.__csRecorderActive : false;");

            logger.debug("Minimal recorder active flag: {}", activeFlag);

            // Check for the presence of the UI indicator
            Object uiExists = browser.executeScript(
                    "return document.getElementById('cs-recorder-indicator') !== null;");

            logger.debug("Minimal UI indicator exists: {}", uiExists);

            // Check for the __csSendEvent function
            Object sendEventExists = browser.executeScript(
                    "return typeof window.__csSendEvent === 'function';");

            logger.debug("Send event function exists: {}", sendEventExists);

            return Boolean.TRUE.equals(activeFlag) &&
                    (Boolean.TRUE.equals(uiExists) || Boolean.TRUE.equals(sendEventExists));
        } catch (Exception e) {
            logger.error("Error verifying minimal script injection: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Inject the full recorder script into the browser
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

            // Get the server's context path from properties (default to /cstestforge)
            String ctxPath = contextPath;
            // Ensure it starts with a slash
            if (!ctxPath.startsWith("/")) {
                ctxPath = "/" + ctxPath;
            }

            // Format the WebSocket URL - use absolute URL to avoid CSP issues
            String wsUrl = String.format("http://%s:%s%s/ws-recorder",
                    serverHost, serverPort, ctxPath);

            logger.debug("WebSocket base URL: {}", wsUrl);
            script = script.replace("__WS_URL__", wsUrl);

            // Add server host and port to the script for use in API calls
            script = script.replace("window.__csServerHost = null;",
                    "window.__csServerHost = '" + serverHost + "';");
            script = script.replace("window.__csServerPort = null;",
                    "window.__csServerPort = '" + serverPort + "';");
            script = script.replace("window.__csContextPath = null;","window.__csContextPath = '" + ctxPath + "';");

            // Force HTTP fallback mode instead of WebSockets to avoid connection issues
            script = script.replace("window.__csRecorderUsingHttpFallback = false",
                    "window.__csRecorderUsingHttpFallback = true");

            // Replace API endpoints to use absolute URLs
            String apiEndpoint = "http://" + serverHost + ":" + serverPort + ctxPath + "/api/recorder/events/" + sessionId.toString();
            script = script.replace("var apiEndpoint = contextPath + '/api/recorder/events/' + sessionId;",
                    "var apiEndpoint = '" + apiEndpoint + "';");

            // Execute the script with most robust method available for browser type
            if (browser instanceof ChromeBrowserInstance) {
                ChromeBrowserInstance chromeBrowser = (ChromeBrowserInstance) browser;

                // Apply CSP bypasses before injecting script
                chromeBrowser.applyCSPBypass();
                chromeBrowser.disableCSP();
                chromeBrowser.fixCORSIssues(serverHost, serverPort);

                // Inject the script using the enhanced method
                Object result = chromeBrowser.injectScript(script);

                if (result != null) {
                    logger.info("Successfully injected recorder script using ChromeBrowserInstance for session: {}", sessionId);

                    // Verify the script was actually injected
                    boolean success = verifyScriptInjection(browser);

                    if (!success) {
                        logger.warn("Script injection verification failed, script may not be running properly");
                    }

                    return success;
                } else {
                    logger.error("Script injection failed for session: {}", sessionId);
                    return false;
                }
            } else {
                // For other browser types, use the browser's injectScript method
                try {
                    // Try direct injection first
                    Object result = browser.injectScript(script);

                    boolean success = false;

                    if (result != null) {
                        logger.debug("Direct script injection result: {}", result);
                        // Verify the script was actually injected
                        success = verifyScriptInjection(browser);
                    } else {
                        logger.warn("Direct script injection returned null, trying alternative method");

                        // Try alternative method with executeScript
                        result = browser.executeScript(script);

                        if (result != null) {
                            logger.debug("Alternative script injection result: {}", result);
                            success = verifyScriptInjection(browser);
                        } else {
                            logger.warn("Alternative script injection failed, trying with script element");

                            // Try creating a script element as a last resort
                            String createScriptElem =
                                    "try {\n" +
                                            "  const script = document.createElement('script');\n" +
                                            "  script.id = 'cs-recorder-script';\n" +
                                            "  script.textContent = arguments[0];\n" +
                                            "  document.head.appendChild(script);\n" +
                                            "  return 'Script element created';\n" +
                                            "} catch(e) {\n" +
                                            "  return 'Error: ' + e.message;\n" +
                                            "}";

                            result = browser.executeScript(createScriptElem, script);
                            logger.debug("Script element creation result: {}", result);

                            // Wait a moment for the script to execute
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }

                            success = verifyScriptInjection(browser);
                        }
                    }

                    return success;
                } catch (Exception e) {
                    logger.error("Exception during script injection: {}", e.getMessage(), e);
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("Failed to inject recorder script for session {}: {}", sessionId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Verify that the script was properly injected and initialized
     *
     * @param browser The browser instance to check
     * @return true if script is active
     */
    private boolean verifyScriptInjection(BrowserInstance browser) {
        try {
            // Check for the presence of the __csRecorderActive flag
            Object activeFlag = browser.executeScript(
                    "return typeof window.__csRecorderActive === 'boolean' ? window.__csRecorderActive : false;");

            logger.debug("Recorder active flag: {}", activeFlag);

            // Check for the presence of the UI indicator
            Object uiExists = browser.executeScript(
                    "return document.getElementById('cs-recorder-indicator') !== null;");

            logger.debug("UI indicator exists: {}", uiExists);

            return Boolean.TRUE.equals(activeFlag);
        } catch (Exception e) {
            logger.error("Error verifying script injection: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Set up a mutation observer to track DOM changes and ensure recorder stays active
     *
     * @param sessionId The session ID
     * @return true if successfully set up
     */
    private boolean setupMutationObserver(UUID sessionId) {
        BrowserInstance browser = getBrowserInstance(sessionId);
        if (browser == null) {
            return false;
        }

        try {
            String observerScript =
                    "(function() {\n" +
                            "  if (window.__csMutationObserver) return 'Observer already exists';\n" +
                            "  \n" +
                            "  console.debug('CSTestForge: Setting up mutation observer');\n" +
                            "  \n" +
                            "  // Create safer mutation observer with error handling\n" +
                            "  window.__csMutationObserver = new MutationObserver(function(mutations) {\n" +
                            "    try {\n" +
                            "      // Check if recorder is still active after DOM changes\n" +
                            "      const uiExists = document.getElementById('cs-recorder-indicator') !== null;\n" +
                            "      const recorderActive = window.__csRecorderActive === true;\n" +
                            "      \n" +
                            "      if (!uiExists && recorderActive) {\n" +
                            "        console.debug('CSTestForge: Recorder UI missing, recreating');\n" +
                            "        // Dispatch custom event for handling\n" +
                            "        window.dispatchEvent(new CustomEvent('cs_recorder_needed'));\n" +
                            "      }\n" +
                            "    } catch(e) {\n" +
                            "      console.error('CSTestForge: Error in mutation observer:', e);\n" +
                            "    }\n" +
                            "  });\n" +
                            "  \n" +
                            "  // Start observing - only observe the body to avoid most errors\n" +
                            "  if (document.body) {\n" +
                            "    window.__csMutationObserver.observe(document.body, {\n" +
                            "      childList: true,\n" +
                            "      subtree: true,\n" +
                            "      attributes: false\n" +
                            "    });\n" +
                            "  } else {\n" +
                            "    // Wait for body to be available\n" +
                            "    document.addEventListener('DOMContentLoaded', function() {\n" +
                            "      if (window.__csMutationObserver) {\n" +
                            "        window.__csMutationObserver.observe(document.body, {\n" +
                            "          childList: true,\n" +
                            "          subtree: true,\n" +
                            "          attributes: false\n" +
                            "        });\n" +
                            "      }\n" +
                            "    });\n" +
                            "  }\n" +
                            "  \n" +
                            "  // Handle navigation events in SPAs\n" +
                            "  let lastUrl = window.location.href;\n" +
                            "  \n" +
                            "  // Monitor for URL changes\n" +
                            "  const urlCheckInterval = setInterval(function() {\n" +
                            "    if (!window.__csRecorderActive) {\n" +
                            "      clearInterval(urlCheckInterval);\n" +
                            "      return;\n" +
                            "    }\n" +
                            "    \n" +
                            "    if (window.location.href !== lastUrl) {\n" +
                            "      console.debug('CSTestForge: URL changed to ' + window.location.href);\n" +
                            "      \n" +
                            "      // Send navigation event - ensuring we convert HEARTBEAT events\n" +
                            "      try {\n" +
                            "        if (typeof window.__csSendEvent === 'function') {\n" +
                            "          const eventData = {\n" +
                            "            type: 'NAVIGATION',\n" +
                            "            sourceUrl: lastUrl,\n" +
                            "            targetUrl: window.location.href,\n" +
                            "            timestamp: new Date().getTime(),\n" +
                            "            url: window.location.href,\n" +
                            "            title: document.title,\n" +
                            "            trigger: 'URL_CHANGE'\n" +
                            "          };\n" +
                            "          \n" +
                            "          window.__csSendEvent(eventData);\n" +
                            "        }\n" +
                            "      } catch(e) {\n" +
                            "        console.error('CSTestForge: Error sending navigation event:', e);\n" +
                            "      }\n" +
                            "      \n" +
                            "      lastUrl = window.location.href;\n" +
                            "    }\n" +
                            "  }, 1000);\n" +
                            "  \n" +
                            "  // Monitor pushState and replaceState\n" +
                            "  const originalPushState = window.history.pushState;\n" +
                            "  window.history.pushState = function() {\n" +
                            "    originalPushState.apply(this, arguments);\n" +
                            "    if (window.__csRecorderActive) {\n" +
                            "      try {\n" +
                            "        if (typeof window.__csSendEvent === 'function') {\n" +
                            "          const eventData = {\n" +
                            "            type: 'NAVIGATION',\n" +
                            "            sourceUrl: lastUrl,\n" +
                            "            targetUrl: window.location.href,\n" +
                            "            timestamp: new Date().getTime(),\n" +
                            "            url: window.location.href,\n" +
                            "            title: document.title,\n" +
                            "            trigger: 'HISTORY_PUSHSTATE'\n" +
                            "          };\n" +
                            "          \n" +
                            "          window.__csSendEvent(eventData);\n" +
                            "        }\n" +
                            "      } catch(e) {\n" +
                            "        console.error('CSTestForge: Error sending navigation event:', e);\n" +
                            "      }\n" +
                            "      \n" +
                            "      lastUrl = window.location.href;\n" +
                            "    }\n" +
                            "  };\n" +
                            "  \n" +
                            "  const originalReplaceState = window.history.replaceState;\n" +
                            "  window.history.replaceState = function() {\n" +
                            "    originalReplaceState.apply(this, arguments);\n" +
                            "    if (window.__csRecorderActive) {\n" +
                            "      try {\n" +
                            "        if (typeof window.__csSendEvent === 'function') {\n" +
                            "          const eventData = {\n" +
                            "            type: 'NAVIGATION',\n" +
                            "            sourceUrl: lastUrl,\n" +
                            "            targetUrl: window.location.href,\n" +
                            "            timestamp: new Date().getTime(),\n" +
                            "            url: window.location.href,\n" +
                            "            title: document.title,\n" +
                            "            trigger: 'HISTORY_REPLACESTATE'\n" +
                            "          };\n" +
                            "          \n" +
                            "          window.__csSendEvent(eventData);\n" +
                            "        }\n" +
                            "      } catch(e) {\n" +
                            "        console.error('CSTestForge: Error sending navigation event:', e);\n" +
                            "      }\n" +
                            "      \n" +
                            "      lastUrl = window.location.href;\n" +
                            "    }\n" +
                            "  };\n" +
                            "  \n" +
                            "  // Handle browser closing event \n" +
                            "  window.addEventListener('beforeunload', function() {\n" +
                            "    try {\n" +
                            "      if (window.__csRecorderActive && typeof window.__csSendEvent === 'function') {\n" +
                            "        window.__csSendEvent({\n" +
                            "          type: 'RECORDER_CONTROL',\n" +
                            "          action: 'BROWSER_CLOSING',\n" +
                            "          url: window.location.href,\n" +
                            "          title: document.title\n" +
                            "        });\n" +
                            "      }\n" +
                            "    } catch(e) {\n" +
                            "      // Ignore errors during unload\n" +
                            "    }\n" +
                            "  });\n" +
                            "  \n" +
                            "  return 'Mutation observer set up successfully';\n" +
                            "})();";

            Object result = browser.executeScript(observerScript);
            logger.debug("Mutation observer setup result: {}", result);

            return result != null;
        } catch (Exception e) {
            logger.error("Failed to set up mutation observer for session {}: {}", sessionId, e.getMessage());
            return false;
        }
    }

    /**
     * Add a listener for the browser closing event
     *
     * @param sessionId The session ID
     */
    private void setupBrowserCloseListener(UUID sessionId) {
        BrowserInstance browser = getBrowserInstance(sessionId);
        if (browser == null) return;

        try {
            String closeListenerScript =
                    "(function() {\n" +
                            "  if (window.__csBrowserCloseListenerActive) return 'Listener already active';\n" +
                            "  window.__csBrowserCloseListenerActive = true;\n" +
                            "  \n" +
                            "  // Create a better way to handle browser closing events\n" +
                            "  function sendBeaconOrXhr(eventData) {\n" +
                            "    try {\n" +
                            "      // Try navigator.sendBeacon first (more reliable for beforeunload)\n" +
                            "      const apiUrl = '" + "http://" + serverHost + ":" + serverPort + contextPath + "/api/recorder/events/" + sessionId.toString() + "';\n" +
                            "      \n" +
                            "      // Ensure we never send HEARTBEAT events\n" +
                            "      if (eventData.type === 'HEARTBEAT') {\n" +
                            "        eventData.type = 'RECORDER_STATUS';\n" +
                            "      }\n" +
                            "      \n" +
                            "      // Add session ID and essential data\n" +
                            "      eventData.sessionId = '" + sessionId + "';\n" +
                            "      eventData.timestamp = new Date().getTime();\n" +
                            "      eventData.url = window.location.href;\n" +
                            "      eventData.title = document.title;\n" +
                            "      \n" +
                            "      // Convert to JSON\n" +
                            "      const data = JSON.stringify(eventData);\n" +
                            "      \n" +
                            "      // Try to use sendBeacon if available\n" +
                            "      if (navigator.sendBeacon) {\n" +
                            "        const blob = new Blob([data], { type: 'application/json' });\n" +
                            "        const sent = navigator.sendBeacon(apiUrl, blob);\n" +
                            "        if (sent) {\n" +
                            "          console.log('CSTestForge: Sent close event using beacon');\n" +
                            "          return true;\n" +
                            "        }\n" +
                            "      }\n" +
                            "      \n" +
                            "      // Fall back to synchronous XHR if sendBeacon fails or is not available\n" +
                            "      const xhr = new XMLHttpRequest();\n" +
                            "      xhr.open('POST', apiUrl, false); // Synchronous XHR\n" +
                            "      xhr.setRequestHeader('Content-Type', 'application/json');\n" +
                            "      xhr.send(data);\n" +
                            "      console.log('CSTestForge: Sent close event using sync XHR');\n" +
                            "      return true;\n" +
                            "    } catch(e) {\n" +
                            "      console.error('CSTestForge: Failed to send close event:', e);\n" +
                            "      return false;\n" +
                            "    }\n" +
                            "  }\n" +
                            "  \n" +
                            "  // Store our original send event function\n" +
                            "  const originalSendEvent = window.__csSendEvent;\n" +
                            "  \n" +
                            "  // Create a special version for unload events\n" +
                            "  window.__csSendCloseEvent = function(action) {\n" +
                            "    return sendBeaconOrXhr({\n" +
                            "      type: 'RECORDER_CONTROL',\n" +
                            "      action: action || 'BROWSER_CLOSING'\n" +
                            "    });\n" +
                            "  };\n" +
                            "  \n" +
                            "  // Add listener for beforeunload event\n" +
                            "  window.addEventListener('beforeunload', function(e) {\n" +
                            "    if (window.__csRecorderActive) {\n" +
                            "      console.log('CSTestForge: Browser close event detected, sending notification');\n" +
                            "      window.__csSendCloseEvent('BROWSER_CLOSING');\n" +
                            "      window.__csRecorderActive = false;\n" +
                            "    }\n" +
                            "  });\n" +
                            "  \n" +
                            "  // Add listener for unload event as backup\n" +
                            "  window.addEventListener('unload', function(e) {\n" +
                            "    if (window.__csRecorderActive) {\n" +
                            "      console.log('CSTestForge: Browser unload event detected, sending notification');\n" +
                            "      window.__csSendCloseEvent('BROWSER_CLOSING');\n" +
                            "      window.__csRecorderActive = false;\n" +
                            "    }\n" +
                            "  });\n" +
                            "  \n" +
                            "  // Also monitor for visibility changes which might indicate closing\n" +
                            "  document.addEventListener('visibilitychange', function() {\n" +
                            "    if (document.visibilityState === 'hidden' && \n" +
                            "        window.__csRecorderActive) {\n" +
                            "      console.log('CSTestForge: Page visibility hidden, sending notification');\n" +
                            "      // Send a visibility change event that might indicate closing\n" +
                            "      if (originalSendEvent) {\n" +
                            "        originalSendEvent({\n" +
                            "          type: 'RECORDER_CONTROL',\n" +
                            "          action: 'VISIBILITY_HIDDEN'\n" +
                            "        });\n" +
                            "      }\n" +
                            "    }\n" +
                            "  });\n" +
                            "  \n" +
                            "  // Add special variable to store recording state even after browser refresh/navigation\n" +
                            "  try {\n" +
                            "    // Store session info in sessionStorage to survive page reloads\n" +
                            "    sessionStorage.setItem('csRecorderSessionId', '" + sessionId + "');\n" +
                            "    sessionStorage.setItem('csRecorderActive', 'true');\n" +
                            "  } catch(e) {\n" +
                            "    console.error('CSTestForge: Failed to store session info in sessionStorage:', e);\n" +
                            "  }\n" +
                            "  \n" +
                            "  return 'Browser close listener installed';\n" +
                            "})();";

            Object result = browser.executeScript(closeListenerScript);
            logger.debug("Browser close listener setup result: {}", result);
        } catch (Exception e) {
            logger.warn("Error setting up browser close listener: {}", e.getMessage());
        }
    }

    /**
     * Setup periodic verification of recorder health and reinject if needed
     *
     * @param sessionId The session ID
     */
    private void setupRecorderHealthCheck(UUID sessionId) {
        Thread healthCheckThread = new Thread(() -> {
            BrowserInstance browser = getBrowserInstance(sessionId);
            if (browser == null) {
                return;
            }

            int checkCount = 0;
            final int MAX_CHECKS = 120; // Run for 10 minutes

            while (activeBrowsers.containsKey(sessionId) && checkCount < MAX_CHECKS) {
                try {
                    Thread.sleep(5000); // Check every 5 seconds
                    checkCount++;

                    // Skip if browser is not responsive - this will now handle cleanup if window is closed
                    if (!isBrowserResponsive(sessionId)) {
                        logger.debug("Browser not responsive during health check for session {}", sessionId);
                        continue;
                    }

                    // Check recorder health
                    boolean isActive = verifyRecorderActive(sessionId);

                    if (!isActive) {
                        logger.warn("Recorder not active for session {}, attempting reinjection", sessionId);
                        injectRecorderScriptWithRetry(sessionId, MAX_INJECTION_RETRIES);
                    } else {
                        // Just verify UI is visible
                        ensureRecorderUIVisible(sessionId);
                    }

                    // Also check for domain changes periodically
                    if (checkCount % 6 == 0) { // Every 30 seconds (5s * 6)
                        checkForDomainChange(sessionId);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.warn("Error in recorder health check: {}", e.getMessage());

                    // Check if it's due to a closed window
                    if (e.getMessage() != null &&
                            (e.getMessage().contains("no such window") ||
                                    e.getMessage().contains("window already closed") ||
                                    e.getMessage().contains("chrome not reachable"))) {
                        cleanupClosedBrowserSession(sessionId);
                        break;
                    }
                }
            }

            logger.info("Recorder health check thread completed for session: {}", sessionId);
        });

        healthCheckThread.setDaemon(true);
        healthCheckThread.setName("recorder-health-check-" + sessionId);
        healthCheckThread.start();
    }

    /**
     * Verify that the recorder is active in the browser
     *
     * @param sessionId The session ID
     * @return true if the recorder is active
     */
    private boolean verifyRecorderActive(UUID sessionId) {
        BrowserInstance browser = getBrowserInstance(sessionId);
        if (browser == null) {
            return false;
        }

        try {
            // Check for the presence of the __csRecorderActive flag
            Object activeFlag = browser.executeScript(
                    "return typeof window.__csRecorderActive === 'boolean' ? window.__csRecorderActive : false;");

            return Boolean.TRUE.equals(activeFlag);
        } catch (Exception e) {
            logger.error("Error verifying recorder active state: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Ensure the recorder UI is visible in the browser
     *
     * @param sessionId The session ID
     */
    private void ensureRecorderUIVisible(UUID sessionId) {
        BrowserInstance browser = getBrowserInstance(sessionId);
        if (browser == null) {
            return;
        }

        try {
            // Check if UI indicator exists and is visible
            Object uiVisible = browser.executeScript(
                    "const indicator = document.getElementById('cs-recorder-indicator');\n" +
                            "if (!indicator) {\n" +
                            "  // Create the indicator if missing\n" +
                            "  const ui = document.createElement('div');\n" +
                            "  ui.id = 'cs-recorder-indicator';\n" +
                            "  ui.style.cssText = 'position:fixed;top:10px;right:10px;background:#333;color:#fff;padding:8px 12px;" +
                            "z-index:2147483647;border-radius:4px;font-family:Arial;box-shadow:0 2px 8px rgba(0,0,0,0.2);display:flex;" +
                            "align-items:center;gap:8px;';\n" +
                            "  ui.innerHTML = '<div style=\"width:10px;height:10px;background:red;border-radius:50%;\"></div>CSTestForge Recording';\n" +
                            "  document.body.appendChild(ui);\n" +
                            "  return true;\n" +
                            "} else {\n" +
                            "  // Ensure existing indicator is visible\n" +
                            "  indicator.style.display = 'flex';\n" +
                            "  indicator.style.visibility = 'visible';\n" +
                            "  return true;\n" +
                            "}");

            logger.debug("Recorder UI visibility check: {}", uiVisible);
        } catch (Exception e) {
            logger.warn("Error ensuring recorder UI visibility: {}", e.getMessage());
        }
    }

    /**
     * Check if browser is responsive and handle closed windows properly
     *
     * @param sessionId The session ID
     * @return true if browser is responsive
     */
    private boolean isBrowserResponsive(UUID sessionId) {
        BrowserInstance browser = getBrowserInstance(sessionId);
        if (browser == null) {
            return false;
        }

        try {
            // First try with a simple method that doesn't execute JavaScript
            String currentUrl = browser.getCurrentUrl();
            if (currentUrl == null) {
                logger.warn("Browser window appears to be closed for session {}", sessionId);
                cleanupClosedBrowserSession(sessionId);
                return false;
            }

            // Check window state with a basic JavaScript test
            try {
                Object windowState = browser.executeScript("return window && window.document ? 'active' : 'closed';");
                if (!"active".equals(windowState)) {
                    logger.warn("Browser window reports closed state for session {}", sessionId);
                    cleanupClosedBrowserSession(sessionId);
                    return false;
                }
            } catch (Exception e) {
                // Error executing script likely means browser is closed
                logger.warn("Error checking window state for session {}: {}", sessionId, e.getMessage());
                if (e.getMessage() != null &&
                        (e.getMessage().contains("no such window") ||
                                e.getMessage().contains("window already closed") ||
                                e.getMessage().contains("chrome not reachable"))) {
                    cleanupClosedBrowserSession(sessionId);
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            logger.debug("Browser not responsive: {}", e.getMessage());
            if (e.getMessage() != null &&
                    (e.getMessage().contains("no such window") ||
                            e.getMessage().contains("window already closed") ||
                            e.getMessage().contains("chrome not reachable"))) {
                cleanupClosedBrowserSession(sessionId);
            }
            return false;
        }
    }

    /**
     * Clean up resources for a browser session where the window was closed
     *
     * @param sessionId The session ID
     */
    private void cleanupClosedBrowserSession(UUID sessionId) {
        logger.info("Cleaning up resources for closed browser session: {}", sessionId);

        try {
            // Remove from active browsers first to prevent recursive calls
            BrowserInstance browser = activeBrowsers.remove(sessionId);

            if (browser != null) {
                try {
                    // Try to close the WebDriver session cleanly
                    browser.stop();
                } catch (Exception e) {
                    logger.debug("Error stopping browser during cleanup: {}", e.getMessage());
                    // Continue with cleanup regardless of errors
                }

                // Notify the WebSocket service about the disconnection if available
                if (webSocketService != null) {
                    try {
                        webSocketService.notifySessionConnectionStatus(sessionId, false);

                        // Update the session status to COMPLETED in the database
                        try {
                            RecordingSession session = recorderService.updateSessionStatus(sessionId, RecordingStatus.COMPLETED);
                            if (session != null) {
                                webSocketService.notifySessionStatusChanged(sessionId, RecordingStatus.COMPLETED);
                                logger.info("Recording session {} marked as COMPLETED due to browser closure", sessionId);
                            }
                        } catch (Exception e) {
                            logger.warn("Error updating session status after browser closure: {}", e.getMessage());
                        }
                    } catch (Exception e) {
                        logger.debug("Error notifying WebSocket service: {}", e.getMessage());
                    }
                }

                logger.info("Successfully cleaned up closed browser session: {}", sessionId);
            }
        } catch (Exception e) {
            logger.error("Error cleaning up closed browser session {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Reconnect browser when session becomes invalid
     *
     * @param sessionId The session ID
     * @return true if reconnected successfully
     */
    public boolean reconnectBrowser(UUID sessionId) {
        logger.info("Attempting to reconnect browser for session: {}", sessionId);

        // First check if the browser instance exists
        BrowserInstance browser = activeBrowsers.get(sessionId);
        if (browser == null) {
            logger.warn("Cannot reconnect - No browser instance found for session: {}", sessionId);
            return false;
        }

        try {
            // Check if browser is still responsive
            boolean isResponsive = false;
            try {
                String currentUrl = browser.getCurrentUrl();
                isResponsive = (currentUrl != null);
                logger.debug("Browser responsiveness check: {} for session {}", isResponsive, sessionId);
            } catch (Exception e) {
                logger.debug("Browser is not responsive for session {}: {}", sessionId, e.getMessage());
                isResponsive = false;
            }

            if (isResponsive) {
                logger.info("Browser for session {} is still responsive, no need to reconnect", sessionId);
                return true;
            }

            // Get the last known URL if available
            String lastUrl = null;
            try {
                lastUrl = browser.getCurrentUrl();
            } catch (Exception e) {
                // Ignore errors - we already know the browser is not responsive
            }

            // Create a new config since we can't access browserType directly from browser
            RecordingConfig config = new RecordingConfig();

            // Try to determine browser type from the browser instance
            BrowserType browserType = browser.getBrowserType();
            config.setBrowserType(browserType.toString());
            config.setCommandTimeoutSeconds(BROWSER_OPERATION_TIMEOUT);

            // Stop the existing browser
            try {
                browser.stop();
            } catch (Exception e) {
                logger.warn("Error stopping existing browser: {}", e.getMessage());
                // Continue anyway
            }

            // Remove from active browsers
            activeBrowsers.remove(sessionId);

            // Create and start a new browser instance
            BrowserInstance newBrowser = createBrowserInstance(sessionId, browserType, config);

            boolean started = newBrowser.start(config.getCommandTimeoutSeconds(), TimeUnit.SECONDS);
            if (started) {
                // Add to active browsers
                activeBrowsers.put(sessionId, newBrowser);

                // Navigate to the last known URL if available
                if (lastUrl != null && !lastUrl.isEmpty() && !lastUrl.equals("about:blank")) {
                    logger.info("Navigating to last known URL: {}", lastUrl);
                    newBrowser.navigate(lastUrl);
                }

                // Wait for page to load
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Apply CSP bypasses for Chrome browser
                if (newBrowser instanceof ChromeBrowserInstance) {
                    ChromeBrowserInstance chromeBrowser = (ChromeBrowserInstance) newBrowser;
                    chromeBrowser.applyCSPBypass();
                    chromeBrowser.disableCSP();
                    chromeBrowser.fixCORSIssues(serverHost, serverPort);
                }

                // Inject recorder script
                boolean injected = injectRecorderScriptWithRetry(sessionId, MAX_INJECTION_RETRIES);

                if (injected) {
                    logger.info("Successfully reconnected browser for session: {}", sessionId);

                    // Setup script verification and domain change detection
                    setupRecorderHealthCheck(sessionId);
                    setupMutationObserver(sessionId);
                    setupDomainChangeDetection(sessionId);

                    return true;
                } else {
                    logger.error("Failed to inject recorder script after browser reconnection");
                    return false;
                }
            } else {
                logger.error("Failed to start new browser for reconnection");
                return false;
            }
        } catch (Exception e) {
            logger.error("Error during browser reconnection: {}", e.getMessage(), e);
            return false;
        }
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

            if (!Files.exists(scriptPath)) {
                logger.error("Recorder script not found at path: {}", scriptPath);
                throw new IOException("Recorder script file not found: " + scriptPath);
            }

            String originalScript = Files.readString(scriptPath);
            recorderScriptCache = originalScript;
        }

        return recorderScriptCache;
    }

    /**
     * Detect the LAN IP address of the server for better connectivity
     *
     * @return The detected LAN IP address or null if not found
     */
    private String detectLanIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // Skip loopback interfaces, virtual interfaces and interfaces that are down
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    // Skip loopback, link-local and IPv6 addresses
                    if (addr.isLoopbackAddress() || addr.isLinkLocalAddress() ||
                            addr.getHostAddress().contains(":")) {
                        continue;
                    }

                    String ipAddress = addr.getHostAddress();
                    // Check if this is a private network IP (like 192.168.x.x, 10.x.x.x, etc.)
                    if (ipAddress.startsWith("192.168.") || ipAddress.startsWith("10.") ||
                            (ipAddress.startsWith("172.") &&
                                    Integer.parseInt(ipAddress.split("\\.")[1]) >= 16 &&
                                    Integer.parseInt(ipAddress.split("\\.")[1]) <= 31)) {
                        return ipAddress;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to detect LAN IP address: {}", e.getMessage());
        }
        return null;
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
            // First check if browser is still responsive
            if (!isBrowserResponsive(sessionId)) {
                logger.warn("Browser appears to be unresponsive, cannot execute script");
                return null;
            }

            // Now execute the script with the verified browser
            return browser.executeScript(script);
        } catch (Exception e) {
            // Enhanced error detection - check for all known browser closure error messages
            String errorMessage = e.getMessage();
            if (errorMessage != null) {
                boolean isClosedWindowError =
                        errorMessage.contains("no such window") ||
                                errorMessage.contains("window already closed") ||
                                errorMessage.contains("chrome not reachable") ||
                                errorMessage.contains("cannot determine loading status") ||
                                errorMessage.contains("target closed") ||
                                errorMessage.contains("disconnected: unable to connect") ||
                                errorMessage.contains("browser has been closed") ||
                                errorMessage.contains("target frame detached") ||
                                errorMessage.contains("Session not found");

                if (isClosedWindowError) {
                    logger.warn("Browser window was closed during script execution for session {}. Error: {}",
                            sessionId, errorMessage);

                    cleanupClosedBrowserSession(sessionId);
                    return null;
                }
            }

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
     * Get a browser instance by session ID
     *
     * @param sessionId The session ID
     * @return The browser instance or null if not found
     */
    public BrowserInstance getBrowserInstance(UUID sessionId) {
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
     * Inject recorder script with multiple strategies for better compatibility
     *
     * @param sessionId The session ID
     * @return true if successful with any strategy
     */
    public boolean injectRecorderScriptWithStrategies(UUID sessionId) {
        BrowserInstance browser = getBrowserInstance(sessionId);
        if (browser == null) {
            logger.error("Cannot inject recorder script - no browser found for session: {}", sessionId);
            return false;
        }

        logger.info("Attempting to inject recorder script with multiple strategies for session: {}", sessionId);

        try {
            // First try with Chrome-specific injection if available
            if (browser instanceof ChromeBrowserInstance) {
                ChromeBrowserInstance chromeBrowser = (ChromeBrowserInstance) browser;
                try {
                    // Apply CSP bypasses for better chance of success
                    chromeBrowser.applyCSPBypass();
                    chromeBrowser.disableCSP();
                    chromeBrowser.fixCORSIssues(serverHost, serverPort);

                    // Try Chrome-specific injection
                    boolean success = chromeBrowser.injectRecorderScriptWithServerDetails(
                            sessionId.toString(), serverHost, serverPort, contextPath);
                    if (success) {
                        logger.info("Successfully injected recorder script using Chrome-specific strategy");
                        return true;
                    }
                } catch (Exception e) {
                    logger.warn("Chrome-specific injection failed: {}", e.getMessage());
                    // Continue to other strategies
                }
            }

            // Strategy 1: Try with our standard retry approach
            try {
                boolean success = injectRecorderScriptWithRetry(sessionId, MAX_INJECTION_RETRIES);
                if (success) {
                    logger.info("Successfully injected recorder script with retry approach");
                    return true;
                }
            } catch (Exception e) {
                logger.warn("Standard retry injection failed: {}", e.getMessage());
            }

            // Strategy 2: Try ultra-compatible script
            try {
                boolean success = injectUltraCompatibleScript(sessionId);
                if (success) {
                    logger.info("Successfully injected ultra-compatible script");
                    return true;
                }
            } catch (Exception e) {
                logger.warn("Ultra-compatible script injection failed: {}", e.getMessage());
            }

            // Strategy 3: Try minimal script as last resort
            try {
                boolean success = injectMinimalRecorderScript(sessionId);
                if (success) {
                    logger.info("Successfully injected minimal recorder script");
                    return true;
                }
            } catch (Exception e) {
                logger.warn("Minimal script injection failed: {}", e.getMessage());
            }

            // If we get here, all strategies failed
            logger.error("All recorder script injection strategies failed for session: {}", sessionId);
            return false;
        } catch (Exception e) {
            logger.error("Error during script injection strategies: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Debug method to check the status of recording in the browser
     *
     * @param sessionId The recording session ID
     * @return Map with debug information
     */
    public Map<String, Object> debugRecorderScript(UUID sessionId) {
        Map<String, Object> debug = new HashMap<>();

        debug.put("sessionId", sessionId);
        debug.put("timestamp", new Date());
        debug.put("serverHost", serverHost);
        debug.put("serverPort", serverPort);
        debug.put("contextPath", contextPath);

        BrowserInstance browser = getBrowserInstance(sessionId);
        if (browser == null) {
            debug.put("error", "No browser instance found for session: " + sessionId);
            return debug;
        }

        try {
            // Check if recorder is active
            Object isActive = browser.executeScript(
                    "return window.__csRecorderActive === true;"
            );
            debug.put("isRecorderActive", isActive);

            // Check for UI elements
            Object hasToolbar = browser.executeScript(
                    "return document.getElementById('cs-recorder-indicator') !== null;"
            );
            debug.put("hasRecorderToolbar", hasToolbar);

            // Check if recorder script is working
            Object testResult = browser.executeScript(
                    "try {\n" +
                            "  if (typeof window.__csSendEvent === 'function' || \n" +
                            "      typeof window._csSendEvent === 'function' || \n" +
                            "      typeof window.__csRecorder === 'object') {\n" +
                            "    return 'Recorder functions available';\n" +
                            "  }\n" +
                            "  return 'Recorder functions not found';\n" +
                            "} catch(e) {\n" +
                            "  return 'Error: ' + e.toString();\n" +
                            "}"
            );
            debug.put("recorderFunctionsTest", testResult);

            // Check for errors
            Object errors = browser.executeScript(
                    "return window.__csRecorderErrors || [];"
            );
            debug.put("errors", errors);

            // Check for connectivity
            Object connectivityTest = browser.executeScript(
                    "try {\n" +
                            "  const xhr = new XMLHttpRequest();\n" +
                            "  xhr.open('HEAD', '" + "http://" + serverHost + ":" + serverPort + contextPath + "/api/recorder/ping', false);\n" +
                            "  try {\n" +
                            "    xhr.send();\n" +
                            "    return { status: xhr.status, statusText: xhr.statusText };\n" +
                            "  } catch(e) {\n" +
                            "    return { error: e.toString() };\n" +
                            "  }\n" +
                            "} catch(e) {\n" +
                            "  return { error: e.toString() };\n" +
                            "}"
            );
            debug.put("connectivityTest", connectivityTest);

            // Get current page info
            debug.put("currentUrl", browser.getCurrentUrl());
            debug.put("pageTitle", browser.getTitle());
            debug.put("browserType", browser.getBrowserType());

            // If this is Chrome, add Chrome-specific debugging
            if (browser instanceof ChromeBrowserInstance) {
                ChromeBrowserInstance chromeBrowser = (ChromeBrowserInstance) browser;
                Map<String, Object> cspAnalysis = chromeBrowser.analyzeCSPIssues();
                debug.put("cspAnalysis", cspAnalysis);
            }

            return debug;
        } catch (Exception e) {
            debug.put("error", "Error generating debug information: " + e.getMessage());
            return debug;
        }
    }

    /**
     * Stop all browser instances during application shutdown
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down all browser instances...");
        for (Map.Entry<UUID, BrowserInstance> entry : new HashMap<>(activeBrowsers).entrySet()) {
            try {
                entry.getValue().stop();
            } catch (Exception e) {
                logger.error("Error stopping browser for session {}: {}", entry.getKey(), e.getMessage());
            }
        }
        activeBrowsers.clear();
    }
}