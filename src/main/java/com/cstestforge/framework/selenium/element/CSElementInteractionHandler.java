package com.cstestforge.framework.selenium.element;

import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cstestforge.framework.selenium.core.CSDriverManager;
import com.cstestforge.framework.selenium.core.CSWait;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Handles all element interactions with smart retry and fallback mechanisms.
 * Provides reliable element operations even in dynamic and challenging UI scenarios.
 */
public class CSElementInteractionHandler {
    private static final Logger logger = LoggerFactory.getLogger(CSElementInteractionHandler.class);
    private static final int DEFAULT_RETRY_COUNT = 3;
    private static final int DEFAULT_RETRY_DELAY_MS = 500;
    private static final int JS_SCROLL_MARGIN = 100;
    private static final List<Class<? extends Throwable>> RETRY_EXCEPTIONS = Arrays.asList(
            StaleElementReferenceException.class,
            ElementClickInterceptedException.class,
            ElementNotInteractableException.class,
            NoSuchElementException.class,
            TimeoutException.class
    );
    
    private final int retryCount;
    private final int retryDelayMs;
    private final CSWait wait;
    
    /**
     * Constructor with default retry settings.
     */
    public CSElementInteractionHandler() {
        this(DEFAULT_RETRY_COUNT, DEFAULT_RETRY_DELAY_MS);
    }
    
    /**
     * Constructor with custom retry settings.
     * 
     * @param retryCount Maximum number of retries
     * @param retryDelayMs Delay between retries in milliseconds
     */
    public CSElementInteractionHandler(int retryCount, int retryDelayMs) {
        this.retryCount = retryCount;
        this.retryDelayMs = retryDelayMs;
        this.wait = new CSWait(CSDriverManager.getDriver());
    }
    
    /**
     * Click on a web element with fallback strategies.
     * 
     * @param element Element to click
     */
    public void click(WebElement element) {
        executeWithRetry(element, "click", webElement -> {
            try {
                // First attempt: Direct click
                webElement.click();
            } catch (Exception e) {
                logger.debug("Direct click failed, trying alternative methods: {}", e.getMessage());
                try {
                    clickWithAlternativeMethod(webElement, e);
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to click element", ex);
                }
            }
        });
    }
    
    /**
     * Click using alternative methods when standard click fails.
     * 
     * @param element Element to click
     * @param originalException Original exception from click attempt
     */
    private void clickWithAlternativeMethod(WebElement element, Exception originalException) throws Exception {
        try {
            // First, try to ensure element is in view and clickable
            scrollToElement(element);
            
            // Wait for element to be clickable
            WebDriverWait clickableWait = new WebDriverWait(CSDriverManager.getDriver(), Duration.ofSeconds(5));
            clickableWait.until(ExpectedConditions.elementToBeClickable(element));
            
            try {
                // Try clicking after scroll and wait
                element.click();
                return;
            } catch (Exception e) {
                logger.debug("Click after scroll failed: {}", e.getMessage());
            }
            
            // Try Actions click
            try {
                new Actions(CSDriverManager.getDriver())
                    .moveToElement(element)
                    .click()
                    .perform();
                return;
            } catch (Exception e) {
                logger.debug("Actions click failed: {}", e.getMessage());
            }
            
            // Try JavaScript click as last resort
            try {
                JavascriptExecutor js = (JavascriptExecutor) CSDriverManager.getDriver();
                js.executeScript("arguments[0].click();", element);
            } catch (Exception e) {
                logger.debug("JavaScript click failed: {}", e.getMessage());
                // If all methods failed, rethrow the original exception
                throw originalException;
            }
        } catch (Exception e) {
            if (e == originalException) {
                throw new RuntimeException("All click methods failed", e);
            }
            throw e;
        }
    }
    
    /**
     * Type text in an element with fallback strategies.
     * 
     * @param element Element to type in
     * @param text Text to type
     * @param clearFirst Whether to clear the field first
     */
    public void sendKeys(WebElement element, CharSequence[] text, boolean clearFirst) {
        executeWithRetry(element, "sendKeys", webElement -> {
            try {
                if (clearFirst) {
                    clearElement(webElement);
                }
                
                // First attempt: Direct sendKeys
                webElement.sendKeys(text);
            } catch (Exception e) {
                logger.debug("Direct sendKeys failed, trying alternative methods: {}", e.getMessage());
                try {
                    sendKeysWithAlternativeMethod(webElement, text, e);
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to send keys to element", ex);
                }
            }
        });
    }
    
