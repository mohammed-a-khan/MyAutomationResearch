import { ElementHandle, Locator, Page } from 'playwright';
import { CSPlaywrightDriver } from '../core/CSPlaywrightDriver';
import { CSBrowserManager } from '../core/CSBrowserManager';

/**
 * Handles all element interactions with smart retry and fallback mechanisms.
 * Provides reliable element operations even in dynamic and challenging UI scenarios.
 */
export class CSElementInteractionHandler {
    private page: Page | null = null;
    private driver: CSPlaywrightDriver;
    private readonly retryCount: number;
    private readonly retryDelayMs: number;
    
    // Default values for retry settings
    private static readonly DEFAULT_RETRY_COUNT = 3;
    private static readonly DEFAULT_RETRY_DELAY_MS = 500;
    
    /**
     * Constructor with default retry settings.
     */
    constructor(
        retryCount?: number,
        retryDelayMs?: number
    ) {
        // Get config values from browser manager
        const config = CSBrowserManager.getInstance().getConfigValue('retryCount');
        this.retryCount = retryCount ?? (parseInt(config, 10) || CSElementInteractionHandler.DEFAULT_RETRY_COUNT);
        
        const delayConfig = CSBrowserManager.getInstance().getConfigValue('retryDelay');
        this.retryDelayMs = retryDelayMs ?? (parseInt(delayConfig, 10) || CSElementInteractionHandler.DEFAULT_RETRY_DELAY_MS);
        
        // Create driver instance
        this.driver = new CSPlaywrightDriver();
    }
    
    /**
     * Get the Playwright Page instance
     */
    private async getPage(): Promise<Page> {
        if (!this.page) {
            this.page = await this.driver.getPage();
        }
        return this.page;
    }
    
    /**
     * Click on a web element with fallback strategies.
     * 
     * @param locator The element locator 
     */
    public async click(locator: Locator): Promise<void> {
        await this.executeWithRetry(locator, 'click', async (element) => {
            try {
                // First attempt: Direct click
                await element.click();
            } catch (error) {
                console.debug('Direct click failed, trying alternative methods:', error);
                await this.clickWithAlternativeMethod(element, error);
            }
        });
    }
    
    /**
     * Click using alternative methods when standard click fails.
     * 
     * @param locator Element to click
     * @param originalError Original error from click attempt
     */
    private async clickWithAlternativeMethod(locator: Locator, originalError: any): Promise<void> {
        try {
            // First, try to ensure element is in view and clickable
            await this.scrollIntoView(locator);
            
            try {
                // Try clicking after scroll
                await locator.click({ force: false, timeout: 5000 });
                return;
            } catch (error) {
                console.debug('Click after scroll failed:', error);
            }
            
            // Try force click
            try {
                await locator.click({ force: true });
                return;
            } catch (error) {
                console.debug('Force click failed:', error);
            }
            
            // Try JavaScript click as last resort
            try {
                const page = await this.getPage();
                const elementHandle = await locator.elementHandle();
                if (elementHandle) {
                    await page.evaluate(({ el }: { el: HTMLElement }) => {
                        el.click();
                    }, { el: elementHandle as any });
                } else {
                    throw new Error('Failed to get element handle');
                }
            } catch (error) {
                console.debug('JavaScript click failed:', error);
                throw originalError;
            }
        } catch (error) {
            if (error === originalError) {
                throw new Error('All click methods failed: ' + (originalError.message || 'Unknown error'));
            }
            throw error;
        }
    }
    
    /**
     * Type text in an element with fallback strategies.
     * 
     * @param locator Element to type in
     * @param text Text to type
     * @param clearFirst Whether to clear the field first
     */
    public async fill(locator: Locator, text: string, clearFirst: boolean = true): Promise<void> {
        await this.executeWithRetry(locator, 'fill', async (element) => {
            try {
                if (clearFirst) {
                    await this.clearElement(element);
                }
                
                // First attempt: Direct fill
                await element.fill(text);
            } catch (error) {
                console.debug('Direct fill failed, trying alternative methods:', error);
                await this.fillWithAlternativeMethod(element, text, error);
            }
        });
    }
    
