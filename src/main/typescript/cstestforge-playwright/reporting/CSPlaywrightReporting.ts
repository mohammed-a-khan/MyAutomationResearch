import { Page, TestInfo } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';
import { CSPlaywrightDriver } from '../core/CSPlaywrightDriver';

/**
 * Status of a test step or test case
 */
export enum Status {
    PASS = 'PASS',
    FAIL = 'FAIL',
    SKIP = 'SKIP',
    INFO = 'INFO',
    WARN = 'WARN'
}

/**
 * Log entry in a test report
 */
interface LogEntry {
    timestamp: Date;
    message: string;
    status: Status;
}

/**
 * Screenshot entry in a test report
 */
interface Screenshot {
    title: string;
    path: string;
    timestamp: Date;
}

/**
 * Step entry in a test report
 */
interface TestStep {
    name: string;
    status: Status;
    logs: LogEntry[];
    screenshots: Screenshot[];
    startTime: Date;
    endTime?: Date;
}

/**
 * Test case entry in the report
 */
interface TestNode {
    name: string;
    description?: string;
    status: Status;
    logs: LogEntry[];
    screenshots: Screenshot[];
    steps: TestStep[];
    metadata: Record<string, string>;
    startTime: Date;
    endTime?: Date;
    executionTime?: number;
}

/**
 * Custom reporting class for Playwright tests.
 * Generates HTML reports without external dependencies.
 */
export class CSPlaywrightReporting {
    // Static properties for report configuration
    private static readonly REPORT_DIR = 'test-output/reports';
    private static readonly SCREENSHOT_DIR = 'test-output/reports/screenshots';
    private static readonly ASSETS_DIR = 'test-output/reports/assets';
    private static readonly REPORT_TITLE = 'CSTestForge Playwright Test Report';
    private static readonly REPORT_TIMESTAMP = new Date().toISOString().replace(/[:.]/g, '-');

    // Counters for test statistics
    private static totalTests: number = 0;
    private static passedTests: number = 0;
    private static failedTests: number = 0;
    private static skippedTests: number = 0;
    
    // Collection of all tests
    private static allTests: TestNode[] = [];
    
    // Current test case and step
    private currentTest: TestNode | null = null;
    private currentStep: TestStep | null = null;
    private page: Page | null = null;
    private driver: CSPlaywrightDriver;
    
    /**
     * Constructor initializes the reporting system
     */
    constructor() {
        this.createDirectories();
        this.createReportAssets();
        this.driver = new CSPlaywrightDriver();
    }
    
    /**
     * Get a page instance for taking screenshots
     */
    private async getPage(): Promise<Page> {
        if (!this.page) {
            this.page = await this.driver.getPage();
        }
        return this.page;
    }
    
    /**
     * Create required directories for reports and screenshots
     */
    private createDirectories(): void {
        try {
            if (!fs.existsSync(CSPlaywrightReporting.REPORT_DIR)) {
                fs.mkdirSync(CSPlaywrightReporting.REPORT_DIR, { recursive: true });
            }
            
            if (!fs.existsSync(CSPlaywrightReporting.SCREENSHOT_DIR)) {
                fs.mkdirSync(CSPlaywrightReporting.SCREENSHOT_DIR, { recursive: true });
            }
            
            if (!fs.existsSync(CSPlaywrightReporting.ASSETS_DIR)) {
                fs.mkdirSync(CSPlaywrightReporting.ASSETS_DIR, { recursive: true });
            }
        } catch (error) {
            console.error('Failed to create report directories:', error);
        }
    }
    
