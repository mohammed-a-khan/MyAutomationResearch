import * as fs from 'fs';
import * as path from 'path';
import { Page } from 'playwright';

/**
 * CSPlaywrightScreenshotHandler provides utilities for taking screenshots with Playwright.
 * Handles screenshot capture, storage, and naming conventions.
 */
export class CSPlaywrightScreenshotHandler {
    // Default directory for storing screenshots
    private static readonly SCREENSHOT_DIR = 'test-output/screenshots';
    
    /**
     * Take a screenshot of the current page
     * 
     * @param page Playwright Page instance
     * @param name Screenshot name
     * @param fullPage Whether to take a full page screenshot
     * @returns Path to the saved screenshot or null if failed
     */
    public static async takeScreenshot(page: Page, name: string, fullPage: boolean = false): Promise<string | null> {
        if (!page) {
            console.warn('Cannot take screenshot: page is null');
            return null;
        }
        
        try {
            this.ensureDirectoryExists();
            
            const timestamp = new Date().toISOString()
                .replace(/:/g, '-')
                .replace(/\..+/, '');
            const sanitizedName = this.sanitizeFilename(name);
            const filename = `${timestamp}_${sanitizedName}.png`;
            const filePath = path.join(this.SCREENSHOT_DIR, filename);
            
            await page.screenshot({
                path: filePath,
                fullPage
            });
            
            console.debug(`Screenshot saved to: ${filePath}`);
            return filePath;
        } catch (error) {
            console.error('Failed to save screenshot:', error);
            return null;
        }
    }
    
    /**
     * Take a screenshot of a specific element
     * 
     * @param page Playwright Page instance
     * @param selector Element selector
     * @param name Screenshot name
     * @returns Path to the saved screenshot or null if failed
     */
    public static async takeElementScreenshot(page: Page, selector: string, name: string): Promise<string | null> {
        if (!page) {
            console.warn('Cannot take element screenshot: page is null');
            return null;
        }
        
        try {
            this.ensureDirectoryExists();
            
            const timestamp = new Date().toISOString()
                .replace(/:/g, '-')
                .replace(/\..+/, '');
            const sanitizedName = this.sanitizeFilename(name);
            const filename = `${timestamp}_${sanitizedName}_element.png`;
            const filePath = path.join(this.SCREENSHOT_DIR, filename);
            
            // Find the element
            const element = await page.$(selector);
            if (!element) {
                console.warn(`Element not found: ${selector}`);
                return null;
            }
            
            // Take element screenshot
            await element.screenshot({
                path: filePath
            });
            
            console.debug(`Element screenshot saved to: ${filePath}`);
            return filePath;
        } catch (error) {
            console.error('Failed to save element screenshot:', error);
            return null;
        }
    }
    
    /**
     * Ensure the screenshot directory exists
     */
    private static ensureDirectoryExists(): void {
        if (!fs.existsSync(this.SCREENSHOT_DIR)) {
            fs.mkdirSync(this.SCREENSHOT_DIR, { recursive: true });
            console.debug(`Created screenshot directory: ${this.SCREENSHOT_DIR}`);
        }
    }
    
    /**
     * Sanitize the filename to remove invalid characters
     * 
     * @param filename Filename to sanitize
     * @returns Sanitized filename
     */
    private static sanitizeFilename(filename: string): string {
        if (!filename || filename.trim() === '') {
            return `screenshot_${Math.random().toString(36).substring(2, 10)}`;
        }
        
        // Replace invalid file characters with underscores
        let sanitized = filename.replace(/[^a-zA-Z0-9.-]/g, '_');
        
        // Limit the length of the filename
        if (sanitized.length > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        
        return sanitized;
    }
} 