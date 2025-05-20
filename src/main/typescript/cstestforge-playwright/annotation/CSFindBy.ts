import { Locator } from 'playwright';
import { CSSelfHealing } from '../ai/CSSelfHealing';
import { CSPlaywrightDriver } from '../core/CSPlaywrightDriver';

/**
 * Selectors configuration for CSFindBy decorator
 */
export interface CSFindByConfig {
    css?: string;
    xpath?: string;
    id?: string;
    name?: string;
    text?: string;
    testId?: string;
    className?: string;
    tag?: string;
    role?: string;
    label?: string;
    title?: string;
    placeholder?: string;
    selfHealing?: boolean;
    friendlyName?: string;
    timeout?: number;
    waitForVisible?: boolean;
}

/**
 * Property descriptor for element decorators
 */
interface ElementPropertyDescriptor extends PropertyDescriptor {
    initializer?: () => any;
}

/**
 * Builds a selector string from CSFindByConfig options
 * 
 * @param config Configuration options
 * @returns Playwright-compatible selector string
 */
function buildSelector(config: CSFindByConfig): string {
    if (config.css) return config.css;
    if (config.xpath) return `xpath=${config.xpath}`;
    if (config.id) return `#${config.id}`;
    if (config.name) return `[name="${config.name}"]`;
    if (config.text) return `text=${config.text}`;
    if (config.testId) return `[data-testid="${config.testId}"]`;
    if (config.className) return `.${config.className}`;
    if (config.tag) return config.tag;
    if (config.role) return `[role="${config.role}"]`;
    if (config.label) return `[aria-label="${config.label}"]`;
    if (config.title) return `[title="${config.title}"]`;
    if (config.placeholder) return `[placeholder="${config.placeholder}"]`;
    
    throw new Error('CSFindBy decorator requires at least one selector property');
}

/**
 * Decorator factory for locating elements using various strategies.
 * Similar to Java Selenium's @FindBy annotation.
 * 
 * @param config Element location configuration
 * @returns Property decorator function
 */
export function CSFindBy(config: CSFindByConfig) {
    return function(target: any, propertyKey: string) {
        let descriptor: ElementPropertyDescriptor = {
            configurable: true,
            enumerable: true,
            
            get: function() {
                // Build the selector from the config
                const selector = buildSelector(config);
                
                // Get the page instance
                return (async (): Promise<Locator> => {
                    const driver = new CSPlaywrightDriver();
                    const page = await driver.getPage();
                    
                    // Use self-healing if specified or create standard locator
                    let locator: Locator;
                    if (config.selfHealing) {
                        locator = await CSSelfHealing.getInstance().findElement(
                            selector, 
                            config.friendlyName || propertyKey
                        );
                    } else {
                        locator = page.locator(selector);
                    }
                    
                    // Apply timeout if specified
                    if (config.timeout) {
                        // Playwright doesn't have a setTimeout method directly on Locator
                        // Use timeout option through waitFor if needed
                        if (config.waitForVisible) {
                            await locator.waitFor({ state: 'visible', timeout: config.timeout });
                        }
                    } else if (config.waitForVisible) {
                        await locator.waitFor({ state: 'visible' });
                    }
                    
                    return locator;
                })();
            }
        };
        
        // Define the property on the target object
        Object.defineProperty(target, propertyKey, descriptor);
    };
}

/**
 * Convenience decorators for common locator strategies
 */
export const CSFindByCss = (selector: string, options: Partial<CSFindByConfig> = {}) =>
    CSFindBy({ css: selector, ...options });

export const CSFindByXPath = (selector: string, options: Partial<CSFindByConfig> = {}) =>
    CSFindBy({ xpath: selector, ...options });

export const CSFindById = (id: string, options: Partial<CSFindByConfig> = {}) =>
    CSFindBy({ id: id, ...options });

export const CSFindByName = (name: string, options: Partial<CSFindByConfig> = {}) =>
    CSFindBy({ name: name, ...options });

export const CSFindByText = (text: string, options: Partial<CSFindByConfig> = {}) =>
    CSFindBy({ text: text, ...options });

export const CSFindByTestId = (testId: string, options: Partial<CSFindByConfig> = {}) =>
    CSFindBy({ testId: testId, ...options });

export const CSFindByClassName = (className: string, options: Partial<CSFindByConfig> = {}) =>
    CSFindBy({ className: className, ...options });

/**
 * CSFindByAI decorator creates a self-healing element
 */
export const CSFindByAI = (selector: string, friendlyName: string, options: Partial<CSFindByConfig> = {}) =>
    CSFindBy({
        css: selector,
        selfHealing: true,
        friendlyName: friendlyName,
        ...options
    }); 