package com.cstestforge.framework.selenium.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for defining test steps within test methods.
 * Helps organize and structure test methods into logical steps.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface CSTestStep {
    /**
     * Description of the test step.
     * Can include Gherkin keywords (Given, When, Then).
     * 
     * @return Step description
     */
    String description() default "";
    
    /**
     * Order of the step within a test.
     * 
     * @return Step order
     */
    int order() default 0;
    
    /**
     * Groups this step belongs to.
     * 
     * @return Array of group names
     */
    String[] groups() default {};
    
    /**
     * Tags for the test step.
     * 
     * @return Array of tags
     */
    String[] tags() default {};
    
    /**
     * Whether to take a screenshot after this step.
     * 
     * @return True to take a screenshot
     */
    boolean screenshot() default false;
    
    /**
     * Whether this step is critical for test success.
     * If a critical step fails, the test will fail immediately.
     * 
     * @return True if step is critical
     */
    boolean critical() default true;
} 