import { test as baseTest, TestInfo, Page, Locator, expect, PlaywrightTestOptions, PlaywrightWorkerOptions, ReporterDescription } from '@playwright/test';
import { CSPlaywrightDriver } from '../core/CSPlaywrightDriver';
import { CSPlaywrightReporting, Status } from '../reporting/CSPlaywrightReporting';
import { CSBrowserManager } from '../core/CSBrowserManager';
import { MetadataRegistry } from '../annotation/CSMetaData';
import * as fs from 'fs';
import * as path from 'path';

/**
 * Configuration options for the CSPlaywrightRunner
 */
export interface CSPlaywrightRunnerConfig {
    /**
     * Base URL for the application under test
     */
    baseUrl?: string;
    
    /**
     * Path to save test reports
     */
    reportPath?: string;
    
    /**
     * Whether to take screenshots on test failures
     */
    screenshotOnFailure?: boolean;
    
    /**
     * Whether to generate an HTML report after test execution
     */
    generateHtmlReport?: boolean;
    
    /**
     * Custom browser options
     */
    browserOptions?: {
        /**
         * Browser to use (chromium, firefox, webkit)
         */
        browserName?: 'chromium' | 'firefox' | 'webkit';
        
        /**
         * Whether to run in headless mode
         */
        headless?: boolean;
        
        /**
         * Browser window size
         */
        viewport?: { width: number, height: number };
        
        /**
         * Whether to record videos of test execution
         */
        recordVideo?: boolean;
        
        /**
         * Additional browser launch arguments
         */
        launchArgs?: string[];
    };
    
    /**
     * Hook called before each test
     */
    beforeEach?: (page: Page, testInfo: TestInfo) => Promise<void>;
    
    /**
     * Hook called after each test
     */
    afterEach?: (page: Page, testInfo: TestInfo) => Promise<void>;
}

/**
 * CSPlaywrightRunner provides an integration layer between CSTestForge framework and Playwright's test runner.
 * It handles test execution, reporting, and framework integration.
 */
export class CSPlaywrightRunner {
    private static instance: CSPlaywrightRunner;
    private config: CSPlaywrightRunnerConfig;
    private reporting: CSPlaywrightReporting;
    
    /**
     * Private constructor for singleton pattern
     */
    private constructor(config: CSPlaywrightRunnerConfig = {}) {
        this.config = this.mergeWithDefaults(config);
        this.reporting = new CSPlaywrightReporting();
    }
    
    /**
     * Get singleton instance of CSPlaywrightRunner
     * 
     * @param config Configuration options
     * @returns CSPlaywrightRunner instance
     */
    public static getInstance(config?: CSPlaywrightRunnerConfig): CSPlaywrightRunner {
        if (!CSPlaywrightRunner.instance) {
            CSPlaywrightRunner.instance = new CSPlaywrightRunner(config);
        }
        return CSPlaywrightRunner.instance;
    }
    
    /**
     * Merge provided config with defaults
     * 
     * @param config User-provided configuration
     * @returns Merged configuration
     */
    private mergeWithDefaults(config: CSPlaywrightRunnerConfig): CSPlaywrightRunnerConfig {
        return {
            baseUrl: config.baseUrl || 'http://localhost:3000',
            reportPath: config.reportPath || 'test-output/reports',
            screenshotOnFailure: config.screenshotOnFailure !== false,
            generateHtmlReport: config.generateHtmlReport !== false,
            browserOptions: {
                browserName: config.browserOptions?.browserName || 'chromium',
                headless: config.browserOptions?.headless !== false,
                viewport: config.browserOptions?.viewport || { width: 1280, height: 720 },
                recordVideo: config.browserOptions?.recordVideo || false,
                launchArgs: config.browserOptions?.launchArgs || []
            },
            beforeEach: config.beforeEach,
            afterEach: config.afterEach
        };
    }
    
