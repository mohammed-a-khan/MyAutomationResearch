package com.cstestforge.framework.selenium.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.openqa.selenium.support.FindBy;

/**
 * Enhanced version of Selenium's FindBy annotation with additional features.
 * Supports JavaScript locators and OR/AND locator combinations.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface CSFindBy {
    /**
     * ID locator.
     * 
     * @return Element ID
     */
    String id() default "";
    
    /**
     * Name locator.
     * 
     * @return Element name
     */
    String name() default "";
    
    /**
     * Class name locator.
     * 
     * @return Element class name
     */
    String className() default "";
    
    /**
     * CSS selector locator.
     * 
     * @return CSS selector
     */
    String css() default "";
    
    /**
     * Tag name locator.
     * 
     * @return Tag name
     */
    String tagName() default "";
    
    /**
     * Link text locator.
     * 
     * @return Link text
     */
    String linkText() default "";
    
    /**
     * Partial link text locator.
     * 
     * @return Partial link text
     */
    String partialLinkText() default "";
    
    /**
     * XPath locator.
     * 
     * @return XPath expression
     */
    String xpath() default "";
    
    /**
     * JavaScript locator.
     * Custom JavaScript that returns an element or array of elements.
     * 
     * @return JavaScript expression
     */
    String javascript() default "";
    
    /**
     * Alternative locators using OR logic.
     * If the primary locator fails, these will be tried in order.
     * 
     * @return Array of FindBy locators to try
     */
    FindBy[] orLocators() default {};
    
    /**
     * Nested locators using AND logic.
     * Finds elements within other elements.
     * 
     * @return Array of FindBy locators to chain
     */
    FindBy[] andLocators() default {};
    
    /**
     * Whether to use the field name as a fallback ID locator.
     * 
     * @return True to use field name as fallback
     */
    boolean fallbackFieldName() default false;
    
    /**
     * Description for the element.
     * Used for improved reporting and self-healing capabilities.
     * 
     * @return Element description
     */
    String description() default "";
} 