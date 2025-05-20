package com.cstestforge.framework.selenium.element;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cstestforge.framework.selenium.core.CSDriverManager;
import com.cstestforge.framework.selenium.core.CSWait;

/**
 * Enhanced WebElement that provides additional functionality and reliability.
 * Adds smart retry, logging, and self-healing capabilities to standard WebElement operations.
 */
public class CSBaseElement {
    private static final Logger logger = LoggerFactory.getLogger(CSBaseElement.class);
    
    private WebElement element;
    private By locator;
    private String name;
    private CSElementInteractionHandler interactionHandler;
    private CSWait wait;
    
    /**
     * Create a new element with WebElement
     * 
     * @param element WebElement
     * @param name Element name for reporting
     */
    public CSBaseElement(WebElement element, String name) {
        this.element = element;
        this.name = name;
        this.interactionHandler = new CSElementInteractionHandler();
        this.wait = new CSWait(CSDriverManager.getDriver());
        logger.debug("Created element: {}", name);
    }
    
    /**
     * Create a new element with a locator
     * 
     * @param locator By locator
     * @param name Element name for reporting
     */
    public CSBaseElement(By locator, String name) {
        this.locator = locator;
        this.name = name;
        this.interactionHandler = new CSElementInteractionHandler();
        this.wait = new CSWait(CSDriverManager.getDriver());
        logger.debug("Created element with locator: {}", name);
    }
    
    /**
     * Get the wrapped WebElement.
     * If element is null and locator is provided, finds the element first.
     * 
     * @return WebElement
     */
    public WebElement getElement() {
        if (element == null && locator != null) {
            element = CSDriverManager.getDriver().findElement(locator);
        }
        return element;
    }
    
    /**
     * Click on the element with smart retry
     */
    public void click() {
        logger.debug("Clicking on element: {}", name);
        interactionHandler.click(getElement());
    }
    
    /**
     * Send keys to the element with smart retry
     * 
     * @param text Text to type
     */
    public void sendKeys(String text) {
        logger.debug("Typing '{}' into element: {}", text, name);
        interactionHandler.sendKeys(getElement(), new CharSequence[]{text}, true);
    }
    
    /**
     * Send keys to the element without clearing it first
     * 
     * @param text Text to type
     */
    public void appendText(String text) {
        logger.debug("Appending '{}' to element: {}", text, name);
        interactionHandler.sendKeys(getElement(), new CharSequence[]{text}, false);
    }
    
    /**
     * Clear the element
     */
    public void clear() {
        logger.debug("Clearing element: {}", name);
        interactionHandler.clearElement(getElement());
    }
    
    /**
     * Check if element is displayed
     * 
     * @return true if element is displayed
     */
    public boolean isDisplayed() {
        try {
            return getElement().isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if element is enabled
     * 
     * @return true if element is enabled
     */
    public boolean isEnabled() {
        try {
            return getElement().isEnabled();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if element is selected
     * 
     * @return true if element is selected
     */
    public boolean isSelected() {
        try {
            return getElement().isSelected();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get element text with smart handling
     * 
     * @return Element text
     */
    public String getText() {
        logger.debug("Getting text from element: {}", name);
        return interactionHandler.getText(getElement());
    }
    
    /**
     * Get attribute value
     * 
     * @param attributeName Attribute name
     * @return Attribute value
     */
    public String getAttribute(String attributeName) {
        logger.debug("Getting attribute '{}' from element: {}", attributeName, name);
        return getElement().getAttribute(attributeName);
    }
    
    /**
     * Select dropdown option by visible text
     * 
     * @param visibleText Text to select
     */
    public void selectByVisibleText(String visibleText) {
        logger.debug("Selecting option '{}' in dropdown: {}", visibleText, name);
        interactionHandler.selectByVisibleText(getElement(), visibleText);
    }
    
    /**
     * Select dropdown option by value
     * 
     * @param value Value to select
     */
    public void selectByValue(String value) {
        logger.debug("Selecting value '{}' in dropdown: {}", value, name);
        interactionHandler.selectByValue(getElement(), value);
    }
    
    /**
     * Hover over the element
     */
    public void hover() {
        logger.debug("Hovering over element: {}", name);
        interactionHandler.hover(getElement());
    }
    
    /**
     * Wait for element to be clickable
     * 
     * @param timeoutSeconds Timeout in seconds
     * @return this element
     */
    public CSBaseElement waitForClickable(int timeoutSeconds) {
        try {
            WebElement clickableElement = wait.forElementClickable(getElement());
            if (clickableElement != null) {
                this.element = clickableElement;
            }
        } catch (Exception e) {
            logger.warn("Timeout waiting for element to be clickable: {}", name);
        }
        
        return this;
    }
    
    /**
     * Wait for element to be visible
     * 
     * @param timeoutSeconds Timeout in seconds
     * @return this element
     */
    public CSBaseElement waitForVisible(int timeoutSeconds) {
        try {
            // For visibility we need to use a different approach
            // as waitForVisible expects a By locator
            if (locator != null) {
                WebElement visibleElement = wait.forElementVisible(locator);
                if (visibleElement != null) {
                    this.element = visibleElement;
                }
            } else {
                // Use explicit wait for element visibility
                boolean isVisible = wait.createWebDriverWait().until(
                    driver -> {
                        try {
                            return getElement().isDisplayed();
                        } catch (Exception e) {
                            return false;
                        }
                    }
                );
                if (!isVisible) {
                    logger.warn("Element not visible after timeout: {}", name);
                }
            }
        } catch (Exception e) {
            logger.warn("Timeout waiting for element to be visible: {}", name);
        }
        
        return this;
    }
    
    /**
     * Scroll element into view
     * 
     * @return this element
     */
    public CSBaseElement scrollIntoView() {
        logger.debug("Scrolling element into view: {}", name);
        interactionHandler.scrollToElement(getElement());
        return this;
    }
    
    /**
     * Get the element name
     * 
     * @return Element name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the locator
     * 
     * @return Locator
     */
    public By getLocator() {
        return locator;
    }
} 