    /**
     * Create a test fixture that integrates with CSTestForge
     * 
     * @param title Test title
     * @param testFn Test function
     */
    public test(title: string, testFn: (page: Page, testInfo: TestInfo) => Promise<void>) {
        // Create a wrapped test using Playwright's test function
        baseTest(title, async ({ page }, testInfo) => {
            // Initialize CSTestForge test tracking
            this.reporting.startTest(title, testInfo.project.name);
            
            // Set metadata from decorators if available
            const metadata = MetadataRegistry.getMetadata(testFn.constructor, testFn.name);
            if (metadata) {
                Object.entries(metadata).forEach(([key, value]) => {
                    this.reporting.addMetadata(key, String(value));
                    testInfo.annotations.push({ type: key, description: String(value) });
                });
            }
            
            try {
                // Run before hook if provided
                if (this.config.beforeEach) {
                    await this.config.beforeEach(page, testInfo);
                }
                
                // Run the actual test
                await testFn(page, testInfo);
                
                // Mark test as passed
                this.reporting.endTest(Status.PASS);
            } catch (error: any) {
                // Take screenshot on failure if configured
                if (this.config.screenshotOnFailure) {
                    await this.reporting.takeScreenshot(`${title.replace(/\s+/g, '_')}_failure`, testInfo);
                }
                
                // Mark test as failed
                this.reporting.log(`Test failed: ${error.message}`, Status.FAIL);
                this.reporting.endTest(Status.FAIL);
                
                // Re-throw the error to let Playwright handle it
                throw error;
            } finally {
                // Run after hook if provided
                if (this.config.afterEach) {
                    await this.config.afterEach(page, testInfo);
                }
            }
        });
    }
    
    /**
     * Create a test fixture with data provider integration
     * 
     * @param title Test title template
     * @param data Test data array
     * @param testFn Test function that receives data
     */
    public testWithData<T>(
        title: string,
        data: T[],
        testFn: (data: T, page: Page, testInfo: TestInfo) => Promise<void>
    ) {
        // For each data item, create a separate test
        data.forEach((item, index) => {
            // Create a descriptive title with data
            const testTitle = `${title} [${index}]`;
            
            // Create a wrapped test using our test method
            this.test(testTitle, async (page, testInfo) => {
                // Run the test with data
                await testFn(item, page, testInfo);
            });
        });
    }
    
    /**
     * Set up global teardown to generate report
     */
    public setupGlobalTeardown() {
        // Register process exit handler
        process.on('exit', () => {
            if (this.config.generateHtmlReport) {
                CSPlaywrightReporting.generateReport();
            }
        });
    }
    
    /**
     * Configure the Playwright project with CSTestForge integration
     * 
     * @returns Configuration object to be used in playwright.config.ts
     */
    public getPlaywrightConfig() {
        // Define the properly typed configuration
        const useConfig: Partial<PlaywrightTestOptions & PlaywrightWorkerOptions> = {
            baseURL: this.config.baseUrl || 'http://localhost:3000',
            screenshot: 'only-on-failure',
            trace: 'retain-on-failure',
            video: this.config.browserOptions?.recordVideo ? 'on-first-retry' : 'off',
            viewport: this.config.browserOptions?.viewport,
        };
        
        // Add headless mode if specified
        if (this.config.browserOptions?.headless !== undefined) {
            useConfig.headless = this.config.browserOptions.headless;
        }
        
        // Add launch args if specified
        if (this.config.browserOptions?.launchArgs?.length) {
            useConfig.launchOptions = {
                args: this.config.browserOptions.launchArgs
            };
        }
        
        // Define reporters
        const reporters: ReporterDescription[] = [
            ['list']
        ];
        
        // Add HTML reporter if directory is specified
        if (this.config.reportPath) {
            reporters.push(['html', { 
                outputFolder: path.join(this.config.reportPath, 'playwright-report')
            }]);
        }
        
        return {
            use: useConfig,
            reporter: reporters
        };
    }
    
    /**
     * Reset the singleton instance (useful for testing)
     */
    public static resetInstance() {
        CSPlaywrightRunner.instance = undefined as any;
    }
}

// Export a pre-configured instance for ease of use
export const csTest = CSPlaywrightRunner.getInstance().test.bind(CSPlaywrightRunner.getInstance());
export const csTestWithData = CSPlaywrightRunner.getInstance().testWithData.bind(CSPlaywrightRunner.getInstance()); 