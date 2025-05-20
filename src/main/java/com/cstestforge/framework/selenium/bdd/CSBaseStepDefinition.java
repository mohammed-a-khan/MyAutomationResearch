package com.cstestforge.framework.selenium.bdd;

import java.io.File;
import java.util.Map;
import java.util.function.Consumer;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cstestforge.framework.selenium.core.CSDriverManager;
import com.cstestforge.framework.selenium.element.CSElementInteractionHandler;
import com.cstestforge.framework.selenium.page.CSBasePage;
import com.cstestforge.framework.selenium.reporting.CSReporting;

import io.cucumber.java.After;
import io.cucumber.java.AfterStep;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

/**
 * Base class for all step definition classes.
 * Provides common functionality for BDD step implementations.
 */
public abstract class CSBaseStepDefinition {
    private static final Logger logger = LoggerFactory.getLogger(CSBaseStepDefinition.class);
    
    // ScenarioContext instance for sharing data between steps
    protected CSScenarioContext scenarioContext;
    
    // Cucumber scenario
    protected Scenario scenario;
    
    // Element interaction handler
    protected CSElementInteractionHandler interactionHandler;
    
    // Reporting instance
    protected CSReporting reporting;
    
    /**
     * Default constructor.
     */
    public CSBaseStepDefinition() {
        this.scenarioContext = new CSScenarioContext();
        this.interactionHandler = new CSElementInteractionHandler();
    }
    
    /**
     * Set up the test environment before each scenario.
     * 
     * @param scenario Cucumber scenario
     */
    @Before
    public void setUp(Scenario scenario) {
        this.scenario = scenario;
        
        // Log scenario information
        logger.info("Starting scenario: {}", scenario.getName());
        
        // Initialize driver if not already initialized
        if (CSDriverManager.getDriver() == null) {
            CSDriverManager.initializeDriver();
        }
        
        // Initialize reporting
        this.reporting = new CSReporting();
        
        // Additional setup steps can be done in child classes
        customSetUp();
    }
    
    /**
     * Custom setup for child classes to implement.
     */
    protected void customSetUp() {
        // To be implemented by child classes if needed
    }
    
    /**
     * Clean up after each scenario.
     * 
     * @param scenario Cucumber scenario
     */
    @After
    public void tearDown(Scenario scenario) {
        try {
            // Take screenshot if scenario failed
            if (scenario.isFailed()) {
                takeScreenshot("FailureScreenshot");
            }
            
            // Clear scenario context
            scenarioContext.clearScenarioScope();
            
            // Additional teardown steps can be done in child classes
            customTearDown();
            
            // Log scenario status
            logger.info("Scenario completed: {} - {}", 
                    scenario.getName(), 
                    scenario.getStatus());
        } catch (Exception e) {
            logger.error("Error in scenario teardown", e);
        }
    }
    
    /**
     * Custom teardown for child classes to implement.
     */
    protected void customTearDown() {
        // To be implemented by child classes if needed
    }
    
    /**
     * Take a screenshot after each step (if configured to do so).
     * 
     * @param scenario Cucumber scenario
     */
    @AfterStep
    public void afterStep(Scenario scenario) {
        // Check if we need to take a screenshot for this step
        Boolean screenshotAfterStep = scenarioContext.get("SCREENSHOT_AFTER_STEP", Boolean.class);
        if (Boolean.TRUE.equals(screenshotAfterStep)) {
            takeScreenshot("StepScreenshot");
        }
    }
    
    /**
     * Take a screenshot and embed it in the Cucumber report.
     * 
     * @param name Screenshot name
     */
    protected void takeScreenshot(String name) {
        WebDriver driver = CSDriverManager.getDriver();
        if (driver instanceof TakesScreenshot) {
            try {
                // Take screenshot
                byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                String screenshotName = name + "_" + System.currentTimeMillis() + ".png";
                
                // Embed in Cucumber report
                scenario.attach(screenshot, "image/png", screenshotName);
                
                // Save to file and add to reporting
                File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                String screenshotPath = "test-output/screenshots/" + screenshotName;
                
                // Ensure directory exists
                File directory = new File("test-output/screenshots");
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                
                // Copy file to destination
                org.apache.commons.io.FileUtils.copyFile(screenshotFile, new File(screenshotPath));
                
                // Add to reporting
                reporting.addScreenshot(scenario.getName(), screenshotPath);
                
                logger.debug("Screenshot taken: {}", screenshotName);
            } catch (Exception e) {
                logger.error("Failed to take screenshot", e);
            }
        }
    }
    
    /**
     * Initialize a page object.
     * 
     * @param <T> Page object type
     * @param pageClass Page class to initialize
     * @return Initialized page object
     */
    protected <T extends CSBasePage> T initializePage(Class<T> pageClass) {
        try {
            T page = pageClass.getDeclaredConstructor().newInstance();
            
            // Store the current page in the context
            scenarioContext.set("CURRENT_PAGE", page);
            
            return page;
        } catch (Exception e) {
            logger.error("Failed to initialize page: {}", pageClass.getName(), e);
            throw new RuntimeException("Failed to initialize page: " + pageClass.getName(), e);
        }
    }
    
    /**
     * Get the current page from the context.
     * 
     * @param <T> Page object type
     * @param pageClass Expected page class
     * @return Current page object
     */
    @SuppressWarnings("unchecked")
    protected <T extends CSBasePage> T getCurrentPage(Class<T> pageClass) {
        CSBasePage page = scenarioContext.get("CURRENT_PAGE", CSBasePage.class);
        
        if (page == null) {
            logger.warn("No current page found in context, initializing {}", pageClass.getName());
            return initializePage(pageClass);
        }
        
        if (!pageClass.isInstance(page)) {
            logger.warn("Current page is of unexpected type. Expected: {}, Actual: {}", 
                    pageClass.getName(), page.getClass().getName());
            return initializePage(pageClass);
        }
        
        return (T) page;
    }
    
    /**
     * Execute action with retry and reporting.
     * 
     * @param actionName Action name for reporting
     * @param element Element to act on
     * @param action Action to execute
     */
    protected void executeAction(String actionName, WebElement element, Consumer<WebElement> action) {
        try {
            interactionHandler.executeWithRetry(element, actionName, action);
        } catch (Exception e) {
            // Log error, take screenshot and rethrow
            logger.error("Action '{}' failed", actionName, e);
            takeScreenshot(actionName + "_error");
            throw e;
        }
    }
    
    /**
     * Convert a data table row to a map.
     * 
     * @param dataTable Cucumber data table row
     * @return Map of column name to value
     */
    protected Map<String, String> tableRowToMap(io.cucumber.datatable.DataTable dataTable) {
        return dataTable.asMaps().get(0);
    }
    
    /**
     * Get the WebDriver instance.
     * 
     * @return WebDriver instance
     */
    protected WebDriver getDriver() {
        return CSDriverManager.getDriver();
    }
    
    /**
     * Get the scenario context.
     * 
     * @return Scenario context
     */
    protected CSScenarioContext getScenarioContext() {
        return scenarioContext;
    }
} 