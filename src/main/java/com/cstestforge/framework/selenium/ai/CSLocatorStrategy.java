package com.cstestforge.framework.selenium.ai;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Advanced element location strategies with fallback mechanisms.
 * Provides self-healing capabilities for element location.
 */
public class CSLocatorStrategy {
    private static final Logger logger = LoggerFactory.getLogger(CSLocatorStrategy.class);
    
    // Cache for successful recovery strategies
    private static final Map<String, By> recoveryCache = new HashMap<>();
    
    private CSLocatorStrategy() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Finds an element using advanced location strategies.
     * 
     * @param driver WebDriver instance
     * @param originalLocator Original locator that failed
     * @param elementName Element name for logging
     * @return WebElement if found
     * @throws NoSuchElementException if element cannot be found
     */
    public static WebElement findElement(WebDriver driver, By originalLocator, String elementName) {
        logger.debug("Attempting to find element '{}' with alternative strategies", elementName);
        
        // Check cache first
        String cacheKey = generateCacheKey(originalLocator, elementName);
        if (recoveryCache.containsKey(cacheKey)) {
            By cachedLocator = recoveryCache.get(cacheKey);
            try {
                WebElement element = driver.findElement(cachedLocator);
                logger.debug("Found element using cached recovery locator: {}", cachedLocator);
                return element;
            } catch (Exception e) {
                logger.debug("Cached recovery locator failed, trying other strategies");
                recoveryCache.remove(cacheKey);
            }
        }
        
        // Generate alternative locators
        List<By> alternativeLocators = generateAlternativeLocators(originalLocator, elementName);
        
        // Try each alternative locator
        for (By locator : alternativeLocators) {
            try {
                WebElement element = driver.findElement(locator);
                logger.info("Found element '{}' using alternative locator: {}", elementName, locator);
                
                // Cache successful strategy
                recoveryCache.put(cacheKey, locator);
                
                return element;
            } catch (Exception e) {
                // Continue to next strategy
            }
        }
        
        // Try using JavaScript-based locators
        WebElement element = findElementByJavaScript(driver, originalLocator, elementName);
        if (element != null) {
            return element;
        }
        
        // If all strategies fail, throw exception
        throw new NoSuchElementException(
                "Failed to find element '" + elementName + "' using all available strategies");
    }
    
    /**
     * Generates a cache key for a locator.
     * 
     * @param locator Locator
     * @param elementName Element name
     * @return Cache key
     */
    private static String generateCacheKey(By locator, String elementName) {
        return locator.toString() + "|" + elementName;
    }
    
    /**
     * Generates alternative locators based on the original locator.
     * 
     * @param originalLocator Original locator
     * @param elementName Element name
     * @return List of alternative locators
     */
    private static List<By> generateAlternativeLocators(By originalLocator, String elementName) {
        List<By> alternatives = new ArrayList<>();
        String locatorString = originalLocator.toString();
        
        // Extract the locator type and value
        String locatorType = extractLocatorType(locatorString);
        String locatorValue = extractLocatorValue(locatorString);
        
        if (locatorValue == null || locatorValue.isEmpty()) {
            return alternatives;
        }
        
        // Add variations based on the locator type
        switch (locatorType) {
            case "id":
                alternatives.add(By.cssSelector("[id*='" + locatorValue + "']")); // Contains ID
                alternatives.add(By.cssSelector("[data-testid='" + locatorValue + "']"));
                alternatives.add(By.cssSelector("[data-test='" + locatorValue + "']"));
                alternatives.add(By.cssSelector("[data-automation='" + locatorValue + "']"));
                break;
                
            case "name":
                alternatives.add(By.cssSelector("[name*='" + locatorValue + "']")); // Contains name
                alternatives.add(By.xpath("//*[contains(@name,'" + locatorValue + "')]"));
                break;
                
            case "css selector":
                if (locatorValue.startsWith("#")) {
                    // ID selector
                    String idValue = locatorValue.substring(1);
                    alternatives.add(By.id(idValue));
                    alternatives.add(By.cssSelector("[id*='" + idValue + "']"));
                } else if (locatorValue.startsWith(".")) {
                    // Class selector
                    String className = locatorValue.substring(1);
                    alternatives.add(By.className(className));
                }
                break;
                
            case "xpath":
                // For XPath, try to extract text content if it contains text()
                if (locatorValue.contains("text()")) {
                    String textContent = extractTextFromXPath(locatorValue);
                    if (textContent != null && !textContent.isEmpty()) {
                        alternatives.add(By.xpath("//*[contains(text(),'" + textContent + "')]"));
                        alternatives.add(By.linkText(textContent));
                        alternatives.add(By.partialLinkText(textContent));
                    }
                }
                break;
        }
        
        // Add strategies based on element name
        String simplifiedName = simplifyElementName(elementName);
        if (!simplifiedName.isEmpty()) {
            alternatives.add(By.id(simplifiedName));
            alternatives.add(By.name(simplifiedName));
            alternatives.add(By.cssSelector("[id*='" + simplifiedName + "']"));
            alternatives.add(By.cssSelector("[name*='" + simplifiedName + "']"));
            alternatives.add(By.cssSelector("[data-testid*='" + simplifiedName + "']"));
            alternatives.add(By.xpath("//*[contains(@id,'" + simplifiedName + "')]"));
            alternatives.add(By.xpath("//*[contains(@name,'" + simplifiedName + "')]"));
            alternatives.add(By.xpath("//*[contains(text(),'" + simplifiedName + "')]"));
        }
        
        return alternatives;
    }
    