    /**
     * Create CSS and JavaScript assets for the report
     */
    private createReportAssets(): void {
        try {
            // Create CSS file
            const css = 
                "body { font-family: 'Segoe UI', Arial, sans-serif; margin: 0; padding: 0; background-color: #f5f5f5; color: #333; }\n" +
                ".container { max-width: 1200px; margin: 0 auto; padding: 20px; }\n" +
                ".header { background: linear-gradient(135deg, #2b63c6 0%, #175aa6 100%); color: white; padding: 20px; margin-bottom: 20px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }\n" +
                ".header h1 { margin: 0; font-size: 24px; }\n" +
                ".dashboard { display: flex; justify-content: space-between; margin-bottom: 30px; }\n" +
                ".metric { flex: 1; padding: 15px; margin: 0 10px; text-align: center; background: white; border-radius: 5px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }\n" +
                ".metric.total { border-top: 4px solid #175aa6; }\n" +
                ".metric.passed { border-top: 4px solid #4CAF50; }\n" +
                ".metric.failed { border-top: 4px solid #F44336; }\n" +
                ".metric.skipped { border-top: 4px solid #FF9800; }\n" +
                ".metric h2 { margin: 0; font-size: 16px; font-weight: normal; }\n" +
                ".metric .value { font-size: 32px; margin: 10px 0; }\n" +
                ".tests { margin-top: 20px; }\n" +
                ".test { background: white; margin-bottom: 15px; border-radius: 5px; overflow: hidden; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }\n" +
                ".test-header { padding: 15px; cursor: pointer; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #eee; }\n" +
                ".test-header.pass { border-left: 5px solid #4CAF50; }\n" +
                ".test-header.fail { border-left: 5px solid #F44336; }\n" +
                ".test-header.skip { border-left: 5px solid #FF9800; }\n" +
                ".test-title { font-weight: bold; flex: 1; }\n" +
                ".test-time { color: #666; margin-right: 20px; font-size: 12px; }\n" +
                ".test-status { font-weight: bold; }\n" +
                ".test-status.PASS { color: #4CAF50; }\n" +
                ".test-status.FAIL { color: #F44336; }\n" +
                ".test-status.SKIP { color: #FF9800; }\n" +
                ".test-content { display: none; padding: 20px; border-top: 1px solid #eee; }\n" +
                ".test-content.show { display: block; }\n" +
                ".logs { margin: 15px 0; padding: 10px; background: #f9f9f9; border-radius: 3px; }\n" +
                ".log { padding: 5px; font-family: monospace; font-size: 12px; }\n" +
                ".log.pass { color: #4CAF50; }\n" +
                ".log.fail { color: #F44336; font-weight: bold; }\n" +
                ".log.skip { color: #FF9800; }\n" +
                ".log.info { color: #2196F3; }\n" +
                ".screenshots { display: flex; flex-wrap: wrap; margin: 15px 0; }\n" +
                ".screenshot { margin: 5px; text-align: center; }\n" +
                ".screenshot img { max-width: 300px; max-height: 200px; border: 1px solid #ddd; border-radius: 3px; }\n" +
                ".steps { margin: 15px 0; }\n" +
                ".step { margin-bottom: 10px; padding: 10px; border-left: 3px solid #ddd; background: #f9f9f9; }\n" +
                ".step.pass { border-left-color: #4CAF50; }\n" +
                ".step.fail { border-left-color: #F44336; }\n" +
                ".step.skip { border-left-color: #FF9800; }\n" +
                ".step-header { display: flex; justify-content: space-between; align-items: center; cursor: pointer; }\n" +
                ".step-title { font-weight: bold; flex: 1; }\n" +
                ".step-status { font-weight: bold; }\n" +
                ".step-status.PASS { color: #4CAF50; }\n" +
                ".step-status.FAIL { color: #F44336; }\n" +
                ".step-status.SKIP { color: #FF9800; }\n" +
                ".step-content { display: none; margin-top: 10px; }\n" +
                ".step-content.show { display: block; }\n" +
                ".toggle-btn { padding: 5px 10px; margin-left: 10px; background: #f5f5f5; border: 1px solid #ddd; border-radius: 3px; cursor: pointer; }\n" +
                ".toggle-btn:hover { background: #e0e0e0; }\n";
                
            // Create JavaScript file
            const javascript = 
                "document.addEventListener('DOMContentLoaded', function() {\n" +
                "  // Toggle test details\n" +
                "  document.querySelectorAll('.test-header').forEach(function(header) {\n" +
                "    header.addEventListener('click', function() {\n" +
                "      var content = this.nextElementSibling;\n" +
                "      content.classList.toggle('show');\n" +
                "    });\n" +
                "  });\n" +
                "  \n" +
                "  // Toggle step details\n" +
                "  document.querySelectorAll('.step-header').forEach(function(header) {\n" +
                "    header.addEventListener('click', function() {\n" +
                "      var content = this.nextElementSibling;\n" +
                "      content.classList.toggle('show');\n" +
                "    });\n" +
                "  });\n" +
                "  \n" +
                "  // Toggle all button\n" +
                "  document.getElementById('toggle-all-btn').addEventListener('click', function() {\n" +
                "    var expanded = this.getAttribute('data-expanded') === 'true';\n" +
                "    document.querySelectorAll('.test-content').forEach(function(content) {\n" +
                "      if (expanded) {\n" +
                "        content.classList.remove('show');\n" +
                "      } else {\n" +
                "        content.classList.add('show');\n" +
                "      }\n" +
                "    });\n" +
                "    this.setAttribute('data-expanded', !expanded);\n" +
                "    this.textContent = expanded ? 'Expand All' : 'Collapse All';\n" +
                "  });\n" +
                "});\n";
            
            // Write CSS file
            fs.writeFileSync(path.join(CSPlaywrightReporting.ASSETS_DIR, 'style.css'), css);
            
            // Write JavaScript file
            fs.writeFileSync(path.join(CSPlaywrightReporting.ASSETS_DIR, 'script.js'), javascript);
        } catch (error) {
            console.error('Failed to create report assets:', error);
        }
    }
    
