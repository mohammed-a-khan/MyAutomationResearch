package com.cstestforge.framework.selenium.element;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.AbstractFindByBuilder;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.pagefactory.ByAll;
import org.openqa.selenium.support.pagefactory.ByChained;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cstestforge.framework.selenium.annotation.CSFindBy;
import com.cstestforge.framework.selenium.core.CSDriverManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Processor for CSFindBy annotations.
 * Converts annotation to Selenium By locator.
 */
public class CSFindByProcessor extends AbstractFindByBuilder {
    private static final Logger logger = LoggerFactory.getLogger(CSFindByProcessor.class);
    
    /**
     * Implements the required buildIt method from AbstractFindByBuilder.
     * This method is called by Selenium's PageFactory.
     * 
     * @param annotation The annotation to process
     * @param field The field being annotated
     * @return By locator based on the annotation
     */
    @Override
    public By buildIt(Object annotation, Field field) {
        if (annotation instanceof CSFindBy) {
            return buildBy((CSFindBy) annotation, field);
        }
        
        throw new IllegalArgumentException("Annotation is not a CSFindBy annotation");
    }
    
    /**
     * Build a By locator from a CSFindBy annotation.
     * 
     * @param annotation CSFindBy annotation
     * @param field Field to be located
     * @return Selenium By locator
     */
    public By buildBy(CSFindBy annotation, Field field) {
        assertValidFindBy(annotation);
        
        // Check for ByAll strategy (multiple OR locators)
        if (annotation.orLocators().length > 0) {
            return buildByAllLocator(annotation, field);
        }
        
        // Check for ByChained strategy (multiple AND locators)
        if (annotation.andLocators().length > 0) {
            return buildByChainedLocator(annotation, field);
        }
        
        // Handle custom JavaScript locator
        if (!annotation.javascript().isEmpty()) {
            return new JavaScriptBy(annotation.javascript());
        }
        
        // Build By locator based on the primary element location strategy
        return buildByLocator(annotation, field);
    }
    
    /**
     * Build a By locator from the annotation's primary element location strategy.
     * 
     * @param annotation CSFindBy annotation
     * @param field The field being annotated
     * @return Selenium By locator
     */
    private By buildByLocator(CSFindBy annotation, Field field) {
        if (!annotation.id().isEmpty()) {
            return By.id(annotation.id());
        }
        if (!annotation.name().isEmpty()) {
            return By.name(annotation.name());
        }
        if (!annotation.className().isEmpty()) {
            return By.className(annotation.className());
        }
        if (!annotation.css().isEmpty()) {
            return By.cssSelector(annotation.css());
        }
        if (!annotation.tagName().isEmpty()) {
            return By.tagName(annotation.tagName());
        }
        if (!annotation.linkText().isEmpty()) {
            return By.linkText(annotation.linkText());
        }
        if (!annotation.partialLinkText().isEmpty()) {
            return By.partialLinkText(annotation.partialLinkText());
        }
        if (!annotation.xpath().isEmpty()) {
            return By.xpath(annotation.xpath());
        }
        
        // If no valid locator strategy is specified, fallback to finding by field name
        if (annotation.fallbackFieldName() && field != null) {
            return By.id(field.getName());
        }
        
        // Return a null locator if no strategy is defined
        return By.id("");
    }
    
    /**
     * Build a ByAll locator (OR logic) from the annotation's orLocators.
     * 
     * @param annotation CSFindBy annotation
     * @param field Field to be located
     * @return ByAll locator
     */
    private By buildByAllLocator(CSFindBy annotation, Field field) {
        List<By> byList = new ArrayList<>();
        
        // Add the primary locator if specified
        By primaryBy = buildByLocator(annotation, field);
        if (primaryBy != null && !isEmptyIdLocator(primaryBy)) {
            byList.add(primaryBy);
        }
        
        // Add all OR locators
        for (FindBy findBy : annotation.orLocators()) {
            byList.add(buildByFromFindBy(findBy));
        }
        
        return new ByAll(byList.toArray(new By[0]));
    }
    
