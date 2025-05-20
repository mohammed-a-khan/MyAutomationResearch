import { test, expect } from '@playwright/test';
import { LoginPage } from '../../page/LoginPage';
import { CSBrowserManager } from '../../../core/CSBrowserManager';
import { CSPlaywrightReporting, Status } from '../../../reporting/CSPlaywrightReporting';

/**
 * BDD step definitions for login functionality.
 * These steps can be used in Cucumber scenarios.
 */
export class LoginSteps {
    private loginPage: LoginPage;
    private username: string = '';
    private password: string = '';
    private reporting: CSPlaywrightReporting;
    
    /**
     * Constructor initializes the page objects.
     */
    constructor() {
        this.loginPage = new LoginPage();
        this.reporting = new CSPlaywrightReporting();
    }

    /**
     * Step: Navigate to the login page
     */
    public async givenUserIsOnLoginPage(): Promise<void> {
        this.reporting.startStep('Given user is on login page');
        await this.loginPage.navigateTo();
        this.reporting.endStep();
    }

    /**
     * Step: Enter username
     * @param username The username to enter
     */
    public async whenUserEntersUsername(username: string): Promise<void> {
        this.reporting.startStep(`When user enters username: ${username}`);
        this.username = username;
        await this.loginPage.enterUsername(username);
        this.reporting.endStep();
    }

    /**
     * Step: Enter password
     * @param password The password to enter
     */
    public async whenUserEntersPassword(password: string): Promise<void> {
        this.reporting.startStep('When user enters password');
        this.password = password;
        await this.loginPage.enterPassword(password);
        this.reporting.endStep();
    }

    /**
     * Step: Click the login button
     */
    public async whenUserClicksLoginButton(): Promise<void> {
        this.reporting.startStep('When user clicks login button');
        await this.loginPage.clickLoginButton();
        this.reporting.endStep();
    }

    /**
     * Step: Verify successful login
     */
    public async thenUserShouldBeLoggedIn(): Promise<void> {
        this.reporting.startStep('Then user should be logged in');
        const isSuccessful = await this.loginPage.isLoginSuccessful();
        
        if (isSuccessful) {
            this.reporting.log('User successfully logged in', Status.PASS);
            this.reporting.endStep(Status.PASS);
        } else {
            this.reporting.log('User failed to log in', Status.FAIL);
            await this.loginPage.takeScreenshot('login_failed');
            this.reporting.endStep(Status.FAIL);
        }
        
        expect(isSuccessful).toBeTruthy();
    }

    /**
     * Step: Verify failed login with error message
     * @param expectedError The expected error message
     */
    public async thenUserShouldSeeErrorMessage(expectedError: string): Promise<void> {
        this.reporting.startStep(`Then user should see error message: ${expectedError}`);
        const errorMessage = await this.loginPage.getErrorMessage();
        
        if (errorMessage.includes(expectedError)) {
            this.reporting.log(`Error message displayed correctly: ${errorMessage}`, Status.PASS);
            this.reporting.endStep(Status.PASS);
        } else {
            this.reporting.log(`Expected error message "${expectedError}" but got "${errorMessage}"`, Status.FAIL);
            await this.loginPage.takeScreenshot('error_message_mismatch');
            this.reporting.endStep(Status.FAIL);
        }
        
        expect(errorMessage).toContain(expectedError);
    }

    /**
     * Step: Perform login with given credentials
     * @param username The username
     * @param password The password
     */
    public async whenUserLogsInWithCredentials(username: string, password: string): Promise<void> {
        this.reporting.startStep(`When user logs in with username: ${username}`);
        await this.loginPage.login(username, password);
        this.reporting.endStep();
    }
    
    /**
     * Clean up after scenario
     */
    public async cleanUp(): Promise<void> {
        // Close browser context to clean up resources
        await CSBrowserManager.getInstance().closeContext();
    }
} 