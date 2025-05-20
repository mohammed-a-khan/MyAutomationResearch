package com.cstestforge.recorder.browser;

import com.cstestforge.recorder.model.BrowserType;
import com.cstestforge.recorder.model.RecordingConfig;
import com.cstestforge.recorder.model.Viewport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Playwright-based implementation of BrowserInstance that communicates with a TypeScript service.
 */
public class PlaywrightBrowserInstance extends AbstractBrowserInstance {
    private static final Logger logger = LoggerFactory.getLogger(PlaywrightBrowserInstance.class);
    
    private static final String DEFAULT_SERVICE_URL = "http://localhost:3500";
    private static final String PLAYWRIGHT_SERVICE_SCRIPT = "playwright-service.js";
    private static final int DEFAULT_SERVICE_PORT = 3500;
    
    private String serviceUrl;
    private Process serviceProcess;
    private RestTemplate restTemplate;
    private String sessionKey;
    private String browserName;
    
    /**
     * Constructor for PlaywrightBrowserInstance
     *
     * @param sessionId The session ID
     * @param browserType The browser type
     * @param config The recording configuration
     */
    public PlaywrightBrowserInstance(UUID sessionId, BrowserType browserType, RecordingConfig config) {
        super(sessionId, browserType, config);
        this.restTemplate = new RestTemplate();
        this.serviceUrl = config.getPlaywrightServiceUrl() != null ? 
                config.getPlaywrightServiceUrl() : DEFAULT_SERVICE_URL;
        this.sessionKey = sessionId.toString();
        
        switch (browserType) {
            case CHROME_PLAYWRIGHT:
                this.browserName = "chromium";
                break;
            case FIREFOX_PLAYWRIGHT:
                this.browserName = "firefox";
                break;
            case WEBKIT_PLAYWRIGHT:
                this.browserName = "webkit";
                break;
            case MSEDGE_PLAYWRIGHT:
                this.browserName = "msedge";
                break;
            default:
                this.browserName = "chromium";
        }
    }
    
    @Override
    protected boolean startBrowser(long timeout, TimeUnit unit) {
        try {
            // Start the TypeScript Playwright service if not already running
            if (!isServiceRunning()) {
                startPlaywrightService();
                
                // Wait for service to be ready
                long timeoutMs = unit.toMillis(timeout);
                long startTime = System.currentTimeMillis();
                boolean isReady = false;
                
                while (System.currentTimeMillis() - startTime < timeoutMs) {
                    if (isServiceRunning()) {
                        isReady = true;
                        break;
                    }
                    Thread.sleep(500);
                }
                
                if (!isReady) {
                    logger.error("Playwright service did not start in time");
                    return false;
                }
            }
            
            // Create a browser session via the service
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("sessionId", sessionKey);
            requestBody.put("browserType", browserName);
            requestBody.put("headless", false);
            
            if (config.getViewport() != null) {
                Map<String, Integer> viewport = new HashMap<>();
                viewport.put("width", config.getViewport().getWidth());
                viewport.put("height", config.getViewport().getHeight());
                requestBody.put("viewport", viewport);
            }
            
            // Add any environment variables
            if (config.getEnvironmentVariables() != null && !config.getEnvironmentVariables().isEmpty()) {
                requestBody.put("environmentVariables", config.getEnvironmentVariables());
            }
            
            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(
                        serviceUrl + "/browser/start", 
                        createJsonHttpEntity(requestBody), 
                        Map.class);
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    logger.info("Successfully started Playwright browser session: {}", sessionKey);
                    return true;
                } else {
                    logger.error("Failed to start Playwright browser: {}", response.getBody());
                    return false;
                }
            } catch (Exception e) {
                logger.error("Error starting Playwright browser: {}", e.getMessage(), e);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error during Playwright browser initialization: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    protected void stopBrowser() {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("sessionId", sessionKey);
            
            try {
                restTemplate.postForEntity(
                        serviceUrl + "/browser/stop", 
                        createJsonHttpEntity(requestBody), 
                        Map.class);
                
                logger.info("Stopped Playwright browser session: {}", sessionKey);
            } catch (Exception e) {
                logger.error("Error stopping Playwright browser session: {}", e.getMessage());
            }
        } finally {
            // Let the service keep running for other sessions
            // If we're the last session, the service will auto-shutdown after a timeout
        }
    }
    
