import { CSPlaywrightReporting, Status } from '../reporting/CSPlaywrightReporting';

/**
 * Options for the CSTestStep decorator.
 */
export interface CSTestStepOptions {
    /** Description of the step */
    description: string;
    /** Whether to take screenshot after step */
    screenshot?: boolean;
    /** Additional metadata for the step */
    metadata?: Record<string, string>;
}

/**
 * Decorator for marking methods as test steps.
 * Similar to Java's @CSTestStep annotation.
 * 
 * @param options Step options including description and screenshot flag
 * @returns Method decorator function
 */
export function CSTestStep(options: CSTestStepOptions) {
    return function(target: any, propertyKey: string, descriptor: PropertyDescriptor) {
        // Store original method
        const originalMethod = descriptor.value;
        
        // Create reporting instance
        const reporting = new CSPlaywrightReporting();
        
        // Replace method with wrapped version that adds reporting
        descriptor.value = async function(...args: any[]) {
            try {
                // Log step start
                reporting.startStep(options.description);
                
                // Add metadata if provided
                if (options.metadata) {
                    Object.entries(options.metadata).forEach(([key, value]) => {
                        reporting.addMetadata(key, value);
                    });
                }
                
                // Execute the original method
                const result = await originalMethod.apply(this, args);
                
                // Log step success
                reporting.endStep(Status.PASS);
                
                // Take screenshot if requested
                if (options.screenshot) {
                    await reporting.takeScreenshot(`${propertyKey}_success`);
                }
                
                return result;
            } catch (error: any) {
                // Log step failure
                reporting.log(`Step failed: ${error.message}`, Status.FAIL);
                reporting.endStep(Status.FAIL);
                
                // Always take screenshot on failure
                await reporting.takeScreenshot(`${propertyKey}_failure`);
                
                // Rethrow error
                throw error;
            }
        };
        
        // Return modified descriptor
        return descriptor;
    };
}

/**
 * Shorthand for creating a test step decorator with minimal configuration.
 * 
 * @param description Step description
 * @param screenshot Whether to take screenshot
 * @returns Method decorator function
 */
export function Step(description: string, screenshot: boolean = false) {
    return CSTestStep({
        description,
        screenshot
    });
} 