    /**
     * Build a ByChained locator (AND logic) from the annotation's andLocators.
     * 
     * @param annotation CSFindBy annotation
     * @param field Field to be located
     * @return ByChained locator
     */
    private By buildByChainedLocator(CSFindBy annotation, Field field) {
        List<By> byList = new ArrayList<>();
        
        // Add the primary locator if specified
        By primaryBy = buildByLocator(annotation, field);
        if (primaryBy != null && !isEmptyIdLocator(primaryBy)) {
            byList.add(primaryBy);
        }
        
        // Add all AND locators
        for (FindBy findBy : annotation.andLocators()) {
            byList.add(buildByFromFindBy(findBy));
        }
        
        return new ByChained(byList.toArray(new By[0]));
    }
    
    /**
     * Check if a By locator is an empty ID locator.
     * 
     * @param by The By locator to check
     * @return True if the locator is an empty ID locator
     */
    private boolean isEmptyIdLocator(By by) {
        return by.toString().equals("By.id: ");
    }
    
    /**
     * Build a By locator from a standard FindBy annotation.
     * 
     * @param findBy FindBy annotation
     * @return Selenium By locator
     */
    @Override
    public By buildByFromFindBy(FindBy findBy) {
        // Use the superclass implementation to handle standard FindBy
        try {
            return super.buildByFromFindBy(findBy);
        } catch (Exception e) {
            logger.error("Failed to build By from FindBy annotation", e);
            return By.id("");
        }
    }
    
    /**
     * Assert that the annotation is valid.
     * 
     * @param annotation CSFindBy annotation
     */
    private void assertValidFindBy(CSFindBy annotation) {
        if (annotation == null) {
            throw new IllegalArgumentException("Cannot build a locator from a null CSFindBy annotation");
        }
        
        // Check that we have at least one location strategy
        if (annotation.id().isEmpty() &&
                annotation.name().isEmpty() &&
                annotation.className().isEmpty() &&
                annotation.css().isEmpty() &&
                annotation.tagName().isEmpty() &&
                annotation.linkText().isEmpty() &&
                annotation.partialLinkText().isEmpty() &&
                annotation.xpath().isEmpty() &&
                annotation.javascript().isEmpty() &&
                annotation.orLocators().length == 0 &&
                annotation.andLocators().length == 0 &&
                !annotation.fallbackFieldName()) {
            throw new IllegalArgumentException("At least one location strategy must be specified in the @CSFindBy annotation");
        }
    }
    
    /**
     * Special By implementation for JavaScript locators.
     */
    public static class JavaScriptBy extends By {
        private final String script;
        
        /**
         * Create a new JavaScriptBy locator.
         * 
         * @param script JavaScript to execute
         */
        public JavaScriptBy(String script) {
            this.script = script;
        }
        
        @Override
        public List<WebElement> findElements(SearchContext context) {
            WebDriver driver = extractDriver(context);
            if (driver instanceof JavascriptExecutor) {
                JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
                Object result = jsExecutor.executeScript(script);
                
                List<WebElement> elements = new ArrayList<>();
                if (result instanceof WebElement) {
                    elements.add((WebElement) result);
                } else if (result instanceof List) {
                    for (Object item : (List<?>) result) {
                        if (item instanceof WebElement) {
                            elements.add((WebElement) item);
                        }
                    }
                }
                return elements;
            }
            return new ArrayList<>();
        }
        
        /**
         * Extract the WebDriver from a SearchContext.
         * 
         * @param context SearchContext
         * @return WebDriver instance
         */
        private WebDriver extractDriver(SearchContext context) {
            // First check if context is directly a WebDriver
            if (context instanceof WebDriver) {
                return (WebDriver) context;
            }
            
            // Try to get the current thread's WebDriver
            try {
                return CSDriverManager.getDriver();
            } catch (Exception e) {
                // Ignore and try reflection as last resort
            }
            
            // Last resort: try to extract driver using reflection
            try {
                // This is a hack that works with RemoteWebElement
                Field field = context.getClass().getDeclaredField("driver");
                field.setAccessible(true);
                return (WebDriver) field.get(context);
            } catch (Exception e) {
                throw new RuntimeException("Unable to extract WebDriver from SearchContext", e);
            }
        }
        
        /**
         * Get the JavaScript to be executed.
         * 
         * @return JavaScript
         */
        public String getScript() {
            return script;
        }
        
        @Override
        public String toString() {
            return "JavaScriptBy(" + script + ")";
        }
    }
} 