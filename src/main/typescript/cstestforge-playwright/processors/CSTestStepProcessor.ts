import { TestStepRegistry, CSTestStepOptions } from '../annotation/CSTestStep';
import { CSPlaywrightReporting, Status } from '../reporting/CSPlaywrightReporting';
import * as fs from 'fs';
import * as path from 'path';

/**
 * Step execution result with full details
 */
export interface StepExecutionResult {
    /** Step description */
    description: string;
    /** Step parameters extracted from step text */
    parameters: Record<string, string>;
    /** Execution status */
    status: 'Pass' | 'Fail' | 'Skipped';
    /** Execution duration in milliseconds */
    durationMs: number;
    /** Error message if failed */
    errorMessage?: string;
    /** Screenshot paths if taken */
    screenshots: string[];
    /** Step output/return value */
    output?: any;
    /** Timestamp of execution */
    timestamp: string;
}

/**
 * Feature file parsed structure
 */
export interface ParsedFeature {
    /** Feature title */
    title: string;
    /** Feature description */
    description: string;
    /** List of scenarios */
    scenarios: ParsedScenario[];
    /** Feature tags */
    tags: string[];
}

/**
 * Scenario parsed structure
 */
export interface ParsedScenario {
    /** Scenario title */
    title: string;
    /** List of steps */
    steps: string[];
    /** Scenario tags */
    tags: string[];
    /** Scenario type (normal or outline) */
    type: 'Scenario' | 'Scenario Outline';
    /** Examples for scenario outline */
    examples?: Record<string, string>[];
}

/**
 * Processor for CSTestStep annotations.
 * Handles registration, discovery, and execution of test steps.
 */
export class CSTestStepProcessor {
    private static instance: CSTestStepProcessor;
    private reporting: CSPlaywrightReporting;
    private executionResults: Map<string, StepExecutionResult[]> = new Map();
    private featureFilesDir: string = 'features';
    private stepDefinitionsLoaded: boolean = false;
    private reportDir: string = 'test-output/steps';
    
    /**
     * Private constructor for singleton pattern
     */
    private constructor() {
        this.reporting = new CSPlaywrightReporting();
        this.createDirectories();
    }
    
    /**
     * Get processor instance
     */
    public static getInstance(): CSTestStepProcessor {
        if (!CSTestStepProcessor.instance) {
            CSTestStepProcessor.instance = new CSTestStepProcessor();
        }
        return CSTestStepProcessor.instance;
    }
    
    /**
     * Create required directories
     */
    private createDirectories(): void {
        if (!fs.existsSync(this.reportDir)) {
            fs.mkdirSync(this.reportDir, { recursive: true });
        }
    }
    