    /**
     * Start a test case.
     * 
     * @param name Test name
     * @param description Optional test description
     */
    public startTest(name: string, description?: string): void {
        this.currentTest = {
            name,
            description,
            status: Status.INFO,
            logs: [],
            screenshots: [],
            steps: [],
            metadata: {},
            startTime: new Date()
        };
        
        CSPlaywrightReporting.totalTests++;
    }
    
    /**
     * End the current test case.
     * 
     * @param status Test status
     */
    public endTest(status: Status = Status.PASS): void {
        if (this.currentTest) {
            this.currentTest.status = status;
            this.currentTest.endTime = new Date();
            this.currentTest.executionTime = this.currentTest.endTime.getTime() - this.currentTest.startTime.getTime();
            
            // Update counters
            if (status === Status.PASS) {
                CSPlaywrightReporting.passedTests++;
            } else if (status === Status.FAIL) {
                CSPlaywrightReporting.failedTests++;
            } else if (status === Status.SKIP) {
                CSPlaywrightReporting.skippedTests++;
            }
            
            // Add to all tests
            CSPlaywrightReporting.allTests.push(this.currentTest);
            this.currentTest = null;
        }
    }
    
    /**
     * Start a test step.
     * 
     * @param name Step name
     */
    public startStep(name: string): void {
        if (this.currentTest) {
            this.currentStep = {
                name,
                status: Status.INFO,
                logs: [],
                screenshots: [],
                startTime: new Date()
            };
        }
    }
    
    /**
     * End the current test step.
     * 
     * @param status Step status
     */
    public endStep(status: Status = Status.PASS): void {
        if (this.currentTest && this.currentStep) {
            this.currentStep.status = status;
            this.currentStep.endTime = new Date();
            
            // Add to current test
            this.currentTest.steps.push(this.currentStep);
            this.currentStep = null;
            
            // Update test status if step failed
            if (status === Status.FAIL && this.currentTest.status !== Status.FAIL) {
                this.currentTest.status = Status.FAIL;
            }
        }
    }
    
    /**
     * Add a log message.
     * 
     * @param message Log message
     * @param status Log status
     */
    public log(message: string, status: Status = Status.INFO): void {
        const logEntry: LogEntry = {
            timestamp: new Date(),
            message,
            status
        };
        
        // Add to current step if exists
        if (this.currentStep) {
            this.currentStep.logs.push(logEntry);
        }
        // Otherwise add to current test
        else if (this.currentTest) {
            this.currentTest.logs.push(logEntry);
        }
        
        // Update test status if log is a failure
        if (status === Status.FAIL && this.currentTest && this.currentTest.status !== Status.FAIL) {
            this.currentTest.status = Status.FAIL;
        }
    }
    
    /**
     * Add metadata to the current test.
     * 
     * @param key Metadata key
     * @param value Metadata value
     */
    public addMetadata(key: string, value: string): void {
        if (this.currentTest) {
            this.currentTest.metadata[key] = value;
        }
    }
    
