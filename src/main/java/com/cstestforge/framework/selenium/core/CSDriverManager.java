package com.cstestforge.framework.selenium.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 * Manages WebDriver instances for the framework.
 * Handles driver initialization, configuration, and cleanup.
 * Implements thread-safe singleton pattern to ensure single driver instance per thread.
 */
public class CSDriverManager {
    private static final Logger logger = LoggerFactory.getLogger(CSDriverManager.class);
    
    // Default configuration values
    private static final String DEFAULT_BROWSER = "chrome";
    private static final boolean DEFAULT_HEADLESS = false;
    private static final int DEFAULT_IMPLICIT_WAIT = 10;
    private static final int DEFAULT_PAGE_LOAD_TIMEOUT = 30;
    private static final int DEFAULT_SCRIPT_TIMEOUT = 30;
    private static final int DEFAULT_WINDOW_WIDTH = 1920;
    private static final int DEFAULT_WINDOW_HEIGHT = 1080;
    private static final boolean DEFAULT_INCOGNITO = true;
    private static final boolean DEFAULT_ACCEPT_INSECURE_CERTS = true;
    
    // Thread-local storage for WebDriver instances
    private static final ThreadLocal<WebDriver> driverThreadLocal = ThreadLocal.withInitial(() -> null);
    
    // Configuration properties
    private static final Properties config = new Properties();
    
    // Track driver instances for cleanup
    private static final Map<String, WebDriver> activeDrivers = new ConcurrentHashMap<>();
    
    // Lock for thread-safe initialization
    private static final ReentrantLock driverInitLock = new ReentrantLock();
    
