package com.cstestforge.recorder.controller;

import com.cstestforge.recorder.browser.BrowserInstance;
import com.cstestforge.recorder.browser.BrowserManager;
import com.cstestforge.recorder.browser.ChromeBrowserInstance;
import com.cstestforge.recorder.model.*;
import com.cstestforge.recorder.model.LoopConfig;
import com.cstestforge.recorder.model.events.LoopEvent;
import com.cstestforge.recorder.service.RecorderService;
import com.cstestforge.recorder.websocket.RecorderWebSocketService;
import com.cstestforge.codegen.controller.CodeBuilderController;
import com.cstestforge.codegen.model.CodeGenerationRequest;
import com.cstestforge.codegen.model.GeneratedCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.context.annotation.Bean;

import jakarta.annotation.PreDestroy;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

/**
 * Controller for handling recording-related endpoints
 */
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/recorder")
public class RecorderController {
    private static final Logger logger = LoggerFactory.getLogger(RecorderController.class);

    @Autowired
    private RecorderService recorderService;

    @Autowired
    private BrowserManager browserManager;

    @Autowired
    private RecorderWebSocketService webSocketService;

    @Autowired
    private CodeBuilderController codeBuilderController;

    /**
     * Configure CORS to allow access from any origin
     * This is critical for proper functioning with sites that have CSP restrictions
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH")
                        .allowedHeaders("*")
                        .exposedHeaders("Access-Control-Allow-Origin", "Access-Control-Allow-Credentials");
            }
        };
    }

    /**
     * Start a recording session
     *
     * @param request The recording request parameters
     * @return Recording session details and status
     */
    @PostMapping("/start")
    public ResponseEntity<RecordingResponse> startRecording(@RequestBody RecordingRequest request) {
        try {
            // Log the entire request
            logger.info("Received recording request - projectId: {}, browserType: {}, framework: {}, url: {}",
                    request.getProjectId(), request.getBrowserType(), request.getFramework(), request.getUrl());

            // Create a recording session
            String projectId = request.getProjectId();

            if (StringUtils.hasLength(projectId) == false) {
                return ResponseEntity.badRequest().body(
                        new RecordingResponse(null, "Project ID is required", false)
                );
            }

            // Generate a session ID for this recording
            UUID sessionId = UUID.randomUUID();

            // Get the browser type
            String browserType = request.getBrowserType();
            if (StringUtils.hasLength(browserType) == false) {
                browserType = "chrome"; // Default to Chrome if not specified
            }

            // Log the selected browser
            logger.info("Selected browser for recording: {}", browserType);

            // Get the framework
            String framework = request.getFramework();
            if (StringUtils.hasLength(framework) == false) {
                framework = "selenium_java_testng"; // Default framework
            }

            // Log the selected framework
            logger.info("Selected framework for recording: {}", framework);

            // Create a recording configuration
            RecordingConfig config = new RecordingConfig();
            config.setBrowserType(browserType);

            // Properly handle the URL
            String baseUrl = request.getUrl();
            if (StringUtils.hasLength(baseUrl)) {
                // Handle about:blank as a special case
                if ("about:blank".equals(baseUrl)) {
                    logger.info("Using special URL: about:blank");
                    config.setBaseUrl(baseUrl);
                } else {
                    // Ensure URL has a protocol
                    if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
                        baseUrl = "https://" + baseUrl;
                        logger.info("Added https:// prefix to URL: {}", baseUrl);
                    }

                    // Validate URL format
                    try {
                        new URL(baseUrl);
                        config.setBaseUrl(baseUrl);
                        logger.info("Using base URL for navigation: {}", baseUrl);
                    } catch (MalformedURLException e) {
                        logger.error("Invalid URL format: {}", baseUrl);
                        return ResponseEntity.badRequest().body(
                                new RecordingResponse(null, "Invalid URL format: " + baseUrl, false)
                        );
                    }
                }
            } else {
                logger.warn("No URL provided, will use blank page");
                config.setBaseUrl("about:blank");
            }

            // Set environment variables for the recording
            Map<String, String> env = new HashMap<>();
            env.put("framework", framework);
            config.setEnvironmentVariables(env);

            // Configure browser with improved options for handling CSP
            Map<String, Object> browserOptions = new HashMap<>();
            browserOptions.put("ignore_certificate_errors", true);
            browserOptions.put("disable_web_security", true);
            browserOptions.put("disable_content_security_policy", true);
            browserOptions.put("allow_running_insecure_content", true);

            config.setBrowserOptions(browserOptions);

            // Set command timeout to 60 seconds for better reliability
            config.setCommandTimeoutSeconds(60);

            // Enable full capturing
            config.setCaptureNetwork(true);
            config.setCaptureConsole(true);

            // Start the browser
            boolean success = browserManager.startBrowser(sessionId, config);

            if (success) {
                // Create a record in the database for this session
                RecordingSession session = new RecordingSession();
                session.setId(sessionId);
                session.setProjectId(projectId);
                session.setBrowser(browserType);
                session.setFramework(framework);
                session.setBaseUrl(baseUrl);
                session.setStartTime(Date.from(Instant.now()));
                session.setStatus(RecordingStatus.ACTIVE);
                session.setName("Recording " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(Instant.now()));

                recorderService.saveSession(session);

                logger.info("Recording session created: {} for project {}", sessionId, projectId);

                // Start event collection for this session
                recorderService.startEventCollection(sessionId);

                // Return session information
                RecordingResponse response = new RecordingResponse(
                        sessionId.toString(),
                        "Recording started successfully",
                        true
                );

                logger.info("Browser started successfully for recording session: {}", sessionId);
                return ResponseEntity.ok(response);
            } else {
                logger.error("Failed to start browser for recording session");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new RecordingResponse(null, "Failed to start browser", false));
            }
        } catch (Exception e) {
            logger.error("Error starting recording session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RecordingResponse(null, "Error: " + e.getMessage(), false));
        }
    }

    /**
     * Add an event to a recording session - with CORS headers for CSP compatibility
     *
     * @param sessionId The session ID
     * @param event The event to add
     * @return ResponseEntity with success/failure status
     */
    @PostMapping("/events/{sessionId}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<?> addEvent(@PathVariable UUID sessionId, @RequestBody RecordedEvent event) {
        try {
            logger.debug("Received event for session {}: type={}", sessionId, event.getType());
            boolean success = recorderService.addEvent(sessionId, event);

            if (success) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Methods", "POST, GET, OPTIONS")
                        .header("Access-Control-Allow-Headers", "Content-Type")
                        .build();
            } else {
                return ResponseEntity.badRequest()
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Methods", "POST, GET, OPTIONS")
                        .header("Access-Control-Allow-Headers", "Content-Type")
                        .body("Failed to add event. Session may be inactive or not found.");
            }
        } catch (Exception e) {
            logger.error("Error processing event for session {}: {}", sessionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Methods", "POST, GET, OPTIONS")
                    .header("Access-Control-Allow-Headers", "Content-Type")
                    .body("Error processing event: " + e.getMessage());
        }
    }

    /**
     * Handle preflight OPTIONS requests for CORS
     * This is critical for browsers to allow cross-origin requests
     */
    @RequestMapping(value = "/events/{sessionId}", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleOptions() {
        return ResponseEntity.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "POST, GET, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With")
                .header("Access-Control-Max-Age", "3600")
                .build();
    }

    /**
     * Reinject the recorder script for a session - improved for CSP compatibility
     *
     * @param sessionId The session ID
     * @return ResponseEntity with success/failure status
     */
    @PostMapping("/reinject/{sessionId}")
    public ResponseEntity<Map<String, Object>> reinjectRecorderScript(@PathVariable UUID sessionId) {
        Map<String, Object> response = new HashMap<>();

        if (!browserManager.isSessionActive(sessionId)) {
            response.put("success", false);
            response.put("error", "No active browser session found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        try {
            BrowserInstance browser = browserManager.getBrowserInstance(sessionId);

            if (browser instanceof ChromeBrowserInstance) {
                ChromeBrowserInstance chromeBrowser = (ChromeBrowserInstance) browser;

                // First apply CSP bypasses
                boolean cspBypassApplied = chromeBrowser.applyCSPBypass();
                response.put("cspBypassApplied", cspBypassApplied);

                boolean cspDisabled = chromeBrowser.disableCSP();
                response.put("cspDisabled", cspDisabled);

                // Get the server host and port
                String serverHost = System.getProperty("server.address");
                if (serverHost == null || serverHost.isEmpty()) {
                    serverHost = System.getProperty("server.host", "localhost");
                }

                String serverPort = System.getProperty("server.port", "8080");
                String contextPath = System.getProperty("server.servlet.context-path", "/cstestforge");

                if (!contextPath.startsWith("/")) {
                    contextPath = "/" + contextPath;
                }

                // Fix CORS issues
                boolean corsFixed = chromeBrowser.fixCORSIssues(serverHost, serverPort);
                response.put("corsFixed", corsFixed);

                // Inject the script with server details
                boolean serverScriptInjected = chromeBrowser.injectRecorderScriptWithServerDetails(
                        sessionId.toString(), serverHost, serverPort, contextPath);
                response.put("serverScriptInjected", serverScriptInjected);

                if (serverScriptInjected) {
                    response.put("success", true);
                    response.put("method", "server-specific");
                    return ResponseEntity.ok(response);
                }
            }

            // If server-specific injection didn't work or not a Chrome browser, try other methods

            // Try ultra-compatible script first
            boolean injected = browserManager.injectUltraCompatibleScript(sessionId);
            response.put("ultraCompatibleInjection", injected);

            if (!injected) {
                // Try minimal script if ultra-compatible fails
                boolean minimalInjected = browserManager.injectMinimalRecorderScript(sessionId);
                response.put("minimalInjection", minimalInjected);

                if (!minimalInjected) {
                    // Try full script as last resort
                    boolean fullInjected = browserManager.injectRecorderScript(sessionId);
                    response.put("fullInjection", fullInjected);

                    response.put("success", fullInjected);
                    if (!fullInjected) {
                        response.put("error", "Failed to inject any recorder script");
                    } else {
                        response.put("method", "full");
                    }
                } else {
                    response.put("success", true);
                    response.put("method", "minimal");
                }
            } else {
                response.put("success", true);
                response.put("method", "ultra-compatible");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error reinjecting recorder script: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Error reinjecting recorder script: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Debug endpoint for checking CSP issues
     *
     * @param sessionId The session ID
     * @return Debug information about CSP settings
     */
    @GetMapping("/debug/csp/{sessionId}")
    public ResponseEntity<Map<String, Object>> debugCspIssues(@PathVariable UUID sessionId) {
        if (!browserManager.isSessionActive(sessionId)) {
            return ResponseEntity.notFound().build();
        }

        BrowserInstance browser = browserManager.getBrowserInstance(sessionId);

        if (browser instanceof ChromeBrowserInstance) {
            ChromeBrowserInstance chromeBrowser = (ChromeBrowserInstance) browser;

            Map<String, Object> cspInfo = chromeBrowser.analyzeCSPIssues();

            // Add server information
            String serverHost = System.getProperty("server.address");
            if (serverHost == null || serverHost.isEmpty()) {
                serverHost = System.getProperty("server.host", "localhost");
            }
            String serverPort = System.getProperty("server.port", "8080");
            String contextPath = System.getProperty("server.servlet.context-path", "/cstestforge");

            if (!contextPath.startsWith("/")) {
                contextPath = "/" + contextPath;
            }

            cspInfo.put("serverHost", serverHost);
            cspInfo.put("serverPort", serverPort);
            cspInfo.put("contextPath", contextPath);
            cspInfo.put("apiEndpoint", "http://" + serverHost + ":" + serverPort + contextPath + "/api/recorder/events/" + sessionId);

            return ResponseEntity.ok(cspInfo);
        }

        Map<String, Object> cspInfo = new HashMap<>();

        try {
            // Get CSP info
            Object cspMeta = browser.executeScript(
                    "const cspMeta = document.querySelector('meta[http-equiv=\"Content-Security-Policy\"]');\n" +
                            "return cspMeta ? cspMeta.content : 'No CSP meta tag found';"
            );

            cspInfo.put("cspMetaTag", cspMeta);

            // Check CSP headers
            Object cspHeaders = browser.executeScript(
                    "const entries = performance.getEntriesByType('navigation');\n" +
                            "if (entries && entries.length > 0 && entries[0].securityPolicy) {\n" +
                            "  return entries[0].securityPolicy;\n" +
                            "} else {\n" +
                            "  return 'No CSP headers detected';\n" +
                            "}"
            );

            cspInfo.put("cspHeaders", cspHeaders);

            // Check for presence of recorder
            Object recorderActive = browser.executeScript("return window.__csRecorderActive === true;");
            cspInfo.put("recorderActive", recorderActive);

            // Check for frame-src directives
            Object frameSrcDirective = browser.executeScript(
                    "try {" +
                            "  const headers = document.querySelectorAll('meta[http-equiv=\"Content-Security-Policy\"]');" +
                            "  for (let i = 0; i < headers.length; i++) {" +
                            "    const content = headers[i].content;" +
                            "    const frameSrcMatch = content.match(/frame-src[^;]+/);" +
                            "    if (frameSrcMatch) return frameSrcMatch[0];" +
                            "  }" +
                            "  return 'No frame-src directive found';" +
                            "} catch(e) {" +
                            "  return 'Error checking frame-src: ' + e.toString();" +
                            "}"
            );
            cspInfo.put("frameSrcDirective", frameSrcDirective);

            // Check for connect-src directives
            Object connectSrcDirective = browser.executeScript(
                    "try {" +
                            "  const headers = document.querySelectorAll('meta[http-equiv=\"Content-Security-Policy\"]');" +
                            "  for (let i = 0; i < headers.length; i++) {" +
                            "    const content = headers[i].content;" +
                            "    const connectSrcMatch = content.match(/connect-src[^;]+/);" +
                            "    if (connectSrcMatch) return connectSrcMatch[0];" +
                            "  }" +
                            "  return 'No connect-src directive found';" +
                            "} catch(e) {" +
                            "  return 'Error checking connect-src: ' + e.toString();" +
                            "}"
            );
            cspInfo.put("connectSrcDirective", connectSrcDirective);

            // Try injecting a simple test script
            Object testInjection = browser.executeScript(
                    "try {" +
                            "  const testDiv = document.createElement('div');" +
                            "  testDiv.id = 'csp-test-div';" +
                            "  testDiv.textContent = 'CSP Test';" +
                            "  document.body.appendChild(testDiv);" +
                            "  return 'Test element injected successfully';" +
                            "} catch(e) {" +
                            "  return 'Error injecting test element: ' + e.toString();" +
                            "}"
            );
            cspInfo.put("testInjection", testInjection);

            // Try a test XHR request to our server
            String serverHost = System.getProperty("server.address");
            if (serverHost == null || serverHost.isEmpty()) {
                serverHost = System.getProperty("server.host", "localhost");
            }
            String serverPort = System.getProperty("server.port", "8080");

            Object xhrTest = browser.executeScript(
                    "try {" +
                            "  const xhr = new XMLHttpRequest();" +
                            "  xhr.open('HEAD', 'http://" + serverHost + ":" + serverPort + "/api/recorder/ping', false);" +
                            "  try {" +
                            "    xhr.send();" +
                            "    return 'XHR test successful: ' + xhr.status;" +
                            "  } catch(e) {" +
                            "    return 'XHR test failed: ' + e.toString();" +
                            "  }" +
                            "} catch(e) {" +
                            "  return 'Error creating XHR: ' + e.toString();" +
                            "}"
            );
            cspInfo.put("xhrTest", xhrTest);

            return ResponseEntity.ok(cspInfo);
        } catch (Exception e) {
            cspInfo.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(cspInfo);
        }
    }

    /**
     * Apply CSP bypass for a session
     *
     * @param sessionId The session ID
     * @return ResponseEntity with success/failure status
     */
    @PostMapping("/bypass-csp/{sessionId}")
    public ResponseEntity<Map<String, Object>> applyCSPBypass(@PathVariable UUID sessionId) {
        Map<String, Object> response = new HashMap<>();

        if (!browserManager.isSessionActive(sessionId)) {
            response.put("success", false);
            response.put("error", "No active browser session found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        BrowserInstance browser = browserManager.getBrowserInstance(sessionId);

        try {
            if (browser instanceof ChromeBrowserInstance) {
                ChromeBrowserInstance chromeBrowser = (ChromeBrowserInstance) browser;

                boolean bypassApplied = chromeBrowser.applyCSPBypass();
                response.put("bypassApplied", bypassApplied);

                boolean cspDisabled = chromeBrowser.disableCSP();
                response.put("cspDisabled", cspDisabled);

                // Get the server host and port
                String serverHost = System.getProperty("server.address");
                if (serverHost == null || serverHost.isEmpty()) {
                    serverHost = System.getProperty("server.host", "localhost");
                }

                String serverPort = System.getProperty("server.port", "8080");

                boolean corsFixed = chromeBrowser.fixCORSIssues(serverHost, serverPort);
                response.put("corsFixed", corsFixed);

                response.put("success", bypassApplied || cspDisabled || corsFixed);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "CSP bypass is only supported for Chrome browser");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error applying CSP bypass: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Add a ping endpoint for connectivity testing
     *
     * @return Simple OK response
     */
    @GetMapping("/ping")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type")
                .body("OK");
    }

    /**
     * Fix connection issues for a session
     *
     * @param sessionId The session ID
     * @return ResponseEntity with the fix results
     */
    @PostMapping("/fix-connection/{sessionId}")
    public ResponseEntity<Map<String, Object>> fixConnectionIssues(@PathVariable UUID sessionId) {
        Map<String, Object> response = new HashMap<>();

        if (!browserManager.isSessionActive(sessionId)) {
            response.put("success", false);
            response.put("error", "No active browser session found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        try {
            BrowserInstance browser = browserManager.getBrowserInstance(sessionId);

            // For Chrome browser, we can use the specialized methods
            if (browser instanceof ChromeBrowserInstance) {
                ChromeBrowserInstance chromeBrowser = (ChromeBrowserInstance) browser;

                // Get the server host and port
                String serverHost = System.getProperty("server.address");
                if (serverHost == null || serverHost.isEmpty()) {
                    serverHost = System.getProperty("server.host", "localhost");
                }

                String serverPort = System.getProperty("server.port", "8080");
                String contextPath = System.getProperty("server.servlet.context-path", "/cstestforge");

                if (!contextPath.startsWith("/")) {
                    contextPath = "/" + contextPath;
                }

                // Apply various fixes
                boolean corsFixed = chromeBrowser.fixCORSIssues(serverHost, serverPort);
                response.put("corsFixed", corsFixed);

                boolean cspBypassApplied = chromeBrowser.applyCSPBypass();
                response.put("cspBypassApplied", cspBypassApplied);

                boolean cspDisabled = chromeBrowser.disableCSP();
                response.put("cspDisabled", cspDisabled);

                // Inject the recorder script with server details
                boolean scriptInjected = chromeBrowser.injectRecorderScriptWithServerDetails(
                        sessionId.toString(), serverHost, serverPort, contextPath);
                response.put("scriptInjected", scriptInjected);

                // Run connection test
                Map<String, Object> connectionTest = runConnectionTest(sessionId, serverHost, serverPort, contextPath);
                response.put("connectionTest", connectionTest);

                response.put("success", corsFixed || cspBypassApplied || cspDisabled || scriptInjected);
                return ResponseEntity.ok(response);
            } else {
                // For other browsers, try the standard script injection methods
                boolean injected = browserManager.injectUltraCompatibleScript(sessionId);
                response.put("ultraCompatibleInjection", injected);

                if (!injected) {
                    injected = browserManager.injectMinimalRecorderScript(sessionId);
                    response.put("minimalInjection", injected);

                    if (!injected) {
                        injected = browserManager.injectRecorderScript(sessionId);
                        response.put("fullInjection", injected);
                    }
                }

                response.put("success", injected);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error fixing connection issues: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Run a connection test for a session
     *
     * @param sessionId The session ID
     * @param serverHost The server host
     * @param serverPort The server port
     * @param contextPath The context path
     * @return Map with test results
     */
    private Map<String, Object> runConnectionTest(UUID sessionId, String serverHost, String serverPort, String contextPath) {
        Map<String, Object> results = new HashMap<>();
        BrowserInstance browser = browserManager.getBrowserInstance(sessionId);

        try {
            // Format API endpoint
            String apiEndpoint = "http://" + serverHost + ":" + serverPort + contextPath + "/api/recorder/events/" + sessionId;

            // Test script to check connectivity
            String testScript =
                    "try {\n" +
                            "  const results = {};\n" +
                            "  \n" +
                            "  // Test 1: Check for recorder script variables\n" +
                            "  results.recorderActive = typeof window.__csRecorderActive === 'boolean';\n" +
                            "  results.recorderSessionId = window.__csRecorderSessionId;\n" +
                            "  results.serverHost = window.__csServerHost;\n" +
                            "  results.serverPort = window.__csServerPort;\n" +
                            "  results.contextPath = window.__csContextPath;\n" +
                            "  results.apiEndpoint = window.__csApiEndpoint || '" + apiEndpoint + "';\n" +
                            "  \n" +
                            "  // Test 2: Try a HEAD request to the API endpoint\n" +
                            "  const testHeadRequest = function() {\n" +
                            "    return new Promise((resolve, reject) => {\n" +
                            "      const xhr = new XMLHttpRequest();\n" +
                            "      xhr.open('HEAD', results.apiEndpoint, true);\n" +
                            "      xhr.onload = function() {\n" +
                            "        resolve({\n" +
                            "          status: xhr.status,\n" +
                            "          statusText: xhr.statusText,\n" +
                            "          success: xhr.status >= 200 && xhr.status < 400\n" +
                            "        });\n" +
                            "      };\n" +
                            "      xhr.onerror = function() {\n" +
                            "        reject({\n" +
                            "          error: 'Network error',\n" +
                            "          success: false\n" +
                            "        });\n" +
                            "      };\n" +
                            "      xhr.send();\n" +
                            "    });\n" +
                            "  };\n" +
                            "  \n" +
                            "  // Test 3: Try to send a test event\n" +
                            "  const testSendEvent = function() {\n" +
                            "    return new Promise((resolve, reject) => {\n" +
                            "      const xhr = new XMLHttpRequest();\n" +
                            "      xhr.open('POST', results.apiEndpoint, true);\n" +
                            "      xhr.setRequestHeader('Content-Type', 'application/json');\n" +
                            "      xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');\n" +
                            "      xhr.onload = function() {\n" +
                            "        resolve({\n" +
                            "          status: xhr.status,\n" +
                            "          statusText: xhr.statusText,\n" +
                            "          success: xhr.status >= 200 && xhr.status < 400\n" +
                            "        });\n" +
                            "      };\n" +
                            "      xhr.onerror = function() {\n" +
                            "        reject({\n" +
                            "          error: 'Network error',\n" +
                            "          success: false\n" +
                            "        });\n" +
                            "      };\n" +
                            "      const testEvent = {\n" +
                            "        type: 'TEST',\n" +
                            "        sessionId: '" + sessionId + "',\n" +
                            "        timestamp: new Date().getTime(),\n" +
                            "        url: window.location.href,\n" +
                            "        title: document.title\n" +
                            "      };\n" +
                            "      xhr.send(JSON.stringify(testEvent));\n" +
                            "    });\n" +
                            "  };\n" +
                            "  \n" +
                            "  // Store pending test promises\n" +
                            "  const tests = [];\n" +
                            "  \n" +
                            "  // Run the HEAD request test\n" +
                            "  tests.push(testHeadRequest().then(result => {\n" +
                            "    results.headRequest = result;\n" +
                            "    return result;\n" +
                            "  }).catch(error => {\n" +
                            "    results.headRequest = error;\n" +
                            "    return error;\n" +
                            "  }));\n" +
                            "  \n" +
                            "  // Run the send event test\n" +
                            "  tests.push(testSendEvent().then(result => {\n" +
                            "    results.sendEvent = result;\n" +
                            "    return result;\n" +
                            "  }).catch(error => {\n" +
                            "    results.sendEvent = error;\n" +
                            "    return error;\n" +
                            "  }));\n" +
                            "  \n" +
                            "  // Wait for all tests to complete\n" +
                            "  return Promise.all(tests).then(() => results);\n" +
                            "} catch(e) {\n" +
                            "  return { error: e.toString() };\n" +
                            "}";

            // Execute the test script asynchronously
            Object scriptResult = browser.executeAsyncScript(testScript);

            if (scriptResult instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> testResults = (Map<String, Object>) scriptResult;
                results.putAll(testResults);
            } else {
                results.put("error", "Unexpected script result type: " +
                        (scriptResult != null ? scriptResult.getClass().getName() : "null"));
            }

            return results;
        } catch (Exception e) {
            results.put("error", "Error running connection test: " + e.getMessage());
            return results;
        }
    }

    /**
     * Create a new recording session
     *
     * @param requestBody Map containing session details
     * @return ResponseEntity with the created recording session
     */
    @PostMapping("/sessions")
    public ResponseEntity<RecordingSession> createSession(@RequestBody Map<String, Object> requestBody) {
        String name = (String) requestBody.get("name");
        String projectId = (String) requestBody.get("projectId");
        RecordingConfig config = (RecordingConfig) requestBody.get("config");
        String createdBy = (String) requestBody.get("createdBy");

        RecordingSession session = recorderService.createSession(name, projectId, config, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    /**
     * Get a recording session by ID
     *
     * @param sessionId The session ID
     * @return ResponseEntity with the recording session
     */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<RecordingSession> getSession(@PathVariable UUID sessionId) {
        RecordingSession session = recorderService.getSession(sessionId);

        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(session);
    }

    /**
     * Get all recording sessions for a project
     *
     * @param projectId The project ID
     * @return ResponseEntity with the list of recording sessions
     */
    @GetMapping("/sessions/project/{projectId}")
    public ResponseEntity<List<RecordingSession>> getSessionsByProject(@PathVariable String projectId) {
        List<RecordingSession> sessions = recorderService.getSessionsByProject(projectId);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Get all active recording sessions
     *
     * @return ResponseEntity with the list of active recording sessions
     */
    @GetMapping("/sessions/active")
    public ResponseEntity<List<RecordingSession>> getActiveSessions() {
        List<RecordingSession> sessions = recorderService.getActiveSessions();
        return ResponseEntity.ok(sessions);
    }

    /**
     * Add an event to a recording session by session key
     *
     * @param sessionKey The session key
     * @param event The event to add
     * @return ResponseEntity with success/failure status
     */
    @PostMapping("/sessions/key/{sessionKey}/events")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<?> addEventByKey(@PathVariable String sessionKey, @RequestBody RecordedEvent event) {
        boolean success = recorderService.addEventByKey(sessionKey, event);

        if (success) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Methods", "POST, GET, OPTIONS")
                    .header("Access-Control-Allow-Headers", "Content-Type")
                    .build();
        } else {
            return ResponseEntity.badRequest()
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Methods", "POST, GET, OPTIONS")
                    .header("Access-Control-Allow-Headers", "Content-Type")
                    .body("Failed to add event. Session may not exist.");
        }
    }

    /**
     * Update the status of a recording session
     *
     * @param sessionId The session ID
     * @param requestBody Map containing the new status
     * @return ResponseEntity with the updated recording session
     */
    @PatchMapping("/sessions/{sessionId}/status")
    public ResponseEntity<RecordingSession> updateSessionStatus(
            @PathVariable UUID sessionId,
            @RequestBody Map<String, String> requestBody) {

        String statusStr = requestBody.get("status");
        RecordingStatus status;

        try {
            status = RecordingStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        RecordingSession updatedSession = recorderService.updateSessionStatus(sessionId, status);

        if (updatedSession == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(updatedSession);
    }

    /**
     * Delete a recording session
     *
     * @param sessionId The session ID
     * @return ResponseEntity with success/failure status
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> deleteSession(@PathVariable UUID sessionId) {
        boolean success = recorderService.deleteSession(sessionId);

        if (success) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Create a loop event in a recording session
     *
     * @param requestBody Map containing loop details and parent event ID
     * @return ResponseEntity with the created loop event
     */
    @PostMapping("/loop")
    public ResponseEntity<LoopEvent> createLoop(@RequestBody Map<String, Object> requestBody) {
        String parentEventId = (String) requestBody.get("parentEventId");
        LoopConfig loopConfig = (LoopConfig) requestBody.get("loop");

        if (parentEventId == null || loopConfig == null) {
            return ResponseEntity.badRequest().build();
        }

        LoopEvent loopEvent = recorderService.createLoopEvent(UUID.fromString(parentEventId), loopConfig);

        if (loopEvent == null) {
            return ResponseEntity.badRequest().body(null);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(loopEvent);
    }

    /**
     * Update a loop configuration for an existing event
     *
     * @param eventId The event ID
     * @param loopConfig The updated loop configuration
     * @return ResponseEntity with the updated loop event
     */
    @PutMapping("/event/{eventId}/loop")
    public ResponseEntity<LoopEvent> updateLoop(
            @PathVariable UUID eventId,
            @RequestBody LoopConfig loopConfig) {

        LoopEvent updatedEvent = recorderService.updateLoopEvent(eventId, loopConfig);

        if (updatedEvent == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(updatedEvent);
    }

    /**
     * Add an event to a loop
     *
     * @param loopEventId The loop event ID
     * @param event The event to add to the loop
     * @return ResponseEntity with success status
     */
    @PostMapping("/loop/{loopEventId}/events")
    public ResponseEntity<?> addEventToLoop(
            @PathVariable UUID loopEventId,
            @RequestBody RecordedEvent event) {

        boolean success = recorderService.addEventToLoop(loopEventId, event);

        if (success) {
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } else {
            return ResponseEntity.badRequest()
                    .body("Failed to add event to loop. Loop may not exist or be in an invalid state.");
        }
    }

    /**
     * Remove an event from a loop
     *
     * @param loopEventId The loop event ID
     * @param eventId The ID of the event to remove
     * @return ResponseEntity with success status
     */
    @DeleteMapping("/loop/{loopEventId}/events/{eventId}")
    public ResponseEntity<?> removeEventFromLoop(
            @PathVariable UUID loopEventId,
            @PathVariable UUID eventId) {

        boolean success = recorderService.removeEventFromLoop(loopEventId, eventId);

        if (success) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all events in a loop
     *
     * @param loopEventId The loop event ID
     * @return ResponseEntity with the list of events in the loop
     */
    @GetMapping("/loop/{loopEventId}/events")
    public ResponseEntity<List<RecordedEvent>> getLoopEvents(@PathVariable UUID loopEventId) {
        List<RecordedEvent> events = recorderService.getLoopEvents(loopEventId);

        if (events == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(events);
    }

    /**
     * Stop a recording session
     *
     * @param requestBody Map containing session details
     * @return ResponseEntity with the updated recording session
     */
    @PostMapping("/stop")
    public ResponseEntity<RecordingSession> stopRecording(@RequestBody Map<String, Object> requestBody) {
        try {
            String sessionParam = (String) requestBody.getOrDefault("sessionId", "");
            if (StringUtils.hasLength(sessionParam) == false) {
                return ResponseEntity.badRequest().build();
            }

            UUID sessionId;
            try {
                sessionId = UUID.fromString(sessionParam);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid session ID format: {}", sessionParam);
                return ResponseEntity.badRequest().build();
            }

            // First, stop the browser to ensure it's closed
            logger.info("Stopping browser for recording session: {}", sessionId);
            browserManager.stopBrowser(sessionId);

            // Update the session status in the database
            RecordingSession session = recorderService.updateSessionStatus(sessionId, RecordingStatus.COMPLETED);

            if (session != null) {
                logger.info("Recording session {} stopped successfully", sessionId);

                // Notify clients via WebSocket that recording has stopped
                webSocketService.notifySessionStatusChanged(sessionId, RecordingStatus.COMPLETED);

                return ResponseEntity.ok(session);
            } else {
                logger.error("Failed to update session {} status to COMPLETED", sessionId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            logger.error("Error stopping recording session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check the status of a recording session
     *
     * @param sessionId The session ID
     * @return ResponseEntity with the recording session status and connection info
     */
    @GetMapping("/status/{sessionId}")
    public ResponseEntity<Map<String, Object>> getRecorderStatus(@PathVariable UUID sessionId) {
        RecordingSession session = recorderService.getSession(sessionId);

        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        boolean isBrowserActive = browserManager.isSessionActive(sessionId);

        Map<String, Object> status = new HashMap<>();
        status.put("session", session);
        status.put("browserActive", isBrowserActive);
        status.put("websocketStatus", "UNKNOWN"); // Can be updated with real WebSocket status if needed

        return ResponseEntity.ok(status);
    }

    /**
     * Debug endpoint for WebSocket connections
     *
     * @return Response with application WebSocket information
     */
    @GetMapping("/debug/websocket")
    public ResponseEntity<Map<String, Object>> debugWebSocket() {
        Map<String, Object> info = new HashMap<>();

        // Add WebSocket connection info
        info.put("websocketEndpoint", "/ws-recorder");
        info.put("sockJsEnabled", true);
        info.put("allowedOrigins", "http://localhost:8080, http://localhost:3000, *");
        info.put("topicPrefix", "/topic");
        info.put("appPrefix", "/app");
        info.put("userPrefix", "/user");
        info.put("serverTime", System.currentTimeMillis());
        info.put("activeWebSocketSessions", getActiveWebSocketSessionsCount());
        info.put("activeBrowsers", browserManager.getActiveBrowsers().size());
        info.put("serverInfo", getServerInfo());

        return ResponseEntity.ok(info);
    }

    /**
     * Get the number of active WebSocket sessions
     *
     * @return Map with active WebSocket sessions count
     */
    private int getActiveWebSocketSessionsCount() {
        try {
            // This is a simplified implementation
            // In a real implementation, you would get this from a WebSocket sessions registry
            return browserManager.getActiveBrowsers().size();
        } catch (Exception e) {
            logger.error("Error getting active WebSocket sessions", e);
            return -1;
        }
    }

    /**
     * Get basic server information
     *
     * @return Map with server information
     */
    private Map<String, Object> getServerInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("os", System.getProperty("os.name"));
        info.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        info.put("freeMemory", Runtime.getRuntime().freeMemory());
        info.put("maxMemory", Runtime.getRuntime().maxMemory());

        // Get server address information
        String serverHost = System.getProperty("server.address");
        if (serverHost == null || serverHost.isEmpty()) {
            serverHost = System.getProperty("server.host", "localhost");
        }
        String serverPort = System.getProperty("server.port", "8080");
        String contextPath = System.getProperty("server.servlet.context-path", "/cstestforge");

        info.put("serverHost", serverHost);
        info.put("serverPort", serverPort);
        info.put("contextPath", contextPath);
        info.put("apiBaseUrl", "http://" + serverHost + ":" + serverPort + contextPath + "/api");

        return info;
    }

    /**
     * Debug endpoint for checking recorder script
     *
     * @return RecorderScript with configuration values
     */
    @GetMapping("/debug/recorder-script")
    public ResponseEntity<Map<String, Object>> debugRecorderScript() {
        Map<String, Object> info = new HashMap<>();

        try {
            // Get server information
            String serverHost = System.getProperty("server.address");
            if (serverHost == null || serverHost.isEmpty()) {
                serverHost = System.getProperty("server.host", "localhost");
            }

            String serverPort = System.getProperty("server.port", "8080");
            String contextPath = System.getProperty("server.servlet.context-path", "/cstestforge");
            if (!contextPath.startsWith("/")) {
                contextPath = "/" + contextPath;
            }

            // Create WebSocket URL
            String wsUrl = String.format("http://%s:%s%s/ws-recorder", serverHost, serverPort, contextPath);

            // Create API endpoint pattern
            String apiEndpointPattern = String.format("http://%s:%s%s/api/recorder/events/SESSION_ID",
                    serverHost, serverPort, contextPath);

            // Load the script from file
            Path scriptPath = Paths.get("src/main/resources/static/js/recorder-script.js");
            String scriptContent = Files.exists(scriptPath) ? Files.readString(scriptPath) : "Script file not found";

            // Add information to response
            info.put("serverHost", serverHost);
            info.put("serverPort", serverPort);
            info.put("contextPath", contextPath);
            info.put("wsUrl", wsUrl);
            info.put("apiEndpointPattern", apiEndpointPattern);
            info.put("scriptExists", Files.exists(scriptPath));
            info.put("scriptLength", scriptContent.length());
            info.put("scriptFirstLines", scriptContent.substring(0, Math.min(500, scriptContent.length())));
            info.put("webSocketEndpoints", List.of("/ws-recorder", "/ws-recorder-sockjs"));
            info.put("recorderApiEndpoints", List.of(
                    "/api/recorder/start",
                    "/api/recorder/stop",
                    "/api/recorder/events/{sessionId}"
            ));

            // Add active browsers
            Map<UUID, String> activeBrowsersMap = new HashMap<>();
            browserManager.getActiveBrowsers().forEach((sessionId, browser) -> {
                try {
                    activeBrowsersMap.put(sessionId, browser.getCurrentUrl());
                } catch (Exception e) {
                    activeBrowsersMap.put(sessionId, "Error getting URL: " + e.getMessage());
                }
            });
            info.put("activeBrowsers", activeBrowsersMap);

            // Add WebSocket session info
            info.put("activeWebSocketSessions", getActiveWebSocketSessionsCount());

            return ResponseEntity.ok(info);
        } catch (Exception e) {
            logger.error("Error generating debug information: {}", e.getMessage(), e);
            info.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(info);
        }
    }

    /**
     * Debug endpoint for checking recorder script status in browser
     *
     * @param sessionId The session ID
     * @return Debug information about the recorder script
     */
    @GetMapping("/debug/browser/{sessionId}")
    public ResponseEntity<Map<String, Object>> debugBrowserScript(@PathVariable UUID sessionId) {
        if (!browserManager.isSessionActive(sessionId)) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No active browser session found for ID: " + sessionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        try {
            Map<String, Object> debug = browserManager.debugRecorderScript(sessionId);
            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            logger.error("Error debugging browser script: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error debugging browser script: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Handle browser close event
     *
     * @param sessionId The session ID
     * @return ResponseEntity with success/failure status
     */
    @PostMapping("/browser-close/{sessionId}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<?> handleBrowserClose(@PathVariable UUID sessionId) {
        logger.info("Received browser close event for session: {}", sessionId);

        try {
            // First, update the session status in the database
            RecordingSession session = recorderService.updateSessionStatus(sessionId, RecordingStatus.COMPLETED);

            if (session != null) {
                // Stop the browser
                browserManager.stopBrowser(sessionId);

                // Notify clients via WebSocket
                webSocketService.notifySessionStatusChanged(sessionId, RecordingStatus.COMPLETED);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Recording session stopped due to browser close");

                return ResponseEntity.ok()
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Methods", "POST, GET, OPTIONS")
                        .header("Access-Control-Allow-Headers", "Content-Type")
                        .body(response);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Methods", "POST, GET, OPTIONS")
                        .header("Access-Control-Allow-Headers", "Content-Type")
                        .body(Map.of("success", false, "error", "Session not found"));
            }
        } catch (Exception e) {
            logger.error("Error handling browser close event: {}", e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Methods", "POST, GET, OPTIONS")
                    .header("Access-Control-Allow-Headers", "Content-Type")
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get all recordings
     *
     * @return ResponseEntity with the list of all recording sessions
     */
    @GetMapping("/recordings")
    public ResponseEntity<List<RecordingSession>> getAllRecordings() {
        try {
            List<RecordingSession> recordings = recorderService.getAllSessions();
            return ResponseEntity.ok(recordings);
        } catch (Exception e) {
            logger.error("Error fetching all recordings: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a recording
     *
     * @param id The recording ID
     * @return ResponseEntity with success/failure status
     */
    @DeleteMapping("/recordings/{id}")
    public ResponseEntity<?> deleteRecording(@PathVariable UUID id) {
        try {
            // Check if the session is active
            if (browserManager.isSessionActive(id)) {
                // Stop the browser first
                browserManager.stopBrowser(id);
            }

            // Delete the session
            boolean deleted = recorderService.deleteSession(id);
            if (deleted) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error deleting recording {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete recording: " + e.getMessage()));
        }
    }

    /**
     * Generate code for a recording
     *
     * @param id The recording ID
     * @param options The code generation options
     * @return ResponseEntity with the generated code
     */
    @PostMapping("/generate-code/{id}")
    public ResponseEntity<Map<String, Object>> generateCode(
            @PathVariable UUID id,
            @RequestBody Map<String, String> options) {
        try {
            // Get the recording session
            RecordingSession session = recorderService.getSession(id);
            if (session == null) {
                return ResponseEntity.notFound().build();
            }

            // Get options
            String framework = options.getOrDefault("framework", "selenium-java");
            String language = options.getOrDefault("language", "java");

            // Create a code generation request for the CodeBuilderController
            CodeGenerationRequest codeRequest = new CodeGenerationRequest();
            codeRequest.setSourceId("rec_" + id.toString());
            codeRequest.setFramework(framework);
            codeRequest.setLanguage(language);

            // Use the CodeBuilderController to generate the code
            ResponseEntity<GeneratedCode> codeResponse =
                    codeBuilderController.generateCodeFromRecording(
                            id.toString(), framework, language, options.getOrDefault("templateId", null));

            // Create response
            Map<String, Object> response = new HashMap<>();
            if (codeResponse.getStatusCode().is2xxSuccessful() && codeResponse.getBody() != null) {
                GeneratedCode generatedCode = codeResponse.getBody();
                response.put("code", generatedCode.getCode());
                response.put("framework", framework);
                response.put("language", language);
                response.put("recordingId", id.toString());
                response.put("recordingName", session.getName());
                response.put("pageObjectCode", generatedCode.getPageObjectCode());
                response.put("generatedAt", generatedCode.getGeneratedAt());
                response.put("id", generatedCode.getId());

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to generate code",
                                "details", "Code generation service returned an error"));
            }
        } catch (Exception e) {
            logger.error("Error generating code for recording {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate code: " + e.getMessage()));
        }
    }
}