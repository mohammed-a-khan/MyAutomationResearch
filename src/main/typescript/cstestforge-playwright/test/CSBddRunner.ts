import { test as playwrightTest, TestInfo, Page } from '@playwright/test';
import { CSTestStepProcessor, ParsedFeature, StepExecutionResult } from '../processors/CSTestStepProcessor';
import { CSPlaywrightReporting, Status } from '../reporting/CSPlaywrightReporting';
import { CSMetaDataProcessor } from '../processors/CSMetaDataProcessor';
import * as fs from 'fs';
import * as path from 'path';

/**
 * Configuration options for the CSBddRunner
 */
export interface CSBddRunnerConfig {
    /**
     * Directory where feature files are located
     */
    featuresDir?: string;
    
    /**
     * Whether to stop execution on first failure within a scenario
     */
    stopOnFailure?: boolean;
    
    /**
     * Tags to include (run only scenarios with these tags)
     */
    includeTags?: string[];
    
    /**
     * Tags to exclude (don't run scenarios with these tags)
     */
    excludeTags?: string[];
    
    /**
     * Whether to generate reports
     */
    generateReports?: boolean;
    
    /**
     * Directory for reports
     */
    reportsDir?: string;
    
    /**
     * Hook called before each scenario
     */
    beforeScenario?: (page: Page, scenario: string) => Promise<void>;
    
    /**
     * Hook called after each scenario
     */
    afterScenario?: (page: Page, scenario: string, success: boolean) => Promise<void>;
}

/**
 * CSBddRunner provides BDD-style test execution using Gherkin syntax.
 * It integrates with the CSTestStep processor to execute feature files.
 */
export class CSBddRunner {
    private static instance: CSBddRunner;
    private config: CSBddRunnerConfig;
    private stepProcessor: CSTestStepProcessor;
    private reporting: CSPlaywrightReporting;
    private metadataProcessor: CSMetaDataProcessor;
    
    /**
     * Private constructor for singleton pattern
     */
    private constructor(config: CSBddRunnerConfig = {}) {
        this.config = this.mergeWithDefaults(config);
        this.stepProcessor = CSTestStepProcessor.getInstance();
        this.reporting = new CSPlaywrightReporting();
        this.metadataProcessor = CSMetaDataProcessor.getInstance();
    }
    
    /**
     * Get singleton instance of CSBddRunner
     * 
     * @param config Configuration options
     * @returns CSBddRunner instance
     */
    public static getInstance(config?: CSBddRunnerConfig): CSBddRunner {
        if (!CSBddRunner.instance) {
            CSBddRunner.instance = new CSBddRunner(config);
        }
        return CSBddRunner.instance;
    }
    
    /**
     * Merge provided config with defaults
     * 
     * @param config User-provided configuration
     * @returns Merged configuration
     */
    private mergeWithDefaults(config: CSBddRunnerConfig): CSBddRunnerConfig {
        return {
            featuresDir: config.featuresDir || 'features',
            stopOnFailure: config.stopOnFailure !== false,
            includeTags: config.includeTags || [],
            excludeTags: config.excludeTags || [],
            generateReports: config.generateReports !== false,
            reportsDir: config.reportsDir || 'test-output/bdd-reports',
            beforeScenario: config.beforeScenario,
            afterScenario: config.afterScenario
        };
    }
    
    /**
     * Create a test for a feature file
     * 
     * @param featureFilePath Path to the feature file
     * @param context Context object to use as 'this' in step methods
     */
    public feature(featureFilePath: string, context?: any) {
        // Parse the feature file
        const feature = this.stepProcessor.parseFeatureFile(featureFilePath);
        
        if (!feature) {
            console.error(`Failed to parse feature file: ${featureFilePath}`);
            return;
        }
        
        // Create a test for the feature
        playwrightTest(`Feature: ${feature.title}`, async ({ page }, testInfo) => {
            // Log the feature in the report
            this.reporting.startTest(`Feature: ${feature.title}`, feature.description);
            
            try {
                // Execute each scenario
                for (const scenario of feature.scenarios) {
                    // Check if scenario should be executed based on tags
                    if (!this.shouldExecuteScenario(scenario.tags)) {
                        this.reporting.log(`Skipping scenario "${scenario.title}" due to tag filtering`, Status.SKIP);
                        continue;
                    }
                    
                    // Log the scenario
                    this.reporting.log(`Executing scenario: ${scenario.title}`, Status.INFO);
                    
                    // Execute before scenario hook if provided
                    if (this.config.beforeScenario) {
                        await this.config.beforeScenario(page, scenario.title);
                    }
                    
                    let scenarioSuccess = true;
                    
                    try {
                        // For regular scenarios
                        if (scenario.type === 'Scenario') {
                            await this.executeScenario(scenario.steps, page, context);
                        } 
                        // For scenario outlines with examples
                        else if (scenario.type === 'Scenario Outline' && scenario.examples) {
                            await this.executeScenarioOutline(scenario, page, context);
                        }
                    } catch (error) {
                        scenarioSuccess = false;
                        this.reporting.log(`Scenario failed: ${error instanceof Error ? error.message : String(error)}`, Status.FAIL);
                    }
                    
                    // Execute after scenario hook if provided
                    if (this.config.afterScenario) {
                        await this.config.afterScenario(page, scenario.title, scenarioSuccess);
                    }
                }
                
                // Mark the test as passed
                this.reporting.endTest(Status.PASS);
                
            } catch (error: any) {
                // Mark the test as failed
                this.reporting.log(`Feature execution failed: ${error.message}`, Status.FAIL);
                this.reporting.endTest(Status.FAIL);
                
                throw error;
            } finally {
                // Generate report if configured
                if (this.config.generateReports) {
                    this.stepProcessor.generateStepReport();
                }
            }
        });
    }
    
