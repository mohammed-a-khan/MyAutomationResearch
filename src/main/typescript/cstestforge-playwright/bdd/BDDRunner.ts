import { test, TestInfo } from '@playwright/test';
import fs from 'fs';
import path from 'path';
import { CSPlaywrightReporting, Status } from '../reporting/CSPlaywrightReporting';
import { ScenarioContext } from './ScenarioContext';

/**
 * Simple Gherkin feature file structure
 */
interface FeatureFile {
    title: string;
    description: string[];
    background?: {
        steps: string[];
    };
    scenarios: {
        name: string;
        tags: string[];
        steps: string[];
    }[];
}

/**
 * BDD Runner for executing Cucumber-style feature files with Playwright.
 * 
 * This class parses Gherkin feature files and runs them using Playwright test framework.
 */
export class BDDRunner {
    private static readonly FEATURES_DIR = path.join(process.cwd(), 'features');
    private static readonly STEPS_MAP = new Map<string, Function>();
    private static readonly HOOKS_MAP = new Map<string, Function[]>();
    
    // Instance of reporting tool
    private static reporting = new CSPlaywrightReporting();
    
    /**
     * Register a step definition
     * 
     * @param pattern Regexp pattern to match step text
     * @param implementation Step implementation function
     */
    public static defineStep(pattern: RegExp, implementation: Function): void {
        BDDRunner.STEPS_MAP.set(pattern.toString(), implementation);
    }
    
    /**
     * Register a before hook
     * 
     * @param hook Hook implementation function
     */
    public static beforeScenario(hook: Function): void {
        const hooks = BDDRunner.HOOKS_MAP.get('beforeScenario') || [];
        hooks.push(hook);
        BDDRunner.HOOKS_MAP.set('beforeScenario', hooks);
    }
    
    /**
     * Register an after hook
     * 
     * @param hook Hook implementation function
     */
    public static afterScenario(hook: Function): void {
        const hooks = BDDRunner.HOOKS_MAP.get('afterScenario') || [];
        hooks.push(hook);
        BDDRunner.HOOKS_MAP.set('afterScenario', hooks);
    }
    
    /**
     * Run all feature files in the features directory
     */
    public static runFeatures(): void {
        // Find all feature files
        const featureFiles = BDDRunner.findFeatureFiles();
        
        // Run each feature file
        featureFiles.forEach(featureFile => {
            BDDRunner.runFeature(featureFile);
        });
    }
    
    /**
     * Run a single feature file
     * 
     * @param featureFile Path to feature file
     */
    public static runFeature(featureFile: string): void {
        // Read and parse the feature file
        const featureContent = fs.readFileSync(featureFile, 'utf8');
        const parsedFeature = BDDRunner.parseFeatureFile(featureContent);
        
        // Create Playwright test for each scenario
        if (parsedFeature) {
            parsedFeature.scenarios.forEach(scenario => {
                // Create test
                test(`${parsedFeature.title}: ${scenario.name}`, async ({ page }, testInfo) => {
                    // Initialize context
                    const context = new ScenarioContext(page);
                    
                    // Start test reporting
                    BDDRunner.reporting.startTest(scenario.name, parsedFeature.title);
                    
                    try {
                        // Run before hooks
                        await BDDRunner.runHooks('beforeScenario', context, testInfo);
                        
                        // Run background steps if present
                        if (parsedFeature.background) {
                            for (const step of parsedFeature.background.steps) {
                                await BDDRunner.executeStep(step, context, testInfo);
                            }
                        }
                        
                        // Run scenario steps
                        for (const step of scenario.steps) {
                            await BDDRunner.executeStep(step, context, testInfo);
                        }
                        
                        // End test as passing
                        BDDRunner.reporting.endTest(Status.PASS);
                    } catch (error: any) {
                        // Log error
                        BDDRunner.reporting.log(`Scenario failed: ${error.message}`, Status.FAIL);
                        
                        // End test as failing
                        BDDRunner.reporting.endTest(Status.FAIL);
                        
                        // Rethrow to fail the test
                        throw error;
                    } finally {
                        // Run after hooks
                        await BDDRunner.runHooks('afterScenario', context, testInfo);
                    }
                });
            });
        }
    }
    
