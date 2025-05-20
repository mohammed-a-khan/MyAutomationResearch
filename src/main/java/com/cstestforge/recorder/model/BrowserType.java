package com.cstestforge.recorder.model;

/**
 * Enum representing the supported browser types.
 */
public enum BrowserType {
    CHROME("chrome"),
    FIREFOX("firefox"),
    EDGE("edge"),
    SAFARI("safari"),
    CHROME_PLAYWRIGHT("chrome-playwright"),
    FIREFOX_PLAYWRIGHT("firefox-playwright"),
    WEBKIT_PLAYWRIGHT("webkit-playwright"),
    MSEDGE_PLAYWRIGHT("edge-playwright");

    private final String value;

    BrowserType(String value) {
        this.value = value;
    }

    /**
     * Get the string value of the browser type.
     *
     * @return The browser type as a string
     */
    public String getValue() {
        return value;
    }

    /**
     * Convert a string to a BrowserType.
     *
     * @param browser The browser string
     * @return The corresponding BrowserType or CHROME as default
     */
    public static BrowserType fromString(String browser) {
        if (browser == null || browser.isEmpty()) {
            return CHROME;
        }

        String lowerBrowser = browser.toLowerCase();
        for (BrowserType type : BrowserType.values()) {
            if (type.value.equals(lowerBrowser)) {
                return type;
            }
        }

        // Default to Chrome if no match
        return CHROME;
    }
} 