    /**
     * Type text using alternative methods when standard fill fails.
     * 
     * @param locator Element to type in
     * @param text Text to type
     * @param originalError Original error from fill attempt
     */
    private async fillWithAlternativeMethod(locator: Locator, text: string, originalError: any): Promise<void> {
        try {
            // First, try to ensure element is in view
            await this.scrollIntoView(locator);
            
            // Try clicking first then type
            try {
                await locator.click();
                await locator.fill(text);
                return;
            } catch (error) {
                console.debug('Click and fill failed:', error);
            }
            
            // Try typing character by character
            try {
                await locator.click();
                await locator.pressSequentially(text);
                return;
            } catch (error) {
                console.debug('Press sequentially failed:', error);
            }
            
            // Try JavaScript to set value as last resort
            try {
                const page = await this.getPage();
                const elementHandle = await locator.elementHandle();
                if (elementHandle) {
                    await page.evaluate(({ el, val }: { el: HTMLInputElement, val: string }) => {
                        el.value = val;
                        const event = new Event('change', { bubbles: true });
                        el.dispatchEvent(event);
                    }, { el: elementHandle as any, val: text });
                } else {
                    throw new Error('Failed to get element handle');
                }
            } catch (error) {
                console.debug('JavaScript value set failed:', error);
                throw originalError;
            }
        } catch (error) {
            if (error === originalError) {
                throw new Error('All fill methods failed: ' + (originalError.message || 'Unknown error'));
            }
            throw error;
        }
    }
    
    /**
     * Clear an input element with fallback strategies.
     * 
     * @param locator Element to clear
     */
    public async clearElement(locator: Locator): Promise<void> {
        await this.executeWithRetry(locator, 'clear', async (element) => {
            try {
                // First attempt: Direct clear
                await element.clear();
            } catch (error) {
                console.debug('Direct clear failed, trying alternative methods:', error);
                
                try {
                    // Try triple-clicking to select all text
                    await element.click({ clickCount: 3 });
                    await element.press('Backspace');
                } catch (e) {
                    console.debug('Triple-click clear failed:', e);
                    
                    // Try JavaScript clear as last resort
                    try {
                        const page = await this.getPage();
                        const elementHandle = await element.elementHandle();
                        if (elementHandle) {
                            await page.evaluate(({ el }: { el: HTMLInputElement }) => {
                                el.value = '';
                                const event = new Event('change', { bubbles: true });
                                el.dispatchEvent(event);
                            }, { el: elementHandle as any });
                        }
                    } catch (e2) {
                        console.debug('JavaScript clear failed:', e2);
                        throw error; // Throw original error
                    }
                }
            }
        });
    }
    
    /**
     * Select dropdown option by visible text.
     * 
     * @param locator Select element
     * @param visibleText Text to select
     */
    public async selectByVisibleText(locator: Locator, visibleText: string): Promise<void> {
        await this.executeWithRetry(locator, 'selectByVisibleText', async (element) => {
            try {
                // Playwright's built-in selectOption can select by label
                await element.selectOption({ label: visibleText });
            } catch (error) {
                console.debug('Standard select failed, trying alternative methods:', error);
                
                try {
                    // Try clicking the dropdown first
                    await element.click();
                    
                    // Then try select by label again
                    await element.selectOption({ label: visibleText });
                } catch (e) {
                    console.debug('Click and select failed:', e);
                    
                    // Try directly clicking the option as last resort
                    try {
                        const page = await this.getPage();
                        const optionSelector = `option:has-text("${visibleText.replace(/"/g, '\\"')}")`;
                        const option = element.locator(optionSelector);
                        await option.click();
                    } catch (e2) {
                        console.debug('Option click failed:', e2);
                        throw error; // Throw original error
                    }
                }
            }
        });
    }
    
    /**
     * Select dropdown option by value.
     * 
     * @param locator Select element
     * @param value Value to select
     */
    public async selectByValue(locator: Locator, value: string): Promise<void> {
        await this.executeWithRetry(locator, 'selectByValue', async (element) => {
            try {
                // Playwright's built-in selectOption can select by value
                await element.selectOption({ value });
            } catch (error) {
                console.debug('Standard select failed, trying alternative methods:', error);
                
                try {
                    // Try clicking the dropdown first
                    await element.click();
                    
                    // Then try select by value again
                    await element.selectOption({ value });
                } catch (e) {
                    console.debug('Click and select failed:', e);
                    
                    // Try directly clicking the option as last resort
                    try {
                        const page = await this.getPage();
                        const optionSelector = `option[value="${value.replace(/"/g, '\\"')}"]`;
                        const option = element.locator(optionSelector);
                        await option.click();
                    } catch (e2) {
                        console.debug('Option click failed:', e2);
                        throw error; // Throw original error
                    }
                }
            }
        });
    }
    
