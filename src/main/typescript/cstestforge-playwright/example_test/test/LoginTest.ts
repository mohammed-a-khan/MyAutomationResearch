import { test, expect } from '@playwright/test';
import { LoginPage } from '../page/LoginPage';
import { CSBrowserManager } from '../../core/CSBrowserManager';
import { CSPlaywrightReporting, Status } from '../../reporting/CSPlaywrightReporting';

/**
 * Test class for login functionality.
 */
test.describe('Login functionality tests', () => {
    let loginPage: LoginPage;
    let reporting: CSPlaywrightReporting;
    
    test.beforeEach(async ({ }, testInfo) => {
        // Initialize the login page and reporting before each test
        loginPage = new LoginPage();
        reporting = new CSPlaywrightReporting();
        
        // Start test reporting
        reporting.startTest(testInfo.title, `Login test - ${testInfo.title}`);
        
        // Add metadata
        reporting.addMetadata('Browser', process.env.BROWSER || 'chromium');
        reporting.addMetadata('Test File', testInfo.file);
        reporting.addMetadata('Test Case', testInfo.title);
    });
    
    test.afterEach(async ({ }, testInfo) => {
        // End test reporting with appropriate status
        const status = testInfo.status === 'passed' ? Status.PASS : 
                        testInfo.status === 'failed' ? Status.FAIL : 
                        Status.SKIP;
        reporting.endTest(status);
        
        // Generate report
        if (testInfo.retry === testInfo.project.retries) {
            CSPlaywrightReporting.generateReport();
        }
        
        // Clean up after each test
        await CSBrowserManager.getInstance().closeContext();
    });
    
    test('Valid login should succeed', async () => {
        // Arrange
        reporting.startStep('Navigate to login page');
        await loginPage.navigateTo();
        reporting.endStep();
        
        // Act
        reporting.startStep('Enter credentials and login');
        await loginPage.enterUsername('validUser');
        await loginPage.enterPassword('validPassword');
        await loginPage.clickLoginButton();
        reporting.endStep();
        
        // Assert
        reporting.startStep('Verify successful login');
        const isLoginSuccessful = await loginPage.isLoginSuccessful();
        
        if (isLoginSuccessful) {
            reporting.log('Login successful, redirected to dashboard', Status.PASS);
        } else {
            reporting.log('Login failed, not redirected to dashboard', Status.FAIL);
            await loginPage.takeScreenshot('login_failed');
        }
        
        expect(isLoginSuccessful).toBeTruthy();
        reporting.endStep(isLoginSuccessful ? Status.PASS : Status.FAIL);
    });
    
    test('Invalid username should show error message', async () => {
        // Arrange
        await loginPage.navigateTo();
        
        // Act
        await loginPage.enterUsername('invalidUser');
        await loginPage.enterPassword('validPassword');
        await loginPage.clickLoginButton();
        
        // Assert
        const errorMessage = await loginPage.getErrorMessage();
        expect(errorMessage).toContain('Invalid username or password');
        
        // Take screenshot of error
        await loginPage.takeScreenshot('invalid_username_error');
    });
    
    test('Invalid password should show error message', async () => {
        // Arrange
        await loginPage.navigateTo();
        
        // Act
        await loginPage.enterUsername('validUser');
        await loginPage.enterPassword('invalidPassword');
        await loginPage.clickLoginButton();
        
        // Assert
        const errorMessage = await loginPage.getErrorMessage();
        expect(errorMessage).toContain('Invalid username or password');
    });
    
    test('Empty username and password should show validation message', async () => {
        // Arrange
        await loginPage.navigateTo();
        
        // Act
        await loginPage.enterUsername('');
        await loginPage.enterPassword('');
        await loginPage.clickLoginButton();
        
        // Assert
        const errorMessage = await loginPage.getErrorMessage();
        expect(errorMessage).toContain('Please enter username and password');
    });
}); 