    /**
     * Execute a test step using its text description
     * 
     * @param stepText Full text of the step (e.g., "Given I login as 'admin'")
     * @param context Context object to use as 'this' in the step method
     * @returns Step execution result
     */
    public async executeStep(stepText: string, context?: any): Promise<StepExecutionResult> {
        // Find matching step definition
        const match = TestStepRegistry.findMatchingStep(stepText);
        if (!match) {
            const errorMessage = `No matching step definition found for: "${stepText}"`;
            console.error(errorMessage);
            
            return {
                description: stepText,
                parameters: {},
                status: 'Fail',
                durationMs: 0,
                errorMessage,
                screenshots: [],
                timestamp: new Date().toISOString()
            };
        }
        
        // Start step in reporting
        this.reporting.startStep(match.options.description);
        
        // Log step execution
        const logMessage = `Executing step: ${stepText}`;
        this.reporting.log(logMessage, match.options.logLevel || Status.INFO);
        
        // Record start time
        const startTime = process.hrtime();
        const screenshots: string[] = [];
        
        try {
            // Take screenshot before execution if needed
            if (match.options.takeScreenshot) {
                const beforeScreenshot = await this.reporting.takeScreenshot(`Before_${match.propertyKey}`);
                if (beforeScreenshot) {
                    screenshots.push(beforeScreenshot);
                }
            }
            
            // Get method reference
            const method = (match.target as any)[match.propertyKey];
            if (!method || typeof method !== 'function') {
                throw new Error(`Method ${match.propertyKey} is not a function`);
            }
            
            // Extract parameter values
            const paramValues = Object.values(match.params);
            
            // Execute step with timeout if needed
            let output;
            if (match.options.timeoutMs) {
                output = await this.executeWithTimeout(
                    () => method.apply(context || match.target, paramValues),
                    match.options.timeoutMs
                );
            } else {
                output = await method.apply(context || match.target, paramValues);
            }
            
            // Take screenshot after execution if needed
            if (match.options.takeScreenshot) {
                const afterScreenshot = await this.reporting.takeScreenshot(`After_${match.propertyKey}`);
                if (afterScreenshot) {
                    screenshots.push(afterScreenshot);
                }
            }
            
            // Calculate duration
            const durationNs = process.hrtime(startTime);
            const durationMs = (durationNs[0] * 1e9 + durationNs[1]) / 1e6;
            
            // Log success
            this.reporting.log(`Step executed successfully`, Status.PASS);
            this.reporting.endStep(Status.PASS);
            
            // Create execution result
            const result: StepExecutionResult = {
                description: stepText,
                parameters: match.params,
                status: 'Pass',
                durationMs,
                screenshots,
                output,
                timestamp: new Date().toISOString()
            };
            
            // Store result
            this.storeStepResult(match.target.constructor.name, match.propertyKey, result);
            
            return result;
            
        } catch (error) {
            // Calculate duration on error
            const durationNs = process.hrtime(startTime);
            const durationMs = (durationNs[0] * 1e9 + durationNs[1]) / 1e6;
            
            // Take failure screenshot
            try {
                const failureScreenshot = await this.reporting.takeScreenshot(`Failed_${match.propertyKey}`);
                if (failureScreenshot) {
                    screenshots.push(failureScreenshot);
                }
            } catch (screenshotError) {
                console.error('Failed to take error screenshot:', screenshotError);
            }
            
            // Handle error
            const errorMessage = error instanceof Error ? error.message : String(error);
            console.error(`Step "${stepText}" failed: ${errorMessage}`);
            
            // Log error
            this.reporting.log(`Step failed: ${errorMessage}`, Status.FAIL);
            this.reporting.endStep(Status.FAIL);
            
            // Create failure result
            const result: StepExecutionResult = {
                description: stepText,
                parameters: match.params,
                status: 'Fail',
                durationMs,
                errorMessage,
                screenshots,
                timestamp: new Date().toISOString()
            };
            
            // Store result
            this.storeStepResult(match.target.constructor.name, match.propertyKey, result);
            
            // Re-throw the error to propagate it to the caller
            throw error;
        }
    }
    
    /**
     * Execute a function with timeout
     */
    private async executeWithTimeout<T>(fn: () => Promise<T>, timeoutMs: number): Promise<T> {
        return new Promise<T>((resolve, reject) => {
            const timeoutId = setTimeout(() => {
                reject(new Error(`Step execution timed out after ${timeoutMs}ms`));
            }, timeoutMs);
            
            fn()
                .then((result) => {
                    clearTimeout(timeoutId);
                    resolve(result);
                })
                .catch((error) => {
                    clearTimeout(timeoutId);
                    reject(error);
                });
        });
    }
    
    /**
     * Store step execution result
     */
    private storeStepResult(className: string, methodName: string, result: StepExecutionResult): void {
        const key = `${className}.${methodName}`;
        
        if (!this.executionResults.has(key)) {
            this.executionResults.set(key, []);
        }
        
        this.executionResults.get(key)!.push(result);
        
        // Write to file
        this.writeStepResultToFile(className, methodName, result);
    }
    