    /**
     * Take a screenshot and add it to the report.
     * 
     * @param title Screenshot title
     * @param testInfo Playwright TestInfo object for automatic screenshots
     * @returns Path to the screenshot or empty string if failed
     */
    public async takeScreenshot(title: string, testInfo?: TestInfo): Promise<string> {
        try {
            let screenshotPath = '';
            
            // Use testInfo if provided (for automatic screenshots)
            if (testInfo) {
                const screenshot = await this.getPage().then(page => page.screenshot());
                // TestInfo.attach returns void, so we need to generate our own path for tracking
                const timestamp = new Date().getTime();
                const fileName = `${title.replace(/[^a-z0-9]/gi, '_').toLowerCase()}_${timestamp}.png`;
                screenshotPath = path.join(CSPlaywrightReporting.SCREENSHOT_DIR, fileName);
                
                // Save the screenshot separately in our structure
                fs.writeFileSync(screenshotPath, screenshot);
                
                // Also attach to the test report
                await testInfo.attach(title, {
                    contentType: 'image/png',
                    body: screenshot
                });
            }
            // Otherwise take screenshot manually
            else {
                const page = await this.getPage();
                const timestamp = new Date().getTime();
                const fileName = `${title.replace(/[^a-z0-9]/gi, '_').toLowerCase()}_${timestamp}.png`;
                screenshotPath = path.join(CSPlaywrightReporting.SCREENSHOT_DIR, fileName);
                
                await page.screenshot({ path: screenshotPath });
            }
            
            const screenshot: Screenshot = {
                title,
                path: screenshotPath,
                timestamp: new Date()
            };
            
            // Add to current step if exists
            if (this.currentStep) {
                this.currentStep.screenshots.push(screenshot);
            }
            // Otherwise add to current test
            else if (this.currentTest) {
                this.currentTest.screenshots.push(screenshot);
            }
            
            return screenshotPath;
        } catch (error) {
            console.error('Failed to take screenshot:', error);
            return '';
        }
    }
    
