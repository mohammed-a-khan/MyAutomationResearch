import { Page, expect } from '@playwright/test';
import { CSTestStep } from '../../bdd/CSTestStep';
import { LoginPage } from '../page/LoginPage';

/**
 * Step definitions for login functionality.
 * Maps Gherkin steps to actual test actions.
 */
export class LoginSteps {
    private page: Page;
    private loginPage: LoginPage;
    
    constructor(page: Page) {
        this.page = page;
        this.loginPage = new LoginPage();
    }
    
    /**
     * Navigate to the login page
     */
    @CSTestStep({
        description: 'I am on the login page',
        screenshot: true
    })
    public async navigateToLoginPage(): Promise<void> {
        await this.loginPage.navigateTo();
    }
    
    /**
     * Enter username
     */
    @CSTestStep({
        description: 'I enter "{0}" as username',
        screenshot: false
    })
    public async enterUsername(username: string): Promise<void> {
        await this.loginPage.enterUsername(username);
    }
    
    /**
     * Enter password
     */
    @CSTestStep({
        description: 'I enter "{0}" as password',
        screenshot: false
    })
    public async enterPassword(password: string): Promise<void> {
        await this.loginPage.enterPassword(password);
    }
    
    /**
     * Click login button
     */
    @CSTestStep({
        description: 'I click the login button',
        screenshot: true
    })
    public async clickLoginButton(): Promise<void> {
        await this.loginPage.clickLoginButton();
    }
    
    /**
     * Verify redirection to dashboard
     */
    @CSTestStep({
        description: 'I should be redirected to the dashboard',
        screenshot: true
    })
    public async verifyRedirectionToDashboard(): Promise<void> {
        await expect(this.page).toHaveURL(/dashboard/);
    }
    
    /**
     * Verify welcome message
     */
    @CSTestStep({
        description: 'I should see a welcome message',
        screenshot: true
    })
    public async verifyWelcomeMessage(): Promise<void> {
        await expect(this.page.locator('.welcome-message')).toBeVisible();
    }
    
    /**
     * Verify error message
     */
    @CSTestStep({
        description: 'I should see an error message "{0}"',
        screenshot: true
    })
    public async verifyErrorMessage(message: string): Promise<void> {
        const errorElement = this.page.locator('.error-message');
        await expect(errorElement).toBeVisible();
        await expect(errorElement).toContainText(message);
    }
    
    /**
     * Verify staying on login page
     */
    @CSTestStep({
        description: 'I should remain on the login page',
        screenshot: true
    })
    public async verifyStayingOnLoginPage(): Promise<void> {
        await expect(this.page).toHaveURL(/login/);
    }
    
    /**
     * Verify login result
     */
    @CSTestStep({
        description: 'the login should be "{0}"',
        screenshot: true
    })
    public async verifyLoginResult(result: string): Promise<void> {
        if (result === 'success') {
            // For successful login, check that we're logged in
            expect(await this.loginPage.isLoginSuccessful()).toBeTruthy();
        } else {
            // For failed login, check that we see an error
            expect(await this.loginPage.isLoginSuccessful()).toBeFalsy();
            const errorMessage = await this.loginPage.getErrorMessage();
            expect(errorMessage).not.toBe('');
        }
    }
    
    /**
     * Verify seeing a specific message
     */
    @CSTestStep({
        description: 'I should see message "{0}"',
        screenshot: true
    })
    public async verifyMessage(message: string): Promise<void> {
        await expect(this.page.locator(`text=${message}`)).toBeVisible();
    }
} 