    /**
     * Execute a scenario
     * 
     * @param steps Array of step texts
     * @param page Playwright page
     * @param context Context object to use as 'this' in step methods
     */
    private async executeScenario(steps: string[], page: Page, context?: any): Promise<StepExecutionResult[]> {
        // Add page to context if it doesn't exist
        const contextWithPage = {
            ...(context || {}),
            page: context?.page || page
        };
        
        // Execute the steps
        return await this.stepProcessor.executeSteps(
            steps, 
            contextWithPage, 
            this.config.stopOnFailure
        );
    }
    
    /**
     * Execute a scenario outline with examples
     * 
     * @param scenario Scenario outline definition
     * @param page Playwright page
     * @param context Context object to use as 'this' in step methods
     */
    private async executeScenarioOutline(scenario: any, page: Page, context?: any): Promise<StepExecutionResult[][]> {
        const results: StepExecutionResult[][] = [];
        
        // Check if examples exist and is an array
        if (!scenario.examples || !Array.isArray(scenario.examples) || scenario.examples.length === 0) {
            this.reporting.log(`Scenario outline has no examples: ${scenario.title}`, Status.WARN);
            return results;
        }
        
        // For each example
        for (const example of scenario.examples) {
            // Replace placeholders in steps with example values
            const resolvedSteps = scenario.steps.map((step: string) => {
                let resolvedStep = step;
                
                for (const [key, value] of Object.entries(example)) {
                    const placeholder = `<${key}>`;
                    resolvedStep = resolvedStep.replace(new RegExp(placeholder, 'g'), String(value));
                }
                
                return resolvedStep;
            });
            
            // Log the example
            this.reporting.log(`Executing example: ${JSON.stringify(example)}`, Status.INFO);
            
            // Execute the resolved steps
            const exampleResults = await this.executeScenario(resolvedSteps, page, context);
            results.push(exampleResults);
        }
        
        return results;
    }
    
    /**
     * Check if a scenario should be executed based on tags
     * 
     * @param scenarioTags Tags for the scenario
     * @returns True if the scenario should be executed
     */
    private shouldExecuteScenario(scenarioTags: string[]): boolean {
        // If no tag filtering is configured, execute all scenarios
        if (!this.config.includeTags?.length && !this.config.excludeTags?.length) {
            return true;
        }
        
        // If exclude tags match, don't execute the scenario
        if (this.config.excludeTags && this.config.excludeTags.length > 0 && 
            scenarioTags.some(tag => this.config.excludeTags!.includes(tag))) {
            return false;
        }
        
        // If include tags are specified, only execute if at least one tag matches
        if (this.config.includeTags && this.config.includeTags.length > 0) {
            return scenarioTags.some(tag => this.config.includeTags!.includes(tag));
        }
        
        // If only exclude tags are specified and none match, execute the scenario
        return true;
    }
    
    /**
     * Find and register all feature files in the features directory
     * 
     * @param context Context object to use as 'this' in step methods
     */
    public loadAllFeatures(context?: any) {
        const featuresDir = this.config.featuresDir || 'features';
        
        if (!fs.existsSync(featuresDir)) {
            console.warn(`Features directory not found: ${featuresDir}`);
            return;
        }
        
        // Find all .feature files
        const featureFiles = this.findFeatureFiles(featuresDir);
        
        // Register each feature file
        for (const file of featureFiles) {
            this.feature(file, context);
        }
    }
    
    /**
     * Find all feature files in a directory
     * 
     * @param directory Directory to search
     * @returns Array of feature file paths
     */
    private findFeatureFiles(directory: string): string[] {
        const results: string[] = [];
        
        const files = fs.readdirSync(directory);
        
        for (const file of files) {
            const filePath = path.join(directory, file);
            const stat = fs.statSync(filePath);
            
            if (stat.isDirectory()) {
                results.push(...this.findFeatureFiles(filePath));
            } else if (file.endsWith('.feature')) {
                results.push(filePath);
            }
        }
        
        return results;
    }
    
    /**
     * Generate BDD test reports
     */
    public generateReports() {
        if (this.config.generateReports) {
            // Generate step report
            this.stepProcessor.generateStepReport();
            
            // Generate consolidated report
            CSPlaywrightReporting.generateReport();
        }
    }
    
    /**
     * Reset the singleton instance (useful for testing)
     */
    public static resetInstance() {
        CSBddRunner.instance = undefined as any;
    }
}

// Export a pre-configured instance for ease of use
export const csBdd = CSBddRunner.getInstance();
export const feature = csBdd.feature.bind(csBdd); 