    /**
     * Type text using alternative methods when standard sendKeys fails.
     * 
     * @param element Element to type in
     * @param text Text to type
     * @param originalException Original exception from sendKeys attempt
     */
    private void sendKeysWithAlternativeMethod(WebElement element, CharSequence[] text, Exception originalException) throws Exception {
        try {
            // First, try to ensure element is in view
            scrollToElement(element);
            
            try {
                // Try clicking to ensure focus and then sending keys
                element.click();
                element.sendKeys(text);
                return;
            } catch (Exception e) {
                logger.debug("Click and sendKeys failed: {}", e.getMessage());
            }
            
            // Try Actions sendKeys
            try {
                new Actions(CSDriverManager.getDriver())
                    .moveToElement(element)
                    .click()
                    .sendKeys(text)
                    .perform();
                return;
            } catch (Exception e) {
                logger.debug("Actions sendKeys failed: {}", e.getMessage());
            }
            
            // Try JavaScript to set value as last resort
            try {
                JavascriptExecutor js = (JavascriptExecutor) CSDriverManager.getDriver();
                
                // Join the text segments
                StringBuilder sb = new StringBuilder();
                for (CharSequence seq : text) {
                    sb.append(seq);
                }
                
                js.executeScript("arguments[0].value = arguments[1];", element, sb.toString());
                
                // Fire change event to trigger JS listeners
                js.executeScript(
                    "var event = new Event('change', { bubbles: true });" +
                    "arguments[0].dispatchEvent(event);", element);
            } catch (Exception e) {
                logger.debug("JavaScript value set failed: {}", e.getMessage());
                // If all methods failed, rethrow the original exception
                throw originalException;
            }
        } catch (Exception e) {
            if (e == originalException) {
                throw new RuntimeException("All sendKeys methods failed", e);
            }
            throw e;
        }
    }
    
    /**
     * Clear an input element with fallback strategies.
     * 
     * @param element Element to clear
     */
    public void clearElement(WebElement element) {
        executeWithRetry(element, "clear", webElement -> {
            try {
                // First attempt: Direct clear
                webElement.clear();
            } catch (Exception e) {
                logger.debug("Direct clear failed, trying alternative methods: {}", e.getMessage());
                
                try {
                    // Try Ctrl+A + Delete
                    webElement.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
                } catch (Exception e2) {
                    logger.debug("Ctrl+A clear failed: {}", e2.getMessage());
                    
                    // Try JavaScript clear as last resort
                    try {
                        JavascriptExecutor js = (JavascriptExecutor) CSDriverManager.getDriver();
                        js.executeScript("arguments[0].value = '';", webElement);
                        
                        // Fire change event
                        js.executeScript(
                            "var event = new Event('change', { bubbles: true });" +
                            "arguments[0].dispatchEvent(event);", webElement);
                    } catch (Exception e3) {
                        logger.debug("JavaScript clear failed: {}", e3.getMessage());
                        throw e; // Throw original exception
                    }
                }
            }
        });
    }
    
    /**
     * Select dropdown option by visible text.
     * 
     * @param element Select element
     * @param visibleText Text to select
     */
    public void selectByVisibleText(WebElement element, String visibleText) {
        executeWithRetry(element, "selectByVisibleText", webElement -> {
            try {
                // First attempt: Standard Select
                new Select(webElement).selectByVisibleText(visibleText);
            } catch (Exception e) {
                logger.debug("Standard select failed, trying alternative methods: {}", e.getMessage());
                
                try {
                    // Try clicking the dropdown first
                    webElement.click();
                    
                    // Then try standard select again
                    new Select(webElement).selectByVisibleText(visibleText);
                } catch (Exception e2) {
                    logger.debug("Click and select failed: {}", e2.getMessage());
                    
                    // Try directly clicking the option as last resort
                    try {
                        String optionXpath = String.format(".//option[normalize-space(text())='%s']", visibleText);
                        WebElement option = webElement.findElement(org.openqa.selenium.By.xpath(optionXpath));
                        option.click();
                    } catch (Exception e3) {
                        logger.debug("Option click failed: {}", e3.getMessage());
                        throw e; // Throw original exception
                    }
                }
            }
        });
    }
    
