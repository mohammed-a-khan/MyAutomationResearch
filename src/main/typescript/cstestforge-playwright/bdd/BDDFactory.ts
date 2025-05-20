import { Page } from 'playwright';
import { CSPlaywrightDriver } from '../core/CSPlaywrightDriver';
import { CSElement } from '../core/CSElement';
import { BDDRunner } from './BDDRunner';
import { CSPlaywrightReporting, Status } from '../reporting/CSPlaywrightReporting';

/**
 * Factory class for creating and managing BDD test components.
 */
export class BDDFactory {
    private static instance: BDDFactory;
    private page!: Page;
    private reporting: CSPlaywrightReporting;
    private driver: CSPlaywrightDriver;
    
    /**
     * Private constructor for singleton pattern
     */
    private constructor() {
        this.reporting = new CSPlaywrightReporting();
        this.driver = new CSPlaywrightDriver();
    }
    
    /**
     * Get singleton instance
     * 
     * @returns BDDFactory instance
     */
    public static getInstance(): BDDFactory {
        if (!BDDFactory.instance) {
            BDDFactory.instance = new BDDFactory();
        }
        return BDDFactory.instance;
    }
    
    /**
     * Get a Page instance
     * 
     * @returns Playwright Page object
     */
    public async getPage(): Promise<Page> {
        if (!this.page) {
            this.page = await this.driver.getPage();
        }
        return this.page;
    }
    
    /**
     * Register a step definition with the BDD runner
     * 
     * @param pattern Regexp pattern to match the step text
     * @param implementation Step implementation function
     */
    public registerStep(pattern: RegExp, implementation: Function): void {
        BDDRunner.defineStep(pattern, implementation);
    }
    
    /**
     * Register a 'Given' step
     * 
     * @param pattern Step pattern
     * @param implementation Step implementation
     */
    public given(pattern: RegExp, implementation: Function): void {
        this.registerStep(new RegExp('^Given ' + pattern.source), implementation);
    }
    
    /**
     * Register a 'When' step
     * 
     * @param pattern Step pattern
     * @param implementation Step implementation
     */
    public when(pattern: RegExp, implementation: Function): void {
        this.registerStep(new RegExp('^When ' + pattern.source), implementation);
    }
    
    /**
     * Register a 'Then' step
     * 
     * @param pattern Step pattern
     * @param implementation Step implementation
     */
    public then(pattern: RegExp, implementation: Function): void {
        this.registerStep(new RegExp('^Then ' + pattern.source), implementation);
    }
    
    /**
     * Register a 'And' step
     * 
     * @param pattern Step pattern
     * @param implementation Step implementation
     */
    public and(pattern: RegExp, implementation: Function): void {
        this.registerStep(new RegExp('^And ' + pattern.source), implementation);
    }
    
    /**
     * Run feature files
     */
    public runFeatures(): void {
        BDDRunner.runFeatures();
    }
    
    /**
     * Create a CSElement for interacting with elements
     * 
     * @param selector Element selector
     * @param name Optional element name
     * @returns CSElement instance
     */
    public createElement(selector: string, name?: string): CSElement {
        return new CSElement(this.page, selector, { name });
    }
    
    /**
     * Log a message to the test report
     * 
     * @param message Message to log
     * @param status Status of the message
     */
    public log(message: string, status: Status = Status.INFO): void {
        this.reporting.log(message, status);
    }
    
    /**
     * Take a screenshot
     * 
     * @param name Screenshot name
     */
    public async takeScreenshot(name: string): Promise<void> {
        await this.reporting.takeScreenshot(name);
    }
} 