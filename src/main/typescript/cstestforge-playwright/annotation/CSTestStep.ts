import { CSPlaywrightReporting, Status } from '../reporting/CSPlaywrightReporting';

/**
 * Test step configuration options
 */
export interface CSTestStepOptions {
    /** Description of the test step with optional parameter placeholders */
    description: string;
    /** Optional screenshot capture flag (default: false) */
    takeScreenshot?: boolean;
    /** Optional log level (default: INFO) */
    logLevel?: Status;
    /** Optional label for the step in reports */
    label?: string;
    /** Optional timeout in milliseconds */
    timeoutMs?: number;
}

/**
 * Registry for mapping Gherkin steps to test methods
 */
export class TestStepRegistry {
    private static registry: Map<RegExp, { 
        target: Object; 
        propertyKey: string; 
        description: string;
        options: CSTestStepOptions;
    }> = new Map();

    /**
     * Register a test step
     */
    public static register(
        target: Object,
        propertyKey: string,
        options: CSTestStepOptions
    ): void {
        // Convert description to a regexp pattern
        // This handles placeholder parameters in curly braces {param}
        // and creates regex groups to extract them
        const description = options.description;
        
        // Create three variations for Given/When/Then/And
        const prefixes = ['Given', 'When', 'Then', 'And', ''];
        
        prefixes.forEach(prefix => {
            // Create regex pattern from description with prefix
            let pattern = description;
            
            // Add prefix if there is one
            if (prefix) {
                pattern = `${prefix}\\s+${pattern}`;
            }
            
            // Convert {param} to regex pattern
            const regexPattern = pattern
                .replace(/\{([^}]+)\}/g, '(?<$1>.+?)')
                .replace(/\//g, '\\/');
                
            const regex = new RegExp(`^${regexPattern}$`, 'i');
            
            // Store in registry
            this.registry.set(regex, { 
                target, 
                propertyKey, 
                description: pattern,
                options
            });
        });
    }

    /**
     * Find a matching test step for a Gherkin step
     * 
     * @param step Gherkin step text
     * @returns Step info with parameters or undefined if not found
     */
    public static findMatchingStep(step: string): { 
        target: Object;
        propertyKey: string;
        params: Record<string, string>;
        options: CSTestStepOptions;
    } | undefined {
        for (const [pattern, info] of this.registry.entries()) {
            const match = step.match(pattern);
            if (match) {
                // Extract named groups from match
                const params: Record<string, string> = match.groups || {};
                
                return {
                    target: info.target,
                    propertyKey: info.propertyKey,
                    params,
                    options: info.options
                };
            }
        }
        
        return undefined;
    }
    
    /**
     * Get all registered steps
     */
    public static getAllSteps(): { pattern: RegExp; description: string }[] {
        return Array.from(this.registry.entries()).map(([pattern, info]) => ({
            pattern,
            description: info.description
        }));
    }
}

/**
 * Decorator for marking a method as a test step that maps to Gherkin syntax.
 * The description can include parameters in curly braces that will be parsed and passed to the method.
 * 
 * @example
 * ```typescript
 * // Basic test step
 * @CSTestStep({
 *     description: "enter user name as '{userName}'"
 * })
 * public enterUserName(userName: string): void {
 *     // Implementation
 *     this.userNameField.fill(userName);
 * }
 * 
 * // Test step with screenshot
 * @CSTestStep({
 *     description: "verify page title is '{title}'",
 *     takeScreenshot: true
 * })
 * public verifyPageTitle(title: string): void {
 *     // Implementation
 *     expect(this.page.title()).toContain(title);
 * }
 * ```
 * 
 * This decorator allows methods to be matched with Gherkin steps like:
 * - Given enter user name as 'akhan'
 * - When enter user name as 'akhan'
 * - And enter user name as 'akhan'
 * - Then verify page title is 'Dashboard'
 */
export function CSTestStep(options: CSTestStepOptions) {
    return function (target: Object, propertyKey: string, descriptor: PropertyDescriptor) {
        // Register the test step
        TestStepRegistry.register(target, propertyKey, options);
        
        // Store the original method
        const originalMethod = descriptor.value;
        
        // Replace the method with test step wrapper
        descriptor.value = async function(...args: any[]) {
            // Create reporting instance
            const reporting = new CSPlaywrightReporting();
            
            // Start step
            reporting.startStep(options.description);
            reporting.log(`Executing step: ${options.description}`, options.logLevel || Status.INFO);
            
            try {
                // Take screenshot before step execution if needed
                if (options.takeScreenshot) {
                    await reporting.takeScreenshot(`Before_${propertyKey}`);
                }
                
                // Execute step with timeout if specified
                let result;
                if (options.timeoutMs) {
                    const timeoutPromise = new Promise((_, reject) => {
                        setTimeout(() => reject(new Error(`Step timeout after ${options.timeoutMs}ms`)), options.timeoutMs);
                    });
                    
                    result = await Promise.race([
                        originalMethod.apply(this, args),
                        timeoutPromise
                    ]);
                } else {
                    result = await originalMethod.apply(this, args);
                }
                
                // Take screenshot after step execution if needed
                if (options.takeScreenshot) {
                    await reporting.takeScreenshot(`After_${propertyKey}`);
                }
                
                // End step as passed
                reporting.log(`Step executed successfully`, Status.PASS);
                reporting.endStep(Status.PASS);
                
                return result;
            } catch (error) {
                // Take failure screenshot
                await reporting.takeScreenshot(`Failed_${propertyKey}`);
                
                // Log error
                reporting.log(`Step failed: ${error instanceof Error ? error.message : 'Unknown error'}`, Status.FAIL);
                
                // End step as failed
                reporting.endStep(Status.FAIL);
                
                // Re-throw the error
                throw error;
            }
        };
        
        return descriptor;
    };
}

/**
 * Utility to execute Gherkin steps programmatically
 */
export class CSStepExecutor {
    /**
     * Execute a Gherkin step
     * 
     * @param stepText Gherkin step text
     * @param context Context object to use as 'this' in step method
     * @returns Promise of step result
     */
    public static async executeStep(stepText: string, context?: any): Promise<any> {
        const match = TestStepRegistry.findMatchingStep(stepText);
        
        if (!match) {
            throw new Error(`No matching step found for: ${stepText}`);
        }
        
        // Get method reference
        const method = (match.target as any)[match.propertyKey];
        
        if (!method || typeof method !== 'function') {
            throw new Error(`Method ${match.propertyKey} is not a function`);
        }
        
        // Extract parameter values
        const paramValues = Object.values(match.params);
        
        // Execute method
        return method.apply(context || match.target, paramValues);
    }
    
    /**
     * Execute a sequence of Gherkin steps
     * 
     * @param steps Array of Gherkin step texts
     * @param context Context object to use as 'this' in step methods
     * @returns Promise of final step result
     */
    public static async executeSteps(steps: string[], context?: any): Promise<any> {
        let result;
        
        for (const step of steps) {
            result = await this.executeStep(step, context);
        }
        
        return result;
    }
} 