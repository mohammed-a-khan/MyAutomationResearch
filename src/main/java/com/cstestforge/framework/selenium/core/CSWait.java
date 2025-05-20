package com.cstestforge.framework.selenium.core;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/**
 * Advanced wait utilities for Selenium WebDriver.
 * Provides custom wait conditions and convenience methods.
 */
public class CSWait {
    private static final Logger logger = LoggerFactory.getLogger(CSWait.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_POLLING_INTERVAL_MS = 500;
    
    private final WebDriver driver;
    private final int timeoutSeconds;
    private final int pollingIntervalMs;
    
    /**
     * Constructor with default timeout and polling interval.
     * 
     * @param driver WebDriver instance
     */
    public CSWait(WebDriver driver) {
        this(driver, DEFAULT_TIMEOUT_SECONDS, DEFAULT_POLLING_INTERVAL_MS);
    }
    
    /**
     * Constructor with custom timeout.
     * 
     * @param driver WebDriver instance
     * @param timeoutSeconds Timeout in seconds
     */
    public CSWait(WebDriver driver, int timeoutSeconds) {
        this(driver, timeoutSeconds, DEFAULT_POLLING_INTERVAL_MS);
    }
    
    /**
     * Constructor with custom timeout and polling interval.
     * 
     * @param driver WebDriver instance
     * @param timeoutSeconds Timeout in seconds
     * @param pollingIntervalMs Polling interval in milliseconds
     */
    public CSWait(WebDriver driver, int timeoutSeconds, int pollingIntervalMs) {
        this.driver = driver;
        this.timeoutSeconds = timeoutSeconds;
        this.pollingIntervalMs = pollingIntervalMs;
    }
    
    /**
     * Creates a new WebDriverWait instance with the configured timeout.
     * 
     * @return WebDriverWait instance
     */
    public WebDriverWait createWebDriverWait() {
        return new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
    }
    
    /**
     * Creates a new FluentWait instance with the configured timeout and polling interval.
     * 
     * @return FluentWait instance
     */
    public FluentWait<WebDriver> createFluentWait() {
        return new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeoutSeconds))
                .pollingEvery(Duration.ofMillis(pollingIntervalMs))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);
    }
    
    /**
     * Waits for the page to load completely.
     */
    public void forPageLoad() {
        try {
            logger.debug("Waiting for page to load");
            
            // First wait for the DOM to be ready
            createWebDriverWait().until(
                    webDriver -> ((JavascriptExecutor) webDriver)
                            .executeScript("return document.readyState").equals("complete"));
            
            // Then wait for jQuery (if present)
            waitForJQuery();
            
            // Then wait for Angular (if present)
            waitForAngular();
            
            logger.debug("Page loaded successfully");
        } catch (TimeoutException e) {
            logger.warn("Timeout waiting for page load", e);
            throw e;
        }
    }
    
    /**
     * Waits for jQuery to be loaded and idle.
     */
    public void waitForJQuery() {
        try {
            createWebDriverWait().until(driver -> {
                Boolean jQueryDefined = (Boolean) ((JavascriptExecutor) driver)
                        .executeScript("return typeof jQuery != 'undefined'");
                
                if (Boolean.FALSE.equals(jQueryDefined)) {
                    return true; // jQuery not used on page
                }
                
                Boolean jQueryActive = (Boolean) ((JavascriptExecutor) driver)
                        .executeScript("return jQuery.active == 0");
                
                return Boolean.TRUE.equals(jQueryActive);
            });
        } catch (Exception e) {
            logger.debug("Error checking jQuery status: {}", e.getMessage());
            // Ignore jQuery errors
        }
    }
    
    /**
     * Waits for Angular to be loaded and idle.
     */
    public void waitForAngular() {
        try {
            createWebDriverWait().until(driver -> {
                Boolean angularDefined = (Boolean) ((JavascriptExecutor) driver)
                        .executeScript("return typeof angular !== 'undefined'");
                
                if (Boolean.FALSE.equals(angularDefined)) {
                    return true; // Angular not used on page
                }
                
                return (Boolean) ((JavascriptExecutor) driver).executeScript(
                        "return angular.element(document).injector().get('$http').pendingRequests.length == 0");
            });
        } catch (Exception e) {
            logger.debug("Error checking Angular status: {}", e.getMessage());
            // Ignore Angular errors
        }
    }
    
    /**
     * Waits for an element to be present in the DOM.
     * 
     * @param locator Element locator
     * @return Located WebElement
     */
    public WebElement forElementPresent(By locator) {
        logger.debug("Waiting for element present: {}", locator);
        return createWebDriverWait().until(ExpectedConditions.presenceOfElementLocated(locator));
    }
    
    /**
     * Waits for an element to be visible.
     * 
     * @param locator Element locator
     * @return Located WebElement
     */
    public WebElement forElementVisible(By locator) {
        logger.debug("Waiting for element visible: {}", locator);
        return createWebDriverWait().until(ExpectedConditions.visibilityOfElementLocated(locator));
    }
    
    /**
     * Waits for an element to be clickable.
     * 
     * @param locator Element locator
     * @return Located WebElement
     */
    public WebElement forElementClickable(By locator) {
        logger.debug("Waiting for element clickable: {}", locator);
        return createWebDriverWait().until(ExpectedConditions.elementToBeClickable(locator));
    }
    
    /**
     * Waits for an element to be clickable.
     * 
     * @param element WebElement
     * @return Same WebElement once clickable
     */
    public WebElement forElementClickable(WebElement element) {
        logger.debug("Waiting for element clickable");
        return createWebDriverWait().until(ExpectedConditions.elementToBeClickable(element));
    }
    
    /**
     * Waits for an element to have a specific text.
     * 
     * @param locator Element locator
     * @param text Expected text
     * @return Located WebElement
     */
    public WebElement forElementWithText(By locator, String text) {
        logger.debug("Waiting for element with text '{}': {}", text, locator);
        return createWebDriverWait().until(driver -> {
            WebElement element = driver.findElement(locator);
            if (element.getText().contains(text)) {
                return element;
            }
            return null;
        });
    }
    
    /**
     * Waits for an element to be invisible.
     * 
     * @param locator Element locator
     * @return true if element is invisible
     */
    public boolean forElementInvisible(By locator) {
        logger.debug("Waiting for element invisible: {}", locator);
        return createWebDriverWait().until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }
    
    /**
     * Waits for multiple elements to be present.
     * 
     * @param locator Elements locator
     * @return List of located WebElements
     */
    public List<WebElement> forElementsPresent(By locator) {
        logger.debug("Waiting for elements present: {}", locator);
        return createWebDriverWait().until(ExpectedConditions.presenceOfAllElementsLocatedBy(locator));
    }
    
    /**
     * Waits for multiple elements to be visible.
     * 
     * @param locator Elements locator
     * @return List of located WebElements
     */
    public List<WebElement> forElementsVisible(By locator) {
        logger.debug("Waiting for elements visible: {}", locator);
        return createWebDriverWait().until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }
    
    /**
     * Waits for text to be present in an element.
     * 
     * @param locator Element locator
     * @param text Text to wait for
     * @return true if text is present
     */
    public boolean forTextPresent(By locator, String text) {
        logger.debug("Waiting for text '{}' in element: {}", text, locator);
        return createWebDriverWait().until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }
    
    /**
     * Waits for a specific attribute to have a specific value.
     * 
     * @param locator Element locator
     * @param attribute Attribute name
     * @param value Expected attribute value
     * @return true if attribute has expected value
     */
    public boolean forAttributeValue(By locator, String attribute, String value) {
        logger.debug("Waiting for attribute '{}' to be '{}' in element: {}", attribute, value, locator);
        return createWebDriverWait().until(ExpectedConditions.attributeToBe(locator, attribute, value));
    }
    
    /**
     * Waits for a URL to contain a specific text.
     * 
     * @param urlFragment URL fragment to wait for
     * @return true if URL contains the fragment
     */
    public boolean forUrlContains(String urlFragment) {
        logger.debug("Waiting for URL to contain: {}", urlFragment);
        return createWebDriverWait().until(ExpectedConditions.urlContains(urlFragment));
    }
    
    /**
     * Waits for an alert to be present.
     * 
     * @return true if alert is present
     */
    public boolean forAlertPresent() {
        logger.debug("Waiting for alert to be present");
        try {
            createWebDriverWait().until(ExpectedConditions.alertIsPresent());
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }
    
    /**
     * Waits for a custom condition.
     * 
     * @param <T> Return type
     * @param condition Condition to wait for
     * @return Result from condition
     */
    public <T> T until(Function<WebDriver, T> condition) {
        logger.debug("Waiting for custom condition");
        return createFluentWait().until(condition);
    }
    
    /**
     * Waits for all AJAX requests to complete.
     */
    public void forAjaxComplete() {
        logger.debug("Waiting for all AJAX requests to complete");
        try {
            createWebDriverWait().until((ExpectedCondition<Boolean>) driver -> {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                
                // Check jQuery active requests
                boolean jQueryDefined = (Boolean) js.executeScript("return typeof jQuery != 'undefined'");
                boolean ajaxComplete = false;
                
                if (jQueryDefined) {
                    ajaxComplete = (Boolean) js.executeScript("return jQuery.active == 0");
                } else {
                    ajaxComplete = true;
                }
                
                // Check Fetch API requests
                boolean fetchComplete = (Boolean) js.executeScript(
                        "return window._activeFetchCount === undefined || window._activeFetchCount === 0");
                
                // Check XHR requests
                boolean xhrComplete = (Boolean) js.executeScript(
                        "return window._activeXhrCount === undefined || window._activeXhrCount === 0");
                
                return ajaxComplete && fetchComplete && xhrComplete;
            });
        } catch (Exception e) {
            logger.warn("Error waiting for AJAX completion: {}", e.getMessage());
        }
    }
    
    /**
     * Gets the configured timeout in seconds.
     * 
     * @return Timeout in seconds
     */
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }
    
    /**
     * Gets the configured polling interval in milliseconds.
     * 
     * @return Polling interval in milliseconds
     */
    public int getPollingIntervalMs() {
        return pollingIntervalMs;
    }
} 