    /**
     * Set checkbox or radio button state.
     * 
     * @param locator Checkbox or radio element
     * @param check Whether to check or uncheck
     */
    public async setCheckboxState(locator: Locator, check: boolean): Promise<void> {
        await this.executeWithRetry(locator, check ? 'check' : 'uncheck', async (element) => {
            try {
                // First check the current state
                const isChecked = await element.isChecked();
                
                // Only act if the current state differs from desired state
                if (isChecked !== check) {
                    // Use Playwright's check/uncheck methods
                    if (check) {
                        await element.check();
                    } else {
                        await element.uncheck();
                    }
                    
                    // Verify change took effect
                    const newState = await element.isChecked();
                    if (newState !== check) {
                        throw new Error("Checkbox state didn't change");
                    }
                }
            } catch (error) {
                console.debug('Direct checkbox interaction failed, trying alternative methods:', error);
                
                // Try JavaScript as fallback
                try {
                    const page = await this.getPage();
                    const elementHandle = await element.elementHandle();
                    if (elementHandle) {
                        await page.evaluate(({ el, state }: { el: HTMLInputElement, state: boolean }) => {
                            el.checked = state;
                            const event = new Event('change', { bubbles: true });
                            el.dispatchEvent(event);
                        }, { el: elementHandle as any, state: check });
                    }
                } catch (e) {
                    console.debug('JavaScript checkbox setting failed:', e);
                    throw error; // Throw original error
                }
            }
        });
    }
    
    /**
     * Hover over an element with fallback strategies.
     * 
     * @param locator Element to hover over
     */
    public async hover(locator: Locator): Promise<void> {
        await this.executeWithRetry(locator, 'hover', async (element) => {
            try {
                // First attempt: Direct hover
                await element.hover();
            } catch (error) {
                console.debug('Direct hover failed, trying alternative methods:', error);
                
                // Try JavaScript hover simulation as fallback
                try {
                    const page = await this.getPage();
                    const elementHandle = await element.elementHandle();
                    if (elementHandle) {
                        await page.evaluate(({ el }: { el: HTMLElement }) => {
                            const event = new MouseEvent('mouseover', {
                                view: window,
                                bubbles: true,
                                cancelable: true
                            });
                            el.dispatchEvent(event);
                        }, { el: elementHandle as any });
                    }
                } catch (e) {
                    console.debug('JavaScript hover failed:', e);
                    throw error; // Throw original error
                }
            }
        });
    }
    
    /**
     * Wait for element text to match a specific value.
     * 
     * @param locator Element to check
     * @param expectedText Expected text
     * @param timeoutSeconds Timeout in seconds
     * @return True if text matched within timeout
     */
    public async waitForText(locator: Locator, expectedText: string, timeoutSeconds: number): Promise<boolean> {
        try {
            await locator.waitFor({ state: 'visible', timeout: timeoutSeconds * 1000 });
            await locator.filter({ hasText: expectedText }).waitFor({ timeout: timeoutSeconds * 1000 });
            return true;
        } catch (error) {
            return false;
        }
    }
    
    /**
     * Scroll element into view.
     * 
     * @param locator Element to scroll to
     */
    public async scrollIntoView(locator: Locator): Promise<void> {
        try {
            await locator.scrollIntoViewIfNeeded();
            
            // Short wait for scroll to complete
            await new Promise(resolve => setTimeout(resolve, 300));
        } catch (error) {
            console.debug('Failed to scroll to element:', error);
        }
    }
    
    /**
     * Execute an action on an element with retry logic.
     * 
     * @param locator Element to operate on
     * @param operationName Name of the operation (for logging)
     * @param action Action to perform
     */
    public async executeWithRetry(
        locator: Locator,
        operationName: string,
        action: (element: Locator) => Promise<void>
    ): Promise<void> {
        let lastError: Error | null = null;
        
        for (let attempt = 0; attempt <= this.retryCount; attempt++) {
            try {
                if (attempt > 0) {
                    console.debug(`Retrying ${operationName} operation, attempt ${attempt}/${this.retryCount}`);
                    await this.sleep(this.retryDelayMs);
                }
                
                await action(locator);
                return; // Success
            } catch (error: any) {
                lastError = error;
                
                // Check if we should retry based on error type
                const shouldRetry = this.shouldRetryError(error);
                
                if (!shouldRetry || attempt >= this.retryCount) {
                    break;
                }
            }
        }
        
        // If we got here, all attempts failed
        if (lastError) {
            throw new Error(`Operation ${operationName} failed after ${this.retryCount + 1} attempts: ${lastError.message}`);
        }
    }
    
