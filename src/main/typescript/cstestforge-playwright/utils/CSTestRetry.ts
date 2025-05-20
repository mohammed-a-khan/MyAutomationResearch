import { TestInfo } from '@playwright/test';
import { CSPlaywrightReporting, Status } from '../reporting/CSPlaywrightReporting';

/**
 * Options for the CSTestRetry decorator
 */
export interface CSTestRetryOptions {
    /** Maximum number of retries */
    maxRetries: number;
    /** Delay between retries in milliseconds */
    delayMs?: number;
    /** List of error types to retry on */
    retryOnErrors?: Array<string | RegExp>;
    /** Whether to take screenshots on failures */
    screenshotOnFailure?: boolean;
}

/**
 * Default options for test retry
 */
const DEFAULT_RETRY_OPTIONS: CSTestRetryOptions = {
    maxRetries: 2,
    delayMs: 1000,
    screenshotOnFailure: true,
    retryOnErrors: [
        'TimeoutError',
        'Error: Navigation timeout',
        'Error: page.click',
        'Error: page.fill',
        /element .*? not found/i,
        /Element is not clickable/i,
        /element not interactable/i
    ]
};

/**
 * Decorator for adding retry logic to test methods.
 * 
 * @param options Retry options or max retry count
 * @returns Method decorator function
 */
export function CSTestRetry(options: CSTestRetryOptions | number) {
    const retryOptions: CSTestRetryOptions = typeof options === 'number' 
        ? { ...DEFAULT_RETRY_OPTIONS, maxRetries: options }
        : { ...DEFAULT_RETRY_OPTIONS, ...options };
    
    return function(target: any, propertyKey: string, descriptor: PropertyDescriptor) {
        // Store the original method
        const originalMethod = descriptor.value;
        const reporting = new CSPlaywrightReporting();
        
        // Replace with method that includes retry logic
        descriptor.value = async function(...args: any[]) {
            let lastError: Error | null = null;
            let attempt = 0;
            
            while (attempt <= retryOptions.maxRetries) {
                try {
                    attempt++;
                    
                    // Log retry attempt if not the first try
                    if (attempt > 1) {
                        reporting.log(`Retrying test (${attempt}/${retryOptions.maxRetries + 1})`, Status.WARN);
                    }
                    
                    // Execute the test method
                    const result = await originalMethod.apply(this, args);
                    
                    // Test passed, return the result
                    return result;
                } catch (error: any) {
                    lastError = error;
                    
                    // Take screenshot if enabled
                    if (retryOptions.screenshotOnFailure) {
                        const testInfo = args.find(arg => arg?.constructor?.name === 'TestInfoImpl') as TestInfo;
                        if (testInfo) {
                            await reporting.takeScreenshot(`${propertyKey}_failure_${attempt}`);
                        }
                    }
                    
                    // Check if this error should trigger a retry
                    const shouldRetry = retryOptions.retryOnErrors
                        ? shouldRetryError(error, retryOptions.retryOnErrors)
                        : true;
                    
                    if (!shouldRetry || attempt > retryOptions.maxRetries) {
                        // No more retries or error should not be retried
                        reporting.log(`Test failed after ${attempt} ${attempt === 1 ? 'attempt' : 'attempts'}: ${error.message}`, Status.FAIL);
                        throw error;
                    }
                    
                    // Log the error and wait before retrying
                    reporting.log(`Attempt ${attempt} failed: ${error.message}`, Status.WARN);
                    
                    if (retryOptions.delayMs && retryOptions.delayMs > 0) {
                        await new Promise(resolve => setTimeout(resolve, retryOptions.delayMs));
                    }
                }
            }
            
            // This should never happen due to the checks above, but just in case
            throw lastError || new Error('Test failed with unknown error');
        };
        
        return descriptor;
    };
}

/**
 * Check if an error should trigger a retry based on configured error types
 * 
 * @param error The error that occurred
 * @param retryOnErrors List of error types to retry on
 * @returns True if the error should trigger a retry
 */
function shouldRetryError(error: Error, retryOnErrors: Array<string | RegExp>): boolean {
    const errorMessage = error.message;
    const errorName = error.name;
    
    return retryOnErrors.some(errorType => {
        if (typeof errorType === 'string') {
            return errorName.includes(errorType) || errorMessage.includes(errorType);
        } else {
            return errorType.test(errorName) || errorType.test(errorMessage);
        }
    });
} 