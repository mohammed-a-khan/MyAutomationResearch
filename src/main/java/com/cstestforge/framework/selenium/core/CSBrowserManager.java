package com.cstestforge.framework.selenium.core;

import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Browser manager for advanced configuration of browser instances.
 * Provides helper methods for browser-specific options and preferences.
 */
public class CSBrowserManager {
    private static final Logger logger = LoggerFactory.getLogger(CSBrowserManager.class);
    private static final Properties browserPreferences = new Properties();
    
    static {
        // Load browser preferences from property files if available
        try {
            browserPreferences.load(CSBrowserManager.class.getClassLoader().getResourceAsStream("browser-config.properties"));
        } catch (Exception e) {
            logger.debug("No browser-config.properties file found, using defaults");
        }
    }
    
    /**
     * Configure Chrome options with advanced settings.
     * 
     * @param headless Whether to run in headless mode
     * @param arguments Additional browser arguments
     * @return Configured ChromeOptions
     */
    public static ChromeOptions configureChromeOptions(boolean headless, String... arguments) {
        ChromeOptions options = new ChromeOptions();
        
        // Set headless mode if requested
        if (headless) {
            options.addArguments("--headless=new");
        }
        
        // Add standard arguments for stability
        options.addArguments("--start-maximized");
        options.addArguments("--disable-infobars");
        options.addArguments("--disable-extensions");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--remote-allow-origins=*");
        
        // Add custom arguments if provided
        if (arguments != null && arguments.length > 0) {
            options.addArguments(arguments);
        }
        
        // Add preferences from configuration
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        
        // Add download directory if configured
        String downloadDir = browserPreferences.getProperty("chrome.download.directory");
        if (downloadDir != null && !downloadDir.isEmpty()) {
            File directory = new File(downloadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            prefs.put("download.default_directory", directory.getAbsolutePath());
            prefs.put("download.prompt_for_download", false);
            prefs.put("download.directory_upgrade", true);
        }
        
        options.setExperimentalOption("prefs", prefs);
        
        // Configure logging
        options.setExperimentalOption("excludeSwitches", Arrays.asList("enable-logging"));
        
        logger.debug("Configured Chrome options: {}", options);
        return options;
    }
    
    /**
     * Configure Firefox options with advanced settings.
     * 
     * @param headless Whether to run in headless mode
     * @param arguments Additional browser arguments
     * @return Configured FirefoxOptions
     */
    public static FirefoxOptions configureFirefoxOptions(boolean headless, String... arguments) {
        FirefoxOptions options = new FirefoxOptions();
        
        // Set headless mode if requested
        if (headless) {
            options.addArguments("-headless");
        }
        
        // Add custom arguments if provided
        if (arguments != null && arguments.length > 0) {
            options.addArguments(arguments);
        }
        
        // Configure Firefox profile
        FirefoxProfile profile = new FirefoxProfile();
        
        // Disable Firefox update notifications
        profile.setPreference("app.update.enabled", false);
        
        // Configure download behavior
        String downloadDir = browserPreferences.getProperty("firefox.download.directory");
        if (downloadDir != null && !downloadDir.isEmpty()) {
            File directory = new File(downloadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            profile.setPreference("browser.download.folderList", 2);
            profile.setPreference("browser.download.manager.showWhenStarting", false);
            profile.setPreference("browser.download.dir", directory.getAbsolutePath());
            profile.setPreference("browser.helperApps.neverAsk.saveToDisk", 
                    "application/pdf,application/x-pdf,application/octet-stream,application/zip,text/csv");
        }
        
        // Set the profile
        options.setProfile(profile);
        
        logger.debug("Configured Firefox options: {}", options);
        return options;
    }
    
    /**
     * Configure Edge options with advanced settings.
     * 
     * @param headless Whether to run in headless mode
     * @param arguments Additional browser arguments
     * @return Configured EdgeOptions
     */
    public static EdgeOptions configureEdgeOptions(boolean headless, String... arguments) {
        EdgeOptions options = new EdgeOptions();
        
        // Set headless mode if requested
        if (headless) {
            options.addArguments("--headless=new");
        }
        
        // Add standard arguments for stability
        options.addArguments("--start-maximized");
        options.addArguments("--disable-infobars");
        options.addArguments("--disable-extensions");
        
        // Add custom arguments if provided
        if (arguments != null && arguments.length > 0) {
            options.addArguments(arguments);
        }
        
        // Configure preferences
        Map<String, Object> prefs = new HashMap<>();
        
        // Add download directory if configured
        String downloadDir = browserPreferences.getProperty("edge.download.directory");
        if (downloadDir != null && !downloadDir.isEmpty()) {
            File directory = new File(downloadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            prefs.put("download.default_directory", directory.getAbsolutePath());
            prefs.put("download.prompt_for_download", false);
        }
        
        options.setExperimentalOption("prefs", prefs);
        
        logger.debug("Configured Edge options: {}", options);
        return options;
    }
    
    /**
     * Configure a proxy for browser options.
     * 
     * @param host Proxy host
     * @param port Proxy port
     * @return Configured Proxy object
     */
    public static Proxy configureProxy(String host, int port) {
        Proxy proxy = new Proxy();
        proxy.setHttpProxy(host + ":" + port);
        proxy.setSslProxy(host + ":" + port);
        return proxy;
    }
    
    /**
     * Add browser extensions from file paths.
     * 
     * @param options ChromeOptions to add extensions to
     * @param extensionPaths Paths to the extension files
     */
    public static void addExtensionsToChrome(ChromeOptions options, String... extensionPaths) {
        if (extensionPaths != null && extensionPaths.length > 0) {
            for (String path : extensionPaths) {
                File extension = new File(path);
                if (extension.exists()) {
                    options.addExtensions(extension);
                    logger.debug("Added Chrome extension: {}", path);
                } else {
                    logger.warn("Chrome extension not found: {}", path);
                }
            }
        }
    }
    
    /**
     * Add browser extensions from file paths.
     * 
     * @param profile FirefoxProfile to add extensions to
     * @param extensionPaths Paths to the extension files
     * @throws Exception if extension cannot be installed
     */
    public static void addExtensionsToFirefox(FirefoxProfile profile, String... extensionPaths) throws Exception {
        if (extensionPaths != null && extensionPaths.length > 0) {
            for (String path : extensionPaths) {
                File extension = new File(path);
                if (extension.exists()) {
                    profile.addExtension(extension);
                    logger.debug("Added Firefox extension: {}", path);
                } else {
                    logger.warn("Firefox extension not found: {}", path);
                }
            }
        }
    }
    
    /**
     * Set a browser preference property.
     * 
     * @param key Property key
     * @param value Property value
     */
    public static void setPreference(String key, String value) {
        browserPreferences.setProperty(key, value);
    }
    
    /**
     * Get a browser preference property.
     * 
     * @param key Property key
     * @return Property value or null if not found
     */
    public static String getPreference(String key) {
        return browserPreferences.getProperty(key);
    }
} 