    /**
     * Write step result to file
     */
    private writeStepResultToFile(className: string, methodName: string, result: StepExecutionResult): void {
        const dir = path.join(this.reportDir, className);
        
        // Create directory if it doesn't exist
        if (!fs.existsSync(dir)) {
            fs.mkdirSync(dir, { recursive: true });
        }
        
        // Create file name with timestamp to avoid overwriting
        const timestamp = new Date().toISOString().replace(/:/g, '-');
        const fileName = `${methodName}_${timestamp}.json`;
        const filePath = path.join(dir, fileName);
        
        // Write to file
        fs.writeFileSync(filePath, JSON.stringify(result, null, 2));
    }
    
    /**
     * Execute a sequence of steps
     * 
     * @param steps Array of step texts
     * @param context Context object to use as 'this' in step methods
     * @param stopOnFailure Whether to stop on first failure
     * @returns Array of step execution results
     */
    public async executeSteps(
        steps: string[], 
        context?: any, 
        stopOnFailure: boolean = true
    ): Promise<StepExecutionResult[]> {
        const results: StepExecutionResult[] = [];
        
        for (const step of steps) {
            try {
                const result = await this.executeStep(step, context);
                results.push(result);
            } catch (error) {
                // If we have a failure result, add it
                const lastResult = results[results.length - 1];
                if (lastResult && lastResult.status === 'Fail') {
                    // Result is already added by executeStep
                } else {
                    // We have a catastrophic error before executeStep could complete
                    results.push({
                        description: step,
                        parameters: {},
                        status: 'Fail',
                        durationMs: 0,
                        errorMessage: error instanceof Error ? error.message : String(error),
                        screenshots: [],
                        timestamp: new Date().toISOString()
                    });
                }
                
                if (stopOnFailure) {
                    break;
                }
            }
        }
        
        return results;
    }
    
