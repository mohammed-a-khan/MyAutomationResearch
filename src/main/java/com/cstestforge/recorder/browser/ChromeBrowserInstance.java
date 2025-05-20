package com.cstestforge.recorder.browser;

import com.cstestforge.recorder.model.BrowserType;
import com.cstestforge.recorder.model.RecordingConfig;
import com.cstestforge.recorder.model.Viewport;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v85.network.Network;
import org.openqa.selenium.devtools.v85.console.Console;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Chrome-specific implementation of BrowserInstance using Selenium WebDriver.
 */
public class ChromeBrowserInstance extends AbstractBrowserInstance {
    private static final Logger logger = LoggerFactory.getLogger(ChromeBrowserInstance.class);
    
    private WebDriver driver;
    private DevTools devTools;
    private ChromeDriverService driverService;
    
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
            // Setup Chrome options
            ChromeOptions options = new ChromeOptions();
            
            // Add arguments to Chrome for better compatibility with recorder
            options.addArguments("--disable-web-security"); // Allow cross-origin requests
            options.addArguments("--disable-popup-blocking"); // Prevent popup blocking
            options.addArguments("--start-maximized"); // Start maximized
            options.addArguments("--remote-allow-origins=*"); // Allow remote origins
            
            // Set experimental options
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("dom.webnotifications.enabled", false); // Disable notifications
            options.setExperimentalOption("prefs", prefs);
            
            // Store environment variables in a property where they can be accessed later
            // but don't add them as Chrome experimental options since it's not supported
            if (config.getEnvironmentVariables() != null && !config.getEnvironmentVariables().isEmpty()) {
                // Just log them for now - don't pass to Chrome options
                logger.info("Framework configuration: {}", config.getEnvironmentVariables());
                // Do NOT use: options.setExperimentalOption("env", config.getEnvironmentVariables());
            }
            
            // Start Chrome driver
            driverService = new ChromeDriverService.Builder().build();
            driver = new ChromeDriver(driverService, options);
            
            // Set default timeouts
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            
            // Initialize DevTools (for CDP features)
            if (driver instanceof ChromeDriver) {
                try {
                    devTools = ((ChromeDriver) driver).getDevTools();
                    devTools.createSession();
                    
                    // Enable network events if requested
                    if (config.isCaptureNetwork()) {
                        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
                        
                        // Add network event listeners
                        devTools.addListener(Network.requestWillBeSent(), request -> {
                            logger.debug("Request sent: {}", request.getRequest().getUrl());
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
                } catch (Exception e) {
                    logger.warn("Failed to initialize Chrome DevTools: {}", e.getMessage());
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
            checkDriverInitialized();
            
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
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            
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
            
            // Wait for page to load
            try {
                // First wait for document.readyState to be 'complete'
                boolean loaded = waitForDocumentReady(30);
                
                if (!loaded) {
                    logger.warn("Page load timeout occurred for URL: {}", navigateUrl);
                } else {
                    logger.info("Document ready state is complete for URL: {}", navigateUrl);
                }
                
                // Regardless of readyState, check if we actually arrived at the URL
                // or were redirected elsewhere
                currentUrl = driver.getCurrentUrl();
                
                // Check if navigation was successful by comparing domain parts
                String requestedDomain = extractDomain(navigateUrl);
                String actualDomain = extractDomain(currentUrl);
                
                if (!actualDomain.equals(requestedDomain)) {
                    logger.warn("Navigation may have been redirected: requested={}, actual={}", 
                            requestedDomain, actualDomain);
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
     * Extract domain from URL for comparison
     */
    private String extractDomain(String url) {
        try {
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
            return url; // Return original if parsing fails
        }
    }
    
    /**
     * Wait for document to be ready
     */
    private boolean waitForDocumentReady(int timeoutSeconds) {
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                .until(webDriver -> {
                    Object readyState = ((RemoteWebDriver) webDriver).executeScript("return document.readyState");
                    logger.debug("Document ready state: {}", readyState);
                    return "complete".equals(readyState);
                });
        } catch (Exception e) {
            logger.warn("Timeout waiting for document ready state: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public void setViewport(Viewport viewport) {
        checkDriverInitialized();
        if (viewport != null) {
            driver.manage().window().setSize(
                    new org.openqa.selenium.Dimension(viewport.getWidth(), viewport.getHeight()));
        }
    }
    
    @Override
    public Object injectScript(String script) {
        checkDriverInitialized();
        if (script == null || script.isEmpty()) {
            return null;
        }
        return executeScript(script);
    }
    
    @Override
    public Object executeAsyncScript(String script, Object... args) {
        checkDriverInitialized();
        if (script == null) {
            return null;
        }
        
        if (driver instanceof RemoteWebDriver) {
            return ((RemoteWebDriver) driver).executeAsyncScript(script, args);
        }
        return null;
    }
    
    @Override
    public Object executeScript(String script, Object... args) {
        checkDriverInitialized();
        if (script == null) {
            return null;
        }
        
        if (driver instanceof RemoteWebDriver) {
            return ((RemoteWebDriver) driver).executeScript(script, args);
        }
        return null;
    }
    
    @Override
    public byte[] captureScreenshot() {
        checkDriverInitialized();
        if (driver instanceof RemoteWebDriver) {
            return ((RemoteWebDriver) driver).getScreenshotAs(OutputType.BYTES);
        }
        return null;
    }
    
    @Override
    public byte[] captureElementScreenshot(String selector) {
        checkDriverInitialized();
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
        try {
            if (devTools == null) {
                logger.debug("Network capturing not supported in this Chrome version");
                return;
            }
            
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
        try {
            if (devTools == null) {
                logger.debug("Console capturing not supported in this Chrome version");
                return;
            }
            
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
        checkDriverInitialized();
        
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(timeoutMs));
            return wait.until(driver -> {
                try {
                    Object result = ((RemoteWebDriver) driver).executeScript("return (" + conditionScript + ")");
                    return Boolean.TRUE.equals(result);
                } catch (Exception e) {
                    return false;
                }
            });
        } catch (Exception e) {
            logger.error("Error waiting for condition: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    protected boolean isResponsive() {
        if (driver == null) {
            return false;
        }
        
        try {
            // Simple check - fetch title
            driver.getTitle();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    protected boolean injectRecorderScript() {
        return executeScript("return true;") != null;
    }
    
    /**
     * Ensure the WebDriver is initialized
     *
     * @throws IllegalStateException if the driver is not initialized
     */
    private void checkDriverInitialized() {
        if (driver == null) {
            throw new IllegalStateException("Chrome browser not initialized");
        }
    }
} 