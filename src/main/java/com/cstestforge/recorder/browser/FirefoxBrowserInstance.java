package com.cstestforge.recorder.browser;

import com.cstestforge.recorder.model.BrowserType;
import com.cstestforge.recorder.model.RecordingConfig;
import com.cstestforge.recorder.model.Viewport;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Firefox-specific implementation of BrowserInstance using Selenium WebDriver.
 */
public class FirefoxBrowserInstance extends AbstractBrowserInstance {
    private static final Logger logger = LoggerFactory.getLogger(FirefoxBrowserInstance.class);
    
    private WebDriver driver;
    
    /**
     * Constructor for FirefoxBrowserInstance
     *
     * @param sessionId The session ID
     * @param browserType The browser type
     * @param config The recording configuration
     */
    public FirefoxBrowserInstance(UUID sessionId, BrowserType browserType, RecordingConfig config) {
        super(sessionId, browserType, config);
    }
    
    @Override
    protected boolean startBrowser(long timeout, TimeUnit unit) {
        try {
            // Setup Firefox options
            FirefoxOptions options = new FirefoxOptions();
            
            // Create and configure Firefox profile
            FirefoxProfile profile = new FirefoxProfile();
            profile.setPreference("dom.webnotifications.enabled", false); // Disable notifications
            profile.setPreference("app.update.auto", false); // Disable updates
            profile.setPreference("app.update.enabled", false);
            profile.setPreference("browser.tabs.remote.autostart", false); // Disable multi-process
            profile.setPreference("browser.tabs.remote.autostart.1", false);
            profile.setPreference("browser.tabs.remote.autostart.2", false);
            
            // Set environment variables if needed
            if (config.getEnvironmentVariables() != null && !config.getEnvironmentVariables().isEmpty()) {
                for (Map.Entry<String, String> entry : config.getEnvironmentVariables().entrySet()) {
                    profile.setPreference(entry.getKey(), entry.getValue());
                }
            }
            
            options.setProfile(profile);
            driver = new FirefoxDriver(options);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            
            return true;
        } catch (Exception e) {
            logger.error("Failed to start Firefox browser: {}", e.getMessage(), e);
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
                logger.error("Error closing Firefox browser: {}", e.getMessage());
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
        // For Firefox, we can't change environment variables after the browser is started
        // This is set during initialization
        logger.debug("Environment variables can only be set before browser start");
    }
    
    @Override
    public void setNetworkCapturing(boolean enabled) {
        super.setNetworkCapturing(enabled);
        // Note: Firefox doesn't have built-in network capturing via DevTools protocol like Chrome
        // For network traffic capturing, you'd typically use a proxy like BrowserMob Proxy
        if (enabled) {
            logger.debug("Network capturing for Firefox requires external proxy setup");
        }
    }
    
    @Override
    public void setConsoleCapturing(boolean enabled) {
        super.setConsoleCapturing(enabled);
        // Note: Firefox doesn't have built-in console capturing via DevTools protocol like Chrome
        // We can use a JavaScript snippet to capture console events
        if (driver != null) {
            try {
                if (enabled) {
                    String consoleCapturingScript = 
                            "window.__csConsoleOriginals = { " +
                            "  log: console.log, " +
                            "  warn: console.warn, " +
                            "  error: console.error, " +
                            "  info: console.info, " +
                            "  debug: console.debug " +
                            "}; " +
                            "function captureConsole(type, args) { " +
                            "  const message = Array.from(args).map(a => String(a)).join(' '); " +
                            "  window.dispatchEvent(new CustomEvent('cs_console_event', { " +
                            "    detail: { type: type, message: message } " +
                            "  })); " +
                            "  return message; " +
                            "} " +
                            "console.log = function() { " +
                            "  const message = captureConsole('log', arguments); " +
                            "  window.__csConsoleOriginals.log.apply(console, arguments); " +
                            "  return message; " +
                            "}; " +
                            "console.error = function() { " +
                            "  const message = captureConsole('error', arguments); " +
                            "  window.__csConsoleOriginals.error.apply(console, arguments); " +
                            "  return message; " +
                            "}; " +
                            "console.warn = function() { " +
                            "  const message = captureConsole('warn', arguments); " +
                            "  window.__csConsoleOriginals.warn.apply(console, arguments); " +
                            "  return message; " +
                            "}; " +
                            "console.info = function() { " +
                            "  const message = captureConsole('info', arguments); " +
                            "  window.__csConsoleOriginals.info.apply(console, arguments); " +
                            "  return message; " +
                            "}; " +
                            "console.debug = function() { " +
                            "  const message = captureConsole('debug', arguments); " +
                            "  window.__csConsoleOriginals.debug.apply(console, arguments); " +
                            "  return message; " +
                            "};";
                    
                    // Inject the console capturing script
                    executeScript(consoleCapturingScript);
                    
                    // Set up event listener for captured console events
                    String eventListenerScript = 
                            "window.addEventListener('cs_console_event', function(event) { " +
                            "  window.dispatchEvent(new CustomEvent('cs_console_captured', { " +
                            "    detail: event.detail " +
                            "  })); " +
                            "});";
                    
                    executeScript(eventListenerScript);
                    
                    logger.debug("Console capturing for Firefox enabled via JavaScript");
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
                            "}";
                    
                    executeScript(restoreScript);
                    
                    logger.debug("Console capturing for Firefox disabled");
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
            throw new IllegalStateException("Firefox browser not initialized");
        }
    }
} 