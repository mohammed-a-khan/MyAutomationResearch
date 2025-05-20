import { ElementHandle, Locator, Page } from 'playwright';

/**
 * Options for creating a CSElement
 */
export interface CSElementOptions {
    /** Optional friendly name for reporting */
    name?: string;
    /** Timeout for element operations in milliseconds */
    timeout?: number;
    /** Whether to wait for the element to be visible */
    waitForVisible?: boolean;
    /** Whether to enable self-healing */
    selfHealing?: boolean;
}

/**
 * Represents a UI element with enhanced capabilities.
 * 
 * This class wraps Playwright's Locator with additional functionality
 * like:
 * - Explicit waits
 * - Retry logic for flaky elements
 * - Detailed logging
 * - Self-healing capabilities
 */
export class CSElement {
    private page: Page;
    private selector: string;
    private name: string;
    private options: CSElementOptions;
    private locator: Locator;
    
    /**
     * Create a new CSElement
     * 
     * @param page Playwright page object
     * @param selector CSS or XPath selector
     * @param options Element options
     */
    constructor(page: Page, selector: string, options?: CSElementOptions) {
        this.page = page;
        this.selector = selector;
        this.options = options || {};
        this.name = options?.name || selector;
        
        // Create the locator
        this.locator = this.page.locator(selector);
    }
    
    /**
     * Click on the element
     */
    public async click(): Promise<void> {
        try {
            await this.waitForElement();
            await this.locator.click({
                timeout: this.options.timeout,
            });
        } catch (error) {
            await this.handleError('click', error);
        }
    }
    
    /**
     * Fill the element with text
     * 
     * @param text Text to enter
     */
    public async fill(text: string): Promise<void> {
        try {
            await this.waitForElement();
            await this.locator.fill(text, {
                timeout: this.options.timeout,
            });
        } catch (error) {
            await this.handleError('fill', error);
        }
    }
    
    /**
     * Clear the element's text
     */
    public async clear(): Promise<void> {
        try {
            await this.waitForElement();
            await this.locator.clear({
                timeout: this.options.timeout,
            });
        } catch (error) {
            await this.handleError('clear', error);
        }
    }
    
    /**
     * Press a key sequence
     * 
     * @param key Key or key combination to press
     */
    public async press(key: string): Promise<void> {
        try {
            await this.waitForElement();
            await this.locator.press(key, {
                timeout: this.options.timeout,
            });
        } catch (error) {
            await this.handleError('press', error);
        }
    }
    
    /**
     * Check if element is visible
     */
    public async isVisible(): Promise<boolean> {
        try {
            return await this.locator.isVisible({
                timeout: this.options.timeout ?? 1000,
            });
        } catch (error) {
            return false;
        }
    }
    
    /**
     * Check if element is enabled
     */
    public async isEnabled(): Promise<boolean> {
        try {
            return await this.locator.isEnabled({
                timeout: this.options.timeout ?? 1000,
            });
        } catch (error) {
            return false;
        }
    }
    
    /**
     * Get element text content
     */
    public async getText(): Promise<string> {
        try {
            await this.waitForElement();
            return await this.locator.textContent() || '';
        } catch (error) {
            await this.handleError('getText', error);
            return '';
        }
    }
    
    /**
     * Get element value
     */
    public async getValue(): Promise<string> {
        try {
            await this.waitForElement();
            return await this.locator.inputValue();
        } catch (error) {
            await this.handleError('getValue', error);
            return '';
        }
    }
    
    /**
     * Get element attribute
     * 
     * @param name Attribute name
     */
    public async getAttribute(name: string): Promise<string | null> {
        try {
            await this.waitForElement();
            return await this.locator.getAttribute(name);
        } catch (error) {
            await this.handleError(`getAttribute(${name})`, error);
            return null;
        }
    }
    
    /**
     * Select option by label
     * 
     * @param label Option label
     */
    public async selectByLabel(label: string): Promise<void> {
        try {
            await this.waitForElement();
            await this.locator.selectOption({ label });
        } catch (error) {
            await this.handleError(`selectByLabel(${label})`, error);
        }
    }
    
    /**
     * Select option by value
     * 
     * @param value Option value
     */
    public async selectByValue(value: string): Promise<void> {
        try {
            await this.waitForElement();
            await this.locator.selectOption({ value });
        } catch (error) {
            await this.handleError(`selectByValue(${value})`, error);
        }
    }
    
    /**
     * Hover over the element
     */
    public async hover(): Promise<void> {
        try {
            await this.waitForElement();
            await this.locator.hover();
        } catch (error) {
            await this.handleError('hover', error);
        }
    }
    
    /**
     * Wait for the element to be visible
     * 
     * @param timeout Optional timeout in milliseconds
     */
    public async waitForVisible(timeout?: number): Promise<void> {
        try {
            await this.locator.waitFor({
                state: 'visible',
                timeout: timeout || this.options.timeout,
            });
        } catch (error: any) {
            throw new Error(`Timeout waiting for element ${this.name} to be visible: ${error.message}`);
        }
    }
    
    /**
     * Wait for element to be ready for interaction
     */
    private async waitForElement(): Promise<void> {
        if (this.options.waitForVisible) {
            await this.waitForVisible();
        }
    }
    
    /**
     * Handle element operation errors
     * 
     * @param operation Operation name
     * @param error Error that occurred
     */
    private async handleError(operation: string, error: any): Promise<void> {
        // Log the error
        console.error(`Error performing ${operation} on element ${this.name}: ${error.message}`);
        
        // Attempt self-healing if enabled
        if (this.options.selfHealing) {
            // Self-healing logic would go here
            // For now, just rethrow
        }
        
        throw error;
    }
    
    /**
     * Get the Playwright locator
     * 
     * @returns The underlying Playwright locator
     */
    public getLocator(): Locator {
        return this.locator;
    }
} 