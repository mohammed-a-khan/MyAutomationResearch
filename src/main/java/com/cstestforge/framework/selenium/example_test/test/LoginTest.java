package com.cstestforge.framework.selenium.example_test.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.cstestforge.framework.selenium.annotation.CSMetaData;
import com.cstestforge.framework.selenium.annotation.CSTestStep;
import com.cstestforge.framework.selenium.core.CSDriverManager;
import com.cstestforge.framework.selenium.example_test.page.LoginPage;
import com.cstestforge.framework.selenium.reporting.CSReporting;
import com.cstestforge.framework.selenium.utils.CSJsonReader;

import java.io.IOException;
import java.util.Map;

/**
 * Example TestNG test class for login functionality.
 * Demonstrates usage of the CSTestForge framework features.
 */
@CSMetaData(
    feature = "Authentication",
    story = "User login",
    description = "Tests the login functionality with various credentials",
    authors = {"CSTestForge Team"},
    tags = {"login", "authentication", "smoke"}
)
public class LoginTest {
    private static final Logger logger = LoggerFactory.getLogger(LoginTest.class);
    
    private CSReporting reporting;
    private LoginPage loginPage;
    
    /**
     * Set up before each test method.
     */
    @BeforeMethod
    public void setUp() {
        // Initialize WebDriver
        CSDriverManager.getDriver(); // This will initialize the driver if not already initialized
        
        // Initialize reporting
        reporting = new CSReporting();
        
        // Initialize login page
        loginPage = new LoginPage();
        
        logger.info("Test setup complete");
    }
    
    /**
     * Clean up after each test method.
     */
    @AfterMethod
    public void tearDown() {
        // Capture final screenshot
        String screenshotPath = reporting.takeScreenshot("FinalState");
        reporting.addScreenshot("Final state", screenshotPath);
        
        // Quit WebDriver
        CSDriverManager.quitDriver();
        
        logger.info("Test teardown complete");
    }
    
    /**
     * Data provider for login test.
     * 
     * @return Test data array
     * @throws IOException If test data cannot be read
     */
    @DataProvider(name = "loginData")
    public Object[][] getLoginData() throws IOException {
        try {
            // Load test data from JSON file
            CSJsonReader jsonReader = new CSJsonReader("src/test/resources/testdata/login-data.json");
            return jsonReader.createDataProvider();
        } catch (IOException e) {
            logger.error("Failed to load login test data", e);
            
            // Fallback test data
            return new Object[][] {
                { Map.of("username", "validUser", "password", "validPass", "expected", "success") },
                { Map.of("username", "invalidUser", "password", "invalidPass", "expected", "failure") }
            };
        }
    }
    
    /**
     * Test valid login.
     */
    @Test
    @CSMetaData(
        testId = "LOGIN-001",
        description = "Test valid login credentials",
        severity = CSMetaData.Severity.CRITICAL
    )
    @CSTestStep(description = "Perform valid login test", screenshot = true)
    public void testValidLogin() {
        // Start reporting
        reporting.startTest("Valid login test", "Authentication test with valid credentials");
        reporting.startStep("Valid login test", "Authentication");
        
        try {
            // Navigate to login page
            loginPage.navigateToLoginPage();
            
            // Perform login
            boolean loginResult = loginPage.login("validUser", "validPass");
            
            // Verify login success
            Assert.assertTrue(loginResult, "Login should be successful with valid credentials");
            Assert.assertFalse(loginPage.isLoginPageDisplayed(), "Login page should not be displayed after successful login");
            
            // End reporting step
            reporting.endStep("Valid login test", true, 0);
            reporting.endTest("Valid login test", true);
        } catch (Exception e) {
            // End reporting step with failure
            reporting.endStep("Valid login test", false, 0, e);
            reporting.endTest("Valid login test", false);
            throw e;
        }
    }
    
    /**
     * Test invalid login.
     */
    @Test
    @CSMetaData(
        testId = "LOGIN-002",
        description = "Test invalid login credentials",
        severity = CSMetaData.Severity.CRITICAL
    )
    @CSTestStep(description = "Perform invalid login test", screenshot = true)
    public void testInvalidLogin() {
        // Start reporting
        reporting.startTest("Invalid login test", "Authentication test with invalid credentials");
        reporting.startStep("Invalid login test", "Authentication");
        
        try {
            // Navigate to login page
            loginPage.navigateToLoginPage();
            
            // Perform login
            boolean loginResult = loginPage.login("invalidUser", "invalidPass");
            
            // Verify login failure
            Assert.assertFalse(loginResult, "Login should fail with invalid credentials");
            Assert.assertTrue(loginPage.isLoginPageDisplayed(), "Login page should still be displayed after failed login");
            Assert.assertFalse(loginPage.getErrorMessage().isEmpty(), "Error message should be displayed");
            
            // End reporting step
            reporting.endStep("Invalid login test", true, 0);
            reporting.endTest("Invalid login test", true);
        } catch (Exception e) {
            // End reporting step with failure
            reporting.endStep("Invalid login test", false, 0, e);
            reporting.endTest("Invalid login test", false);
            throw e;
        }
    }
    
    /**
     * Data-driven login test.
     * 
     * @param testData Test data map
     */
    @Test(dataProvider = "loginData")
    @CSMetaData(
        testId = "LOGIN-003",
        description = "Data-driven login test",
        severity = CSMetaData.Severity.NORMAL
    )
    @CSTestStep(description = "Perform data-driven login test", screenshot = true)
    public void testDataDrivenLogin(Map<String, Object> testData) {
        // Extract test data
        String username = (String) testData.get("username");
        String password = (String) testData.get("password");
        String expected = (String) testData.get("expected");
        
        // Start reporting
        String testName = "Data-driven login test: " + username;
        reporting.startTest(testName, "Data-driven authentication test");
        reporting.startStep(testName, "Authentication");
        
        try {
            // Navigate to login page
            loginPage.navigateToLoginPage();
            
            // Perform login
            boolean loginResult = loginPage.login(username, password);
            
            // Verify login result based on expected outcome
            if ("success".equals(expected)) {
                Assert.assertTrue(loginResult, "Login should be successful with credentials: " + username);
                Assert.assertFalse(loginPage.isLoginPageDisplayed(), "Login page should not be displayed after successful login");
            } else {
                Assert.assertFalse(loginResult, "Login should fail with credentials: " + username);
                Assert.assertTrue(loginPage.isLoginPageDisplayed(), "Login page should still be displayed after failed login");
                Assert.assertFalse(loginPage.getErrorMessage().isEmpty(), "Error message should be displayed");
            }
            
            // End reporting step
            reporting.endStep(testName, true, 0);
            reporting.endTest(testName, true);
        } catch (Exception e) {
            // End reporting step with failure
            reporting.endStep(testName, false, 0, e);
            reporting.endTest(testName, false);
            throw e;
        }
    }
} 