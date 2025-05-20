import { Page } from 'playwright';
import { CSPlaywrightDriver } from './CSPlaywrightDriver';
import { CSElementInteractionHandler } from '../element/CSElementInteractionHandler';
import { CSPlaywrightReporting, Status } from '../reporting/CSPlaywrightReporting';
import { CSSelfHealing } from '../ai/CSSelfHealing';

/**
 * Base page class that all page objects should extend.
 * Provides common functionality and ensures consistency across page objects.
 */
export abstract class CSBasePage {
    protected page!: Page;
    protected elementHandler: CSElementInteractionHandler;
    protected reporting: CSPlaywrightReporting;
    protected selfHealing: CSSelfHealing;
    private initialized: boolean = false;
    private driver: CSPlaywrightDriver;
    
    // Page URL - should be overridden by subclasses
    protected abstract readonly pageUrl: string;
    
    // Page title for validation - should be overridden by subclasses
    protected abstract readonly pageTitle: string;
    
    /**
     * Constructor initializes the handlers
     */
    constructor() {
        this.elementHandler = new CSElementInteractionHandler();
        this.reporting = new CSPlaywrightReporting();
        this.selfHealing = CSSelfHealing.getInstance();
        this.driver = new CSPlaywrightDriver();
    }
    
    /**
     * Initialize the page and its elements.
     * This is done lazily to ensure the page is ready.
     */
    protected async initialize(): Promise<void> {
        if (!this.initialized) {
            this.page = await this.driver.getPage();
            await this.initializeElements();
            this.initialized = true;
        }
    }
    
    /**
     * Initialize element locators - to be implemented by subclasses
     */
    protected abstract initializeElements(): Promise<void>;
    
    /**
     * Navigate to this page using its defined URL
     */
    public async navigateTo(): Promise<void> {
        await this.initialize();
        
        // Log the navigation
        this.reporting.log(`Navigating to ${this.pageUrl}`);
        
        // Perform navigation
        await this.driver.navigateTo(this.pageUrl);
        await this.page.waitForLoadState('networkidle');
        
        // Take screenshot after navigation
        await this.reporting.takeScreenshot(`Navigate_to_${this.constructor.name}`);
        
        // Validate page
        await this.validatePage();
    }
    
    /**
     * Validate that we're on the correct page
     */
    protected async validatePage(): Promise<void> {
        try {
            // Wait for title to be present
            await this.page.waitForFunction(`document.title.includes('${this.pageTitle}')`, { timeout: 10000 })
                .catch(() => {
                    this.reporting.log(`Page title does not match expected: ${this.pageTitle}`, Status.WARN);
                });
            
            // Check URL
            const currentUrl = await this.driver.getCurrentUrl();
            const pageUrl = this.pageUrl.replace(/^\//g, ''); // Remove leading slash for comparison
            
            // Check if current URL contains expected URL or vice versa
            // This allows for flexibility in URL structure
            const isCurrentPage = currentUrl.includes(pageUrl) || pageUrl.includes(currentUrl);
            
            if (!isCurrentPage) {
                this.reporting.log(`Current URL ${currentUrl} does not match expected URL ${this.pageUrl}`, Status.WARN);
            }
            
            // Perform additional validation that may be implemented by child classes
            await this.performAdditionalValidation();
            
        } catch (error) {
            this.reporting.log(`Page validation failed: ${error}`, Status.FAIL);
            throw error;
        }
    }
    
    /**
     * Additional validation that can be implemented by subclasses
     */
    protected async performAdditionalValidation(): Promise<void> {
        // To be implemented by subclasses if needed
    }
    
    /**
     * Take a screenshot with a descriptive name
     */
    public async takeScreenshot(description: string): Promise<void> {
        await this.driver.takeScreenshot(description);
    }
    
    /**
     * Get the page title
     */
    public async getTitle(): Promise<string> {
        return this.driver.getTitle();
    }
    
    /**
     * Get the current URL
     */
    public async getCurrentUrl(): Promise<string> {
        return this.driver.getCurrentUrl();
    }
    
    /**
     * Check if an element is visible
     */
    public async isElementVisible(selector: string): Promise<boolean> {
        try {
            const element = this.page.locator(selector);
            return await element.isVisible();
        } catch (error) {
            return false;
        }
    }
    
    /**
     * Wait for element to be visible with timeout
     */
    public async waitForElement(selector: string, timeoutMs: number = 10000): Promise<boolean> {
        try {
            const element = this.page.locator(selector);
            await element.waitFor({ state: 'visible', timeout: timeoutMs });
            return true;
        } catch (error) {
            return false;
        }
    }
} 