    /**
     * Generate the HTML report
     * @returns Path to the generated report
     */
    public static generateReport(): string {
        try {
            // Create index.html
            const indexPath = path.join(this.REPORT_DIR, 'index.html');
            let html = '';
            
            // HTML header
            html += "<!DOCTYPE html>\n";
            html += "<html>\n<head>\n";
            html += "<meta charset=\"UTF-8\">\n";
            html += "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n";
            html += `<title>${this.REPORT_TITLE}</title>\n`;
            html += "<link rel=\"stylesheet\" href=\"assets/style.css\">\n";
            html += "</head>\n<body>\n";
            html += "<div class=\"header\">\n";
            html += "<div class=\"container\">\n";
            html += `<h1>${this.REPORT_TITLE} - ${this.REPORT_TIMESTAMP}</h1>\n`;
            html += "</div>\n";
            html += "</div>\n";
            html += "<div class=\"container\">\n";
            
            // Dashboard
            html += "<div class=\"dashboard\">\n";
            html += `<div class=\"metric total\"><h2>Total Tests</h2><div class=\"value\">${this.totalTests}</div></div>\n`;
            html += `<div class=\"metric passed\"><h2>Passed</h2><div class=\"value\">${this.passedTests}</div></div>\n`;
            html += `<div class=\"metric failed\"><h2>Failed</h2><div class=\"value\">${this.failedTests}</div></div>\n`;
            html += `<div class=\"metric skipped\"><h2>Skipped</h2><div class=\"value\">${this.skippedTests}</div></div>\n`;
            html += "</div>\n";
            
            // Test controls
            html += "<div style=\"text-align: right; margin-bottom: 10px;\">\n";
            html += "<button id=\"toggle-all-btn\" class=\"toggle-btn\" data-expanded=\"false\">Expand All</button>\n";
            html += "</div>\n";
            
            // Tests section
            html += "<div class=\"tests\">\n";
            
            // Add each test
            for (const test of this.allTests) {
                // Test container
                html += "<div class=\"test\">\n";
                
                // Test header
                const statusClass = test.status === Status.PASS ? "pass" : 
                                    test.status === Status.FAIL ? "fail" : "skip";
                
                html += `<div class=\"test-header ${statusClass}\">\n`;
                html += `<div class=\"test-title\">${test.name}</div>\n`;
                html += `<div class=\"test-time\">${this.formatTime(test.executionTime || 0)}</div>\n`;
                html += `<div class=\"test-status ${test.status}\">${test.status}</div>\n`;
                html += "</div>\n";
                
                // Test content
                html += "<div class=\"test-content\">\n";
                
                // Test description
                if (test.description) {
                    html += `<div>${test.description}</div>\n`;
                }
                
                // Test metadata
                if (Object.keys(test.metadata).length > 0) {
                    html += "<div style=\"margin: 10px 0;\">\n";
                    for (const [key, value] of Object.entries(test.metadata)) {
                        html += `<div><strong>${key}:</strong> ${value}</div>\n`;
                    }
                    html += "</div>\n";
                }
                
                // Test logs
                if (test.logs.length > 0) {
                    html += "<div class=\"logs\">\n";
                    for (const log of test.logs) {
                        html += `<div class=\"log ${log.status.toLowerCase()}\">${log.message}</div>\n`;
                    }
                    html += "</div>\n";
                }
                
                // Test screenshots
                if (test.screenshots.length > 0) {
                    html += "<div class=\"screenshots\">\n";
                    for (const screenshot of test.screenshots) {
                        const relativePath = path.relative(this.REPORT_DIR, screenshot.path).replace(/\\/g, '/');
                        html += "<div class=\"screenshot\">\n";
                        html += `<div>${screenshot.title}</div>\n`;
                        html += `<a href=\"${relativePath}\" target=\"_blank\">`;
                        html += `<img src=\"${relativePath}\" alt=\"${screenshot.title}\">`;
                        html += "</a>\n";
                        html += "</div>\n";
                    }
                    html += "</div>\n";
                }
                
                // Steps
                if (test.steps.length > 0) {
                    html += "<div class=\"steps\">\n";
                    html += "<h3>Steps</h3>\n";
                    
                    for (const step of test.steps) {
                        const stepStatusClass = step.status === Status.PASS ? "pass" : 
                                              step.status === Status.FAIL ? "fail" : "skip";
                        
                        html += `<div class=\"step ${stepStatusClass}\">\n`;
                        html += "<div class=\"step-header\">\n";
                        html += `<div class=\"step-title\">${step.name}</div>\n`;
                        html += `<div class=\"step-status ${step.status}\">${step.status}</div>\n`;
                        html += "</div>\n";
                        
                        html += "<div class=\"step-content\">\n";
                        
                        // Step logs
                        if (step.logs.length > 0) {
                            html += "<div class=\"logs\">\n";
                            for (const log of step.logs) {
                                html += `<div class=\"log ${log.status.toLowerCase()}\">${log.message}</div>\n`;
                            }
                            html += "</div>\n";
                        }
                        
                        // Step screenshots
                        if (step.screenshots.length > 0) {
                            html += "<div class=\"screenshots\">\n";
                            for (const screenshot of step.screenshots) {
                                const relativePath = path.relative(this.REPORT_DIR, screenshot.path).replace(/\\/g, '/');
                                html += "<div class=\"screenshot\">\n";
                                html += `<div>${screenshot.title}</div>\n`;
                                html += `<a href=\"${relativePath}\" target=\"_blank\">`;
                                html += `<img src=\"${relativePath}\" alt=\"${screenshot.title}\">`;
                                html += "</a>\n";
                                html += "</div>\n";
                            }
                            html += "</div>\n";
                        }
                        
                        html += "</div>\n"; // End step content
                        html += "</div>\n"; // End step
                    }
                    
                    html += "</div>\n"; // End steps
                }
                
                html += "</div>\n"; // End test content
                html += "</div>\n"; // End test
            }
            
            html += "</div>\n"; // End tests section
            
            // Footer
            html += "<div style=\"margin-top: 30px; text-align: center; color: #666; font-size: 0.8em;\">\n";
            html += `Report generated on: ${new Date().toISOString()}<br>\n`;
            html += "CSTestForge Playwright Framework\n";
            html += "</div>\n";
            
            html += "</div>\n"; // End container
            
            // Add JavaScript
            html += "<script src=\"assets/script.js\"></script>\n";
            
            html += "</body>\n</html>";
            
            // Write HTML to file
            fs.writeFileSync(indexPath, html);
            
            return indexPath;
        } catch (error) {
            console.error('Failed to generate report:', error);
            return '';
        }
    }
    
    /**
     * Format time in milliseconds to a readable format
     * 
     * @param ms Time in milliseconds
     * @returns Formatted time string
     */
    private static formatTime(ms: number): string {
        if (ms < 1000) {
            return `${ms}ms`;
        } else if (ms < 60000) {
            return `${(ms / 1000).toFixed(2)}s`;
        } else {
            const minutes = Math.floor(ms / 60000);
            const seconds = ((ms % 60000) / 1000).toFixed(2);
            return `${minutes}m ${seconds}s`;
        }
    }
} 