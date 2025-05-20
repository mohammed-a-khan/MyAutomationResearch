package com.cstestforge.framework.selenium.core;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;

import com.cstestforge.framework.selenium.annotation.CSMetaData;
import com.cstestforge.framework.selenium.annotation.CSTestStepProcessor;
import com.cstestforge.framework.selenium.reporting.CSReporting;

/**
 * Base test class that provides common test setup and teardown functionality.
 * All Selenium test classes should extend this class.
 */
public abstract class CSBaseTest {
    private static final Logger logger = LoggerFactory.getLogger(CSBaseTest.class);
    
    /** WebDriver instance for the test */
    protected WebDriver driver;
    
    /** Reporting instance for the test */
    protected CSReporting reporting;
    
    /** Test step processor for annotations */
    protected CSTestStepProcessor testStepProcessor;
    
    /** Current test name */
    protected String testName;
    
    /**
     * Setup method that runs before the test class.
     * Initializes WebDriver and reporting.
     */
    @BeforeClass
    public void setupClass() {
        reporting = new CSReporting();
        logger.info("Test class setup completed");
    }
    
    /**
     * Teardown method that runs after the test class.
     * Cleans up resources.
     */
    @AfterClass
    public void tearDownClass() {
        logger.info("Test class teardown completed");
    }
    
    /**
     * Setup method that runs before each test method.
     * Initializes WebDriver for the test.
     * 
     * @param browser Browser to use (optional parameter from TestNG XML)
     */
    @BeforeMethod
    @Parameters(value = {"browser"})
    public void setupTest(String browser) {
        // Initialize WebDriver
        if (browser != null && !browser.isEmpty()) {
            // Set system property to override default browser
            System.setProperty("browser", browser.toLowerCase());
        }
        
        // Get driver instance from manager
        driver = CSDriverManager.getDriver();
        
        // Initialize test step processor
        testStepProcessor = new CSTestStepProcessor(driver);
        
        // Get test method name
        testName = getClass().getSimpleName();
        
        logger.info("Starting test: {}", testName);
        reporting.startTest(testName, "Test execution");
    }
    
    /**
     * Teardown method that runs after each test method.
     * Closes WebDriver and records test result.
     * 
     * @param result TestNG test result
     */
    @AfterMethod
    public void tearDownTest(ITestResult result) {
        boolean testPassed = result.isSuccess();
        
        if (testPassed) {
            logger.info("Test passed: {}", testName);
            reporting.info("Test completed successfully");
        } else {
            logger.error("Test failed: {}", testName);
            if (result.getThrowable() != null) {
                reporting.error("Test failed: " + result.getThrowable().getMessage(), result.getThrowable());
                logger.error("Test failure reason: {}", result.getThrowable().getMessage());
            }
            
            // Take screenshot on failure
            if (driver != null) {
                String screenshotPath = reporting.takeScreenshot("Failure_" + testName);
                reporting.addScreenshot("Failure Screenshot", screenshotPath);
            }
        }
        
        // Report test result
        reporting.endTest(testName, testPassed);
        
        // Quit the driver
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                logger.error("Error while quitting driver", e);
            }
            driver = null;
        }
    }
} 