package com.cstestforge.framework.selenium.example_test.page;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cstestforge.framework.selenium.annotation.CSFindBy;
import com.cstestforge.framework.selenium.annotation.CSMetaData;
import com.cstestforge.framework.selenium.annotation.CSTestStep;
import com.cstestforge.framework.selenium.core.CSDriverManager;
import com.cstestforge.framework.selenium.element.CSElementInteractionHandler;
import com.cstestforge.framework.selenium.page.CSBasePage;

/**
 * Example login page object using the CSTestForge framework.
 * Demonstrates usage of CSFindBy annotations and self-healing capabilities.
 */
public class LoginPage extends CSBasePage {
    private static final Logger logger = LoggerFactory.getLogger(LoginPage.class);
    
    // Element interaction handler for robust interactions
    private final CSElementInteractionHandler interactionHandler;
    
    // Page URL
    private static final String LOGIN_URL = "https://example.com/login";
    
    // Page elements with CSFindBy annotation for self-healing
    @CSFindBy(id = "username", orLocators = {
            @FindBy(css = "input[name='username']"),
            @FindBy(xpath = "//input[@placeholder='Username']")
    })
    private WebElement usernameField;
    
    @CSFindBy(id = "password", orLocators = {
            @FindBy(css = "input[name='password']"),
            @FindBy(xpath = "//input[@placeholder='Password']"),
            @FindBy(xpath = "//input[@type='password']")
    })
    private WebElement passwordField;
    
    @CSFindBy(id = "loginButton", orLocators = {
            @FindBy(css = "button[type='submit']"),
            @FindBy(xpath = "//button[contains(text(), 'Login')]"),
            @FindBy(xpath = "//input[@value='Login']")
    })
    private WebElement loginButton;
    
    @CSFindBy(css = ".error-message", orLocators = {
            @FindBy(className = "error"),
            @FindBy(xpath = "//div[contains(@class, 'error')]")
    })
    private WebElement errorMessage;
    
    /**
     * Constructor
     */
    public LoginPage() {
        super();
        // Initialize page elements
        PageFactory.initElements(CSDriverManager.getDriver(), this);
        this.interactionHandler = new CSElementInteractionHandler();
        
        // Set page URL in base class
        setPageUrl(LOGIN_URL);
    }
    
    /**
     * Navigate to the login page
     * 
     * @return This page object
     */
    @CSTestStep(description = "Navigate to login page", screenshot = true)
    public LoginPage navigateToLoginPage() {
        logger.info("Navigating to login page: {}", LOGIN_URL);
        navigateToPage();
        return this;
    }
    
    /**
     * Enter username in the username field
     * 
     * @param username Username to enter
     * @return This page object
     */
    @CSTestStep(description = "Enter username")
    @CSMetaData(description = "Enters the username in the login form")
    public LoginPage enterUsername(String username) {
        logger.info("Entering username: {}", username);
        interactionHandler.sendKeys(usernameField, new CharSequence[]{username}, true);
        return this;
    }
    
    /**
     * Enter password in the password field
     * 
     * @param password Password to enter
     * @return This page object
     */
    @CSTestStep(description = "Enter password")
    @CSMetaData(description = "Enters the password in the login form")
    public LoginPage enterPassword(String password) {
        logger.info("Entering password: *****");
        interactionHandler.sendKeys(passwordField, new CharSequence[]{password}, true);
        return this;
    }
    
    /**
     * Click the login button
     * 
     * @return This page object
     */
    @CSTestStep(description = "Click login button", screenshot = true)
    @CSMetaData(description = "Clicks the login button to submit the form")
    public LoginPage clickLoginButton() {
        logger.info("Clicking login button");
        interactionHandler.click(loginButton);
        return this;
    }
    
    /**
     * Perform login with username and password
     * 
     * @param username Username
     * @param password Password
     * @return True if login is successful
     */
    @CSTestStep(description = "Login with credentials", screenshot = true)
    @CSMetaData(description = "Performs the complete login flow")
    public boolean login(String username, String password) {
        navigateToLoginPage();
        enterUsername(username);
        enterPassword(password);
        clickLoginButton();
        
        // Check if error message is displayed
        try {
            if (isElementDisplayed(errorMessage)) {
                logger.warn("Login failed: {}", interactionHandler.getText(errorMessage));
                return false;
            }
        } catch (Exception e) {
            // Error message not found, login might be successful
        }
        
        logger.info("Login successful");
        return true;
    }
    
    /**
     * Check if the login page is displayed
     * 
     * @return True if login page is displayed
     */
    public boolean isLoginPageDisplayed() {
        return isPageDisplayed() && isElementDisplayed(usernameField) && 
               isElementDisplayed(passwordField) && isElementDisplayed(loginButton);
    }
    
    /**
     * Get the error message text
     * 
     * @return Error message text or empty string if not displayed
     */
    public String getErrorMessage() {
        try {
            if (isElementDisplayed(errorMessage)) {
                return interactionHandler.getText(errorMessage);
            }
        } catch (Exception e) {
            logger.debug("Error message element not found");
        }
        return "";
    }
} 