    /**
     * Extracts the locator type from a locator string.
     * 
     * @param locatorString Locator string
     * @return Locator type
     */
    private static String extractLocatorType(String locatorString) {
        if (locatorString.startsWith("By.id")) {
            return "id";
        } else if (locatorString.startsWith("By.name")) {
            return "name";
        } else if (locatorString.startsWith("By.cssSelector")) {
            return "css selector";
        } else if (locatorString.startsWith("By.xpath")) {
            return "xpath";
        } else if (locatorString.startsWith("By.className")) {
            return "class name";
        } else if (locatorString.startsWith("By.linkText")) {
            return "link text";
        } else if (locatorString.startsWith("By.partialLinkText")) {
            return "partial link text";
        } else if (locatorString.startsWith("By.tagName")) {
            return "tag name";
        }
        
        return "";
    }
    
    /**
     * Extracts the locator value from a locator string.
     * 
     * @param locatorString Locator string
     * @return Locator value
     */
    private static String extractLocatorValue(String locatorString) {
        int start = locatorString.indexOf(':') + 1;
        if (start > 0) {
            return locatorString.substring(start).trim();
        }
        return "";
    }
    
    /**
     * Extracts text content from an XPath expression.
     * 
     * @param xpath XPath expression
     * @return Text content
     */
    private static String extractTextFromXPath(String xpath) {
        if (xpath.contains("text()='")) {
            int start = xpath.indexOf("text()='") + 7;
            int end = xpath.indexOf("'", start);
            if (end > start) {
                return xpath.substring(start, end);
            }
        } else if (xpath.contains("text()=\"")) {
            int start = xpath.indexOf("text()=\"") + 7;
            int end = xpath.indexOf("\"", start);
            if (end > start) {
                return xpath.substring(start, end);
            }
        } else if (xpath.contains("contains(text(),'")) {
            int start = xpath.indexOf("contains(text(),'") + 16;
            int end = xpath.indexOf("'", start);
            if (end > start) {
                return xpath.substring(start, end);
            }
        } else if (xpath.contains("contains(text(),\"")) {
            int start = xpath.indexOf("contains(text(),\"") + 16;
            int end = xpath.indexOf("\"", start);
            if (end > start) {
                return xpath.substring(start, end);
            }
        }
        
        return null;
    }
    
    /**
     * Simplifies an element name for use in locators.
     * 
     * @param elementName Element name
     * @return Simplified name
     */
    private static String simplifyElementName(String elementName) {
        if (elementName == null || elementName.isEmpty()) {
            return "";
        }
        
        // Remove common prefixes/suffixes
        String[] prefixes = {"btn", "txt", "lbl", "chk", "img", "select", "input", "dropdown"};
        String name = elementName.toLowerCase();
        
        for (String prefix : prefixes) {
            if (name.startsWith(prefix)) {
                name = name.substring(prefix.length());
            }
        }
        
        // Separate camelCase
        name = name.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase();
        
        // Clean up other characters
        name = name.replaceAll("[^a-zA-Z0-9\\s]", " ").trim();
        name = name.replaceAll("\\s+", " ");
        
        return name;
    }
    
