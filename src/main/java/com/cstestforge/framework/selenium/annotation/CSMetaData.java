package com.cstestforge.framework.selenium.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for adding metadata to tests.
 * Used for test organization, reporting, and traceability.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface CSMetaData {
    /**
     * Test ID for traceability.
     * 
     * @return Test ID
     */
    String testId() default "";
    
    /**
     * Feature that this test belongs to.
     * 
     * @return Feature name
     */
    String feature() default "";
    
    /**
     * User story that this test covers.
     * 
     * @return Story name or ID
     */
    String story() default "";
    
    /**
     * Description of the test.
     * 
     * @return Test description
     */
    String description() default "";
    
    /**
     * Authors of the test.
     * 
     * @return Array of author names
     */
    String[] authors() default {};
    
    /**
     * Tags for test organization and filtering.
     * 
     * @return Array of tags
     */
    String[] tags() default {};
    
    /**
     * Linked issue IDs for traceability.
     * 
     * @return Array of issue IDs
     */
    String[] linkedIssues() default {};
    
    /**
     * Requirement ID that this test validates.
     * 
     * @return Requirement ID
     */
    String requirement() default "";
    
    /**
     * Test severity level.
     * 
     * @return Severity level
     */
    Severity severity() default Severity.NORMAL;
    
    /**
     * Custom properties for additional metadata.
     * Format: "key=value"
     * 
     * @return Array of custom property strings
     */
    String[] customProperties() default {};
    
    /**
     * Test severity levels.
     */
    enum Severity {
        /** Blocking severity - test failure prevents further testing */
        BLOCKER,
        
        /** Critical severity - major functionality affected */
        CRITICAL,
        
        /** Normal severity - standard functionality */
        NORMAL,
        
        /** Minor severity - edge cases and minor features */
        MINOR,
        
        /** Trivial severity - cosmetic issues */
        TRIVIAL
    }
} 