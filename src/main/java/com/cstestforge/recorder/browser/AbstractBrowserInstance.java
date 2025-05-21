package com.cstestforge.recorder.browser;

import com.cstestforge.recorder.model.BrowserType;
import com.cstestforge.recorder.model.RecordingConfig;
import com.cstestforge.recorder.model.Viewport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract base class for browser instances that implements common functionality.
 */
public abstract class AbstractBrowserInstance implements BrowserInstance {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractBrowserInstance.class);
    
    protected final UUID sessionId;
    protected final BrowserType browserType;
    protected final RecordingConfig config;
    protected final AtomicBoolean running;
    
    protected String currentUrl;
    protected String currentTitle;
    protected AtomicBoolean networkCapturingEnabled;
    protected AtomicBoolean consoleCapturingEnabled;
    
    /**
     * Constructor with session ID, browser type, and configuration.
     *
     * @param sessionId Session ID
     * @param browserType Browser type
     * @param config Recording configuration
     */
    protected AbstractBrowserInstance(UUID sessionId, BrowserType browserType, RecordingConfig config) {
        this.sessionId = sessionId;
        this.browserType = browserType;
        this.config = config;
        this.running = new AtomicBoolean(false);
        this.networkCapturingEnabled = new AtomicBoolean(config.isCaptureNetwork());
        this.consoleCapturingEnabled = new AtomicBoolean(config.isCaptureConsole());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean start(long timeout, TimeUnit unit) {
        try {
            logger.info("Starting {} browser for session {}", browserType, sessionId);
            boolean result = startBrowser(timeout, unit);
            if (result) {
                running.set(true);
                
                // Set viewport if specified
                if (config.getViewport() != null) {
                    setViewport(config.getViewport());
                }
                
                // Set environment variables
                if (config.getEnvironmentVariables() != null) {
                    setEnvironmentVariables(config.getEnvironmentVariables());
                }
                
                // Enable network/console capturing if needed
                setNetworkCapturing(config.isCaptureNetwork());
                setConsoleCapturing(config.isCaptureConsole());
            }
            return result;
        } catch (Exception e) {
            logger.error("Failed to start {} browser for session {}", browserType, sessionId, e);
            return false;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            try {
                logger.info("Stopping {} browser for session {}", browserType, sessionId);
                stopBrowser();
            } catch (Exception e) {
                logger.error("Error stopping {} browser for session {}", browserType, sessionId, e);
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setNetworkCapturing(boolean enabled) {
        this.networkCapturingEnabled.set(enabled);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setConsoleCapturing(boolean enabled) {
        this.consoleCapturingEnabled.set(enabled);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getCurrentUrl() {
        return currentUrl;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getTitle() {
        return currentTitle;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public BrowserType getBrowserType() {
        return browserType;
    }
    
    /**
     * Start the browser implementation.
     *
     * @param timeout Timeout value
     * @param unit Timeout unit
     * @return True if browser started successfully
     */
    protected abstract boolean startBrowser(long timeout, TimeUnit unit);
    
    /**
     * Stop the browser implementation.
     */
    protected abstract void stopBrowser();
    
    /**
     * Check if the browser is still responsive.
     *
     * @return True if the browser is responsive
     */
    protected abstract boolean isResponsive();
    
    /**
     * Inject the recorder script into the page.
     *
     * @return True if the script was successfully injected
     */
    protected abstract boolean injectRecorderScript();
} 