    /**
     * Select dropdown option by value.
     * 
     * @param element Select element
     * @param value Value to select
     */
    public void selectByValue(WebElement element, String value) {
        executeWithRetry(element, "selectByValue", webElement -> {
            try {
                // First attempt: Standard Select
                new Select(webElement).selectByValue(value);
            } catch (Exception e) {
                logger.debug("Standard select failed, trying alternative methods: {}", e.getMessage());
                
                try {
                    // Try clicking the dropdown first
                    webElement.click();
                    
                    // Then try standard select again
                    new Select(webElement).selectByValue(value);
                } catch (Exception e2) {
                    logger.debug("Click and select failed: {}", e2.getMessage());
                    
                    // Try directly clicking the option as last resort
                    try {
                        String optionXpath = String.format(".//option[@value='%s']", value);
                        WebElement option = webElement.findElement(org.openqa.selenium.By.xpath(optionXpath));
                        option.click();
                    } catch (Exception e3) {
                        logger.debug("Option click failed: {}", e3.getMessage());
                        throw e; // Throw original exception
                    }
                }
            }
        });
    }
    
    /**
     * Set checkbox or radio button state.
     * 
     * @param element Checkbox or radio element
     * @param check Whether to check or uncheck
     */
    public void setCheckboxState(WebElement element, boolean check) {
        executeWithRetry(element, check ? "check" : "uncheck", webElement -> {
            try {
                // First check the current state
                boolean isChecked = webElement.isSelected();
                
                // Only act if the current state differs from desired state
                if (isChecked != check) {
                    // First attempt: Direct click
                    webElement.click();
                    
                    // Verify change took effect
                    if (webElement.isSelected() != check) {
                        throw new ElementNotInteractableException("Checkbox state didn't change");
                    }
                }
            } catch (Exception e) {
                logger.debug("Direct checkbox interaction failed, trying alternative methods: {}", e.getMessage());
                
                // Try JavaScript as fallback
                try {
                    JavascriptExecutor js = (JavascriptExecutor) CSDriverManager.getDriver();
                    js.executeScript("arguments[0].checked = arguments[1];", webElement, check);
                    
                    // Fire change event
                    js.executeScript(
                        "var event = new Event('change', { bubbles: true });" +
                        "arguments[0].dispatchEvent(event);", webElement);
                } catch (Exception e2) {
                    logger.debug("JavaScript checkbox setting failed: {}", e2.getMessage());
                    throw e; // Throw original exception
                }
            }
        });
    }
    
    /**
     * Hover over an element with fallback strategies.
     * 
     * @param element Element to hover over
     */
    public void hover(WebElement element) {
        executeWithRetry(element, "hover", webElement -> {
            try {
                // First attempt: Actions hover
                new Actions(CSDriverManager.getDriver())
                    .moveToElement(webElement)
                    .perform();
            } catch (Exception e) {
                logger.debug("Actions hover failed, trying alternative methods: {}", e.getMessage());
                
                // Try JavaScript hover simulation as fallback
                try {
                    JavascriptExecutor js = (JavascriptExecutor) CSDriverManager.getDriver();
                    js.executeScript(
                        "var event = new MouseEvent('mouseover', {" +
                        "  'view': window," +
                        "  'bubbles': true," +
                        "  'cancelable': true" +
                        "});" +
                        "arguments[0].dispatchEvent(event);", webElement);
                } catch (Exception e2) {
                    logger.debug("JavaScript hover failed: {}", e2.getMessage());
                    throw e; // Throw original exception
                }
            }
        });
    }
    
    /**
     * Wait for element text to match a specific value.
     * 
     * @param element Element to check
     * @param expectedText Expected text
     * @param timeoutSeconds Timeout in seconds
     * @return True if text matched within timeout
     */
    public boolean waitForText(WebElement element, String expectedText, int timeoutSeconds) {
        try {
            WebDriverWait textWait = new WebDriverWait(CSDriverManager.getDriver(), Duration.ofSeconds(timeoutSeconds));
            return textWait.until(textMatches(element, expectedText));
        } catch (TimeoutException e) {
            return false;
        }
    }
    
    /**
     * Condition that checks if element text matches expected value.
     * 
     * @param element Element to check
     * @param expectedText Expected text
     * @return ExpectedCondition for text matching
     */
    private ExpectedCondition<Boolean> textMatches(final WebElement element, final String expectedText) {
        return new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                try {
                    String actualText = element.getText();
                    return expectedText.equals(actualText);
                } catch (StaleElementReferenceException e) {
                    return false;
                }
            }
            
