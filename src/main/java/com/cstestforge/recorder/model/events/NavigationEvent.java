package com.cstestforge.recorder.model.events;

import com.cstestforge.recorder.model.RecordedEvent;
import com.cstestforge.recorder.model.RecordedEventType;

/**
 * Represents a navigation event recorded during a browser session.
 */
public class NavigationEvent extends RecordedEvent {
    
    private String sourceUrl;
    private String targetUrl;
    private boolean isRedirect;
    private boolean isBackNavigation;
    private boolean isForwardNavigation;
    private boolean isRefresh;
    private long loadTimeMs;
    private NavigationTrigger trigger;
    
    /**
     * Default constructor
     */
    public NavigationEvent() {
        super(RecordedEventType.NAVIGATION);
    }
    
    /**
     * Constructor with source and target URLs
     *
     * @param sourceUrl The URL navigated from
     * @param targetUrl The URL navigated to
     */
    public NavigationEvent(String sourceUrl, String targetUrl) {
        super(RecordedEventType.NAVIGATION);
        this.sourceUrl = sourceUrl;
        this.targetUrl = targetUrl;
    }
    
    /**
     * Get the source URL
     *
     * @return The URL navigated from
     */
    public String getSourceUrl() {
        return sourceUrl;
    }
    
    /**
     * Set the source URL
     *
     * @param sourceUrl The URL navigated from
     */
    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
    
    /**
     * Get the target URL
     *
     * @return The URL navigated to
     */
    public String getTargetUrl() {
        return targetUrl;
    }
    
    /**
     * Set the target URL
     *
     * @param targetUrl The URL navigated to
     */
    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }
    
    /**
     * Check if this navigation is a redirect
     *
     * @return True if this is a redirect
     */
    public boolean isRedirect() {
        return isRedirect;
    }
    
    /**
     * Set whether this navigation is a redirect
     *
     * @param redirect True if this is a redirect
     */
    public void setRedirect(boolean redirect) {
        isRedirect = redirect;
    }
    
    /**
     * Check if this is a back navigation
     *
     * @return True if this is a back navigation
     */
    public boolean isBackNavigation() {
        return isBackNavigation;
    }
    
    /**
     * Set whether this is a back navigation
     *
     * @param backNavigation True if this is a back navigation
     */
    public void setBackNavigation(boolean backNavigation) {
        isBackNavigation = backNavigation;
    }
    
    /**
     * Check if this is a forward navigation
     *
     * @return True if this is a forward navigation
     */
    public boolean isForwardNavigation() {
        return isForwardNavigation;
    }
    
    /**
     * Set whether this is a forward navigation
     *
     * @param forwardNavigation True if this is a forward navigation
     */
    public void setForwardNavigation(boolean forwardNavigation) {
        isForwardNavigation = forwardNavigation;
    }
    
    /**
     * Check if this is a page refresh
     *
     * @return True if this is a page refresh
     */
    public boolean isRefresh() {
        return isRefresh;
    }
    
    /**
     * Set whether this is a page refresh
     *
     * @param refresh True if this is a page refresh
     */
    public void setRefresh(boolean refresh) {
        isRefresh = refresh;
    }
    
    /**
     * Get the page load time in milliseconds
     *
     * @return The page load time
     */
    public long getLoadTimeMs() {
        return loadTimeMs;
    }
    
    /**
     * Set the page load time in milliseconds
     *
     * @param loadTimeMs The page load time
     */
    public void setLoadTimeMs(long loadTimeMs) {
        this.loadTimeMs = loadTimeMs;
    }
    
    /**
     * Get the navigation trigger
     *
     * @return The navigation trigger
     */
    public NavigationTrigger getTrigger() {
        return trigger;
    }
    
    /**
     * Set the navigation trigger
     *
     * @param trigger The navigation trigger
     */
    public void setTrigger(NavigationTrigger trigger) {
        this.trigger = trigger;
    }
    
    @Override
    public boolean isValid() {
        return targetUrl != null && !targetUrl.isEmpty();
    }
    
    @Override
    public String toHumanReadableDescription() {
        if (isBackNavigation) {
            return "Navigate back to " + targetUrl;
        } else if (isForwardNavigation) {
            return "Navigate forward to " + targetUrl;
        } else if (isRefresh) {
            return "Refresh page " + targetUrl;
        } else if (isRedirect) {
            return "Redirect from " + sourceUrl + " to " + targetUrl;
        } else {
            return "Navigate to " + targetUrl;
        }
    }
    
    /**
     * Enum representing what triggered a navigation event
     */
    public enum NavigationTrigger {
        LINK_CLICK,
        FORM_SUBMISSION,
        SCRIPT,
        USER_INITIATED,
        REDIRECT,
        RELOAD,
        BACK_BUTTON,
        FORWARD_BUTTON,
        ADDRESS_BAR,
        HISTORY_API,
        OTHER
    }
} 