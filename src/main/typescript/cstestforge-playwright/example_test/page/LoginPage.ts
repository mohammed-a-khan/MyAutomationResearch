import { Locator } from 'playwright';
import { CSBasePage } from '../../core/CSBasePage';
import { CSFindById, CSFindByAI, CSFindByClassName } from '../../annotation/CSFindBy';

/**
 * Page object for the login page.
 * Implements the Page Object Model pattern.
 */
export class LoginPage extends CSBasePage {
    // Define the page URL and title
    protected readonly pageUrl: string = '/login';
    protected readonly pageTitle: string = 'Login Page';
    
    // Define elements using decorators
    @CSFindById('username', { waitForVisible: true })
    private usernameInput!: Promise<Locator>;
    
    @CSFindById('password')
    private passwordInput!: Promise<Locator>;
    
    @CSFindById('login-button')
    private loginButton!: Promise<Locator>;
    
    @CSFindByClassName('error-message')
    private errorMessage!: Promise<Locator>;
    
    // Self-healing element example
    @CSFindByAI('#login-form', 'Login Form')
    private loginForm!: Promise<Locator>;
    
    /**
     * Initialize elements that need special handling
     */
    protected async initializeElements(): Promise<void> {
        // Any additional initialization can go here
    }
    
    /**
     * Enter username in the username field.
     * 
     * @param username The username to enter
     */
    public async enterUsername(username: string): Promise<void> {
        const usernameField = await this.usernameInput;
        await this.elementHandler.fill(usernameField, username);
    }
    
    /**
     * Enter password in the password field.
     * 
     * @param password The password to enter
     */
    public async enterPassword(password: string): Promise<void> {
        const passwordField = await this.passwordInput;
        await this.elementHandler.fill(passwordField, password);
    }
    
    /**
     * Click the login button.
     */
    public async clickLoginButton(): Promise<void> {
        const button = await this.loginButton;
        await this.elementHandler.click(button);
    }
    
    /**
     * Perform login with the given credentials.
     * 
     * @param username The username
     * @param password The password
     */
    public async login(username: string, password: string): Promise<void> {
        await this.navigateTo();
        await this.enterUsername(username);
        await this.enterPassword(password);
        await this.clickLoginButton();
    }
    
    /**
     * Check if the login was successful.
     * 
     * @returns True if login was successful, false otherwise
     */
    public async isLoginSuccessful(): Promise<boolean> {
        try {
            // Check if we're redirected to the dashboard
            return this.page.url().includes('/dashboard');
        } catch (error) {
            return false;
        }
    }
    
    /**
     * Get the error message if login failed.
     * 
     * @returns The error message text
     */
    public async getErrorMessage(): Promise<string> {
        try {
            const error = await this.errorMessage;
            await error.waitFor({ state: 'visible', timeout: 5000 });
            return await this.elementHandler.getText(error);
        } catch (error) {
            return '';
        }
    }
    
    /**
     * Additional validation for login page
     */
    protected async performAdditionalValidation(): Promise<void> {
        // Check if login form is present
        const form = await this.loginForm;
        const isVisible = await form.isVisible();
        if (!isVisible) {
            throw new Error('Login form is not visible');
        }
    }
} 