    // Static initialization block to load configuration
    static {
        loadConfiguration();
        
        // Add shutdown hook to clean up drivers
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down all WebDriver instances");
            quitAllDrivers();
        }));
    }
    
    /**
     * Private constructor to prevent instantiation
     */
    private CSDriverManager() {
        // Utility class should not be instantiated
    }
    
    /**
     * Load configuration from properties file
     */
    private static void loadConfiguration() {
        String configPath = System.getProperty("cstestforge.config", "src/test/resources/cstestforge.properties");
        File configFile = new File(configPath);
        
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                config.load(fis);
                logger.info("Loaded configuration from: {}", configPath);
            } catch (IOException e) {
                logger.warn("Failed to load configuration from: {}. Using defaults.", configPath, e);
            }
        } else {
            logger.info("Configuration file not found at: {}. Using defaults.", configPath);
        }
    }
    
    /**
     * Get the current WebDriver instance from ThreadLocal or initialize one if not exists.
     * This method ensures thread-safety for driver initialization.
     * 
     * @return WebDriver instance
     */
    public static WebDriver getDriver() {
        WebDriver driver = driverThreadLocal.get();
        
        if (driver == null) {
            driverInitLock.lock();
            try {
                // Double-check after acquiring lock
                driver = driverThreadLocal.get();
                if (driver == null) {
                    driver = initializeDriver();
                }
            } finally {
                driverInitLock.unlock();
            }
        }
        
        return driver;
    }
    
    /**
     * Initialize a new WebDriver instance with specified browser
     * 
     * @param browserName Browser name to initialize
     * @return WebDriver instance
     */
    public static WebDriver initializeDriver(String browserName) {
        System.setProperty("browser", browserName);
        return initializeDriver();
    }
    
    /**
     * Initialize a new WebDriver instance with specified browser and headless mode
     * 
     * @param browserName Browser name to initialize
     * @param headless Whether to run in headless mode
     * @return WebDriver instance
     */
    public static WebDriver initializeDriver(String browserName, boolean headless) {
        System.setProperty("browser", browserName);
        System.setProperty("headless", String.valueOf(headless));
        return initializeDriver();
    }
    
    /**
     * Initialize a new WebDriver instance
     * 
     * @return WebDriver instance
     */
    public static WebDriver initializeDriver() {
        String browser = config.getProperty("browser", System.getProperty("browser", DEFAULT_BROWSER)).toLowerCase();
        boolean headless = Boolean.parseBoolean(config.getProperty("headless", System.getProperty("headless", String.valueOf(DEFAULT_HEADLESS))));
        boolean incognito = Boolean.parseBoolean(config.getProperty("incognito", String.valueOf(DEFAULT_INCOGNITO)));
        boolean acceptInsecureCerts = Boolean.parseBoolean(config.getProperty("acceptInsecureCerts", String.valueOf(DEFAULT_ACCEPT_INSECURE_CERTS)));
        int implicitWait = Integer.parseInt(config.getProperty("implicitWait", String.valueOf(DEFAULT_IMPLICIT_WAIT)));
        int pageLoadTimeout = Integer.parseInt(config.getProperty("pageLoadTimeout", String.valueOf(DEFAULT_PAGE_LOAD_TIMEOUT)));
        int scriptTimeout = Integer.parseInt(config.getProperty("scriptTimeout", String.valueOf(DEFAULT_SCRIPT_TIMEOUT)));
        int windowWidth = Integer.parseInt(config.getProperty("windowWidth", String.valueOf(DEFAULT_WINDOW_WIDTH)));
        int windowHeight = Integer.parseInt(config.getProperty("windowHeight", String.valueOf(DEFAULT_WINDOW_HEIGHT)));
        
        // Check for remote execution
        boolean isRemote = Boolean.parseBoolean(config.getProperty("remote.enabled", "false"));
        String remoteUrl = config.getProperty("remote.url", "http://localhost:4444/wd/hub");
        
        WebDriver driver;
        
        logger.info("Initializing WebDriver: browser={}, headless={}, remote={}", browser, headless, isRemote);
        
        try {
            if (isRemote) {
                driver = initializeRemoteDriver(browser, headless, incognito, acceptInsecureCerts, remoteUrl);
            } else {
                switch (browser) {
                    case "chrome":
                        driver = initializeChromeDriver(headless, incognito, acceptInsecureCerts);
                        break;
                    case "firefox":
                        driver = initializeFirefoxDriver(headless, incognito, acceptInsecureCerts);
                        break;
                    case "edge":
                        driver = initializeEdgeDriver(headless, incognito, acceptInsecureCerts);
                        break;
                    case "safari":
                        driver = initializeSafariDriver(acceptInsecureCerts);
                        break;
                    default:
                        logger.warn("Unsupported browser: {}. Defaulting to Chrome.", browser);
                        driver = initializeChromeDriver(headless, incognito, acceptInsecureCerts);
                }
            }
            
            // Configure timeouts
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(pageLoadTimeout));
            driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(scriptTimeout));
            
            // Set window size
            driver.manage().window().setSize(new Dimension(windowWidth, windowHeight));
            
            // Store driver in ThreadLocal
            driverThreadLocal.set(driver);
            
            // Track for cleanup
            String threadId = Thread.currentThread().getId() + "-" + System.currentTimeMillis();
            activeDrivers.put(threadId, driver);
            
            logger.info("Successfully initialized {} driver", browser);
            return driver;
            
        } catch (Exception e) {
            logger.error("Failed to initialize WebDriver for browser: {}", browser, e);
            throw new RuntimeException("Failed to initialize WebDriver: " + e.getMessage(), e);
        }
    }
    
    /**
     * Initialize a Remote WebDriver
     * 
     * @param browser Browser type
     * @param headless Whether to run in headless mode
     * @param incognito Whether to run in incognito/private mode
     * @param acceptInsecureCerts Whether to accept insecure certificates
     * @param remoteUrl Remote WebDriver URL
     * @return RemoteWebDriver instance
     */
    private static WebDriver initializeRemoteDriver(String browser, boolean headless, boolean incognito, 
                                                   boolean acceptInsecureCerts, String remoteUrl) {
        MutableCapabilities capabilities;
        
        switch (browser) {
            case "chrome":
                capabilities = getChromeOptions(headless, incognito, acceptInsecureCerts);
                break;
            case "firefox":
                capabilities = getFirefoxOptions(headless, incognito, acceptInsecureCerts);
                break;
            case "edge":
                capabilities = getEdgeOptions(headless, incognito, acceptInsecureCerts);
                break;
            case "safari":
                capabilities = getSafariOptions(acceptInsecureCerts);
                break;
            default:
                logger.warn("Unsupported remote browser: {}. Defaulting to Chrome.", browser);
                capabilities = getChromeOptions(headless, incognito, acceptInsecureCerts);
        }
        
        try {
            return new RemoteWebDriver(new URL(remoteUrl), capabilities);
        } catch (Exception e) {
            logger.error("Failed to initialize Remote WebDriver", e);
            throw new RuntimeException("Failed to initialize Remote WebDriver: " + e.getMessage(), e);
        }
    }
    
    /**
     * Initialize Chrome WebDriver with WebDriverManager
     * 
     * @param headless Whether to run in headless mode
     * @param incognito Whether to run in incognito mode
     * @param acceptInsecureCerts Whether to accept insecure certificates
     * @return ChromeDriver instance
     */
    private static WebDriver initializeChromeDriver(boolean headless, boolean incognito, boolean acceptInsecureCerts) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = getChromeOptions(headless, incognito, acceptInsecureCerts);
        return new ChromeDriver(options);
    }
    
    /**
     * Get Chrome options
     * 
     * @param headless Whether to run in headless mode
     * @param incognito Whether to run in incognito mode
     * @param acceptInsecureCerts Whether to accept insecure certificates
     * @return ChromeOptions instance
     */
    private static ChromeOptions getChromeOptions(boolean headless, boolean incognito, boolean acceptInsecureCerts) {
        ChromeOptions options = new ChromeOptions();
        
        options.setAcceptInsecureCerts(acceptInsecureCerts);
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        options.setUnhandledPromptBehaviour(UnexpectedAlertBehaviour.ACCEPT);
        
        if (headless) {
            options.addArguments("--headless=new");
        }
        
        if (incognito) {
            options.addArguments("--incognito");
        }
        
        // Common arguments for stability
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--remote-allow-origins=*");
        
        // Configure logging
        options.addArguments("--log-level=3");
        options.addArguments("--silent");
        
        // Add additional Chrome options from configuration
        String additionalOptions = config.getProperty("chrome.options", "");
        if (!additionalOptions.isEmpty()) {
            for (String option : additionalOptions.split(",")) {
                options.addArguments(option.trim());
            }
        }
        
        // Set download directory if specified
        String downloadDir = config.getProperty("chrome.downloadDir", "");
        if (!downloadDir.isEmpty()) {
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("download.default_directory", getAbsolutePath(downloadDir));
            prefs.put("download.prompt_for_download", false);
            prefs.put("download.directory_upgrade", true);
            prefs.put("safebrowsing.enabled", true);
            options.setExperimentalOption("prefs", prefs);
        }
        
        return options;
    }
    
    /**
     * Initialize Firefox WebDriver with WebDriverManager
     * 
     * @param headless Whether to run in headless mode
     * @param incognito Whether to run in private mode
     * @param acceptInsecureCerts Whether to accept insecure certificates
     * @return FirefoxDriver instance
     */
    private static WebDriver initializeFirefoxDriver(boolean headless, boolean incognito, boolean acceptInsecureCerts) {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = getFirefoxOptions(headless, incognito, acceptInsecureCerts);
        return new FirefoxDriver(options);
    }
    
    /**
     * Get Firefox options
     * 
     * @param headless Whether to run in headless mode
     * @param incognito Whether to run in private mode
     * @param acceptInsecureCerts Whether to accept insecure certificates
     * @return FirefoxOptions instance
     */
    private static FirefoxOptions getFirefoxOptions(boolean headless, boolean incognito, boolean acceptInsecureCerts) {
        FirefoxOptions options = new FirefoxOptions();
        FirefoxProfile profile = new FirefoxProfile();
        
        options.setAcceptInsecureCerts(acceptInsecureCerts);
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        options.setUnhandledPromptBehaviour(UnexpectedAlertBehaviour.ACCEPT);
        
        if (headless) {
            options.addArguments("-headless");
        }
        
        if (incognito) {
            options.addArguments("-private");
        }
        
        // Set download behavior
        String downloadDir = config.getProperty("firefox.downloadDir", "");
        if (!downloadDir.isEmpty()) {
            profile.setPreference("browser.download.folderList", 2);
            profile.setPreference("browser.download.dir", getAbsolutePath(downloadDir));
            profile.setPreference("browser.helperApps.neverAsk.saveToDisk", 
                    "application/pdf,application/x-pdf,application/octet-stream,application/zip,application/x-zip,application/x-zip-compressed");
            profile.setPreference("browser.download.manager.showWhenStarting", false);
            profile.setPreference("pdfjs.disabled", true);
        }
        
        // Set performance preferences
        profile.setPreference("network.http.pipelining", true);
        profile.setPreference("network.http.proxy.pipelining", true);
        profile.setPreference("network.http.pipelining.maxrequests", 8);
        profile.setPreference("browser.cache.disk.enable", true);
        profile.setPreference("browser.cache.memory.enable", true);
        profile.setPreference("permissions.default.image", 2);
        
        options.setProfile(profile);
        options.addArguments("--width=1920");
        options.addArguments("--height=1080");
        
        // Add additional Firefox options from configuration
        String additionalOptions = config.getProperty("firefox.options", "");
        if (!additionalOptions.isEmpty()) {
            for (String option : additionalOptions.split(",")) {
                options.addArguments(option.trim());
            }
        }
        
        return options;
    }
    
    /**
     * Initialize Edge WebDriver with WebDriverManager
     * 
     * @param headless Whether to run in headless mode
     * @param incognito Whether to run in InPrivate mode
     * @param acceptInsecureCerts Whether to accept insecure certificates
     * @return EdgeDriver instance
     */
    private static WebDriver initializeEdgeDriver(boolean headless, boolean incognito, boolean acceptInsecureCerts) {
        WebDriverManager.edgedriver().setup();
        EdgeOptions options = getEdgeOptions(headless, incognito, acceptInsecureCerts);
        return new EdgeDriver(options);
    }
    
    /**
     * Get Edge options
     * 
     * @param headless Whether to run in headless mode
     * @param incognito Whether to run in InPrivate mode
     * @param acceptInsecureCerts Whether to accept insecure certificates
     * @return EdgeOptions instance
     */
    private static EdgeOptions getEdgeOptions(boolean headless, boolean incognito, boolean acceptInsecureCerts) {
        EdgeOptions options = new EdgeOptions();
        
        options.setAcceptInsecureCerts(acceptInsecureCerts);
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        options.setUnhandledPromptBehaviour(UnexpectedAlertBehaviour.ACCEPT);
        
        if (headless) {
            options.addArguments("--headless=new");
        }
        
        if (incognito) {
            options.addArguments("--inprivate");
        }
        
        // Common arguments
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--window-size=1920,1080");
        
        // Set download directory if specified
        String downloadDir = config.getProperty("edge.downloadDir", "");
        if (!downloadDir.isEmpty()) {
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("download.default_directory", getAbsolutePath(downloadDir));
            prefs.put("download.prompt_for_download", false);
            options.setExperimentalOption("prefs", prefs);
        }
        
        // Add additional Edge options from configuration
        String additionalOptions = config.getProperty("edge.options", "");
        if (!additionalOptions.isEmpty()) {
            for (String option : additionalOptions.split(",")) {
                options.addArguments(option.trim());
            }
        }
        
        return options;
    }
    
    /**
     * Initialize Safari WebDriver
     * 
     * @param acceptInsecureCerts Whether to accept insecure certificates
     * @return SafariDriver instance
     */
    private static WebDriver initializeSafariDriver(boolean acceptInsecureCerts) {
        SafariOptions options = getSafariOptions(acceptInsecureCerts);
        return new SafariDriver(options);
    }
    
    /**
     * Get Safari options
     * 
     * @param acceptInsecureCerts Whether to accept insecure certificates
     * @return SafariOptions instance
     */
    private static SafariOptions getSafariOptions(boolean acceptInsecureCerts) {
        SafariOptions options = new SafariOptions();
        
        // Safari doesn't support headless mode
        // The Safari driver doesn't support most of the capabilities
        options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, acceptInsecureCerts);
        options.setCapability("safari:automaticInspection", false);
        options.setCapability("safari:automaticProfiling", false);
        
        return options;
    }
    
    /**
     * Quit the current WebDriver instance
     */
    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                driver.quit();
                logger.debug("WebDriver instance quit successfully");
            } catch (Exception e) {
                logger.warn("Error quitting WebDriver instance", e);
            } finally {
                driverThreadLocal.remove();
                // Remove from active drivers
                activeDrivers.entrySet().removeIf(entry -> entry.getValue() == driver);
            }
        }
    }
    
    /**
     * Quit all active WebDriver instances
     */
    public static void quitAllDrivers() {
        for (Map.Entry<String, WebDriver> entry : activeDrivers.entrySet()) {
            try {
                entry.getValue().quit();
                logger.debug("WebDriver instance {} quit successfully", entry.getKey());
            } catch (Exception e) {
                logger.warn("Error quitting WebDriver instance {}", entry.getKey(), e);
            }
        }
        activeDrivers.clear();
        driverThreadLocal.remove();
    }
    
    /**
     * Take a screenshot with the current WebDriver if it supports TakesScreenshot interface
     * 
     * @param name Name to identify the screenshot
     * @return Path to the screenshot file or null if failed
     */
    public static String takeScreenshot(String name) {
        WebDriver driver = getDriver();
        if (driver != null) {
            try {
                return CSScreenshotHandler.takeScreenshot(driver, name);
            } catch (Exception e) {
                logger.warn("Failed to take screenshot: {}", name, e);
            }
        }
        return null;
    }
    
    /**
     * Get the title of the current page
     * 
     * @return Page title or empty string if not available
     */
    public static String getCurrentTitle() {
        WebDriver driver = getDriver();
        if (driver != null) {
            try {
                return driver.getTitle();
            } catch (Exception e) {
                logger.warn("Failed to get current title", e);
            }
        }
        return "";
    }
    
    /**
     * Get the URL of the current page
     * 
     * @return Current URL or empty string if not available
     */
    public static String getCurrentUrl() {
        WebDriver driver = getDriver();
        if (driver != null) {
            try {
                return driver.getCurrentUrl();
            } catch (Exception e) {
                logger.warn("Failed to get current URL", e);
            }
        }
        return "";
    }
    
    /**
     * Convert a relative path to absolute path
     * 
     * @param relativePath Relative path
     * @return Absolute path
     */
    private static String getAbsolutePath(String relativePath) {
        Path path = Paths.get(relativePath);
        if (path.isAbsolute()) {
            return path.toString();
        } else {
            return Paths.get(System.getProperty("user.dir"), relativePath).toString();
        }
    }
}