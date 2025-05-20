package com.cstestforge.recorder.browser;

import com.cstestforge.recorder.model.Viewport;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Interface defining operations for browser control.
 * Implemented by different browser types (Chrome, Firefox, etc.).
 */
public interface BrowserInstance {

    /**
     * Start the browser with a timeout.
     *
     * @param timeout Timeout value
     * @param unit Timeout unit
     * @return True if browser started successfully
     */
    boolean start(long timeout, TimeUnit unit);

    /**
     * Stop the browser and clean up resources.
     */
    void stop();

    /**
     * Navigate to a specific URL.
     *
     * @param url URL to navigate to
     * @return True if navigation was successful
     */
    boolean navigate(String url);

    /**
     * Set the browser viewport dimensions.
     *
     * @param viewport Viewport configuration
     */
    void setViewport(Viewport viewport);

    /**
     * Inject a JavaScript script into the current page.
     *
     * @param script JavaScript code to inject
     * @return Result of the script execution, if any
     */
    Object injectScript(String script);

    /**
     * Execute an asynchronous JavaScript script.
     *
     * @param script JavaScript code to execute
     * @param args Arguments to pass to the script
     * @return Result of the script execution
     */
    Object executeAsyncScript(String script, Object... args);

    /**
     * Execute a synchronous JavaScript script.
     *
     * @param script JavaScript code to execute
     * @param args Arguments to pass to the script
     * @return Result of the script execution
     */
    Object executeScript(String script, Object... args);

    /**
     * Capture a screenshot of the current page.
     *
     * @return Screenshot as a byte array (PNG format)
     */
    byte[] captureScreenshot();

    /**
     * Capture a screenshot of a specific element.
     *
     * @param selector CSS selector for the element
     * @return Screenshot as a byte array (PNG format)
     */
    byte[] captureElementScreenshot(String selector);

    /**
     * Check if the browser is running.
     *
     * @return True if the browser is running
     */
    boolean isRunning();

    /**
     * Get the current URL of the browser.
     *
     * @return Current URL
     */
    String getCurrentUrl();

    /**
     * Get the title of the current page.
     *
     * @return Page title
     */
    String getTitle();
    
    /**
     * Set browser environment variables.
     *
     * @param environmentVariables Map of environment variables
     */
    void setEnvironmentVariables(Map<String, String> environmentVariables);
    
    /**
     * Enable or disable network traffic capturing.
     *
     * @param enabled True to enable network traffic capturing
     */
    void setNetworkCapturing(boolean enabled);
    
    /**
     * Enable or disable console output capturing.
     *
     * @param enabled True to enable console output capturing
     */
    void setConsoleCapturing(boolean enabled);
    
    /**
     * Wait for a condition to be satisfied.
     *
     * @param conditionScript JavaScript that returns true when the condition is met
     * @param timeoutMs Timeout in milliseconds
     * @return True if the condition was satisfied within the timeout
     */
    boolean waitForCondition(String conditionScript, long timeoutMs);
} 