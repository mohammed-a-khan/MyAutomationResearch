package com.cstestforge.framework.selenium.page;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cstestforge.framework.selenium.annotation.CSFindBy;
import com.cstestforge.framework.selenium.core.CSDriverManager;
import com.cstestforge.framework.selenium.element.CSElementInteractionHandler;

/**
 * Base class for all page objects in the framework.
 * Provides common functionality for page interaction and navigation.
 */
public abstract class CSBasePage {
    protected static final Logger logger = LoggerFactory.getLogger(CSBasePage.class);
    
    // Default wait timeouts
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_POLLING = Duration.ofMillis(500);
    
    // WebDriverWait instance for explicit waits
    protected final WebDriverWait wait;
    
    // Element interaction handler
    protected final CSElementInteractionHandler interactionHandler;
    
    // Page URL
    private String pageUrl;
    
    /**
     * Base constructor that initializes the page
     */
    public CSBasePage() {
        WebDriver driver = CSDriverManager.getDriver();
        this.wait = new WebDriverWait(driver, DEFAULT_TIMEOUT, DEFAULT_POLLING);
        this.interactionHandler = new CSElementInteractionHandler();
        
        // Initialize @FindBy and @CSFindBy annotations
        PageFactory.initElements(driver, this);
    }
    
    /**
     * Sets the page URL
     * 
     * @param pageUrl URL to set
     */
    protected void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }
    
    /**
     * Gets the page URL
     * 
     * @return Page URL
     */
    public String getPageUrl() {
        return pageUrl;
    }
    
    /**
     * Navigate to the page URL
     */
    public void navigateToPage() {
        if (pageUrl == null || pageUrl.isEmpty()) {
            throw new IllegalStateException("Page URL is not set. Call setPageUrl() first.");
        }
        
        logger.info("Navigating to page: {}", pageUrl);
        CSDriverManager.getDriver().get(pageUrl);
        waitForPageLoad();
    }
    
    /**
     * Wait for page to fully load
     */
    public void waitForPageLoad() {
        wait.until(driver -> ((JavascriptExecutor) driver).executeScript("return document.readyState").equals("complete"));
    }
    
    /**
     * Check if page is currently displayed
     * 
     * @return true if page is displayed
     */
    public boolean isPageDisplayed() {
        if (pageUrl == null || pageUrl.isEmpty()) {
            return false;
        }
        
        // Simple URL check (can be overridden for more complex checks)
        String currentUrl = CSDriverManager.getDriver().getCurrentUrl();
        boolean isCurrentPage = currentUrl.contains(pageUrl) || pageUrl.contains(currentUrl);
        
        if (!isCurrentPage) {
            logger.debug("Page not displayed. Current URL: {}, Expected URL: {}", currentUrl, pageUrl);
        }
        
        return isCurrentPage;
    }
    
    /**
     * Check if element is displayed
     * 
     * @param element Element to check
     * @return true if element is displayed
     */
    public boolean isElementDisplayed(WebElement element) {
        if (element == null) {
            return false;
        }
        
        try {
            return element.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        } catch (Exception e) {
            logger.warn("Error checking if element is displayed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Wait for element to be visible
     * 
     * @param element Element to wait for
     * @return The element if found, null otherwise
     */
    public WebElement waitForElementVisible(WebElement element) {
        try {
            return wait.until(ExpectedConditions.visibilityOf(element));
        } catch (TimeoutException e) {
            logger.warn("Timeout waiting for element to be visible");
            return null;
        }
    }
    
    /**
     * Wait for element to be clickable
     * 
     * @param element Element to wait for
     * @return The element if clickable, null otherwise
     */
    public WebElement waitForElementClickable(WebElement element) {
        try {
            return wait.until(ExpectedConditions.elementToBeClickable(element));
        } catch (TimeoutException e) {
            logger.warn("Timeout waiting for element to be clickable");
            return null;
        }
    }
    
    /**
     * Find element by locator
     * 
     * @param by Locator to use
     * @return WebElement or null if not found
     */
    protected WebElement findElement(By by) {
        try {
            return CSDriverManager.getDriver().findElement(by);
        } catch (NoSuchElementException e) {
            logger.warn("Element not found: {}", by);
            return null;
        }
    }
    
    /**
     * Find elements by locator
     * 
     * @param by Locator to use
     * @return List of WebElements or empty list if none found
     */
    protected List<WebElement> findElements(By by) {
        return CSDriverManager.getDriver().findElements(by);
    }
    
    /**
     * Get page title
     * 
     * @return Page title
     */
    public String getPageTitle() {
        return CSDriverManager.getDriver().getTitle();
    }
    
    /**
     * Execute JavaScript on the page
     * 
     * @param script JavaScript to execute
     * @param args Arguments to pass to script
     * @return Result of the script execution
     */
    protected Object executeJavaScript(String script, Object... args) {
        return ((JavascriptExecutor) CSDriverManager.getDriver()).executeScript(script, args);
    }
    
    /**
     * Scroll element into view
     * 
     * @param element Element to scroll to
     */
    protected void scrollIntoView(WebElement element) {
        executeJavaScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
    }
    
    /**
     * Get current URL
     * 
     * @return Current URL
     */
    public String getCurrentUrl() {
        return CSDriverManager.getDriver().getCurrentUrl();
    }
} 