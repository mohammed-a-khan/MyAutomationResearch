package com.cstestforge.execution.model;

/**
 * Enumeration of supported browser types for test execution
 */
public enum BrowserType {
    /**
     * Chrome browser
     */
    CHROME,
    
    /**
     * Firefox browser
     */
    FIREFOX,
    
    /**
     * Edge browser
     */
    EDGE,
    
    /**
     * Safari browser
     */
    SAFARI,
    
    /**
     * Opera browser
     */
    OPERA;
    
    /**
     * Get the display name for this browser type (properly capitalized)
     * 
     * @return Browser display name
     */
    public String getDisplayName() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
} 