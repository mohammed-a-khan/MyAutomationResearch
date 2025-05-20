package com.cstestforge.recorder.browser;

import com.cstestforge.recorder.model.BrowserType;
import com.cstestforge.recorder.model.RecordingConfig;
import com.cstestforge.recorder.model.Viewport;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v115.network.Network;
import org.openqa.selenium.devtools.v115.console.Console;
import org.openqa.selenium.remote.RemoteWebDriver;
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
 * Microsoft Edge-specific implementation of BrowserInstance using Selenium WebDriver.
 */
public class EdgeBrowserInstance extends AbstractBrowserInstance {
    private static final Logger logger = LoggerFactory.getLogger(EdgeBrowserInstance.class);
    
    private WebDriver driver;
    private DevTools devTools;
    
    /**
     * Constructor for EdgeBrowserInstance
     *
     * @param sessionId The session ID
     * @param browserType The browser type
     * @param config The recording configuration
     */
    public EdgeBrowserInstance(UUID sessionId, BrowserType browserType, RecordingConfig config) {
        super(sessionId, browserType, config);
    }
    
    @Override
    protected boolean startBrowser(long timeout, TimeUnit unit) {
        try {
            // Setup Edge options
            EdgeOptions options = new EdgeOptions();
            
            // Add arguments to Edge for better compatibility with recorder
            options.addArguments("--disable-web-security"); // Allow cross-origin requests
            options.addArguments("--disable-popup-blocking"); // Prevent popup blocking
            options.addArguments("--start-maximized"); // Start maximized
            
            // Set experimental options
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("profile.default_content_setting_values.notifications", 2); // Disable notifications
            options.setExperimentalOption("prefs", prefs);
            
            // Setup environment variables if any
            if (config.getEnvironmentVariables() != null && !config.getEnvironmentVariables().isEmpty()) {
                options.setExperimentalOption("env", config.getEnvironmentVariables());
            }
            
            // Start Edge driver
            driver = new EdgeDriver(options);
            
            // Set default timeouts
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            
            // Setup devtools for network capture if enabled
            setupDevTools();
            
            return true;
        } catch (Exception e) {
            logger.error("Failed to start Edge browser: {}", e.getMessage(), e);
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
                logger.error("Error closing Edge browser: {}", e.getMessage());
            } finally {
                driver = null;
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
            
            logger.info("Navigating to URL: {}", url);
            
            // Set page load timeout
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            
            // Perform navigation
            driver.get(url);
            
            // Store current URL and title
            currentUrl = driver.getCurrentUrl();
            currentTitle = driver.getTitle();
            
            // Wait for page to load
            try {
                new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(webDriver -> "complete".equals(
                        ((RemoteWebDriver) webDriver).executeScript("return document.readyState")));
            } catch (Exception e) {
                logger.warn("Page load wait timed out, but navigation was successful: {}", url);
                // Still consider navigation successful even if page load timeout occurs
                return true;
            }
            
            logger.info("Successfully navigated to URL: {} (actual: {})", url, currentUrl);
            return true;
        } catch (Exception e) {
            logger.error("Failed to navigate to URL {}: {}", url, e.getMessage());
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
        // For Edge, we can't change environment variables after the browser is started
        // This is set during initialization
        logger.debug("Environment variables can only be set before browser start");
    }
    
    @Override
    public void setNetworkCapturing(boolean enabled) {
        super.setNetworkCapturing(enabled);
        if (devTools != null && driver != null) {
            try {
                if (enabled) {
                    devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
                    
                    // Set up network listeners
                    devTools.addListener(Network.requestWillBeSent(), requestWillBeSent -> {
                        String url = requestWillBeSent.getRequest().getUrl();
                        logger.debug("Network request: {}", url);
                        // Here you would typically send this to a collector or websocket
                    });
                    
                    devTools.addListener(Network.responseReceived(), responseReceived -> {
                        String url = responseReceived.getResponse().getUrl();
                        int status = responseReceived.getResponse().getStatus();
                        logger.debug("Network response: {} ({})", url, status);
                        // Here you would typically send this to a collector or websocket
                    });
                } else {
                    devTools.send(Network.disable());
                }
            } catch (Exception e) {
                logger.error("Failed to set network capturing: {}", e.getMessage());
            }
        }
    }
    
    @Override
    public void setConsoleCapturing(boolean enabled) {
        super.setConsoleCapturing(enabled);
        if (devTools != null && driver != null) {
            try {
                if (enabled) {
                    devTools.send(Console.enable());
                    
                    // Set up console listeners
                    devTools.addListener(Console.messageAdded(), messageAdded -> {
                        String message = messageAdded.getText();
                        String level = messageAdded.getLevel().toString();
                        logger.debug("Console [{}]: {}", level, message);
                        // Here you would typically send this to a collector or websocket
                    });
                } else {
                    devTools.send(Console.disable());
                }
            } catch (Exception e) {
                logger.error("Failed to set console capturing: {}", e.getMessage());
            }
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
     * Set up DevTools for network and console monitoring
     */
    private void setupDevTools() {
        if (driver instanceof EdgeDriver) {
            try {
                devTools = ((EdgeDriver) driver).getDevTools();
                devTools.createSession();
                
                // Set up network monitoring if enabled
                if (networkCapturingEnabled.get()) {
                    setNetworkCapturing(true);
                }
                
                // Set up console monitoring if enabled
                if (consoleCapturingEnabled.get()) {
                    setConsoleCapturing(true);
                }
            } catch (Exception e) {
                logger.error("Failed to set up DevTools: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Ensure the WebDriver is initialized
     *
     * @throws IllegalStateException if the driver is not initialized
     */
    private void checkDriverInitialized() {
        if (driver == null) {
            throw new IllegalStateException("Edge browser not initialized");
        }
    }
} 