    /**
     * Parse a feature file into a structured format
     * 
     * @param featureFilePath Path to the feature file
     * @returns Parsed feature or null if file doesn't exist or is invalid
     */
    public parseFeatureFile(featureFilePath: string): ParsedFeature | null {
        if (!fs.existsSync(featureFilePath)) {
            console.error(`Feature file not found: ${featureFilePath}`);
            return null;
        }
        
        const content = fs.readFileSync(featureFilePath, 'utf8');
        
        // Split content into lines and filter out comments and empty lines
        const lines = content.split(/\r?\n/)
            .map(line => line.trim())
            .filter(line => line !== '' && !line.startsWith('#'));
        
        // Initial parsed feature
        const feature: ParsedFeature = {
            title: '',
            description: '',
            scenarios: [],
            tags: []
        };
        
        let currentScenario: ParsedScenario | null = null;
        let collectingExamples = false;
        let collectingDescription = false;
        let featureDescription: string[] = [];
        let lineIndex = 0;
        
        // Parse tags before feature
        if (lines[lineIndex] && lines[lineIndex].startsWith('@')) {
            feature.tags = lines[lineIndex].split('@')
                .filter(tag => tag.trim() !== '')
                .map(tag => tag.trim());
            lineIndex++;
        }
        
        // Parse feature title
        if (lines[lineIndex] && lines[lineIndex].startsWith('Feature:')) {
            feature.title = lines[lineIndex].substring(8).trim();
            lineIndex++;
            collectingDescription = true;
        } else {
            console.error('Feature definition not found in file');
            return null;
        }
        
        // Parse feature description and scenarios
        while (lineIndex < lines.length) {
            const line = lines[lineIndex];
            
            // End collecting feature description when a scenario or tags are found
            if (line.startsWith('Scenario:') || line.startsWith('Scenario Outline:') || line.startsWith('@')) {
                if (collectingDescription) {
                    feature.description = featureDescription.join('\n').trim();
                    collectingDescription = false;
                }
            }
            
            // Collect feature description
            if (collectingDescription) {
                featureDescription.push(line);
                lineIndex++;
                continue;
            }
            
            // Parse scenario tags
            let scenarioTags: string[] = [];
            if (line.startsWith('@')) {
                scenarioTags = line.split('@')
                    .filter(tag => tag.trim() !== '')
                    .map(tag => tag.trim());
                lineIndex++;
                continue;
            }
            
            // Parse scenario or scenario outline
            if (line.startsWith('Scenario:') || line.startsWith('Scenario Outline:')) {
                // Finish collecting examples for previous scenario outline
                collectingExamples = false;
                
                // Create new scenario
                currentScenario = {
                    title: line.includes(':') ? line.split(':')[1].trim() : '',
                    steps: [],
                    tags: scenarioTags,
                    type: line.startsWith('Scenario Outline:') ? 'Scenario Outline' : 'Scenario'
                };
                
                feature.scenarios.push(currentScenario);
                lineIndex++;
                continue;
            }
            
            // Parse examples section
            if (line.startsWith('Examples:') && currentScenario && currentScenario.type === 'Scenario Outline') {
                collectingExamples = true;
                currentScenario.examples = [];
                lineIndex++;
                
                // Skip empty lines
                while (lineIndex < lines.length && lines[lineIndex].trim() === '') {
                    lineIndex++;
                }
                
                // Parse header row
                if (lineIndex < lines.length) {
                    const headerLine = lines[lineIndex].trim();
                    if (headerLine.startsWith('|') && headerLine.endsWith('|')) {
                        const headers = headerLine
                            .split('|')
                            .map(cell => cell.trim())
                            .filter(cell => cell !== '');
                        
                        lineIndex++;
                        
                        // Parse data rows
                        while (lineIndex < lines.length) {
                            const dataLine = lines[lineIndex].trim();
                            
                            // End of examples section
                            if (!dataLine.startsWith('|')) {
                                break;
                            }
                            
                            const cells = dataLine
                                .split('|')
                                .map(cell => cell.trim())
                                .filter(cell => cell !== '');
                            
                            // Create example object
                            if (cells.length === headers.length) {
                                const example: Record<string, string> = {};
                                
                                for (let i = 0; i < headers.length; i++) {
                                    example[headers[i]] = cells[i];
                                }
                                
                                currentScenario.examples!.push(example);
                            }
                            
                            lineIndex++;
                        }
                        
                        continue;
                    }
                }
            }
            
            // Skip examples section rows
            if (collectingExamples) {
                lineIndex++;
                continue;
            }
            
            // Parse steps
            if (currentScenario && (
                line.startsWith('Given ') || 
                line.startsWith('When ') || 
                line.startsWith('Then ') || 
                line.startsWith('And ') || 
                line.startsWith('But ')
            )) {
                currentScenario.steps.push(line);
                lineIndex++;
                continue;
            }
            
            // Move to next line
            lineIndex++;
        }
        
        return feature;
    }
    
    /**
     * Execute a feature file
     * 
     * @param featureFilePath Path to the feature file
     * @param context Context object to use as 'this' in step methods
     * @returns Results of execution
     */
    public async executeFeatureFile(
        featureFilePath: string, 
        context?: any
    ): Promise<{
        feature: ParsedFeature,
        results: Record<string, StepExecutionResult[]>
    } | null> {
        const feature = this.parseFeatureFile(featureFilePath);
        
        if (!feature) {
            return null;
        }
        
        const results: Record<string, StepExecutionResult[]> = {};
        
        // Execute each scenario
        for (const scenario of feature.scenarios) {
            if (scenario.type === 'Scenario') {
                // Regular scenario
                results[scenario.title] = await this.executeSteps(scenario.steps, context);
            } else {
                // Scenario outline with examples
                if (scenario.examples && scenario.examples.length > 0) {
                    results[scenario.title] = [];
                    
                    for (const example of scenario.examples) {
                        // Replace placeholders in steps with example values
                        const resolvedSteps = scenario.steps.map(step => {
                            let resolvedStep = step;
                            
                            for (const [key, value] of Object.entries(example)) {
                                const placeholder = `<${key}>`;
                                resolvedStep = resolvedStep.replace(new RegExp(placeholder, 'g'), value);
                            }
                            
                            return resolvedStep;
                        });
                        
                        // Execute the steps with replaced values
                        const exampleResults = await this.executeSteps(resolvedSteps, context);
                        results[scenario.title].push(...exampleResults);
                    }
                }
            }
        }
        
        return { feature, results };
    }
    