    /**
     * Finds an element using JavaScript DOM traversal and heuristics.
     * 
     * @param driver WebDriver instance
     * @param originalLocator Original locator
     * @param elementName Element name
     * @return WebElement if found, null otherwise
     */
    private static WebElement findElementByJavaScript(WebDriver driver, By originalLocator, String elementName) {
        String simplifiedName = simplifyElementName(elementName);
        
        logger.debug("Attempting JavaScript-based element finding for: {}", elementName);
        
        try {
            // Try to find by simplified name
            if (!simplifiedName.isEmpty()) {
                String jsSimplified = 
                    "return (function() { " +
                    "  var elements = document.querySelectorAll('*'); " +
                    "  for (var i = 0; i < elements.length; i++) { " +
                    "    var el = elements[i]; " +
                    "    if (el.id && el.id.toLowerCase().indexOf('" + simplifiedName + "') !== -1) return el; " +
                    "    if (el.name && el.name.toLowerCase().indexOf('" + simplifiedName + "') !== -1) return el; " +
                    "    if (el.placeholder && el.placeholder.toLowerCase().indexOf('" + simplifiedName + "') !== -1) return el; " +
                    "    if (el.innerText && el.innerText.toLowerCase().indexOf('" + simplifiedName + "') !== -1) return el; " +
                    "    var dataTestId = el.getAttribute('data-testid'); " +
                    "    if (dataTestId && dataTestId.toLowerCase().indexOf('" + simplifiedName + "') !== -1) return el; " +
                    "  } " +
                    "  return null; " +
                    "})();";
                
                Object result = ((JavascriptExecutor) driver).executeScript(jsSimplified);
                if (result instanceof WebElement) {
                    WebElement element = (WebElement) result;
                    logger.info("Found element using JavaScript for: {}", elementName);
                    
                    // Try to determine the By locator for future use
                    By determinedLocator = determineLocatorFromElement(driver, element);
                    if (determinedLocator != null) {
                        recoveryCache.put(generateCacheKey(originalLocator, elementName), determinedLocator);
                    }
                    
                    return element;
                }
            }
            
            // Try using original locator value as a text search
            String locatorValue = extractLocatorValue(originalLocator.toString());
            if (locatorValue != null && !locatorValue.isEmpty()) {
                String jsLocatorValue = 
                    "return (function() { " +
                    "  var elements = document.querySelectorAll('*'); " +
                    "  for (var i = 0; i < elements.length; i++) { " +
                    "    var el = elements[i]; " +
                    "    if (el.innerText && el.innerText.toLowerCase().indexOf('" + locatorValue.toLowerCase() + "') !== -1) return el; " +
                    "    var ariaLabel = el.getAttribute('aria-label'); " +
                    "    if (ariaLabel && ariaLabel.toLowerCase().indexOf('" + locatorValue.toLowerCase() + "') !== -1) return el; " +
                    "  } " +
                    "  return null; " +
                    "})();";
                
                Object result = ((JavascriptExecutor) driver).executeScript(jsLocatorValue);
                if (result instanceof WebElement) {
                    WebElement element = (WebElement) result;
                    logger.info("Found element using JavaScript with locator value for: {}", elementName);
                    
                    By determinedLocator = determineLocatorFromElement(driver, element);
                    if (determinedLocator != null) {
                        recoveryCache.put(generateCacheKey(originalLocator, elementName), determinedLocator);
                    }
                    
                    return element;
                }
            }
        } catch (Exception e) {
            logger.trace("JavaScript-based element finding failed", e);
        }
        
        return null;
    }
    
    /**
     * Determines a By locator from an existing element.
     * 
     * @param driver WebDriver instance
     * @param element WebElement
     * @return By locator if determined, null otherwise
     */
    private static By determineLocatorFromElement(WebDriver driver, WebElement element) {
        try {
            String id = (String) ((JavascriptExecutor) driver).executeScript("return arguments[0].id;", element);
            if (id != null && !id.isEmpty()) {
                return By.id(id);
            }
            
            String name = (String) ((JavascriptExecutor) driver).executeScript("return arguments[0].name;", element);
            if (name != null && !name.isEmpty()) {
                return By.name(name);
            }
            
            String className = (String) ((JavascriptExecutor) driver).executeScript("return arguments[0].className;", element);
            if (className != null && !className.isEmpty() && !className.contains(" ")) {
                return By.className(className);
            }
            
            // Generate an XPath that uses position
            String tagName = (String) ((JavascriptExecutor) driver).executeScript("return arguments[0].tagName;", element);
            if (tagName != null && !tagName.isEmpty()) {
                String xpath = generateXPathToElement(driver, element);
                if (xpath != null) {
                    return By.xpath(xpath);
                }
            }
        } catch (Exception e) {
            logger.trace("Failed to determine locator from element", e);
        }
        
        return null;
    }
    
    /**
     * Generates an XPath to the element.
     * 
     * @param driver WebDriver instance
     * @param element WebElement
     * @return XPath string if generated, null otherwise
     */
    private static String generateXPathToElement(WebDriver driver, WebElement element) {
        try {
            return (String) ((JavascriptExecutor) driver).executeScript(
                "function getPathTo(element) {" +
                "    if (element.id !== '') return '//*[@id=\"' + element.id + '\"]';" +
                "    if (element === document.body) return '/html/body';" +
                "    var index = 0;" +
                "    var siblings = element.parentNode.childNodes;" +
                "    for (var i = 0; i < siblings.length; i++) {" +
                "        var sibling = siblings[i];" +
                "        if (sibling === element) return getPathTo(element.parentNode) + '/' + element.tagName.toLowerCase() + '[' + (index + 1) + ']';" +
                "        if (sibling.nodeType === 1 && sibling.tagName === element.tagName) index++;" +
                "    }" +
                "}" +
                "return getPathTo(arguments[0]);", element);
        } catch (Exception e) {
            logger.trace("Failed to generate XPath to element", e);
            return null;
        }
    }
} 