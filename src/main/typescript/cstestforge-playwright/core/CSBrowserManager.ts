import { Browser, BrowserContext, BrowserType, Page, chromium, devices, firefox, webkit } from 'playwright';
import * as fs from 'fs';
import * as path from 'path';

/**
 * CSBrowserManager manages Playwright browser instances using a singleton pattern.
 * Handles browser initialization, configuration, and cleanup.
 */
export class CSBrowserManager {
    private static instance: CSBrowserManager;
    private browserType: BrowserType;
    private browser: Browser | null = null;
    private context: BrowserContext | null = null;
    private page: Page | null = null;
    private config: Record<string, string> = {};
    
    // Thread-local like storage for multi-browser support
    private static readonly contextMap = new Map<string, BrowserContext>();
    private static readonly pageMap = new Map<string, Page>();

    /**
     * Private constructor to enforce singleton pattern
     */
    private constructor() {
        this.loadConfiguration();
        this.browserType = this.getBrowserType();
    }

    /**
     * Get singleton instance of CSBrowserManager
     */
    public static getInstance(): CSBrowserManager {
        if (!CSBrowserManager.instance) {
            CSBrowserManager.instance = new CSBrowserManager();
        }
        return CSBrowserManager.instance;
    }

    /**
     * Load configuration from properties file
     */
    private loadConfiguration(): void {
        try {
            const configPath = process.env.CSTESTFORGE_CONFIG || 'src/test/resources/cstestforge.properties';
            const content = fs.readFileSync(path.resolve(configPath), 'utf-8');
            
            content.split('\n').forEach((line: string) => {
                line = line.trim();
                if (line && !line.startsWith('#')) {
                    const [key, value] = line.split('=').map((part: string) => part.trim());
                    if (key && value !== undefined) {
                        this.config[key] = value;
                    }
                }
            });
            
            console.log('Loaded configuration from:', configPath);
        } catch (error) {
            console.warn('Failed to load configuration file, using defaults');
        }
    }

    /**
     * Get browser type based on configuration
     */
    private getBrowserType(): BrowserType {
        const browser = (this.config.browser || 'chrome').toLowerCase();
        
        switch (browser) {
            case 'firefox':
                return firefox;
            case 'webkit':
            case 'safari':
                return webkit;
            case 'chrome':
            case 'chromium':
            default:
                return chromium;
        }
    }

    /**
     * Get a browser instance, creating one if needed
     */
    public async getBrowser(): Promise<Browser> {
        if (!this.browser) {
            const headless = this.config.headless === 'true';
            const args: string[] = [];
            
            // Add browser-specific options
            if (this.browserType === chromium) {
                const chromeOptions = this.config['chrome.options'] || '';
                if (chromeOptions) {
                    args.push(...chromeOptions.split(',').map(opt => opt.trim()));
                }
            }
            
            this.browser = await this.browserType.launch({
                headless,
                args,
                downloadsPath: path.resolve(this.config['webdriver.download.directory'] || 'downloads'),
            });
            
            // Setup close handler
            process.on('beforeExit', async () => {
                await this.closeBrowser();
            });
        }
        
        return this.browser;
    }

    /**
     * Get a browser context for the current thread/session
     */
    public async getContext(sessionId: string = 'default'): Promise<BrowserContext> {
        // Check if context already exists
        if (CSBrowserManager.contextMap.has(sessionId)) {
            return CSBrowserManager.contextMap.get(sessionId)!;
        }
        
        // Create a new context
        const browser = await this.getBrowser();
        
        const contextOptions: Record<string, any> = {
            viewport: {
                width: parseInt(this.config.windowWidth || '1920'),
                height: parseInt(this.config.windowHeight || '1080'),
            },
            acceptDownloads: true,
            recordVideo: this.config['report.video.enabled'] === 'true' ? {
                dir: path.resolve(this.config['report.directory'] || 'test-output/videos'),
                size: { width: 1280, height: 720 },
            } : undefined,
        };
        
        // Add device emulation if specified
        const deviceName = this.config['device'];
        if (deviceName && devices[deviceName]) {
            Object.assign(contextOptions, devices[deviceName]);
        }
        
        const context = await browser.newContext(contextOptions);
        
        // Configure timeouts
        const timeout = parseInt(this.config.implicitWait || '10') * 1000;
        context.setDefaultTimeout(timeout);
        
        // Store in map
        CSBrowserManager.contextMap.set(sessionId, context);
        
        return context;
    }

    /**
     * Get a page for the current thread/session
     */
    public async getPage(sessionId: string = 'default'): Promise<Page> {
        // Check if page already exists
        if (CSBrowserManager.pageMap.has(sessionId)) {
            return CSBrowserManager.pageMap.get(sessionId)!;
        }
        
        // Get or create context
        const context = await this.getContext(sessionId);
        
        // Create a new page in this context
        const page = await context.newPage();
        
        // Store in map
        CSBrowserManager.pageMap.set(sessionId, page);
        
        return page;
    }

    /**
     * Close a specific context and page
     */
    public async closeContext(sessionId: string = 'default'): Promise<void> {
        if (CSBrowserManager.pageMap.has(sessionId)) {
            CSBrowserManager.pageMap.delete(sessionId);
        }
        
        if (CSBrowserManager.contextMap.has(sessionId)) {
            const context = CSBrowserManager.contextMap.get(sessionId)!;
            await context.close();
            CSBrowserManager.contextMap.delete(sessionId);
        }
    }

    /**
     * Close all contexts and the browser
     */
    public async closeBrowser(): Promise<void> {
        // Close all contexts first
        for (const sessionId of CSBrowserManager.contextMap.keys()) {
            await this.closeContext(sessionId);
        }
        
        // Close the browser
        if (this.browser) {
            await this.browser.close();
            this.browser = null;
        }
    }

    /**
     * Get configuration value by key
     */
    public getConfigValue(key: string, defaultValue: string = ''): string {
        return this.config[key] || defaultValue;
    }
} 