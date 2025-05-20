package com.cstestforge.framework.selenium.annotation;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openqa.selenium.WebDriver;

import com.cstestforge.framework.selenium.reporting.CSReporting;

/**
 * Processor for the CSTestStep annotation.
 * This class handles execution of test steps and provides reporting functionality.
 */
public class CSTestStepProcessor {
    private static final Logger logger = LoggerFactory.getLogger(CSTestStepProcessor.class);
    
    private final WebDriver driver;
    private final CSReporting reporting;
    
    /**
     * Constructor for the test step processor.
     * 
     * @param driver WebDriver instance
     */
    public CSTestStepProcessor(WebDriver driver) {
        this.driver = driver;
        this.reporting = new CSReporting();
    }
    
    /**
     * Process all test step methods in a class.
     * 
     * @param testInstance Instance of the test class
     */
    public void processTestSteps(Object testInstance) {
        Class<?> testClass = testInstance.getClass();
        List<Method> testStepMethods = findTestStepMethods(testClass);
        
        logger.debug("Found {} test step methods in class {}", testStepMethods.size(), testClass.getSimpleName());
        
        for (Method method : testStepMethods) {
            CSTestStep annotation = method.getAnnotation(CSTestStep.class);
            executeTestStep(testInstance, method, annotation);
        }
    }
    
    /**
     * Find all methods annotated with CSTestStep.
     * 
     * @param testClass Test class to search
     * @return List of annotated methods
     */
    private List<Method> findTestStepMethods(Class<?> testClass) {
        return Arrays.stream(testClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(CSTestStep.class))
                .collect(Collectors.toList());
    }
    
    /**
     * Execute a test step method and handle reporting.
     * 
     * @param testInstance Instance of the test class
     * @param method Method to execute
     * @param annotation TestStep annotation
     */
    private void executeTestStep(Object testInstance, Method method, CSTestStep annotation) {
        String stepDescription = annotation.description();
        if (stepDescription.isEmpty()) {
            stepDescription = formatMethodName(method.getName());
        }
        
        logger.debug("Executing test step: {}", stepDescription);
        
        // Start step in the reporting system
        CSReporting.TestNode stepNode = reporting.startStep(stepDescription, stepDescription);
        long startTime = System.currentTimeMillis();
        
        try {
            method.setAccessible(true);
            method.invoke(testInstance);
            
            // End step with success
            long executionTime = System.currentTimeMillis() - startTime;
            reporting.endStep(stepDescription, true, executionTime);
            reporting.info("Step completed successfully");
            
            // Take screenshot if required by annotation
            if (annotation.screenshot()) {
                String screenshotPath = reporting.takeScreenshot(stepDescription);
                reporting.addScreenshot("Step result", screenshotPath);
            }
        } catch (Exception e) {
            // End step with failure
            long executionTime = System.currentTimeMillis() - startTime;
            reporting.endStep(stepDescription, false, executionTime, e);
            reporting.error("Step failed: " + e.getMessage(), e);
            
            // Always take screenshot on failure
            String screenshotPath = reporting.takeScreenshot("Failure_" + stepDescription);
            reporting.addScreenshot("Failure", screenshotPath);
            
            throw new RuntimeException("Test step execution failed: " + stepDescription, e);
        }
    }
    
    /**
     * Format a method name as a readable step description.
     * 
     * @param methodName Method name to format
     * @return Formatted step description
     */
    private String formatMethodName(String methodName) {
        // Convert camelCase to space-separated words
        String formatted = methodName.replaceAll("([a-z])([A-Z])", "$1 $2");
        
        // Capitalize first letter
        return formatted.substring(0, 1).toUpperCase() + formatted.substring(1);
    }
} 