    /**
     * Determine if we should retry based on the error
     */
    private shouldRetryError(error: any): boolean {
        // Common Playwright error messages that are retriable
        const retriableSubstrings = [
            'Element is not visible',
            'Element is detached from document',
            'Element is covered by another element',
            'Element is not attached to the DOM',
            'Element is obscured',
            'Target closed',
            'Navigation failed',
            'timeout',
            'Execution context was destroyed',
            'frame was detached'
        ];
        
        if (!error || !error.message) {
            return false;
        }
        
        const errorMessage = error.message.toLowerCase();
        return retriableSubstrings.some(substring => 
            errorMessage.includes(substring.toLowerCase())
        );
    }
    
    /**
     * Get text from an element with retry and fallback.
     * 
     * @param locator Element to get text from
     * @return Element text
     */
    public async getText(locator: Locator): Promise<string> {
        return this.executeWithRetryAndResult(locator, 'getText', async (element) => {
            try {
                // First attempt: Direct textContent
                const text = await element.textContent();
                if (text !== null && text.trim() !== '') {
                    return text.trim();
                }
                
                // Check input value as fallback
                const value = await element.inputValue();
                if (value !== null && value !== '') {
                    return value;
                }
                
                // Check inner text as another fallback
                const innerText = await element.innerText();
                if (innerText !== null && innerText !== '') {
                    return innerText;
                }
                
                return text || '';
            } catch (error) {
                console.debug('getText failed, trying JavaScript:', error);
                
                // Try JavaScript as fallback
                try {
                    const page = await this.getPage();
                    const elementHandle = await element.elementHandle();
                    if (elementHandle) {
                        const jsResult = await page.evaluate(({ el }: { el: HTMLElement }) => {
                            if ('value' in el && (el as HTMLInputElement).value !== undefined && 
                                (el as HTMLInputElement).value !== '') {
                                return (el as HTMLInputElement).value;
                            } else if (el.textContent !== undefined) {
                                return el.textContent;
                            } else {
                                return el.innerText;
                            }
                        }, { el: elementHandle as any });
                        
                        return jsResult || '';
                    }
                } catch (e) {
                    console.debug('JavaScript getText failed:', e);
                    throw error; // Throw original error
                }
                
                return '';
            }
        });
    }
    
    /**
     * Execute a function on an element with retry logic and return a result.
     * 
     * @param locator Element to operate on
     * @param operationName Name of the operation (for logging)
     * @param func Function to execute
     * @return Result of the function
     */
    private async executeWithRetryAndResult<T>(
        locator: Locator,
        operationName: string,
        func: (element: Locator) => Promise<T>
    ): Promise<T> {
        let lastError: Error | null = null;
        
        for (let attempt = 0; attempt <= this.retryCount; attempt++) {
            try {
                if (attempt > 0) {
                    console.debug(`Retrying ${operationName} operation, attempt ${attempt}/${this.retryCount}`);
                    await this.sleep(this.retryDelayMs);
                }
                
                return await func(locator);
            } catch (error: any) {
                lastError = error;
                
                // Check if we should retry based on error type
                const shouldRetry = this.shouldRetryError(error);
                
                if (!shouldRetry || attempt >= this.retryCount) {
                    break;
                }
            }
        }
        
        // If we got here, all attempts failed
        if (lastError) {
            throw new Error(`Operation ${operationName} failed after ${this.retryCount + 1} attempts: ${lastError.message}`);
        }
        
        throw new Error(`Operation ${operationName} failed with unknown error`);
    }
    
    /**
     * Thread sleep with exception handling.
     * 
     * @param milliseconds Sleep time in milliseconds
     */
    private async sleep(milliseconds: number): Promise<void> {
        return new Promise(resolve => setTimeout(resolve, milliseconds));
    }
} 