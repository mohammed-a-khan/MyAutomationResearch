package com.cstestforge.framework.selenium.example_test.bdd.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

import com.cstestforge.framework.selenium.bdd.CSBaseStepDefinition;
import com.cstestforge.framework.selenium.example_test.page.LoginPage;

/**
 * Step definitions for login functionality.
 * Extends CSBaseStepDefinition to inherit common BDD functionality.
 */
public class LoginSteps extends CSBaseStepDefinition {
    
    private LoginPage loginPage;
    
    /**
     * Constructor initializes the page object.
     */
    public LoginSteps() {
        super();
    }
    
    /**
     * Custom setup initializes page objects needed for this step definition.
     */
    @Override
    protected void customSetUp() {
        loginPage = initializePage(LoginPage.class);
    }
    
    /**
     * Navigate to the login page.
     */
    @Given("the user is on the login page")
    public void theUserIsOnTheLoginPage() {
        loginPage.navigateToLoginPage();
        Assert.assertTrue(loginPage.isLoginPageDisplayed(), "Login page is not displayed");
    }
    
    /**
     * Enter username and password.
     * 
     * @param username Username
     * @param password Password
     */
    @When("the user enters username {string} and password {string}")
    public void theUserEntersUsernameAndPassword(String username, String password) {
        loginPage.enterUsername(username);
        loginPage.enterPassword(password);
    }
    
    /**
     * Click the login button.
     */
    @When("the user clicks the login button")
    public void theUserClicksTheLoginButton() {
        loginPage.clickLoginButton();
    }
    
    /**
     * Perform login with username and password.
     * 
     * @param username Username
     * @param password Password
     */
    @When("the user logs in with username {string} and password {string}")
    public void theUserLogsInWithUsernameAndPassword(String username, String password) {
        boolean loginResult = loginPage.login(username, password);
        scenarioContext.set("LOGIN_RESULT", loginResult);
    }
    
    /**
     * Verify successful login.
     */
    @Then("the login should be successful")
    public void theLoginShouldBeSuccessful() {
        Boolean loginResult = scenarioContext.getOrDefault("LOGIN_RESULT", Boolean.class, false);
        Assert.assertTrue(loginResult, "Login was not successful");
        Assert.assertFalse(loginPage.isLoginPageDisplayed(), "Login page is still displayed after login");
    }
    
    /**
     * Verify failed login.
     */
    @Then("the login should fail")
    public void theLoginShouldFail() {
        Boolean loginResult = scenarioContext.getOrDefault("LOGIN_RESULT", Boolean.class, true);
        Assert.assertFalse(loginResult, "Login was successful when it should have failed");
        Assert.assertTrue(loginPage.isLoginPageDisplayed(), "Login page is not displayed after failed login");
    }
    
    /**
     * Verify error message displayed.
     */
    @Then("an error message should be displayed")
    public void anErrorMessageShouldBeDisplayed() {
        String errorMessage = loginPage.getErrorMessage();
        Assert.assertFalse(errorMessage.isEmpty(), "No error message displayed");
        scenarioContext.set("ERROR_MESSAGE", errorMessage);
    }
    
    /**
     * Verify specific error message.
     * 
     * @param expectedMessage Expected error message
     */
    @Then("the error message should contain {string}")
    public void theErrorMessageShouldContain(String expectedMessage) {
        String errorMessage = loginPage.getErrorMessage();
        Assert.assertTrue(errorMessage.contains(expectedMessage), 
                "Error message does not contain expected text. Actual message: " + errorMessage);
    }
} 