    /**
     * Execute all feature files in a directory
     * 
     * @param directory Directory containing feature files
     * @param context Context object to use as 'this' in step methods
     * @returns Results of execution
     */
    public async executeFeatureDirectory(
        directory: string = this.featureFilesDir,
        context?: any
    ): Promise<Record<string, { feature: ParsedFeature, results: Record<string, StepExecutionResult[]> }>> {
        if (!fs.existsSync(directory)) {
            console.error(`Feature directory not found: ${directory}`);
            return {};
        }
        
        const featureFiles = this.findFeatureFiles(directory);
        const results: Record<string, { feature: ParsedFeature, results: Record<string, StepExecutionResult[]> }> = {};
        
        for (const file of featureFiles) {
            const featureResult = await this.executeFeatureFile(file, context);
            
            if (featureResult) {
                results[file] = featureResult;
            }
        }
        
        return results;
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
     * Generate report for step executions
     */
    public generateStepReport(): void {
        // Read all result files
        const allResults: StepExecutionResult[] = [];
        
        if (fs.existsSync(this.reportDir)) {
            this.traverseDirectory(this.reportDir, (filePath) => {
                if (filePath.endsWith('.json')) {
                    try {
                        const content = fs.readFileSync(filePath, 'utf8');
                        const result = JSON.parse(content) as StepExecutionResult;
                        allResults.push(result);
                    } catch (error) {
                        console.error(`Error reading result file ${filePath}:`, error);
                    }
                }
            });
        }
        
        // Create summary report
        const report = {
            timestamp: new Date().toISOString(),
            totalSteps: allResults.length,
            passedSteps: allResults.filter(r => r.status === 'Pass').length,
            failedSteps: allResults.filter(r => r.status === 'Fail').length,
            skippedSteps: allResults.filter(r => r.status === 'Skipped').length,
            totalDurationMs: allResults.reduce((sum, r) => sum + r.durationMs, 0),
            steps: allResults.sort((a, b) => a.timestamp.localeCompare(b.timestamp))
        };
        
        // Write report to file
        const reportPath = path.join(this.reportDir, 'step-execution-report.json');
        fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
        
        // Generate HTML report
        this.generateHtmlReport(report);
    }
    
    /**
     * Traverse directory recursively
     */
    private traverseDirectory(dir: string, callback: (filePath: string) => void): void {
        const files = fs.readdirSync(dir);
        
        for (const file of files) {
            const filePath = path.join(dir, file);
            const stat = fs.statSync(filePath);
            
            if (stat.isDirectory()) {
                this.traverseDirectory(filePath, callback);
            } else {
                callback(filePath);
            }
        }
    }
    
    /**
     * Generate HTML report
     */
    private generateHtmlReport(data: any): void {
        const htmlFile = path.join(this.reportDir, 'step-execution-report.html');
        
        // Create HTML content
        let html = `
        <!DOCTYPE html>
        <html>
        <head>
            <title>Step Execution Report</title>
            <style>
                body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }
                h1 { color: #333; }
                .summary { display: flex; margin-bottom: 20px; }
                .summary-item { 
                    flex: 1; 
                    padding: 15px; 
                    margin: 0 10px; 
                    border-radius: 5px; 
                    text-align: center;
                    color: white;
                }
                .total { background-color: #2196F3; }
                .passed { background-color: #4CAF50; }
                .failed { background-color: #F44336; }
                .skipped { background-color: #FF9800; }
                table { width: 100%; border-collapse: collapse; }
                th, td { padding: 10px; text-align: left; border-bottom: 1px solid #ddd; }
                th { background-color: #f2f2f2; }
                tr.Pass { background-color: #e8f5e9; }
                tr.Fail { background-color: #ffebee; }
                tr.Skipped { background-color: #fff3e0; }
                .expandable { cursor: pointer; }
                .details { display: none; padding: 10px; background-color: #f9f9f9; border: 1px solid #ddd; margin-top: 5px; }
                .expanded .details { display: block; }
                .step-params { margin-top: 10px; }
                .param { margin-left: 20px; }
                .screenshots { display: flex; flex-wrap: wrap; margin-top: 10px; }
                .screenshot { margin: 5px; }
                .screenshot img { max-width: 300px; max-height: 200px; border: 1px solid #ddd; }
            </style>
        </head>
        <body>
            <h1>Step Execution Report</h1>
            <p>Generated: ${data.timestamp}</p>
            
            <div class="summary">
                <div class="summary-item total">
                    <h2>Total Steps</h2>
                    <div class="count">${data.totalSteps}</div>
                </div>
                <div class="summary-item passed">
                    <h2>Passed</h2>
                    <div class="count">${data.passedSteps}</div>
                </div>
                <div class="summary-item failed">
                    <h2>Failed</h2>
                    <div class="count">${data.failedSteps}</div>
                </div>
                <div class="summary-item skipped">
                    <h2>Skipped</h2>
                    <div class="count">${data.skippedSteps}</div>
                </div>
            </div>
            
            <h2>Step Details</h2>
            <table>
                <thead>
                    <tr>
                        <th>Step</th>
                        <th>Status</th>
                        <th>Duration</th>
                        <th>Timestamp</th>
                    </tr>
                </thead>
                <tbody>
        `;
        
        // Add rows for each step
        for (const step of data.steps) {
            html += `
                <tr class="${step.status} expandable" onclick="this.classList.toggle('expanded')">
                    <td>${this.escapeHtml(step.description)}</td>
                    <td>${step.status}</td>
                    <td>${(step.durationMs / 1000).toFixed(2)}s</td>
                    <td>${step.timestamp}</td>
                    <td>
                        <div class="details">
            `;
            
            // Add parameters
            if (Object.keys(step.parameters).length > 0) {
                html += `<div class="step-params"><strong>Parameters:</strong>`;
                
                for (const [key, value] of Object.entries(step.parameters)) {
                    html += `<div class="param">${key}: ${this.escapeHtml(String(value))}</div>`;
                }
                
                html += `</div>`;
            }
            
            // Add error message
            if (step.errorMessage) {
                html += `<div class="error-message"><strong>Error:</strong> ${this.escapeHtml(step.errorMessage)}</div>`;
            }
            
            // Add screenshots
            if (step.screenshots && step.screenshots.length > 0) {
                html += `<div class="screenshots">`;
                
                for (const screenshot of step.screenshots) {
                    // Create relative path from report directory
                    const relativePath = path.relative(this.reportDir, screenshot).replace(/\\/g, '/');
                    
                    html += `
                        <div class="screenshot">
                            <a href="${relativePath}" target="_blank">
                                <img src="${relativePath}" alt="Screenshot">
                            </a>
                        </div>
                    `;
                }
                
                html += `</div>`;
            }
            
            html += `
                        </div>
                    </td>
                </tr>
            `;
        }
        
        html += `
                </tbody>
            </table>
            
            <script>
                // Add click handler for expandable rows
                document.querySelectorAll('.expandable').forEach(row => {
                    row.addEventListener('click', () => {
                        row.classList.toggle('expanded');
                    });
                });
            </script>
        </body>
        </html>
        `;
        
        fs.writeFileSync(htmlFile, html);
    }
    
    /**
     * Escape HTML special characters
     */
    private escapeHtml(text: string): string {
        return text
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }
} 