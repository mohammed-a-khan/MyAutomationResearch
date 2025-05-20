package com.cstestforge.recorder.browser;

import com.cstestforge.recorder.model.BrowserType;
import com.cstestforge.recorder.model.RecordingConfig;
import com.cstestforge.recorder.model.Viewport;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Safari-specific implementation of BrowserInstance using Selenium WebDriver.
 * Note: Safari WebDriver has more limitations compared to Chrome/Firefox regarding
 * JavaScript injection and DevTools Protocol access.
 */
public class SafariBrowserInstance extends AbstractBrowserInstance {
    private static final Logger logger = LoggerFactory.getLogger(SafariBrowserInstance.class);
    
    private WebDriver driver;
    
    /**
     * Constructor for SafariBrowserInstance
     *
     * @param sessionId The session ID
     * @param browserType The browser type
     * @param config The recording configuration
     */
    public SafariBrowserInstance(UUID sessionId, BrowserType browserType, RecordingConfig config) {
        super(sessionId, browserType, config);
    }
    
    @Override
    protected boolean startBrowser(long timeout, TimeUnit unit) {
        try {
            // Setup Safari options
            SafariOptions options = new SafariOptions();
            
            // Safari has limited options compared to Chrome/Firefox
            options.setAutomaticInspection(true);
            
            // Start Safari driver
            driver = new SafariDriver(options);
            
            // Set default timeouts
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            
            return true;
        } catch (Exception e) {
            logger.error("Failed to start Safari browser: {}", e.getMessage(), e);
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
                logger.error("Error closing Safari browser: {}", e.getMessage());
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
        
        // For Safari, we need to inject script via executeScript
        return executeScript(script);
    }
    
    @Override
    public Object executeAsyncScript(String script, Object... args) {
        checkDriverInitialized();
        if (script == null) {
            return null;
        }
        
        if (driver instanceof RemoteWebDriver) {
            try {
                return ((RemoteWebDriver) driver).executeAsyncScript(script, args);
            } catch (Exception e) {
                logger.warn("Failed to execute async script in Safari: {}", e.getMessage());
                return null;
            }
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
            try {
                return ((RemoteWebDriver) driver).executeScript(script, args);
            } catch (Exception e) {
                logger.warn("Failed to execute script in Safari: {}", e.getMessage());
                return null;
            }
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
        // Safari doesn't support setting environment variables
        logger.debug("Safari doesn't support setting environment variables");
    }
    
    @Override
    public void setNetworkCapturing(boolean enabled) {
        super.setNetworkCapturing(enabled);
        // Safari WebDriver doesn't support DevTools Protocol for network capture
        if (enabled) {
            logger.debug("Network capturing is not supported in Safari WebDriver");
            
            // Try to implement basic network capturing using JavaScript
            try {
                String networkCaptureScript = 
                        "if (!window.__csNetworkMonitorSetup) { " +
                        "  window.__csNetworkMonitorSetup = true; " +
                        "  window.__csNetworkRequests = []; " +
                        "  const originalFetch = window.fetch; " +
                        "  window.fetch = async function(resource, init) { " +
                        "    const startTime = new Date().getTime(); " +
                        "    let request = { " +
                        "      url: resource instanceof Request ? resource.url : resource, " +
                        "      method: resource instanceof Request ? resource.method : (init?.method || 'GET'), " +
                        "      startTime: startTime " +
                        "    }; " +
                        "    window.__csNetworkRequests.push(request); " +
                        "    try { " +
                        "      const response = await originalFetch.apply(this, arguments); " +
                        "      request.endTime = new Date().getTime(); " +
                        "      request.status = response.status; " +
                        "      request.statusText = response.statusText; " +
                        "      return response; " +
                        "    } catch (error) { " +
                        "      request.endTime = new Date().getTime(); " +
                        "      request.error = error.toString(); " +
                        "      throw error; " +
                        "    } " +
                        "  }; " +
                        "  const originalXhrOpen = XMLHttpRequest.prototype.open; " +
                        "  const originalXhrSend = XMLHttpRequest.prototype.send; " +
                        "  XMLHttpRequest.prototype.open = function(method, url) { " +
                        "    this.__csRequest = { " +
                        "      url: url, " +
                        "      method: method, " +
                        "      startTime: new Date().getTime() " +
                        "    }; " +
                        "    window.__csNetworkRequests.push(this.__csRequest); " +
                        "    return originalXhrOpen.apply(this, arguments); " +
                        "  }; " +
                        "  XMLHttpRequest.prototype.send = function() { " +
                        "    if (this.__csRequest) { " +
                        "      this.addEventListener('load', () => { " +
                        "        this.__csRequest.endTime = new Date().getTime(); " +
                        "        this.__csRequest.status = this.status; " +
                        "        this.__csRequest.statusText = this.statusText; " +
                        "      }); " +
                        "      this.addEventListener('error', () => { " +
                        "        this.__csRequest.endTime = new Date().getTime(); " +
                        "        this.__csRequest.error = 'Network error'; " +
                        "      }); " +
                        "    } " +
                        "    return originalXhrSend.apply(this, arguments); " +
                        "  }; " +
                        "} " +
                        "return 'Network monitoring enabled';";
                
                executeScript(networkCaptureScript);
            } catch (Exception e) {
                logger.error("Failed to inject network capture script in Safari: {}", e.getMessage());
            }
        }
    }
    
    @Override
    public void setConsoleCapturing(boolean enabled) {
        super.setConsoleCapturing(enabled);
        // Safari WebDriver doesn't support DevTools Protocol for console capture
        if (driver != null) {
            try {
                if (enabled) {
                    String consoleCapturingScript = 
                            "if (!window.__csConsoleMonitorSetup) { " +
                            "  window.__csConsoleMonitorSetup = true; " +
                            "  window.__csConsoleOriginals = { " +
                            "    log: console.log, " +
                            "    warn: console.warn, " +
                            "    error: console.error, " +
                            "    info: console.info, " +
                            "    debug: console.debug " +
                            "  }; " +
                            "  window.__csConsoleMessages = []; " +
                            "  function captureConsole(type, args) { " +
                            "    const message = Array.from(args).map(a => String(a)).join(' '); " +
                            "    const entry = { type: type, message: message, timestamp: new Date().getTime() }; " +
                            "    window.__csConsoleMessages.push(entry); " +
                            "    return message; " +
                            "  } " +
                            "  console.log = function() { " +
                            "    const message = captureConsole('log', arguments); " +
                            "    window.__csConsoleOriginals.log.apply(console, arguments); " +
                            "    return message; " +
                            "  }; " +
                            "  console.error = function() { " +
                            "    const message = captureConsole('error', arguments); " +
                            "    window.__csConsoleOriginals.error.apply(console, arguments); " +
                            "    return message; " +
                            "  }; " +
                            "  console.warn = function() { " +
                            "    const message = captureConsole('warn', arguments); " +
                            "    window.__csConsoleOriginals.warn.apply(console, arguments); " +
                            "    return message; " +
                            "  }; " +
                            "  console.info = function() { " +
                            "    const message = captureConsole('info', arguments); " +
                            "    window.__csConsoleOriginals.info.apply(console, arguments); " +
                            "    return message; " +
                            "  }; " +
                            "  console.debug = function() { " +
                            "    const message = captureConsole('debug', arguments); " +
                            "    window.__csConsoleOriginals.debug.apply(console, arguments); " +
                            "    return message; " +
                            "  }; " +
                            "} " +
                            "return 'Console monitoring enabled';";
                    
                    // Inject the console capturing script
                    executeScript(consoleCapturingScript);
                    
                    logger.debug("Console capturing for Safari enabled via JavaScript");
                } else {
                    // Restore original console methods
                    String restoreScript = 
                            "if (window.__csConsoleOriginals) { " +
                            "  console.log = window.__csConsoleOriginals.log; " +
                            "  console.error = window.__csConsoleOriginals.error; " +
                            "  console.warn = window.__csConsoleOriginals.warn; " +
                            "  console.info = window.__csConsoleOriginals.info; " +
                            "  console.debug = window.__csConsoleOriginals.debug; " +
                            "  delete window.__csConsoleOriginals; " +
                            "  delete window.__csConsoleMonitorSetup; " +
                            "  delete window.__csConsoleMessages; " +
                            "} " +
                            "return 'Console monitoring disabled';";
                    
                    executeScript(restoreScript);
                    
                    logger.debug("Console capturing for Safari disabled");
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
     * Ensure the WebDriver is initialized
     *
     * @throws IllegalStateException if the driver is not initialized
     */
    private void checkDriverInitialized() {
        if (driver == null) {
            throw new IllegalStateException("Safari browser not initialized");
        }
    }
} 