package com.cstestforge.recorder.browser;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v85.network.Network;
import org.openqa.selenium.devtools.v85.console.Console;
import org.openqa.selenium.devtools.v85.page.Page;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cstestforge.recorder.model.BrowserType;
import com.cstestforge.recorder.model.RecordingConfig;
import com.cstestforge.recorder.model.Viewport;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Chrome-specific implementation of BrowserInstance using Selenium WebDriver.
 * Enhanced with improved CSP handling and script injection capabilities.
 */
public class ChromeBrowserInstance extends AbstractBrowserInstance {
    private static final Logger logger = LoggerFactory.getLogger(ChromeBrowserInstance.class);

    private WebDriver driver;
    private DevTools devTools;
    private ChromeDriverService driverService;
    private boolean devToolsEnabled = false;

    /**
     * Constructor for ChromeBrowserInstance
     *
     * @param sessionId The session ID
     * @param browserType The browser type
     * @param config The recording configuration
     */
    public ChromeBrowserInstance(UUID sessionId, BrowserType browserType, RecordingConfig config) {
        super(sessionId, browserType, config);
    }

    @Override
    protected boolean startBrowser(long timeout, TimeUnit unit) {
        try {
            // Setup Chrome options with better compatibility
            ChromeOptions options = new ChromeOptions();

            // Add arguments to Chrome for better compatibility with recorder
            options.addArguments("--disable-web-security"); // Allow cross-origin requests
            options.addArguments("--disable-popup-blocking"); // Prevent popup blocking
            options.addArguments("--disable-extensions"); // Disable extensions that might interfere
            options.addArguments("--start-maximized"); // Start maximized
            options.addArguments("--remote-allow-origins=*"); // Allow remote origins
            options.addArguments("--disable-features=IsolateOrigins,site-per-process"); // Disable site isolation
            options.addArguments("--disable-site-isolation-trials"); // Disable site isolation trials

            // Disable content security policy for better script injection
            options.addArguments("--disable-content-security-policy");
            options.addArguments("--allow-running-insecure-content"); // Allow insecure content
            options.addArguments("--allow-file-access-from-files"); // Allow access to local files
            options.addArguments("--no-sandbox"); // Disable sandbox for better compatibility
            options.addArguments("--disable-dev-shm-usage"); // Overcome limited resources in some CI environments
            options.addArguments("--disable-gpu"); // Disable GPU acceleration (unnecessary for our use case)
            options.addArguments("--ignore-certificate-errors"); // Ignore certificate errors

            // Set experimental options
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("dom.webnotifications.enabled", false); // Disable notifications
            prefs.put("profile.default_content_setting_values.notifications", 2); // Also disable notifications

            // Allow JavaScript from all sources (helps with script injection)
            prefs.put("profile.content_settings.exceptions.javascript", new HashMap<>());

            // Disable strict origins policy
            prefs.put("profile.content_settings.exceptions.strict_origin_isolation", new HashMap<>());

            // Bypass CSP - critical for recorder script to work
            prefs.put("profile.default_content_setting_values.automatic_downloads", 1);
            prefs.put("profile.content_settings.exceptions.automatic_downloads.*.setting", 1);
            prefs.put("profile.content_settings.enable_quiet_permission_ui_enabling_method.automatic_downloads", 1);

            // Setting to bypass CSP for websockets and XHR
            prefs.put("profile.content_settings.exceptions.plugins.*.per_origin.content_setting", 1);
            prefs.put("profile.managed_default_content_settings.plugins", 1);
            prefs.put("profile.default_content_settings.popups", 0);

            options.setExperimentalOption("prefs", prefs);

            // Apply any specific browser options from the config
            if (config.getBrowserOptions() != null && !config.getBrowserOptions().isEmpty()) {
                Map<String, Object> configOptions = config.getBrowserOptions();

                for (Map.Entry<String, Object> entry : configOptions.entrySet()) {
                    String optionKey = entry.getKey();
                    Object optionValue = entry.getValue();

                    // Handle different types of browser options
                    if (optionKey.startsWith("argument.") && optionValue instanceof String) {
                        options.addArguments((String) optionValue);
                    } else if (optionKey.startsWith("experimental.") && optionValue instanceof String) {
                        String expKey = optionKey.substring("experimental.".length());
                        prefs.put(expKey, optionValue);
                    } else if (optionValue instanceof Boolean && (Boolean) optionValue) {
                        // For boolean true options, add them as arguments
                        switch (optionKey) {
                            case "disable_web_security":
                                options.addArguments("--disable-web-security");
                                break;
                            case "disable_content_security_policy":
                                options.addArguments("--disable-content-security-policy");
                                break;
                            case "ignore_certificate_errors":
                                options.addArguments("--ignore-certificate-errors");
                                break;
                            case "allow_running_insecure_content":
                                options.addArguments("--allow-running-insecure-content");
                                break;
                            // Add more options as needed
                        }
                    }
                }

                // Apply any updated experimental options
                options.setExperimentalOption("prefs", prefs);
            }

            // Store environment variables in a property where they can be accessed later
            if (config.getEnvironmentVariables() != null && !config.getEnvironmentVariables().isEmpty()) {
                logger.info("Framework configuration: {}", config.getEnvironmentVariables());
            }

            logger.info("Starting Chrome browser with options: {}", options);

            // Start Chrome driver
            driverService = new ChromeDriverService.Builder().build();
            driver = new ChromeDriver(driverService, options);

            // Set default timeouts for better reliability
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30));
            driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(120));
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(120));

            // Initialize DevTools (for CDP features)
            if (driver instanceof ChromeDriver) {
                try {
                    devTools = ((ChromeDriver) driver).getDevTools();
                    devTools.createSession();
                    devToolsEnabled = true;

                    // Enable network events if requested
                    if (config.isCaptureNetwork()) {
                        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));

                        // Add network event listeners
                        devTools.addListener(Network.requestWillBeSent(), request -> {
                            logger.debug("Request sent: {}", request.getRequest().getUrl());
                        });

                        devTools.addListener(Network.responseReceived(), response -> {
                            logger.debug("Response received: {} ({})",
                                    response.getResponse().getUrl(),
                                    response.getResponse().getStatus());
                        });
                    }

                    // Enable console events
                    if (consoleCapturingEnabled.get()) {
                        devTools.send(Console.enable());

                        // Add console event listeners
                        devTools.addListener(Console.messageAdded(), message -> {
                            logger.debug("Console {}: {}", message.getLevel(), message.getText());
                        });
                    }

                    // Disable security features that might block script execution
                    try {
                        devTools.send(Page.enable());
                        devTools.send(Page.setBypassCSP(true));
                        logger.debug("CSP bypass enabled via DevTools");
                    } catch (Exception e) {
                        logger.warn("Failed to bypass CSP via DevTools: {}", e.getMessage());
                    }
                } catch (Exception e) {
                    logger.warn("Failed to initialize Chrome DevTools: {}", e.getMessage());
                    devToolsEnabled = false;
                    // Continue even if DevTools initialization fails
                }
            }

            return true;
        } catch (Exception e) {
            logger.error("Failed to start Chrome browser: {}", e.getMessage(), e);
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception ex) {
                    logger.error("Error cleaning up driver: {}", ex.getMessage());
                }
                driver = null;
            }
            return false;
        }
    }

    /**
     * Create a method to check if the driver is still valid
     * This helps prevent "invalid session id" errors
     */
    public boolean isDriverValid() {
        if (driver == null) {
            return false;
        }

        try {
            // A simple operation to check if the driver is still valid
            driver.getCurrentUrl();
            return true;
        } catch (Exception e) {
            if (e.getMessage() != null &&
                    (e.getMessage().contains("invalid session id") ||
                            e.getMessage().contains("session deleted") ||
                            e.getMessage().contains("browser has closed"))) {
                logger.debug("Driver session is invalid: {}", e.getMessage());
                return false;
            }
            // If it's some other error, the driver might still be valid
            logger.warn("Error checking driver validity: {}", e.getMessage());
            return true;
        }
    }

    @Override
    protected void stopBrowser() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                logger.error("Error closing Chrome browser: {}", e.getMessage());
            } finally {
                driver = null;
            }
        }

        if (driverService != null && driverService.isRunning()) {
            try {
                driverService.stop();
            } catch (Exception e) {
                logger.error("Error stopping Chrome driver service: {}", e.getMessage());
            }
        }
    }

    @Override
    public boolean navigate(String url) {
        try {
            if (!isDriverValid()) {
                logger.error("Cannot navigate - driver session is invalid");
                return false;
            }

            if (url == null || url.trim().isEmpty()) {
                logger.error("Cannot navigate to null or empty URL");
                return false;
            }

            // Ensure URL has a proper protocol
            String navigateUrl = url;
            if (!navigateUrl.startsWith("http://") &&
                    !navigateUrl.startsWith("https://") &&
                    !navigateUrl.equals("about:blank")) { // Special handling for about:blank
                navigateUrl = "https://" + navigateUrl;
                logger.info("Added https:// prefix to URL: {}", navigateUrl);
            }

            logger.info("Navigating to URL: {}", navigateUrl);

            // Set a page load timeout
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(120));

            try {
                // First check if the URL is valid
                if (!navigateUrl.equals("about:blank")) { // Skip validation for about:blank
                    boolean isValid = (boolean) executeScript(
                            "try { new URL(arguments[0]); return true; } catch(e) { return false; }",
                            navigateUrl);

                    if (!isValid) {
                        logger.error("Invalid URL format: {}", navigateUrl);
                        return false;
                    }
                }

                // Apply CSP bypass before navigation
                applyCSPBypass();
                disableCSP();

                // Perform the navigation
                driver.get(navigateUrl);

                // Get the current URL and title
                currentUrl = driver.getCurrentUrl();
                currentTitle = driver.getTitle();

                logger.info("Navigation performed, current URL: {}, title: {}", currentUrl, currentTitle);
            } catch (Exception e) {
                logger.error("Error during navigation: {}", e.getMessage());
                return false;
            }

            // Wait for page to load with better error handling
            try {
                // First wait for document.readyState to be 'complete'
                boolean loaded = waitForDocumentReadyWithRetry(60);

                if (!loaded) {
                    logger.warn("Page load timeout occurred for URL: {}", navigateUrl);
                } else {
                    logger.info("Document ready state is complete for URL: {}", navigateUrl);
                }

                // Regardless of readyState, check if we actually arrived at the URL
                // or were redirected elsewhere
                currentUrl = driver.getCurrentUrl();

                // Apply CSP bypasses again after navigation
                applyCSPBypass();
                disableCSP();

                // Wait a bit longer for scripts to initialize
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                logger.info("Successfully navigated to URL: {} (actual: {})", navigateUrl, currentUrl);
                return true;
            } catch (Exception e) {
                logger.warn("Navigation completed but page may not be fully loaded: {}", navigateUrl);
                // Still consider navigation successful even if page load timeout occurs
                return true;
            }
        } catch (Exception e) {
            logger.error("Failed to navigate to URL {}: {}", url, e.getMessage(), e);
            return false;
        }
    }

    /**
     * New method to wait for document ready state with retry
     */
    private boolean waitForDocumentReadyWithRetry(int timeoutSeconds) {
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000);
        int attempts = 0;

        while (System.currentTimeMillis() < endTime && attempts < 3) {
            try {
                Object readyState = executeScript("return document.readyState");
                logger.debug("Document ready state: {}", readyState);

                if ("complete".equals(readyState)) {
                    return true;
                }

                // Wait a bit before checking again
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            } catch (Exception e) {
                logger.warn("Error checking document ready state (attempt {}): {}", attempts + 1, e.getMessage());
                attempts++;

                // If the session is invalid, stop trying
                if (e.getMessage() != null &&
                        (e.getMessage().contains("invalid session id") ||
                                e.getMessage().contains("session deleted") ||
                                e.getMessage().contains("browser has closed"))) {
                    return false;
                }

                // Small delay before retry
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        // Return false if we exceeded timeout or max attempts
        return false;
    }

    @Override
    public void setViewport(Viewport viewport) {
        if (!isDriverValid()) {
            logger.error("Cannot set viewport - driver session is invalid");
            return;
        }

        if (viewport != null) {
            try {
                driver.manage().window().setSize(
                        new org.openqa.selenium.Dimension(viewport.getWidth(), viewport.getHeight()));
            } catch (Exception e) {
                logger.error("Error setting viewport: {}", e.getMessage());
            }
        }
    }

    @Override
    public Object injectScript(String script) {
        if (!isDriverValid()) {
            logger.error("Cannot inject script - driver session is invalid");
            return null;
        }

        if (script == null || script.isEmpty()) {
            return null;
        }

        try {
            // Log that we're trying to inject a script
            logger.debug("Injecting script with length: {} characters", script.length());

            // If script is too large, split it into smaller chunks
            if (script.length() > 10000) {
                return injectLargeScript(script);
            }

            // Try multiple injection methods for better cross-site compatibility

            // Method 1: Direct executeScript (best for CSP compatibility)
            try {
                Object result = executeScript(script);
                if (result != null) {
                    logger.debug("Direct script execution result: {}", result);
                    return result;
                }
            } catch (Exception e) {
                logger.warn("Direct script execution failed: {}", e.getMessage());
            }

            // If the driver is no longer valid, don't try other methods
            if (!isDriverValid()) {
                logger.error("Driver session became invalid during script injection");
                return null;
            }

            // Method 2: Create script element in head (more compatible with CSP)
            try {
                String createHeadScript =
                        "try {\n" +
                                "  const script = document.createElement('script');\n" +
                                "  script.id = 'cs-recorder-script';\n" +
                                "  script.textContent = arguments[0];\n" +
                                "  if (document.head) {\n" +
                                "    document.head.appendChild(script);\n" +
                                "    return 'Script added to head';\n" +
                                "  } else if (document.documentElement) {\n" +
                                "    // Insert at beginning of document if head not available\n" +
                                "    document.documentElement.insertBefore(script, document.documentElement.firstChild);\n" +
                                "    return 'Script added to documentElement';\n" +
                                "  }\n" +
                                "  return false;\n" +
                                "} catch(e) {\n" +
                                "  console.error('Script creation error:', e);\n" +
                                "  return { error: e.toString() };\n" +
                                "}";

                Object result = executeScript(createHeadScript, script);
                if (result != null && !(result instanceof Boolean && !(Boolean)result)) {
                    logger.debug("Script element injection result: {}", result);
                    return result;
                } else {
                    logger.warn("Script element injection failed or returned false: {}", result);
                }
            } catch (Exception e) {
                logger.warn("Script element injection exception: {}", e.getMessage());
            }

            // If the driver is no longer valid, don't try other methods
            if (!isDriverValid()) {
                logger.error("Driver session became invalid during script injection");
                return null;
            }

            // Method 3: Use async setTimeout to delay execution (can help with some timing issues)
            try {
                String asyncScript =
                        "try {\n" +
                                "  setTimeout(function() {\n" +
                                "    try {\n" +
                                "      eval(arguments[0]);\n" +
                                "    } catch(e) {\n" +
                                "      console.error('Delayed script execution error:', e);\n" +
                                "    }\n" +
                                "  }, 0);\n" +
                                "  return 'Async execution scheduled';\n" +
                                "} catch(e) {\n" +
                                "  return { error: e.toString() };\n" +
                                "}";

                Object result = executeScript(asyncScript, script);
                logger.debug("Async script scheduling result: {}", result);
                return result;
            } catch (Exception e) {
                logger.error("All script injection methods failed: {}", e.getMessage(), e);
                return null;
            }
        } catch (Exception e) {
            logger.error("Error injecting script: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Inject a large script by splitting it into smaller chunks to avoid timeouts
     */
    private Object injectLargeScript(String script) {
        try {
            logger.info("Script is large ({}), splitting into chunks", script.length());

            // First try to inject a simple test script to check if injection works
            Object testResult = executeScript("return 'Test script execution'");
            logger.debug("Test script execution result: {}", testResult);

            if (testResult == null) {
                logger.error("Test script execution failed, cannot inject large script");
                return null;
            }

            // Extract the IIFE wrapper if present
            boolean hasIIFE = script.trim().startsWith("(function()") && script.trim().endsWith("})();");
            String scriptBody = script;

            if (hasIIFE) {
                // Remove the IIFE wrapper
                scriptBody = script.substring(script.indexOf("{") + 1, script.lastIndexOf("}"));
            }

            // Create a function to hold our script
            String setupFunction =
                    "window.__csRecorderScript = function() {\n" +
                            "  try {\n" +
                            scriptBody + "\n" +
                            "    return true;\n" +
                            "  } catch(e) {\n" +
                            "    console.error('Script execution error:', e);\n" +
                            "    return { error: e.toString() };\n" +
                            "  }\n" +
                            "};\n";

            // Split the setup function into chunks
            int chunkSize = 8000; // Characters per chunk
            int chunkCount = (int) Math.ceil((double) setupFunction.length() / chunkSize);

            logger.info("Splitting script into {} chunks", chunkCount);

            for (int i = 0; i < chunkCount; i++) {
                int start = i * chunkSize;
                int end = Math.min(start + chunkSize, setupFunction.length());
                String chunk = setupFunction.substring(start, end);

                // Escape any quotes in the chunk
                chunk = chunk.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n");

                // Inject the chunk
                String chunkScript = "try { eval('" + chunk + "'); return 'Chunk " + (i + 1) + " of " + chunkCount + " injected'; } " +
                        "catch(e) { console.error('Chunk " + (i + 1) + " error:', e); return { error: e.toString() }; }";

                Object result = executeScript(chunkScript);
                logger.debug("Chunk {} result: {}", (i + 1), result);

                // Wait a moment between chunks
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Now execute the assembled function
            String executeScript =
                    "try {\n" +
                            "  if (typeof window.__csRecorderScript === 'function') {\n" +
                            "    return window.__csRecorderScript();\n" +
                            "  } else {\n" +
                            "    return { error: 'Script function not found' };\n" +
                            "  }\n" +
                            "} catch(e) {\n" +
                            "  console.error('Function execution error:', e);\n" +
                            "  return { error: e.toString() };\n" +
                            "}";

            return executeScript(executeScript);
        } catch (Exception e) {
            logger.error("Error injecting large script: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Implementation of the abstract method from AbstractBrowserInstance
     * This method injects the recorder script into the browser
     *
     * @return True if script injection succeeded, false otherwise
     */
    @Override
    protected boolean injectRecorderScript() {
        if (!isDriverValid()) {
            logger.error("Cannot inject recorder script - driver session is invalid");
            return false;
        }

        try {
            // Apply CSP bypasses before injecting script
            applyCSPBypass();
            disableCSP();

            // Try to inject a minimal recorder script that logs events to console
            String minimalScript =
                    "(function() {\n" +
                            "  window.__csRecorderActive = true;\n" +
                            "  console.log('CSTestForge Recorder initialized');\n" +
                            "  \n" +
                            "  // Create a minimal UI indicator\n" +
                            "  const ui = document.createElement('div');\n" +
                            "  ui.id = 'cs-recorder-indicator';\n" +
                            "  ui.style.cssText = 'position:fixed;top:10px;right:10px;background:#333;color:#fff;padding:8px;z-index:2147483647;border-radius:3px;font-family:Arial;';\n" +
                            "  ui.textContent = 'CSTestForge Recording';\n" +
                            "  document.body ? document.body.appendChild(ui) : setTimeout(() => document.body && document.body.appendChild(ui), 1000);\n" +
                            "  \n" +
                            "  // Track clicks\n" +
                            "  document.addEventListener('click', function(e) {\n" +
                            "    console.log('Click:', e.target.tagName, e.target.id || '', e.target.className || '');\n" +
                            "  }, true);\n" +
                            "  \n" +
                            "  // Track form submissions\n" +
                            "  document.addEventListener('submit', function(e) {\n" +
                            "    console.log('Form submit:', e.target.tagName, e.target.id || '', e.target.action || '');\n" +
                            "  }, true);\n" +
                            "  \n" +
                            "  // Track input changes\n" +
                            "  document.addEventListener('input', function(e) {\n" +
                            "    if (!e.target || !['INPUT', 'TEXTAREA', 'SELECT'].includes(e.target.tagName)) return;\n" +
                            "    console.log('Input:', e.target.tagName, e.target.id || '', e.target.name || '', e.target.type || '');\n" +
                            "  }, true);\n" +
                            "  \n" +
                            "  return true;\n" +
                            "})();";

            Object result = injectScript(minimalScript);
            logger.debug("Recorder script injection result: {}", result);

            // Verify injection was successful
            try {
                Boolean active = (Boolean) executeScript("return window.__csRecorderActive === true;");
                if (Boolean.TRUE.equals(active)) {
                    logger.info("Recorder script injected successfully");
                    return true;
                } else {
                    logger.warn("Recorder script may not be active");
                    return false;
                }
            } catch (Exception e) {
                logger.error("Error verifying recorder script: {}", e.getMessage());
                return false;
            }
        } catch (Exception e) {
            logger.error("Failed to inject recorder script: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Injects the recorder script with server details to work around CSP restrictions
     *
     * @param sessionId The recording session ID
     * @param serverHost The server hostname (typically localhost)
     * @param serverPort The server port (typically 8080)
     * @param contextPath The application context path (e.g., /cstestforge)
     * @return True if injection was successful
     */
    public boolean injectRecorderScriptWithServerDetails(String sessionId, String serverHost, String serverPort, String contextPath) {
        if (!isDriverValid()) {
            logger.error("Cannot inject recorder script - driver session is invalid");
            return false;
        }

        try {
            logger.info("Injecting CSP-compatible recorder script with server details: {}:{}{}",
                    serverHost, serverPort, contextPath);

            Object result = injectCSPCompatibleRecorderScript(sessionId, serverHost, serverPort, contextPath);

            if (result != null) {
                logger.info("Successfully injected CSP-compatible recorder script");
                return true;
            }

            logger.warn("CSP-compatible script injection failed, trying minimal script");
            result = injectMinimalRecorderScript(sessionId, serverHost, serverPort, contextPath);

            if (result != null) {
                logger.info("Successfully injected minimal recorder script");
                return true;
            }

            logger.error("Failed to inject any recorder script");
            return false;
        } catch (Exception e) {
            logger.error("Error injecting recorder script with server details: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Inject a minimal recorder script that works with CSP-restricted sites
     * This script specifically uses absolute URLs to localhost server instead of relative paths
     */
    public Object injectCSPCompatibleRecorderScript(String sessionId, String serverHost, String serverPort, String contextPath) {
        if (!isDriverValid()) {
            logger.error("Cannot inject CSP-compatible script - driver session is invalid");
            return null;
        }

        try {
            String fullServerUrl = "http://" + serverHost + ":" + serverPort;
            if (!contextPath.startsWith("/")) {
                contextPath = "/" + contextPath;
            }

            String apiEndpoint = fullServerUrl + contextPath + "/api/recorder/events/" + sessionId;

            String cspCompatibleScript =
                    "(function() {\n" +
                            "  window.__csRecorderActive = true;\n" +
                            "  window.__csRecorderSessionId = '" + sessionId + "';\n" +
                            "  window.__csServerHost = '" + serverHost + "';\n" +
                            "  window.__csServerPort = '" + serverPort + "';\n" +
                            "  window.__csContextPath = '" + contextPath + "';\n" +
                            "  window.__csApiEndpoint = '" + apiEndpoint + "';\n" +
                            "  console.debug('CSTestForge: CSP-compatible recorder initialized');\n" +
                            "  \n" +
                            "  // Create minimal UI with improved visibility and controls\n" +
                            "  const ui = document.createElement('div');\n" +
                            "  ui.id = 'cs-recorder-indicator';\n" +
                            "  ui.style.cssText = 'position:fixed;top:10px;right:10px;z-index:2147483647;background:#333;color:#fff;padding:8px 12px;box-shadow:0 2px 6px rgba(0,0,0,0.3);border-radius:4px;font-family:Arial;display:flex;align-items:center;';\n" +
                            "  \n" +
                            "  // Add recording indicator\n" +
                            "  const indicator = document.createElement('div');\n" +
                            "  indicator.style.cssText = 'width:10px;height:10px;background:red;border-radius:50%;margin-right:8px;';\n" +
                            "  ui.appendChild(indicator);\n" +
                            "  \n" +
                            "  // Add text\n" +
                            "  const text = document.createElement('span');\n" +
                            "  text.textContent = 'CSTestForge Recording';\n" +
                            "  ui.appendChild(text);\n" +
                            "  \n" +
                            "  // Add control buttons\n" +
                            "  const buttonContainer = document.createElement('div');\n" +
                            "  buttonContainer.style.cssText = 'display:flex;margin-left:10px;';\n" +
                            "  \n" +
                            "  // Pause/Resume button\n" +
                            "  const pauseBtn = document.createElement('button');\n" +
                            "  pauseBtn.textContent = '⏸';\n" +
                            "  pauseBtn.style.cssText = 'border:none;background:none;color:white;font-size:16px;cursor:pointer;margin:0 5px;padding:0;';\n" +
                            "  pauseBtn.title = 'Pause Recording';\n" +
                            "  pauseBtn.onclick = function(e) {\n" +
                            "    e.stopPropagation();\n" +
                            "    if (!window.__csRecorderPaused) {\n" +
                            "      window.__csRecorderPaused = true;\n" +
                            "      pauseBtn.textContent = '▶';\n" +
                            "      pauseBtn.title = 'Resume Recording';\n" +
                            "      indicator.style.background = '#888';\n" +
                            "      sendEvent({type: 'RECORDER_CONTROL', action: 'PAUSE'});\n" +
                            "    } else {\n" +
                            "      window.__csRecorderPaused = false;\n" +
                            "      pauseBtn.textContent = '⏸';\n" +
                            "      pauseBtn.title = 'Pause Recording';\n" +
                            "      indicator.style.background = 'red';\n" +
                            "      sendEvent({type: 'RECORDER_CONTROL', action: 'RESUME'});\n" +
                            "    }\n" +
                            "  };\n" +
                            "  buttonContainer.appendChild(pauseBtn);\n" +
                            "  \n" +
                            "  // Stop button\n" +
                            "  const stopBtn = document.createElement('button');\n" +
                            "  stopBtn.textContent = '⏹';\n" +
                            "  stopBtn.style.cssText = 'border:none;background:none;color:white;font-size:16px;cursor:pointer;margin:0 5px;padding:0;';\n" +
                            "  stopBtn.title = 'Stop Recording';\n" +
                            "  stopBtn.onclick = function(e) {\n" +
                            "    e.stopPropagation();\n" +
                            "    window.__csRecorderActive = false;\n" +
                            "    indicator.style.background = '#888';\n" +
                            "    text.textContent = 'Recording Stopped';\n" +
                            "    sendEvent({type: 'RECORDER_CONTROL', action: 'STOP'});\n" +
                            "    setTimeout(function() {\n" +
                            "      ui.parentNode && ui.parentNode.removeChild(ui);\n" +
                            "    }, 3000);\n" +
                            "  };\n" +
                            "  buttonContainer.appendChild(stopBtn);\n" +
                            "  \n" +
                            "  ui.appendChild(buttonContainer);\n" +
                            "  \n" +
                            "  // Append UI to body when ready\n" +
                            "  function appendUI() {\n" +
                            "    if (document.body) {\n" +
                            "      document.body.appendChild(ui);\n" +
                            "    } else {\n" +
                            "      setTimeout(appendUI, 100);\n" +
                            "    }\n" +
                            "  }\n" +
                            "  appendUI();\n" +
                            "  \n" +
                            "  // Track status for pause/resume functionality\n" +
                            "  window.__csRecorderPaused = false;\n" +
                            "  \n" +
                            "  // Function to send events to the server via XHR with absolute URLs\n" +
                            "  function sendEvent(eventData) {\n" +
                            "    if (window.__csRecorderPaused && eventData.type !== 'RECORDER_CONTROL') {\n" +
                            "      return; // Don't record events while paused (except control events)\n" +
                            "    }\n" +
                            "    \n" +
                            "    try {\n" +
                            "      // Add required data\n" +
                            "      eventData.sessionId = window.__csRecorderSessionId;\n" +
                            "      eventData.timestamp = eventData.timestamp || new Date().getTime();\n" +
                            "      eventData.url = window.location.href;\n" +
                            "      eventData.title = document.title;\n" +
                            "      \n" +
                            "      // Use absolute URL to avoid CSP issues\n" +
                            "      const xhr = new XMLHttpRequest();\n" +
                            "      xhr.open('POST', window.__csApiEndpoint, true);\n" +
                            "      xhr.setRequestHeader('Content-Type', 'application/json');\n" +
                            "      xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');\n" +
                            "      \n" +
                            "      // Add success/error handlers\n" +
                            "      xhr.onload = function() {\n" +
                            "        if (xhr.status >= 200 && xhr.status < 300) {\n" +
                            "          console.debug('CSTestForge: Event sent successfully');\n" +
                            "        } else {\n" +
                            "          console.error('CSTestForge: Error sending event:', xhr.statusText);\n" +
                            "        }\n" +
                            "      };\n" +
                            "      \n" +
                            "      xhr.onerror = function() {\n" +
                            "        console.error('CSTestForge: Network error when sending event');\n" +
                            "        // Store in local storage for retry if possible\n" +
                            "        try {\n" +
                            "          const pendingEvents = JSON.parse(localStorage.getItem('csTestForge_pendingEvents') || '[]');\n" +
                            "          pendingEvents.push(eventData);\n" +
                            "          localStorage.setItem('csTestForge_pendingEvents', JSON.stringify(pendingEvents));\n" +
                            "        } catch(e) {\n" +
                            "          console.error('CSTestForge: Failed to store event in local storage:', e);\n" +
                            "        }\n" +
                            "      };\n" +
                            "      \n" +
                            "      // Send the data\n" +
                            "      xhr.send(JSON.stringify(eventData));\n" +
                            "      return true;\n" +
                            "    } catch(e) {\n" +
                            "      console.error('CSTestForge: Failed to send event:', e);\n" +
                            "      return false;\n" +
                            "    }\n" +
                            "  }\n" +
                            "  \n" +
                            "  // Attach event listeners for recording\n" +
                            "  function attachEventListeners() {\n" +
                            "    // Track clicks with improved event capturing\n" +
                            "    document.addEventListener('click', function(e) {\n" +
                            "      if (!window.__csRecorderActive || window.__csRecorderPaused) return;\n" +
                            "      \n" +
                            "      const target = e.target;\n" +
                            "      const elementInfo = {\n" +
                            "        tagName: target.tagName.toLowerCase(),\n" +
                            "        id: target.id || null,\n" +
                            "        name: target.name || null,\n" +
                            "        className: target.className || null,\n" +
                            "        textContent: target.textContent ? target.textContent.trim().substring(0, 100) : null,\n" +
                            "        attributes: {}\n" +
                            "      };\n" +
                            "      \n" +
                            "      // Collect more attribute data for better element identification\n" +
                            "      ['type', 'placeholder', 'value', 'href', 'src', 'alt', 'title', 'role', 'aria-label'].forEach(attr => {\n" +
                            "        if (target.hasAttribute(attr)) {\n" +
                            "          elementInfo.attributes[attr] = target.getAttribute(attr);\n" +
                            "        }\n" +
                            "      });\n" +
                            "      \n" +
                            "      // Try to get a CSS selector for the element\n" +
                            "      try {\n" +
                            "        let selector = '';\n" +
                            "        if (target.id) {\n" +
                            "          selector = '#' + target.id;\n" +
                            "        } else if (target.className && typeof target.className === 'string') {\n" +
                            "          const classes = target.className.split(' ').filter(c => c.trim());\n" +
                            "          if (classes.length > 0) {\n" +
                            "            selector = target.tagName.toLowerCase() + '.' + classes.join('.');\n" +
                            "          }\n" +
                            "        }\n" +
                            "        \n" +
                            "        if (!selector) {\n" +
                            "          // Generate a basic selector based on attributes\n" +
                            "          selector = target.tagName.toLowerCase();\n" +
                            "          if (target.name) selector += '[name=\"' + target.name + '\"]';\n" +
                            "          else if (target.type) selector += '[type=\"' + target.type + '\"]';\n" +
                            "          else if (target.textContent && target.textContent.trim().length < 20) {\n" +
                            "            selector += ':contains(\"' + target.textContent.trim() + '\")';\n" +
                            "          }\n" +
                            "        }\n" +
                            "        \n" +
                            "        elementInfo.selector = selector;\n" +
                            "      } catch(e) {\n" +
                            "        console.error('CSTestForge: Error generating selector:', e);\n" +
                            "      }\n" +
                            "      \n" +
                            "      sendEvent({\n" +
                            "        type: 'CLICK',\n" +
                            "        elementInfo: elementInfo,\n" +
                            "        ctrlKey: e.ctrlKey,\n" +
                            "        altKey: e.altKey,\n" +
                            "        shiftKey: e.shiftKey,\n" +
                            "        metaKey: e.metaKey\n" +
                            "      });\n" +
                            "    }, true);\n" +
                            "    \n" +
                            "    // Track form submissions\n" +
                            "    document.addEventListener('submit', function(e) {\n" +
                            "      if (!window.__csRecorderActive || window.__csRecorderPaused) return;\n" +
                            "      \n" +
                            "      const form = e.target;\n" +
                            "      const formData = {};\n" +
                            "      \n" +
                            "      // Safely extract form data\n" +
                            "      if (form && form.elements) {\n" +
                            "        for (let i = 0; i < form.elements.length; i++) {\n" +
                            "          const element = form.elements[i];\n" +
                            "          if (element.name && element.value !== undefined) {\n" +
                            "            // Mask passwords and sensitive fields\n" +
                            "            if (element.type === 'password' || element.name.toLowerCase().includes('password')) {\n" +
                            "              formData[element.name] = '********';\n" +
                            "            } else {\n" +
                            "              formData[element.name] = element.value;\n" +
                            "            }\n" +
                            "          }\n" +
                            "        }\n" +
                            "      }\n" +
                            "      \n" +
                            "      // Create element info for the form\n" +
                            "      const elementInfo = {\n" +
                            "        tagName: form.tagName.toLowerCase(),\n" +
                            "        id: form.id || null,\n" +
                            "        name: form.name || null,\n" +
                            "        action: form.action || null,\n" +
                            "        method: form.method || 'get',\n" +
                            "        attributes: {}\n" +
                            "      };\n" +
                            "      \n" +
                            "      sendEvent({\n" +
                            "        type: 'FORM_SUBMIT',\n" +
                            "        elementInfo: elementInfo,\n" +
                            "        formData: formData\n" +
                            "      });\n" +
                            "    }, true);\n" +
                            "    \n" +
                            "    // Track input changes with debouncing\n" +
                            "    const inputDebounceTime = 500; // ms\n" +
                            "    const inputTimeouts = new Map();\n" +
                            "    \n" +
                            "    document.addEventListener('input', function(e) {\n" +
                            "      if (!window.__csRecorderActive || window.__csRecorderPaused) return;\n" +
                            "      \n" +
                            "      const target = e.target;\n" +
                            "      \n" +
                            "      // Only handle input elements\n" +
                            "      if (!target || !['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName)) {\n" +
                            "        return;\n" +
                            "      }\n" +
                            "      \n" +
                            "      // Clear any existing timeout for this element\n" +
                            "      const elementId = target.id || target.name || Math.random().toString(36).substring(7);\n" +
                            "      if (inputTimeouts.has(elementId)) {\n" +
                            "        clearTimeout(inputTimeouts.get(elementId));\n" +
                            "      }\n" +
                            "      \n" +
                            "      // Set a new timeout\n" +
                            "      inputTimeouts.set(elementId, setTimeout(function() {\n" +
                            "        // Determine if this is a password field\n" +
                            "        const isPassword = target.type === 'password' || target.name.toLowerCase().includes('password');\n" +
                            "        \n" +
                            "        // Create element info\n" +
                            "        const elementInfo = {\n" +
                            "          tagName: target.tagName.toLowerCase(),\n" +
                            "          id: target.id || null,\n" +
                            "          name: target.name || null,\n" +
                            "          type: target.type || null,\n" +
                            "          placeholder: target.placeholder || null,\n" +
                            "          attributes: {}\n" +
                            "        };\n" +
                            "        \n" +
                            "        // Try to get selector\n" +
                            "        try {\n" +
                            "          if (target.id) {\n" +
                            "            elementInfo.selector = '#' + target.id;\n" +
                            "          } else if (target.name) {\n" +
                            "            elementInfo.selector = target.tagName.toLowerCase() + '[name=\"' + target.name + '\"]';\n" +
                            "          }\n" +
                            "        } catch(e) {}\n" +
                            "        \n" +
                            "        sendEvent({\n" +
                            "          type: 'INPUT',\n" +
                            "          value: isPassword ? '********' : target.value,\n" +
                            "          elementInfo: elementInfo,\n" +
                            "          isPasswordField: isPassword\n" +
                            "        });\n" +
                            "        \n" +
                            "        // Remove the timeout\n" +
                            "        inputTimeouts.delete(elementId);\n" +
                            "      }, inputDebounceTime));\n" +
                            "    }, true);\n" +
                            "    \n" +
                            "    // Track navigation events\n" +
                            "    // Monitor history API\n" +
                            "    const originalPushState = history.pushState;\n" +
                            "    const originalReplaceState = history.replaceState;\n" +
                            "    \n" +
                            "    history.pushState = function() {\n" +
                            "      const result = originalPushState.apply(this, arguments);\n" +
                            "      if (window.__csRecorderActive && !window.__csRecorderPaused) {\n" +
                            "        sendEvent({\n" +
                            "          type: 'NAVIGATION',\n" +
                            "          sourceUrl: window.__csLastUrl || document.referrer,\n" +
                            "          targetUrl: window.location.href,\n" +
                            "          trigger: 'HISTORY_PUSH_STATE'\n" +
                            "        });\n" +
                            "        window.__csLastUrl = window.location.href;\n" +
                            "      }\n" +
                            "      return result;\n" +
                            "    };\n" +
                            "    \n" +
                            "    history.replaceState = function() {\n" +
                            "      const result = originalReplaceState.apply(this, arguments);\n" +
                            "      if (window.__csRecorderActive && !window.__csRecorderPaused) {\n" +
                            "        sendEvent({\n" +
                            "          type: 'NAVIGATION',\n" +
                            "          sourceUrl: window.__csLastUrl || document.referrer,\n" +
                            "          targetUrl: window.location.href,\n" +
                            "          trigger: 'HISTORY_REPLACE_STATE'\n" +
                            "        });\n" +
                            "        window.__csLastUrl = window.location.href;\n" +
                            "      }\n" +
                            "      return result;\n" +
                            "    };\n" +
                            "    \n" +
                            "    // Track popstate events\n" +
                            "    window.addEventListener('popstate', function() {\n" +
                            "      if (window.__csRecorderActive && !window.__csRecorderPaused) {\n" +
                            "        setTimeout(function() {\n" +
                            "          sendEvent({\n" +
                            "            type: 'NAVIGATION',\n" +
                            "            sourceUrl: window.__csLastUrl || document.referrer,\n" +
                            "            targetUrl: window.location.href,\n" +
                            "            trigger: 'POPSTATE'\n" +
                            "          });\n" +
                            "          window.__csLastUrl = window.location.href;\n" +
                            "        }, 0);\n" +
                            "      }\n" +
                            "    });\n" +
                            "  }\n" +
                            "  \n" +
                            "  // Track initial URL\n" +
                            "  window.__csLastUrl = window.location.href;\n" +
                            "  \n" +
                            "  // Attach all event listeners\n" +
                            "  attachEventListeners();\n" +
                            "  \n" +
                            "  // Send initial event\n" +
                            "  sendEvent({\n" +
                            "    type: 'INIT',\n" +
                            "    userAgent: navigator.userAgent,\n" +
                            "    viewport: {\n" +
                            "      width: window.innerWidth,\n" +
                            "      height: window.innerHeight\n" +
                            "    },\n" +
                            "    referrer: document.referrer\n" +
                            "  });\n" +
                            "  \n" +
                            "  // Set up heartbeat to ensure server knows we're still alive\n" +
                            "  const heartbeatInterval = setInterval(function() {\n" +
                            "    if (window.__csRecorderActive) {\n" +
                            "      sendEvent({\n" +
                            "        type: 'HEARTBEAT',\n" +
                            "        status: window.__csRecorderPaused ? 'PAUSED' : 'RECORDING'\n" +
                            "      });\n" +
                            "    } else {\n" +
                            "      clearInterval(heartbeatInterval);\n" +
                            "    }\n" +
                            "  }, 30000);\n" +
                            "  \n" +
                            "  console.debug('CSTestForge: CSP-compatible recorder setup complete');\n" +
                            "  return 'CSP-compatible recorder initialized';\n" +
                            "})();";

            // Execute the script directly
            Object result = executeScript(cspCompatibleScript);
            logger.debug("CSP-compatible script execution result: {}", result);

            // Wait a moment for the script to execute and setup
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Verify injection
            boolean verified = verifyCSPCompatibleInjection();
            logger.debug("CSP-compatible script verification result: {}", verified);
            return verified ? result : null;

        } catch (Exception e) {
            logger.error("Failed to inject CSP-compatible script: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Verify that the CSP-compatible script was properly injected
     */
    private boolean verifyCSPCompatibleInjection() {
        try {
            Object activeFlag = executeScript("return typeof window.__csRecorderActive === 'boolean' ? window.__csRecorderActive : false;");
            logger.debug("Recorder active flag: {}", activeFlag);

            Object uiExists = executeScript("return document.getElementById('cs-recorder-indicator') !== null;");
            logger.debug("Recorder UI indicator exists: {}", uiExists);

            return Boolean.TRUE.equals(activeFlag) || Boolean.TRUE.equals(uiExists);
        } catch (Exception e) {
            logger.error("Error verifying CSP-compatible script injection: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Inject a minimal recorder script as a fallback
     */
    public Object injectMinimalRecorderScript(String sessionId, String serverHost, String serverPort, String contextPath) {
        if (!isDriverValid()) {
            logger.error("Cannot inject minimal script - driver session is invalid");
            return null;
        }

        try {
            // Ensure contextPath starts with /
            if (!contextPath.startsWith("/")) {
                contextPath = "/" + contextPath;
            }

            // Use absolute URL to avoid CSP issues
            String apiUrl = "http://" + serverHost + ":" + serverPort + contextPath + "/api/recorder/events/" + sessionId;

            String minimalScript =
                    "(function() {\n" +
                            "  window.__csRecorderActive = true;\n" +
                            "  window.__csRecorderSessionId = '" + sessionId + "';\n" +
                            "  console.log('CSTestForge: Minimal recorder initialized');\n" +
                            "  \n" +
                            "  // Create minimal UI\n" +
                            "  const ui = document.createElement('div');\n" +
                            "  ui.id = 'cs-recorder-indicator';\n" +
                            "  ui.style.cssText = 'position:fixed;top:10px;right:10px;background:#333;color:#fff;padding:8px;z-index:2147483647;border-radius:3px;font-family:Arial;';\n" +
                            "  ui.textContent = 'CSTestForge Recording';\n" +
                            "  document.body ? document.body.appendChild(ui) : setTimeout(() => document.body && document.body.appendChild(ui), 1000);\n" +
                            "  \n" +
                            "  // Send events via HTTP directly with absolute URL\n" +
                            "  window.__csSendEvent = function(eventData) {\n" +
                            "    try {\n" +
                            "      const xhr = new XMLHttpRequest();\n" +
                            "      xhr.open('POST', '" + apiUrl + "', true);\n" +
                            "      xhr.setRequestHeader('Content-Type', 'application/json');\n" +
                            "      xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');\n" +
                            "      eventData.sessionId = '" + sessionId + "';\n" +
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
                            "    timestamp: new Date().getTime(),\n" +
                            "    userAgent: navigator.userAgent\n" +
                            "  });\n" +
                            "  \n" +
                            "  // Track clicks\n" +
                            "  document.addEventListener('click', function(e) {\n" +
                            "    __csSendEvent({\n" +
                            "      type: 'CLICK',\n" +
                            "      url: window.location.href,\n" +
                            "      title: document.title,\n" +
                            "      timestamp: new Date().getTime(),\n" +
                            "      elementInfo: {\n" +
                            "        tagName: e.target.tagName.toLowerCase(),\n" +
                            "        id: e.target.id || null,\n" +
                            "        className: e.target.className || null,\n" +
                            "        text: e.target.textContent ? e.target.textContent.trim().substring(0, 100) : null\n" +
                            "      }\n" +
                            "    });\n" +
                            "  }, true);\n" +
                            "  \n" +
                            "  // Track form submissions\n" +
                            "  document.addEventListener('submit', function(e) {\n" +
                            "    __csSendEvent({\n" +
                            "      type: 'FORM_SUBMIT',\n" +
                            "      url: window.location.href,\n" +
                            "      title: document.title,\n" +
                            "      timestamp: new Date().getTime(),\n" +
                            "      elementInfo: {\n" +
                            "        tagName: e.target.tagName.toLowerCase(),\n" +
                            "        id: e.target.id || null\n" +
                            "      }\n" +
                            "    });\n" +
                            "  }, true);\n" +
                            "  \n" +
                            "  // Track input changes\n" +
                            "  document.addEventListener('input', function(e) {\n" +
                            "    if (!e.target || !['INPUT', 'TEXTAREA', 'SELECT'].includes(e.target.tagName)) return;\n" +
                            "    __csSendEvent({\n" +
                            "      type: 'INPUT',\n" +
                            "      url: window.location.href,\n" +
                            "      title: document.title,\n" +
                            "      timestamp: new Date().getTime(),\n" +
                            "      value: e.target.type === 'password' ? '********' : e.target.value,\n" +
                            "      elementInfo: {\n" +
                            "        tagName: e.target.tagName.toLowerCase(),\n" +
                            "        id: e.target.id || null,\n" +
                            "        name: e.target.name || null,\n" +
                            "        type: e.target.type || null\n" +
                            "      }\n" +
                            "    });\n" +
                            "  }, true);\n" +
                            "  \n" +
                            "  console.debug('CSTestForge: Minimal recorder setup complete');\n" +
                            "  return 'Minimal recorder initialized';\n" +
                            "})();";

            Object result = executeScript(minimalScript);
            logger.debug("Minimal script execution result: {}", result);

            // Wait a moment for the script to execute and setup
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Verify injection
            boolean verified = verifyMinimalScriptInjection();
            logger.debug("Minimal script verification result: {}", verified);
            return verified ? result : null;

        } catch (Exception e) {
            logger.error("Failed to inject minimal recorder script for session {}: {}", sessionId, e.getMessage());
            return null;
        }
    }

    /**
     * Verify that the minimal script was properly injected and initialized
     */
    private boolean verifyMinimalScriptInjection() {
        try {
            // Check for the presence of the __csRecorderActive flag
            Object activeFlag = executeScript(
                    "return typeof window.__csRecorderActive === 'boolean' ? window.__csRecorderActive : false;");

            logger.debug("Minimal recorder active flag: {}", activeFlag);

            // Check for the presence of the UI indicator
            Object uiExists = executeScript(
                    "return document.getElementById('cs-recorder-indicator') !== null;");

            logger.debug("Minimal UI indicator exists: {}", uiExists);

            // Check for the __csSendEvent function
            Object sendEventExists = executeScript(
                    "return typeof window.__csSendEvent === 'function';");

            logger.debug("Send event function exists: {}", sendEventExists);

            return Boolean.TRUE.equals(activeFlag) &&
                    (Boolean.TRUE.equals(uiExists) || Boolean.TRUE.equals(sendEventExists));
        } catch (Exception e) {
            logger.error("Error verifying minimal script injection: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Object executeAsyncScript(String script, Object... args) {
        if (!isDriverValid()) {
            logger.error("Cannot execute async script - driver session is invalid");
            return null;
        }

        if (script == null) {
            return null;
        }

        try {
            if (driver instanceof JavascriptExecutor) {
                return ((JavascriptExecutor) driver).executeAsyncScript(script, args);
            }
        } catch (Exception e) {
            logger.error("Error executing async script: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public Object executeScript(String script, Object... args) {
        if (driver == null) {
            logger.error("Cannot execute script - driver is null");
            return null;
        }

        if (script == null) {
            logger.error("Cannot execute null script");
            return null;
        }

        try {
            if (!isDriverValid()) {
                logger.error("Cannot execute script - driver session is invalid");
                return null;
            }

            if (driver instanceof JavascriptExecutor) {
                return ((JavascriptExecutor) driver).executeScript(script, args);
            }
        } catch (Exception e) {
            // Log the error but don't rethrow it
            if (e.getMessage() != null &&
                    (e.getMessage().contains("invalid session id") ||
                            e.getMessage().contains("session deleted") ||
                            e.getMessage().contains("browser has closed"))) {
                logger.error("Error executing script: invalid session: {}", e.getMessage());
            } else {
                logger.error("Error executing script: {}", e.getMessage());
            }
        }
        return null;
    }

    @Override
    public byte[] captureScreenshot() {
        if (!isDriverValid()) {
            logger.error("Cannot capture screenshot - driver session is invalid");
            return null;
        }

        try {
            if (driver instanceof ChromeDriver) {
                return ((ChromeDriver) driver).getScreenshotAs(OutputType.BYTES);
            }
        } catch (Exception e) {
            logger.error("Error capturing screenshot: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public byte[] captureElementScreenshot(String selector) {
        if (!isDriverValid()) {
            logger.error("Cannot capture element screenshot - driver session is invalid");
            return null;
        }

        try {
            WebElement element = driver.findElement(By.cssSelector(selector));
            if (element != null) {
                return element.getScreenshotAs(OutputType.BYTES);
            }
        } catch (Exception e) {
            logger.error("Failed to capture element screenshot with selector {}: {}", selector, e.getMessage());
        }
        return null;
    }

    @Override
    public String getCurrentUrl() {
        if (driver != null) {
            try {
                if (!isDriverValid()) {
                    return currentUrl; // Return last known URL if driver is invalid
                }

                currentUrl = driver.getCurrentUrl();
                return currentUrl;
            } catch (Exception e) {
                logger.warn("Failed to get current URL: {}", e.getMessage());
            }
        }
        return currentUrl;
    }

    @Override
    public String getTitle() {
        if (driver != null) {
            try {
                if (!isDriverValid()) {
                    return currentTitle; // Return last known title if driver is invalid
                }

                currentTitle = driver.getTitle();
                return currentTitle;
            } catch (Exception e) {
                logger.warn("Failed to get page title: {}", e.getMessage());
            }
        }
        return currentTitle;
    }

    @Override
    public void setEnvironmentVariables(Map<String, String> environmentVariables) {
        // For Chrome, we can't change environment variables after the browser is started
        // This is set during initialization
        logger.debug("Environment variables can only be set before browser start");
    }

    @Override
    public void setNetworkCapturing(boolean enabled) {
        super.setNetworkCapturing(enabled);

        if (!isDriverValid() || !devToolsEnabled) {
            logger.debug("Network capturing not available - driver invalid or DevTools not supported");
            return;
        }

        try {
            if (enabled) {
                devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
                logger.info("Network capturing enabled successfully using DevTools");
            } else {
                devTools.send(Network.disable());
                logger.info("Network capturing disabled successfully");
            }
        } catch (Exception e) {
            logger.error("Error setting network capturing: {}", e.getMessage());
        }
    }

    @Override
    public void setConsoleCapturing(boolean enabled) {
        super.setConsoleCapturing(enabled);

        if (!isDriverValid() || !devToolsEnabled) {
            logger.debug("Console capturing not available - driver invalid or DevTools not supported");
            return;
        }

        try {
            if (enabled) {
                devTools.send(Console.enable());
                logger.info("Console capturing enabled successfully using DevTools");
            } else {
                devTools.send(Console.disable());
                logger.info("Console capturing disabled successfully");
            }
        } catch (Exception e) {
            logger.error("Error setting console capturing: {}", e.getMessage());
        }
    }

    @Override
    public boolean waitForCondition(String conditionScript, long timeoutMs) {
        if (!isDriverValid()) {
            logger.error("Cannot wait for condition - driver session is invalid");
            return false;
        }

        try {
            long endTime = System.currentTimeMillis() + timeoutMs;

            while (System.currentTimeMillis() < endTime) {
                try {
                    Object result = executeScript("return " + conditionScript);
                    if (Boolean.TRUE.equals(result)) {
                        return true;
                    }
                } catch (Exception e) {
                    logger.debug("Error evaluating condition: {}", e.getMessage());

                    // If the session is invalid, stop waiting
                    if (e.getMessage() != null &&
                            (e.getMessage().contains("invalid session id") ||
                                    e.getMessage().contains("session deleted") ||
                                    e.getMessage().contains("browser has closed"))) {
                        return false;
                    }
                }

                // Small delay to avoid excessive CPU usage
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }

            return false;
        } catch (Exception e) {
            logger.error("Error waiting for condition: {}", e.getMessage());
            return false;
        }
    }

    @Override
    protected boolean isResponsive() {
        return isDriverValid();
    }

    /**
     * Apply CSP bypass on the current page by modifying headers
     * Note: This is an advanced technique and may not work on all sites
     * @return True if successfully applied the bypass
     */
    public boolean applyCSPBypass() {
        if (!isDriverValid()) {
            logger.error("Cannot apply CSP bypass - driver session is invalid");
            return false;
        }

        try {
            // Attempt to disable CSP by injecting a meta tag
            String bypassScript =
                    "try {\n" +
                            "  // Remove existing CSP meta tags\n" +
                            "  const existingCSP = document.querySelector('meta[http-equiv=\"Content-Security-Policy\"]');\n" +
                            "  if (existingCSP && existingCSP.parentNode) {\n" +
                            "    existingCSP.parentNode.removeChild(existingCSP);\n" +
                            "  }\n" +
                            "  \n" +
                            "  // Create a permissive CSP meta tag\n" +
                            "  const meta = document.createElement('meta');\n" +
                            "  meta.httpEquiv = 'Content-Security-Policy';\n" +
                            "  meta.content = \"default-src * 'unsafe-inline' 'unsafe-eval' data: blob:; connect-src * 'unsafe-inline' 'unsafe-eval' data: blob:;\";\n" +
                            "  \n" +
                            "  // Insert at the beginning of head\n" +
                            "  if (document.head) {\n" +
                            "    document.head.insertBefore(meta, document.head.firstChild);\n" +
                            "    return true;\n" +
                            "  }\n" +
                            "  return false;\n" +
                            "} catch(e) {\n" +
                            "  console.error('CSP bypass error:', e);\n" +
                            "  return false;\n" +
                            "}";

            Object result = executeScript(bypassScript);
            logger.debug("CSP bypass result: {}", result);

            // Also use DevTools if available
            if (devToolsEnabled) {
                try {
                    devTools.send(Page.setBypassCSP(true));
                    logger.debug("CSP bypass enabled via DevTools");
                } catch (Exception e) {
                    logger.warn("Failed to bypass CSP via DevTools: {}", e.getMessage());
                }
            }

            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            logger.error("Failed to apply CSP bypass: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Disable Content Security Policy (CSP) checks for the page
     * This is a more direct approach than applyCSPBypass and works better in some cases
     * @return True if successfully disabled CSP
     */
    public boolean disableCSP() {
        if (!isDriverValid()) {
            logger.error("Cannot disable CSP - driver session is invalid");
            return false;
        }

        try {
            // Use JavaScript to dynamically override the CSP headers
            String disableCSPScript =
                    "try {\n" +
                            "  // Override the 'set' method of Headers prototype\n" +
                            "  const originalSet = Headers.prototype.set;\n" +
                            "  Headers.prototype.set = function(name, value) {\n" +
                            "    // Skip setting any CSP headers\n" +
                            "    if (name.toLowerCase() === 'content-security-policy' ||\n" +
                            "        name.toLowerCase() === 'content-security-policy-report-only') {\n" +
                            "      return;\n" +
                            "    }\n" +
                            "    return originalSet.apply(this, arguments);\n" +
                            "  };\n" +
                            "  \n" +
                            "  // Also remove CSP meta tags\n" +
                            "  const cspTags = document.querySelectorAll('meta[http-equiv=\"Content-Security-Policy\"], meta[http-equiv=\"Content-Security-Policy-Report-Only\"]');\n" +
                            "  cspTags.forEach(tag => tag.parentNode.removeChild(tag));\n" +
                            "  \n" +
                            "  // Create observer to remove any new CSP meta tags\n" +
                            "  const observer = new MutationObserver(mutations => {\n" +
                            "    for (const mutation of mutations) {\n" +
                            "      if (mutation.type === 'childList' && mutation.addedNodes.length > 0) {\n" +
                            "        for (const node of mutation.addedNodes) {\n" +
                            "          if (node.tagName === 'META' && \n" +
                            "              (node.httpEquiv === 'Content-Security-Policy' || \n" +
                            "               node.httpEquiv === 'Content-Security-Policy-Report-Only')) {\n" +
                            "            node.parentNode.removeChild(node);\n" +
                            "          }\n" +
                            "        }\n" +
                            "      }\n" +
                            "    }\n" +
                            "  });\n" +
                            "  \n" +
                            "  // Start observing\n" +
                            "  observer.observe(document.documentElement, {\n" +
                            "    childList: true,\n" +
                            "    subtree: true\n" +
                            "  });\n" +
                            "  \n" +
                            "  return true;\n" +
                            "} catch(e) {\n" +
                            "  console.error('Failed to disable CSP:', e);\n" +
                            "  return false;\n" +
                            "}";

            Object result = executeScript(disableCSPScript);
            logger.debug("CSP disabling result: {}", result);

            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            logger.error("Failed to disable CSP: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Fix connection issues related to cross-origin restrictions
     * @param serverHost The recorder server host
     * @param serverPort The recorder server port
     * @return True if CORS issues were fixed successfully
     */
    public boolean fixCORSIssues(String serverHost, String serverPort) {
        if (!isDriverValid()) {
            logger.error("Cannot fix CORS issues - driver session is invalid");
            return false;
        }

        try {
            // Inject a script that fixes CORS issues by proxying requests
            String corsFixScript =
                    "try {\n" +
                            "  // Add a special flag to avoid re-injecting\n" +
                            "  if (window.__csCORSFixed) return 'CORS already fixed';\n" +
                            "  window.__csCORSFixed = true;\n" +
                            "  \n" +
                            "  // Create a proxy for XMLHttpRequest\n" +
                            "  const originalXHR = window.XMLHttpRequest;\n" +
                            "  const serverHost = '" + serverHost + "';\n" +
                            "  const serverPort = '" + serverPort + "';\n" +
                            "  \n" +
                            "  // Override XHR for our specific endpoints\n" +
                            "  window.XMLHttpRequest = function() {\n" +
                            "    const xhr = new originalXHR();\n" +
                            "    const originalOpen = xhr.open;\n" +
                            "    \n" +
                            "    xhr.open = function(method, url, async, user, password) {\n" +
                            "      // Check if this is a URL to our recording server\n" +
                            "      let modifiedUrl = url;\n" +
                            "      if (url.includes('/api/recorder/events/') && !url.startsWith('http://' + serverHost + ':' + serverPort)) {\n" +
                            "        modifiedUrl = 'http://' + serverHost + ':' + serverPort + \n" +
                            "          (url.startsWith('/') ? url : '/' + url);\n" +
                            "        console.debug('CSTestForge: Proxying XHR to ' + modifiedUrl);\n" +
                            "      }\n" +
                            "      return originalOpen.call(this, method, modifiedUrl, async, user, password);\n" +
                            "    };\n" +
                            "    \n" +
                            "    return xhr;\n" +
                            "  };\n" +
                            "  \n" +
                            "  // Also patch fetch API\n" +
                            "  const originalFetch = window.fetch;\n" +
                            "  window.fetch = function(resource, init) {\n" +
                            "    if (typeof resource === 'string' && resource.includes('/api/recorder/events/') && \n" +
                            "        !resource.startsWith('http://' + serverHost + ':' + serverPort)) {\n" +
                            "      const modifiedUrl = 'http://' + serverHost + ':' + serverPort + \n" +
                            "        (resource.startsWith('/') ? resource : '/' + resource);\n" +
                            "      console.debug('CSTestForge: Proxying fetch to ' + modifiedUrl);\n" +
                            "      return originalFetch.call(this, modifiedUrl, init);\n" +
                            "    }\n" +
                            "    return originalFetch.apply(this, arguments);\n" +
                            "  };\n" +
                            "  \n" +
                            "  return 'CORS issues fixed';\n" +
                            "} catch(e) {\n" +
                            "  console.error('Failed to fix CORS issues:', e);\n" +
                            "  return 'Error: ' + e.toString();\n" +
                            "}";

            Object result = executeScript(corsFixScript);
            logger.debug("CORS fix result: {}", result);

            return result != null && !result.toString().startsWith("Error");
        } catch (Exception e) {
            logger.error("Failed to fix CORS issues: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if there are Content Security Policy issues on the current page
     * @return A map with CSP analysis information
     */
    public Map<String, Object> analyzeCSPIssues() {
        Map<String, Object> cspInfo = new HashMap<>();

        if (!isDriverValid()) {
            cspInfo.put("error", "Driver session is invalid");
            return cspInfo;
        }

        try {
            // Check for CSP meta tags
            Object cspMetaTags = executeScript(
                    "const cspTags = document.querySelectorAll('meta[http-equiv=\"Content-Security-Policy\"], meta[http-equiv=\"Content-Security-Policy-Report-Only\"]');\n" +
                            "const results = [];\n" +
                            "cspTags.forEach(tag => results.push({\n" +
                            "  httpEquiv: tag.httpEquiv,\n" +
                            "  content: tag.content\n" +
                            "}));\n" +
                            "return results;"
            );
            cspInfo.put("cspMetaTags", cspMetaTags);

            // Check for specific restrictive CSP directives
            Object cspDirectives = executeScript(
                    "try {\n" +
                            "  const cspContent = document.querySelector('meta[http-equiv=\"Content-Security-Policy\"]')?.content || '';\n" +
                            "  return {\n" +
                            "    hasConnectSrc: cspContent.includes('connect-src'),\n" +
                            "    connectSrc: cspContent.match(/connect-src([^;]+)/)?.[0] || 'None',\n" +
                            "    hasScriptSrc: cspContent.includes('script-src'),\n" +
                            "    scriptSrc: cspContent.match(/script-src([^;]+)/)?.[0] || 'None',\n" +
                            "    hasDefaultSrc: cspContent.includes('default-src'),\n" +
                            "    defaultSrc: cspContent.match(/default-src([^;]+)/)?.[0] || 'None',\n" +
                            "    hasUnsafeInline: cspContent.includes(\"'unsafe-inline'\"),\n" +
                            "    hasUnsafeEval: cspContent.includes(\"'unsafe-eval'\")\n" +
                            "  };\n" +
                            "} catch(e) { return { error: e.toString() }; }"
            );
            cspInfo.put("cspDirectives", cspDirectives);

            // Test if we can perform XHR requests to our server
            // Note: The URL doesn't need to be valid, we just want to see if the browser attempts the connection
            Object xhrTest = executeScript(
                    "try {\n" +
                            "  const xhr = new XMLHttpRequest();\n" +
                            "  xhr.open('HEAD', 'http://localhost:8080/test-connection', false);\n" +
                            "  try {\n" +
                            "    xhr.send();\n" +
                            "    return { canMakeXHR: true, status: xhr.status };\n" +
                            "  } catch(e) {\n" +
                            "    return { canMakeXHR: false, error: e.toString() };\n" +
                            "  }\n" +
                            "} catch(e) { return { error: e.toString() }; }"
            );
            cspInfo.put("xhrTest", xhrTest);

            // Test if fetch API is also restricted
            Object fetchTest = executeAsyncScript(
                    "const callback = arguments[arguments.length - 1];\n" +
                            "try {\n" +
                            "  fetch('http://localhost:8080/test-connection', { method: 'HEAD' })\n" +
                            "    .then(() => callback({ canFetch: true }))\n" +
                            "    .catch(err => callback({ canFetch: false, error: err.toString() }));\n" +
                            "} catch(e) {\n" +
                            "  callback({ error: e.toString() });\n" +
                            "}"
            );
            cspInfo.put("fetchTest", fetchTest);

            return cspInfo;
        } catch (Exception e) {
            cspInfo.put("error", e.getMessage());
            return cspInfo;
        }
    }

    /**
     * Dump console logs from the browser for debugging
     */
    public void dumpConsoleLogs() {
        try {
            // Use JavaScript to get console logs since Selenium's console logs don't always work reliably
            Object consoleLogs = executeScript(
                    "return (function() {\n" +
                            "  if (!window.__consoleLogs) return 'No console logs captured';\n" +
                            "  return window.__consoleLogs.join('\\n');\n" +
                            "})();"
            );

            logger.info("Browser console logs: {}", consoleLogs);
        } catch (Exception e) {
            logger.error("Failed to retrieve console logs: {}", e.getMessage());
        }
    }

    /**
     * Utility method to debug the page and find potential issues with script execution
     */
    public Map<String, Object> debugPage() {
        Map<String, Object> debugInfo = new HashMap<>();

        try {
            // Basic info
            debugInfo.put("url", getCurrentUrl());
            debugInfo.put("title", getTitle());

            // Check document.readyState
            try {
                Object readyState = executeScript("return document.readyState");
                debugInfo.put("readyState", readyState);
            } catch (Exception e) {
                debugInfo.put("readyState_error", e.getMessage());
            }

            // Check for JS errors
            try {
                Object jsErrors = executeScript(
                        "return window.jsErrors || 'No JS errors captured';"
                );
                debugInfo.put("jsErrors", jsErrors);
            } catch (Exception e) {
                debugInfo.put("jsErrors_error", e.getMessage());
            }

            // Check for recorder script
            try {
                Object recorderActive = executeScript("return window.__csRecorderActive === true;");
                debugInfo.put("recorderActive", recorderActive);

                Object recorderUI = executeScript("return document.getElementById('cs-recorder-indicator') !== null;");
                debugInfo.put("recorderUI", recorderUI);

                // Check for server configuration
                Object serverHost = executeScript("return window.__csServerHost || 'Not set';");
                debugInfo.put("serverHost", serverHost);

                Object serverPort = executeScript("return window.__csServerPort || 'Not set';");
                debugInfo.put("serverPort", serverPort);

                Object contextPath = executeScript("return window.__csContextPath || 'Not set';");
                debugInfo.put("contextPath", contextPath);

                Object apiEndpoint = executeScript("return window.__csApiEndpoint || 'Not set';");
                debugInfo.put("apiEndpoint", apiEndpoint);
            } catch (Exception e) {
                debugInfo.put("recorder_error", e.getMessage());
            }

            return debugInfo;
        } catch (Exception e) {
            debugInfo.put("main_error", e.getMessage());
            return debugInfo;
        }
    }

    @Override
    public BrowserType getBrowserType() {
        return browserType;
    }
}