    /**
     * Parse a Gherkin feature file
     * 
     * @param content File content
     * @returns Parsed feature file structure
     */
    private static parseFeatureFile(content: string): FeatureFile | null {
        try {
            const lines = content.split('\n').map(line => line.trim());
            let currentSection: 'feature' | 'background' | 'scenario' | null = null;
            let currentScenario: { name: string, tags: string[], steps: string[] } | null = null;
            let currentTags: string[] = [];
            
            const feature: FeatureFile = {
                title: '',
                description: [],
                scenarios: []
            };
            
            for (let i = 0; i < lines.length; i++) {
                const line = lines[i];
                
                // Skip empty lines and comments
                if (!line || line.startsWith('#')) {
                    continue;
                }
                
                // Handle tags
                if (line.startsWith('@')) {
                    currentTags = line.split(' ').filter(tag => tag.startsWith('@'));
                    continue;
                }
                
                // Handle Feature
                if (line.startsWith('Feature:')) {
                    currentSection = 'feature';
                    feature.title = line.substring('Feature:'.length).trim();
                    continue;
                }
                
                // Handle Background
                if (line.startsWith('Background:')) {
                    currentSection = 'background';
                    feature.background = { steps: [] };
                    continue;
                }
                
                // Handle Scenario or Scenario Outline
                if (line.startsWith('Scenario:') || line.startsWith('Scenario Outline:')) {
                    currentSection = 'scenario';
                    currentScenario = {
                        name: line.substring(line.indexOf(':') + 1).trim(),
                        tags: [...currentTags],
                        steps: []
                    };
                    feature.scenarios.push(currentScenario);
                    currentTags = []; // Reset tags
                    continue;
                }
                
                // Handle Examples section (skip it for now)
                if (line.startsWith('Examples:')) {
                    // Skip the Examples section
                    while (i + 1 < lines.length && !lines[i + 1].startsWith('Scenario:') && !lines[i + 1].startsWith('Scenario Outline:')) {
                        i++;
                    }
                    continue;
                }
                
                // Handle steps
                if (/^(Given|When|Then|And|But) /.test(line)) {
                    if (currentSection === 'background' && feature.background) {
                        feature.background.steps.push(line);
                    } else if (currentSection === 'scenario' && currentScenario) {
                        currentScenario.steps.push(line);
                    }
                    continue;
                }
                
                // Feature description
                if (currentSection === 'feature' && !line.startsWith('Scenario:') && !line.startsWith('Background:')) {
                    feature.description.push(line);
                }
            }
            
            return feature;
        } catch (error) {
            console.error('Error parsing feature file:', error);
            return null;
        }
    }
    
    /**
     * Execute a single step
     * 
     * @param stepText Step text from feature file
     * @param context Scenario context
     * @param testInfo Playwright test info
     */
    private static async executeStep(stepText: string, context: ScenarioContext, testInfo: TestInfo): Promise<void> {
        BDDRunner.reporting.startStep(stepText);
        
        // Find matching step definition
        let stepImplementation = null;
        let stepParams: any[] = [];
        
        for (const [patternString, implementation] of BDDRunner.STEPS_MAP.entries()) {
            const pattern = new RegExp(patternString.slice(1, -1)); // Remove leading/trailing slashes
            const match = stepText.match(pattern);
            
            if (match) {
                stepImplementation = implementation;
                // Extract capture groups as parameters
                stepParams = match.slice(1);
                break;
            }
        }
        
        if (stepImplementation) {
            try {
                // Run step implementation
                await stepImplementation.apply(context, stepParams);
                BDDRunner.reporting.endStep(Status.PASS);
            } catch (error: any) {
                // Log step failure
                BDDRunner.reporting.log(`Step failed: ${error.message}`, Status.FAIL);
                BDDRunner.reporting.endStep(Status.FAIL);
                
                // Take screenshot
                await BDDRunner.reporting.takeScreenshot(`step_failure_${Date.now()}`);
                
                // Rethrow error
                throw error;
            }
        } else {
            // No matching step definition found
            BDDRunner.reporting.log(`No step definition found for: ${stepText}`, Status.FAIL);
            BDDRunner.reporting.endStep(Status.FAIL);
            throw new Error(`No step definition found for: ${stepText}`);
        }
    }
    
    /**
     * Run hooks for a specific hook point
     * 
     * @param hookName Name of the hook point
     * @param context Scenario context
     * @param testInfo Playwright test info
     */
    private static async runHooks(hookName: string, context: ScenarioContext, testInfo: TestInfo): Promise<void> {
        const hooks = BDDRunner.HOOKS_MAP.get(hookName) || [];
        
        for (const hook of hooks) {
            try {
                await hook.call(context, testInfo);
            } catch (error: any) {
                BDDRunner.reporting.log(`Hook ${hookName} failed: ${error.message}`, Status.FAIL);
                throw error;
            }
        }
    }
    
    /**
     * Find all feature files in the features directory
     */
    private static findFeatureFiles(): string[] {
        if (!fs.existsSync(BDDRunner.FEATURES_DIR)) {
            return [];
        }
        
        const files: string[] = [];
        
        function traverse(dir: string) {
            fs.readdirSync(dir).forEach(file => {
                const fullPath = path.join(dir, file);
                if (fs.statSync(fullPath).isDirectory()) {
                    traverse(fullPath);
                } else if (file.endsWith('.feature')) {
                    files.push(fullPath);
                }
            });
        }
        
        traverse(BDDRunner.FEATURES_DIR);
        return files;
    }
} 