import { MetadataRegistry, CSMetaDataOptions } from '../annotation/CSMetaData';
import * as fs from 'fs';
import * as path from 'path';
import * as https from 'https';

/**
 * Azure DevOps API integration options
 */
export interface ADOIntegrationOptions {
    /** Azure DevOps organization */
    organization: string;
    /** Azure DevOps project */
    project: string;
    /** Personal access token for API access */
    pat: string;
    /** API version */
    apiVersion?: string;
}

/**
 * Processor for CSMetaData annotations.
 * Handles metadata processing, reporting, and integration with Azure DevOps.
 */
export class CSMetaDataProcessor {
    private static instance: CSMetaDataProcessor;
    private adoOptions?: ADOIntegrationOptions;
    private reportDir: string = 'test-output/metadata';
    
    /**
     * Private constructor for singleton pattern
     */
    private constructor() {
        this.createDirectories();
    }
    
    /**
     * Get processor instance
     */
    public static getInstance(): CSMetaDataProcessor {
        if (!CSMetaDataProcessor.instance) {
            CSMetaDataProcessor.instance = new CSMetaDataProcessor();
        }
        return CSMetaDataProcessor.instance;
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
     * Configure Azure DevOps integration
     */
    public configureADOIntegration(options: ADOIntegrationOptions): void {
        this.adoOptions = {
            ...options,
            apiVersion: options.apiVersion || '7.0'
        };
    }
    
    /**
     * Process metadata for a test case
     * 
     * @param target Class instance
     * @param methodName Method name
     * @param testResult Test result (pass/fail)
     * @param duration Test duration in milliseconds
     * @param errorMessage Error message if test failed
     */
    public async processMetadata(
        target: Object, 
        methodName: string, 
        testResult: 'Pass' | 'Fail' | 'Skipped', 
        duration: number,
        errorMessage?: string
    ): Promise<void> {
        const metadata = MetadataRegistry.getMetadata(target, methodName);
        if (!metadata) {
            return;
        }
        
        // Write metadata to file for reporting
        await this.writeMetadataToReport(target, methodName, metadata, testResult, duration, errorMessage);
        
        // Update test case in Azure DevOps if configured
        if (this.adoOptions && metadata.id) {
            await this.updateTestCaseInADO(metadata, testResult, duration, errorMessage);
        }
    }
    
    /**
     * Write metadata to a JSON report file
     */
    private async writeMetadataToReport(
        target: Object,
        methodName: string,
        metadata: CSMetaDataOptions,
        testResult: 'Pass' | 'Fail' | 'Skipped',
        duration: number,
        errorMessage?: string
    ): Promise<void> {
        // Create report file name from class and method
        const className = target.constructor.name;
        const fileName = `${className}.${methodName}.json`;
        const filePath = path.join(this.reportDir, fileName);
        
        // Create report data
        const reportData = {
            ...metadata,
            className,
            methodName,
            testResult,
            duration,
            errorMessage,
            timestamp: new Date().toISOString()
        };
        
        // Write to file
        fs.writeFileSync(filePath, JSON.stringify(reportData, null, 2));
    }
    
    /**
     * Update test case in Azure DevOps
     */
    private async updateTestCaseInADO(
        metadata: CSMetaDataOptions,
        testResult: 'Pass' | 'Fail' | 'Skipped',
        duration: number,
        errorMessage?: string
    ): Promise<void> {
        if (!this.adoOptions || !metadata.id) {
            return;
        }
        
        try {
            // Create test run with results
            const testRunData = {
                name: `Automated run for ${metadata.title || metadata.id}`,
                automated: true,
                comment: `Executed through CSTestForge at ${new Date().toISOString()}`,
                testCases: [
                    {
                        id: metadata.id,
                        outcome: testResult,
                        durationInMs: duration,
                        errorMessage: errorMessage || '',
                        priority: metadata.priority,
                        owner: metadata.owner,
                    }
                ]
            };
            
            // Create a test run
            const testRun = await this.callADOApi(
                `/${this.adoOptions.project}/_apis/test/runs`, 
                'POST', 
                testRunData
            );
            
            if (testRun && testRun.id) {
                // Update the work item with test results
                const workItemData: {
                    fields: Record<string, any>
                } = {
                    fields: {
                        "System.History": `Test run completed with result: ${testResult}.\n${errorMessage ? `Error: ${errorMessage}` : ''}`,
                    }
                };
                
                if (metadata.priority) {
                    workItemData.fields["Microsoft.VSTS.Common.Priority"] = metadata.priority;
                }
                
                if (metadata.severity) {
                    workItemData.fields["Microsoft.VSTS.Common.Severity"] = metadata.severity;
                }
                
                // Update the work item
                await this.callADOApi(
                    `/${this.adoOptions.project}/_apis/wit/workitems/${metadata.id}`, 
                    'PATCH',
                    workItemData
                );
                
                // Add test result details to test run
                const testResultData = {
                    testRunId: testRun.id,
                    testResults: [
                        {
                            testCaseId: metadata.id,
                            outcome: testResult,
                            durationInMs: duration,
                            errorMessage: errorMessage,
                            state: "Completed"
                        }
                    ]
                };
                
                await this.callADOApi(
                    `/${this.adoOptions.project}/_apis/test/runs/${testRun.id}/results`, 
                    'POST',
                    testResultData
                );
            }
        } catch (error) {
            console.error(`Failed to update test case in Azure DevOps: ${error}`);
        }
    }
    
    /**
     * Call Azure DevOps REST API
     */
    private async callADOApi(
        apiPath: string,
        method: 'GET' | 'POST' | 'PATCH' | 'PUT' | 'DELETE',
        body?: any
    ): Promise<any> {
        if (!this.adoOptions) {
            throw new Error('Azure DevOps integration not configured');
        }
        
        return new Promise((resolve, reject) => {
            const options = {
                hostname: 'dev.azure.com',
                path: `/${this.adoOptions!.organization}${apiPath}?api-version=${this.adoOptions!.apiVersion}`,
                method,
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Basic ${Buffer.from(`:${this.adoOptions!.pat}`).toString('base64')}`
                }
            };
            
            const req = https.request(options, (res) => {
                let data = '';
                
                res.on('data', (chunk) => {
                    data += chunk;
                });
                
                res.on('end', () => {
                    if (res.statusCode && res.statusCode >= 200 && res.statusCode < 300) {
                        try {
                            resolve(data ? JSON.parse(data) : {});
                        } catch (e) {
                            resolve(data);
                        }
                    } else {
                        reject(new Error(`HTTP error ${res.statusCode}: ${data}`));
                    }
                });
            });
            
            req.on('error', (error) => {
                reject(error);
            });
            
            if (body) {
                req.write(JSON.stringify(body));
            }
            
            req.end();
        });
    }
    
    /**
     * Generate a full report of all test metadata
     */
    public generateFullReport(): void {
        const reportData: any[] = [];
        
        // Read all individual metadata files
        const files = fs.readdirSync(this.reportDir);
        for (const file of files) {
            if (file.endsWith('.json')) {
                const filePath = path.join(this.reportDir, file);
                const content = fs.readFileSync(filePath, 'utf8');
                try {
                    const testData = JSON.parse(content);
                    reportData.push(testData);
                } catch (e) {
                    console.error(`Error parsing metadata file ${file}: ${e}`);
                }
            }
        }
        
        // Write consolidated report
        const consolidatedReport = {
            timestamp: new Date().toISOString(),
            testCount: reportData.length,
            passedCount: reportData.filter(t => t.testResult === 'Pass').length,
            failedCount: reportData.filter(t => t.testResult === 'Fail').length,
            skippedCount: reportData.filter(t => t.testResult === 'Skipped').length,
            tests: reportData
        };
        
        fs.writeFileSync(
            path.join(this.reportDir, 'consolidated-report.json'), 
            JSON.stringify(consolidatedReport, null, 2)
        );
        
        // Generate HTML report
        this.generateHtmlReport(consolidatedReport);
    }
    
    /**
     * Generate HTML report from consolidated data
     */
    private generateHtmlReport(data: any): void {
        const htmlFile = path.join(this.reportDir, 'report.html');
        
        // Create HTML content
        let html = `
        <!DOCTYPE html>
        <html>
        <head>
            <title>Test Metadata Report</title>
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
                tr.failed { background-color: #ffebee; }
                tr.passed { background-color: #e8f5e9; }
                tr.skipped { background-color: #fff3e0; }
            </style>
        </head>
        <body>
            <h1>Test Metadata Report</h1>
            <p>Generated: ${data.timestamp}</p>
            
            <div class="summary">
                <div class="summary-item total">
                    <h2>Total Tests</h2>
                    <div class="count">${data.testCount}</div>
                </div>
                <div class="summary-item passed">
                    <h2>Passed</h2>
                    <div class="count">${data.passedCount}</div>
                </div>
                <div class="summary-item failed">
                    <h2>Failed</h2>
                    <div class="count">${data.failedCount}</div>
                </div>
                <div class="summary-item skipped">
                    <h2>Skipped</h2>
                    <div class="count">${data.skippedCount}</div>
                </div>
            </div>
            
            <h2>Test Details</h2>
            <table>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Title</th>
                        <th>Class/Method</th>
                        <th>Priority</th>
                        <th>Result</th>
                        <th>Duration</th>
                        <th>Error</th>
                    </tr>
                </thead>
                <tbody>
        `;
        
        // Add rows for each test
        for (const test of data.tests) {
            html += `
                <tr class="${test.testResult.toLowerCase()}">
                    <td>${test.id || ''}</td>
                    <td>${test.title || ''}</td>
                    <td>${test.className}.${test.methodName}</td>
                    <td>${test.priority || ''}</td>
                    <td>${test.testResult}</td>
                    <td>${(test.duration / 1000).toFixed(2)}s</td>
                    <td>${test.errorMessage || ''}</td>
                </tr>
            `;
        }
        
        html += `
                </tbody>
            </table>
        </body>
        </html>
        `;
        
        fs.writeFileSync(htmlFile, html);
    }
} 