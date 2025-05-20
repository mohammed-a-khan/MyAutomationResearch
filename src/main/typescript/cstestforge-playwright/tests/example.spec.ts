import { Page, expect } from '@playwright/test';
import { csTest, csTestWithData } from '../test/CSPlaywrightRunner';
import { CSMetaData } from '../annotation/CSMetaData';
import { CSTestStep } from '../annotation/CSTestStep';
import { CSDataProvider } from '../annotation/CSDataProvider';
import { LoginPage } from '../example_test/page/LoginPage';

/**
 * Example test suite using CSTestForge features
 */
class ExampleTests {
  /**
   * Simple test with metadata
   */
  @CSMetaData({
    id: 'TC-001',
    title: 'Basic Navigation Test',
    priority: 'P0',
    labels: ['smoke', 'navigation']
  })
  public async testBasicNavigation(page: Page) {
    // Test implementation using CSTestForge features
    await page.goto('/');
    
    // Using test steps for better reporting
    await this.verifyPageTitle(page, 'Welcome to the Demo App');
    await this.clickOnLoginLink(page);
    
    // Take a screenshot for the report
    await page.screenshot({ path: 'test-output/screenshots/homepage.png' });
    
    // Assertions
    expect(page.url()).toContain('/login');
  }
  
  /**
   * Data-driven test example using inline data
   */
  public async testWithMultipleUsers(userData: any, page: Page) {
    const { username, password, expected } = userData;
    
    // Navigate to login page
    await page.goto('/login');
    
    // Enter credentials
    await page.fill('#username', username);
    await page.fill('#password', password);
    
    // Click login button
    await page.click('#login-button');
    
    // Check if login was successful based on expected result
    if (expected === 'success') {
      await expect(page.locator('.welcome-message')).toBeVisible();
    } else {
      await expect(page.locator('.error-message')).toBeVisible();
    }
  }
  
  /**
   * Test with page object model
   */
  @CSMetaData({
    id: 'TC-003',
    title: 'Login with Page Object',
    severity: 'Critical'
  })
  public async testLoginWithPageObject(page: Page) {
    // Create page object
    const loginPage = new LoginPage();
    
    // Navigate to login page
    await loginPage.navigateTo();
    
    // Perform login
    await loginPage.login('testuser', 'password123');
    
    // Verify login was successful
    expect(await loginPage.isLoginSuccessful()).toBeTruthy();
  }
  
  /**
   * Test step for verifying page title
   */
  @CSTestStep({
    description: 'Verify page title is "{0}"',
    takeScreenshot: true
  })
  private async verifyPageTitle(page: Page, expectedTitle: string) {
    const title = await page.title();
    expect(title).toBe(expectedTitle);
  }
  
  /**
   * Test step for clicking on the login link
   */
  @CSTestStep({
    description: 'Click on the login link',
    takeScreenshot: true
  })
  private async clickOnLoginLink(page: Page) {
    await page.click('a[href="/login"]');
  }
}

// Access instance methods from the test class
const testInstance = new ExampleTests();

// Register the basic test
csTest('should navigate to login page', async (page) => {
  await testInstance.testBasicNavigation(page);
});

// Register data-driven test with sample data
csTestWithData(
  'should attempt login with different credentials',
  [
    { username: 'validuser', password: 'validpass', expected: 'success' },
    { username: 'invaliduser', password: 'invalidpass', expected: 'failure' },
    { username: 'lockeduser', password: 'validpass', expected: 'failure' }
  ],
  async (data, page) => {
    await testInstance.testWithMultipleUsers(data, page);
  }
);

// Register the page object test
csTest('should login using page object model', async (page) => {
  await testInstance.testLoginWithPageObject(page);
}); 