    @Override
    public boolean navigate(String url) {
        checkServiceRunning();
        
        if (url == null || url.trim().isEmpty()) {
            logger.error("Cannot navigate to null or empty URL");
            return false;
        }
        
        logger.info("Navigating to URL: {}", url);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("sessionId", sessionKey);
        requestBody.put("url", url);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    serviceUrl + "/browser/navigate", 
                    createJsonHttpEntity(requestBody), 
                    Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseData = response.getBody();
                Boolean success = (Boolean) responseData.getOrDefault("success", false);
                currentUrl = (String) responseData.getOrDefault("url", url);
                currentTitle = (String) responseData.getOrDefault("title", "");
                
                logger.info("Navigation to {} {}", url, success ? "succeeded" : "failed");
                logger.debug("Current URL: {}, Title: {}", currentUrl, currentTitle);
                
                return success;
            } else {
                logger.error("Failed to navigate: {}", response.getBody());
                return false;
            }
        } catch (Exception e) {
            logger.error("Error during navigation: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public void setViewport(Viewport viewport) {
        if (viewport == null) return;
        
        checkServiceRunning();
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("sessionId", sessionKey);
        requestBody.put("width", viewport.getWidth());
        requestBody.put("height", viewport.getHeight());
        
        try {
            restTemplate.postForEntity(
                    serviceUrl + "/browser/viewport", 
                    createJsonHttpEntity(requestBody), 
                    Map.class);
        } catch (Exception e) {
            logger.error("Failed to set viewport: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public Object injectScript(String script) {
        checkServiceRunning();
        
        if (script == null || script.isEmpty()) {
            return null;
        }
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("sessionId", sessionKey);
        requestBody.put("script", script);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    serviceUrl + "/browser/inject", 
                    createJsonHttpEntity(requestBody), 
                    Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.debug("Script injected successfully");
                return true;
            } else {
                logger.error("Failed to inject script: {}", response.getBody());
                return null;
            }
        } catch (Exception e) {
            logger.error("Error injecting script: {}", e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public Object executeAsyncScript(String script, Object... args) {
        checkServiceRunning();
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("sessionId", sessionKey);
        requestBody.put("script", script);
        requestBody.put("args", args);
        requestBody.put("async", true);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    serviceUrl + "/browser/execute", 
                    createJsonHttpEntity(requestBody), 
                    Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().get("result");
            }
        } catch (Exception e) {
            logger.error("Error executing async script: {}", e.getMessage(), e);
        }
        return null;
    }
    
    @Override
    public Object executeScript(String script, Object... args) {
        checkServiceRunning();
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("sessionId", sessionKey);
        requestBody.put("script", script);
        requestBody.put("args", args);
        requestBody.put("async", false);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    serviceUrl + "/browser/execute", 
                    createJsonHttpEntity(requestBody), 
                    Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().get("result");
            }
        } catch (Exception e) {
            logger.error("Error executing script: {}", e.getMessage(), e);
        }
        return null;
    }
    
    @Override
    public byte[] captureScreenshot() {
        checkServiceRunning();
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("sessionId", sessionKey);
        requestBody.put("fullPage", true);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    serviceUrl + "/browser/screenshot", 
                    createJsonHttpEntity(requestBody), 
                    Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String base64Image = (String) response.getBody().get("data");
                if (base64Image != null && !base64Image.isEmpty()) {
                    return Base64.getDecoder().decode(base64Image);
                }
            }
        } catch (Exception e) {
            logger.error("Error capturing screenshot: {}", e.getMessage(), e);
        }
        return null;
    }
    
    @Override
    public byte[] captureElementScreenshot(String selector) {
        checkServiceRunning();
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("sessionId", sessionKey);
        requestBody.put("selector", selector);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    serviceUrl + "/browser/element-screenshot", 
                    createJsonHttpEntity(requestBody), 
                    Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String base64Image = (String) response.getBody().get("data");
                if (base64Image != null && !base64Image.isEmpty()) {
                    return Base64.getDecoder().decode(base64Image);
                }
            }
        } catch (Exception e) {
            logger.error("Error capturing element screenshot: {}", e.getMessage(), e);
        }
        return null;
    }
    
    @Override
    public String getCurrentUrl() {
        checkServiceRunning();
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("sessionId", sessionKey);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    serviceUrl + "/browser/info", 
                    createJsonHttpEntity(requestBody), 
                    Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                currentUrl = (String) response.getBody().getOrDefault("url", currentUrl);
            }
        } catch (Exception e) {
            logger.error("Error getting current URL: {}", e.getMessage(), e);
        }
        return currentUrl;
    }
    
    @Override
    public String getTitle() {
        checkServiceRunning();
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("sessionId", sessionKey);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    serviceUrl + "/browser/info", 
                    createJsonHttpEntity(requestBody), 
                    Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                currentTitle = (String) response.getBody().getOrDefault("title", currentTitle);
            }
        } catch (Exception e) {
            logger.error("Error getting page title: {}", e.getMessage(), e);
        }
        return currentTitle;
    }
    
    @Override
    public void setEnvironmentVariables(Map<String, String> environmentVariables) {
        // Environment variables must be set during browser launch
        logger.debug("Environment variables can only be set before browser start");
    }
    
    @Override
    public void setNetworkCapturing(boolean enabled) {
        super.setNetworkCapturing(enabled);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("sessionId", sessionKey);
        requestBody.put("enabled", enabled);
        
        try {
            restTemplate.postForEntity(
                    serviceUrl + "/browser/network-capture", 
                    createJsonHttpEntity(requestBody), 
                    Map.class);
        } catch (Exception e) {
            logger.error("Failed to set network capturing: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void setConsoleCapturing(boolean enabled) {
        super.setConsoleCapturing(enabled);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("sessionId", sessionKey);
        requestBody.put("enabled", enabled);
        
        try {
            restTemplate.postForEntity(
                    serviceUrl + "/browser/console-capture", 
                    createJsonHttpEntity(requestBody), 
                    Map.class);
        } catch (Exception e) {
            logger.error("Failed to set console capturing: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public boolean waitForCondition(String conditionScript, long timeoutMs) {
        checkServiceRunning();
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("sessionId", sessionKey);
        requestBody.put("condition", conditionScript);
        requestBody.put("timeout", timeoutMs);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    serviceUrl + "/browser/wait", 
                    createJsonHttpEntity(requestBody), 
                    Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Boolean.TRUE.equals(response.getBody().get("result"));
            }
        } catch (Exception e) {
            logger.error("Error waiting for condition: {}", e.getMessage(), e);
        }
        return false;
    }
    
    @Override
    protected boolean isResponsive() {
        if (!isServiceRunning()) {
            return false;
        }
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("sessionId", sessionKey);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    serviceUrl + "/browser/ping", 
                    createJsonHttpEntity(requestBody), 
                    Map.class);
            
            return response.getStatusCode().is2xxSuccessful() && 
                   Boolean.TRUE.equals(response.getBody().get("isAlive"));
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    protected boolean injectRecorderScript() {
        return injectScript("true;") != null;
    }
    
    /**
     * Start the TypeScript Playwright service
     * 
     * @throws IOException if there is an error starting the service
     */
    private void startPlaywrightService() throws IOException {
        String nodeExe = findNodeExecutable();
        if (nodeExe == null) {
            throw new IOException("Node.js executable not found");
        }
        
        // Check if the service script exists
        String scriptPath = getPlaywrightServiceScriptPath();
        Path scriptFile = Paths.get(scriptPath);
        
        if (!Files.exists(scriptFile)) {
            throw new IOException("Playwright service script not found: " + scriptPath);
        }
        
        // Build the command to start the service
        ProcessBuilder processBuilder = new ProcessBuilder(
                nodeExe,
                scriptPath,
                "--port=" + DEFAULT_SERVICE_PORT
        );
        
        processBuilder.redirectErrorStream(true);
        
        // Start the service process
        serviceProcess = processBuilder.start();
        
        // Monitor the process output in a separate thread
        CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(serviceProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.debug("Playwright Service: {}", line);
                    if (line.contains("Playwright service started on port")) {
                        logger.info("Playwright service started successfully");
                    }
                }
            } catch (IOException e) {
                logger.error("Error reading from Playwright service: {}", e.getMessage(), e);
            }
        });
        
        // Add shutdown hook to stop the service
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (serviceProcess != null && serviceProcess.isAlive()) {
                serviceProcess.destroy();
                logger.info("Playwright service stopped during shutdown");
            }
        }));
    }
    
    /**
     * Check if the Playwright service is running
     * 
     * @return true if the service is running
     */
    private boolean isServiceRunning() {
        if (serviceProcess != null && serviceProcess.isAlive()) {
            try {
                // Ping the service API
                ResponseEntity<Map> response = restTemplate.getForEntity(serviceUrl + "/health", Map.class);
                return response.getStatusCode().is2xxSuccessful();
            } catch (Exception e) {
                // Service not responding to API calls
                return false;
            }
        }
        return false;
    }
    
    /**
     * Find the Node.js executable on the system
     * 
     * @return Path to Node.js executable or null if not found
     */
    private String findNodeExecutable() {
        // First check if NODE_PATH environment variable is set
        String nodePath = System.getenv("NODE_PATH");
        if (nodePath != null && !nodePath.isEmpty()) {
            File nodeFile = new File(nodePath);
            if (nodeFile.exists() && nodeFile.canExecute()) {
                return nodeFile.getAbsolutePath();
            }
        }
        
        // Check common locations
        String[] possibleLocations;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            possibleLocations = new String[]{
                "node",
                "C:\\Program Files\\nodejs\\node.exe",
                "C:\\Program Files (x86)\\nodejs\\node.exe"
            };
        } else {
            possibleLocations = new String[]{
                "node",
                "/usr/bin/node",
                "/usr/local/bin/node"
            };
        }
        
        // Try each location
        for (String location : possibleLocations) {
            try {
                ProcessBuilder pb = new ProcessBuilder(location, "--version");
                Process process = pb.start();
                if (process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0) {
                    return location;
                }
            } catch (Exception ignored) {
                // Try next location
            }
        }
        
        return null;
    }
    
    /**
     * Get the path to the Playwright service script
     * 
     * @return Path to the service script
     */
    private String getPlaywrightServiceScriptPath() {
        // First check if PLAYWRIGHT_SERVICE_PATH environment variable is set
        String servicePath = System.getenv("PLAYWRIGHT_SERVICE_PATH");
        if (servicePath != null && !servicePath.isEmpty()) {
            return servicePath;
        }
        
        // Use the default location
        return "src/main/resources/static/js/" + PLAYWRIGHT_SERVICE_SCRIPT;
    }
    
    /**
     * Create an HttpEntity with JSON headers
     * 
     * @param body The request body
     * @return HttpEntity with JSON content type
     */
    private HttpEntity<Object> createJsonHttpEntity(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
    
    /**
     * Check if the service is running and throw an exception if not
     * 
     * @throws IllegalStateException if the service is not running
     */
    private void checkServiceRunning() {
        if (!isServiceRunning()) {
            throw new IllegalStateException("Playwright service is not running");
        }
    }
} 