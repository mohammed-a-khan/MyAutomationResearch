import { Browser, BrowserContext, Page, chromium, firefox, webkit } from 'playwright';
import { CSBrowserManager } from './CSBrowserManager';
import { CSPlaywrightScreenshotHandler } from '../utils/CSPlaywrightScreenshotHandler';

/**
 * Manages Playwright browser instances and pages.
 * Core driver class that provides browser control similar to WebDriver in Selenium.
 */
export class CSPlaywrightDriver {
    private page: Page | null = null;
    private browserManager: CSBrowserManager;
    private sessionId: string;

    /**
     * Creates an instance of the driver
     * @param sessionId Optional session identifier for multi-browser testing
     */
    constructor(sessionId: string = 'default') {
        this.browserManager = CSBrowserManager.getInstance();
        this.sessionId = sessionId;
    }

    /**
     * Get the current Playwright page
     * @returns The Playwright Page object
     */
    public async getPage(): Promise<Page> {
        if (!this.page) {
            this.page = await this.browserManager.getPage(this.sessionId);
        }
        return this.page;
    }

    /**
     * Get the browser context
     * @returns The BrowserContext object
     */
    public async getContext(): Promise<BrowserContext> {
        return this.browserManager.getContext(this.sessionId);
    }
    
    /**
     * Get the browser instance
     * @returns The Browser object
     */
    public async getBrowser(): Promise<Browser> {
        return this.browserManager.getBrowser();
    }

    /**
     * Navigate to a specific URL
     * @param url The URL to navigate to
     */
    public async navigateTo(url: string): Promise<void> {
        const page = await this.getPage();
        await page.goto(url);
    }

    /**
     * Refresh the current page
     */
    public async refresh(): Promise<void> {
        const page = await this.getPage();
        await page.reload();
    }

    /**
     * Get the current URL
     * @returns The current URL
     */
    public async getCurrentUrl(): Promise<string> {
        const page = await this.getPage();
        return page.url();
    }

    /**
     * Get the current page title
     * @returns The page title
     */
    public async getTitle(): Promise<string> {
        const page = await this.getPage();
        return page.title();
    }

    /**
     * Take a screenshot
     * @param name Optional name for the screenshot
     * @param path Optional path to save the screenshot
     * @param fullPage Whether to take a full page screenshot
     * @returns Path to the saved screenshot or buffer data if path not provided
     */
    public async takeScreenshot(name?: string, path?: string, fullPage: boolean = false): Promise<string | Buffer | null> {
        const page = await this.getPage();
        
        // If path provided, just take a direct screenshot
        if (path) {
            return await page.screenshot({ path, fullPage });
        }
        
        // Otherwise use the handler for more advanced screenshot management
        return await CSPlaywrightScreenshotHandler.takeScreenshot(
            page, 
            name || `screenshot_${Date.now()}`,
            fullPage
        );
    }
    
    /**
     * Take a screenshot of a specific element
     * @param selector Selector for the element
     * @param name Name for the screenshot
     * @returns Path to the saved screenshot or null if failed
     */
    public async takeElementScreenshot(selector: string, name: string): Promise<string | null> {
        const page = await this.getPage();
        return await CSPlaywrightScreenshotHandler.takeElementScreenshot(page, selector, name);
    }

    /**
     * Execute JavaScript in the browser
     * @param script JavaScript to execute
     * @param args Arguments to pass to the script
     * @returns Result of the script execution
     */
    public async executeScript<T>(script: string, ...args: any[]): Promise<T> {
        const page = await this.getPage();
        return await page.evaluate(script, ...args);
    }

    /**
     * Wait for a specified time
     * @param ms Time to wait in milliseconds
     */
    public async wait(ms: number): Promise<void> {
        await new Promise(resolve => setTimeout(resolve, ms));
    }

    /**
     * Navigate back in browser history
     */
    public async back(): Promise<void> {
        const page = await this.getPage();
        await page.goBack();
    }

    /**
     * Navigate forward in browser history
     */
    public async forward(): Promise<void> {
        const page = await this.getPage();
        await page.goForward();
    }

    /**
     * Close the current browser instance
     */
    public async close(): Promise<void> {
        await this.browserManager.closeContext(this.sessionId);
        this.page = null;
    }

    /**
     * Create a new tab/page
     * @returns The new CSPlaywrightDriver instance
     */
    public async newTab(): Promise<CSPlaywrightDriver> {
        const newSessionId = `${this.sessionId}_${Date.now()}`;
        return new CSPlaywrightDriver(newSessionId);
    }

    /**
     * Get all available cookies
     * @returns Array of cookies
     */
    public async getCookies(): Promise<any[]> {
        const context = await this.getContext();
        return await context.cookies();
    }

    /**
     * Set a cookie
     * @param cookie Cookie object to set
     */
    public async setCookie(cookie: any): Promise<void> {
        const context = await this.getContext();
        await context.addCookies([cookie]);
    }

    /**
     * Clear all cookies
     */
    public async clearCookies(): Promise<void> {
        const context = await this.getContext();
        await context.clearCookies();
    }

    /**
     * Get the session ID
     * @returns The session ID
     */
    public getSessionId(): string {
        return this.sessionId;
    }
} 