            @Override
            public String toString() {
                return "text to match '" + expectedText + "'";
            }
        };
    }
    
    /**
     * Scroll element into view.
     * 
     * @param element Element to scroll to
     */
    public void scrollToElement(WebElement element) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) CSDriverManager.getDriver();
            
            // Scroll the element into view with margin
            js.executeScript(
                "arguments[0].scrollIntoView({block: 'center', inline: 'center'});" +
                "window.scrollBy(0, -" + JS_SCROLL_MARGIN + ");", element);
            
            // Short wait for scroll to complete
            sleep(300);
        } catch (Exception e) {
            logger.debug("Failed to scroll to element: {}", e.getMessage());
        }
    }
    
    /**
     * Execute an action on an element with retry logic.
     * 
     * @param element Element to operate on
     * @param operationName Name of the operation (for logging)
     * @param action Action to perform
     */
    public void executeWithRetry(WebElement element, String operationName, Consumer<WebElement> action) {
        try {
            Exception lastException = null;
            
            for (int attempt = 0; attempt <= retryCount; attempt++) {
                try {
                    if (attempt > 0) {
                        logger.debug("Retrying {} operation, attempt {}/{}", operationName, attempt, retryCount);
                        sleep(retryDelayMs);
                    }
                    
                    action.accept(element);
                    return; // Success
                } catch (Exception e) {
                    lastException = e;
                    
                    // Only retry if exception is in our retry list
                    boolean shouldRetry = false;
                    for (Class<? extends Throwable> exceptionClass : RETRY_EXCEPTIONS) {
                        if (exceptionClass.isInstance(e)) {
                            shouldRetry = true;
                            break;
                        }
                    }
                    
                    if (!shouldRetry || attempt >= retryCount) {
                        break;
                    }
                }
            }
            
            // If we got here, all attempts failed
            if (lastException != null) {
                throw new RuntimeException("Operation " + operationName + " failed after " + 
                        (retryCount + 1) + " attempts", lastException);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute action on element", e);
        }
    }
    
    /**
     * Execute a function on an element with retry logic and return a result.
     * 
     * @param element Element to operate on
     * @param operationName Name of the operation (for logging)
     * @param function Function to execute
     * @return Result of the function
     */
    private <T> T executeWithRetryAndResult(WebElement element, String operationName, 
            Function<WebElement, T> function) {
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= retryCount; attempt++) {
            try {
                if (attempt > 0) {
                    logger.debug("Retrying {} operation, attempt {}/{}", operationName, attempt, retryCount);
                    sleep(retryDelayMs);
                }
                
                return function.apply(element);
            } catch (Exception e) {
                lastException = e;
                
                // Only retry if exception is in our retry list
                boolean shouldRetry = false;
                for (Class<? extends Throwable> exceptionClass : RETRY_EXCEPTIONS) {
                    if (exceptionClass.isInstance(e)) {
                        shouldRetry = true;
                        break;
                    }
                }
                
                if (!shouldRetry || attempt >= retryCount) {
                    break;
                }
            }
        }
        
        // If we got here, all attempts failed
        if (lastException != null) {
            throw new RuntimeException("Operation " + operationName + " failed after " + 
                    (retryCount + 1) + " attempts", lastException);
        }
        
        return null; // Shouldn't reach here
    }
    
    /**
     * Get text from an element with retry and fallback.
     * 
     * @param element Element to get text from
     * @return Element text
     */
    public String getText(WebElement element) {
        return executeWithRetryAndResult(element, "getText", webElement -> {
            try {
                // First attempt: Direct getText
                String text = webElement.getText();
                if (text == null || text.isEmpty()) {
                    // Check value attribute as fallback
                    String value = webElement.getAttribute("value");
                    if (value != null && !value.isEmpty()) {
                        return value;
                    }
                    
                    // Check textContent as another fallback
                    String textContent = webElement.getAttribute("textContent");
                    if (textContent != null && !textContent.isEmpty()) {
                        return textContent;
                    }
                }
                return text;
            } catch (Exception e) {
                logger.debug("getText failed, trying JavaScript: {}", e.getMessage());
                
                // Try JavaScript as fallback
                try {
                    JavascriptExecutor js = (JavascriptExecutor) CSDriverManager.getDriver();
                    String jsResult = (String) js.executeScript(
                        "if (arguments[0].value !== undefined && arguments[0].value !== '')" +
                        "  return arguments[0].value;" +
                        "else if (arguments[0].textContent !== undefined)" +
                        "  return arguments[0].textContent;" +
                        "else" +
                        "  return arguments[0].innerText;", 
                        webElement);
                    
                    return jsResult != null ? jsResult : "";
                } catch (Exception e2) {
                    logger.debug("JavaScript getText failed: {}", e2.getMessage());
                    throw e; // Throw original exception
                }
            }
        });
    }
    
    /**
     * Thread sleep with exception handling.
     * 
     * @param milliseconds Sleep time in milliseconds
     */
    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
} 