import { Page, TestInfo } from '@playwright/test';
import { CSMetaData } from '../../annotation/CSMetaData';
import { csTest } from '../../test/CSPlaywrightRunner';
import { LoginSteps } from '../steps/LoginSteps';

/**
 * Example test that demonstrates how to use step definitions without a feature file
 * This is a programmatic BDD approach using our test steps
 */
class LoginBDDTest {
    private steps: LoginSteps | null = null;

    /**
     * This method shows how to execute a sequence of BDD steps programmatically
     * It simulates the "Successful login with valid credentials" scenario
     */
    @CSMetaData({
        id: 'BDD-TC-001',
        title: 'Login with valid credentials BDD style',
        priority: 'P0',
        labels: ['smoke', 'authentication', 'bdd']
    })
    public async testSuccessfulLogin(page: Page) {
        // Initialize steps if not already done
        this.steps = this.steps || new LoginSteps(page);
        
        // Execute steps in sequence as defined in a feature file
        await this.steps.navigateToLoginPage();
        await this.steps.enterUsername('validuser');
        await this.steps.enterPassword('password123');
        await this.steps.clickLoginButton();
        await this.steps.verifyRedirectionToDashboard();
        await this.steps.verifyWelcomeMessage();
    }

    /**
     * This method demonstrates how to execute a sequence of BDD steps for a failed login
     * It simulates the "Failed login with invalid credentials" scenario
     */
    @CSMetaData({
        id: 'BDD-TC-002',
        title: 'Login with invalid credentials BDD style',
        priority: 'P1',
        labels: ['authentication', 'bdd']
    })
    public async testFailedLogin(page: Page) {
        // Initialize steps if not already done
        this.steps = this.steps || new LoginSteps(page);
        
        // Execute steps in sequence as defined in a feature file
        await this.steps.navigateToLoginPage();
        await this.steps.enterUsername('invaliduser');
        await this.steps.enterPassword('wrongpassword');
        await this.steps.clickLoginButton();
        await this.steps.verifyErrorMessage('Invalid username or password');
        await this.steps.verifyStayingOnLoginPage();
    }

    /**
     * This method demonstrates data-driven testing with BDD steps
     * It simulates the "Login with different user types" scenario outline
     */
    @CSMetaData({
        id: 'BDD-TC-003',
        title: 'Login with different user types BDD style',
        priority: 'P1',
        labels: ['authentication', 'data-driven', 'bdd']
    })
    public async testLoginWithDifferentUsers(userData: any, page: Page) {
        const { username, password, result, message } = userData;
        
        // Initialize steps if not already done
        this.steps = this.steps || new LoginSteps(page);
        
        // Execute steps with data
        await this.steps.navigateToLoginPage();
        await this.steps.enterUsername(username);
        await this.steps.enterPassword(password);
        await this.steps.clickLoginButton();
        await this.steps.verifyLoginResult(result);
        await this.steps.verifyMessage(message);
    }
}

// Create an instance of the test class
const loginBDDTest = new LoginBDDTest();

// Register the tests using our CSPlaywrightRunner
csTest('Login with valid credentials (BDD)', async (page) => {
    await loginBDDTest.testSuccessfulLogin(page);
});

csTest('Login with invalid credentials (BDD)', async (page) => {
    await loginBDDTest.testFailedLogin(page);
});

// The following test would typically be registered using csTestWithData
// Showing it here for completeness but would need additional setup
/*
csTestWithData(
    'Login with different user types (BDD)',
    [
        { username: 'admin', password: 'admin123', result: 'success', message: 'Welcome, Administrator' },
        { username: 'customer', password: 'customer123', result: 'success', message: 'Welcome back, Customer' },
        { username: 'locked', password: 'locked123', result: 'failure', message: 'Your account has been locked' },
        { username: 'inactive', password: 'inactive123', result: 'failure', message: 'Account is inactive' }
    ],
    async (data, page) => {
        await loginBDDTest.testLoginWithDifferentUsers(data, page);
    }
);
*/ 