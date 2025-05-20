/**
 * CSTestForge Playwright Service
 * 
 * A standalone TypeScript service that manages Playwright browser instances
 * and provides a REST API for the Java backend to interact with them.
 */

import express from 'express';
import { chromium, firefox, webkit, Browser, BrowserContext, Page, ElementHandle } from 'playwright';
import cors from 'cors';
import { v4 as uuidv4 } from 'uuid';
import bodyParser from 'body-parser';
import { Request, Response } from 'express';

interface BrowserSession {
  id: string;
  browser: Browser;
  context: BrowserContext;
  page: Page;
  browserType: string;
  startTime: Date;
  lastUsed: Date;
}

interface ViewportSize {
  width: number;
  height: number;
}

interface SessionConfig {
  sessionId: string;
  browserType: string;
  headless: boolean;
  viewport?: ViewportSize;
  environmentVariables?: Record<string, string>;
  userAgent?: string;
}

// Storage for active browser sessions
const activeSessions = new Map<string, BrowserSession>();

// Configure Express
const app = express();
app.use(cors());
app.use(bodyParser.json({ limit: '50mb' }));

// Constants
const DEFAULT_SERVICE_PORT = 3500;
const SESSION_TIMEOUT_MS = 30 * 60 * 1000;

// Session cleanup interval (30 minutes of inactivity)
setInterval(cleanupInactiveSessions, 5 * 60 * 1000);

/**
 * Cleanup inactive sessions to manage resources
 */
function cleanupInactiveSessions(): void {
  const now = new Date();
  
  for (const [sessionId, session] of activeSessions.entries()) {
    const inactiveTime = now.getTime() - session.lastUsed.getTime();
    
    if (inactiveTime > SESSION_TIMEOUT_MS) {
      console.log(`Closing inactive session: ${sessionId}`);
      closeBrowserSession(sessionId).catch(err => {
        console.error(`Error closing session ${sessionId}:`, err);
      });
    }
  }
}

/**
 * Close a browser session
 */
async function closeBrowserSession(sessionId: string): Promise<void> {
  const session = activeSessions.get(sessionId);
  if (!session) return;
  
  try {
    await session.page.close();
    await session.context.close();
    await session.browser.close();
  } finally {
    activeSessions.delete(sessionId);
  }
}

/**
 * Create a new browser session
 */
async function createBrowserSession(config: SessionConfig): Promise<BrowserSession | null> {
  try {
    let browser: Browser;
    
    // Launch the appropriate browser
    switch (config.browserType) {
      case 'chromium':
        browser = await chromium.launch({ headless: config.headless });
        break;
      case 'firefox':
        browser = await firefox.launch({ headless: config.headless });
        break;
      case 'webkit':
        browser = await webkit.launch({ headless: config.headless });
        break;
      case 'msedge':
        browser = await chromium.launch({ 
          channel: 'msedge',
          headless: config.headless 
        });
        break;
      default:
        browser = await chromium.launch({ headless: config.headless });
    }
    
    // Create a new browser context with viewport settings
    const contextOptions: any = {};
    
    if (config.viewport) {
      contextOptions.viewport = config.viewport;
    }
    
    if (config.userAgent) {
      contextOptions.userAgent = config.userAgent;
    }
    
    const context = await browser.newContext(contextOptions);
    
    // Set environment variables if provided
    if (config.environmentVariables) {
      await context.setExtraHTTPHeaders(
        Object.entries(config.environmentVariables).reduce((acc, [key, value]) => {
          acc[`X-Env-${key}`] = value;
          return acc;
        }, {} as Record<string, string>)
      );
    }
    
    // Create a page
    const page = await context.newPage();
    
    // Create and store the session
    const session: BrowserSession = {
      id: config.sessionId,
      browser,
      context,
      page,
      browserType: config.browserType,
      startTime: new Date(),
      lastUsed: new Date()
    };
    
    activeSessions.set(config.sessionId, session);
    
    return session;
  } catch (error) {
    console.error('Error creating browser session:', error);
    return null;
  }
}

/**
 * Update the last used timestamp for a session
 */
function updateSessionActivity(sessionId: string): void {
  const session = activeSessions.get(sessionId);
  if (session) {
    session.lastUsed = new Date();
  }
}

/**
 * Get a browser session by ID
 */
function getSession(sessionId: string): BrowserSession | undefined {
  const session = activeSessions.get(sessionId);
  if (session) {
    updateSessionActivity(sessionId);
  }
  return session;
}

// REST API endpoints

// Health check endpoint
app.get('/health', (req: Request, res: Response) => {
  res.json({ status: 'ok', sessions: activeSessions.size });
});

// Start a browser session
app.post('/browser/start', async (req: Request, res: Response) => {
  try {
    const config: SessionConfig = req.body;
    
    // Ensure required fields are present
    if (!config.sessionId || !config.browserType) {
      return res.status(400).json({ error: 'Missing required parameters' });
    }
    
    // Check if session already exists
    if (activeSessions.has(config.sessionId)) {
      return res.json({ success: true, message: 'Session already exists' });
    }
    
    const session = await createBrowserSession(config);
    if (session) {
      res.json({ success: true, sessionId: session.id });
    } else {
      res.status(500).json({ error: 'Failed to create browser session' });
    }
  } catch (error) {
    console.error('Error in /browser/start:', error);
    res.status(500).json({ error: String(error) });
  }
});

// Stop a browser session
app.post('/browser/stop', async (req: Request, res: Response) => {
  try {
    const { sessionId } = req.body;
    
    if (!sessionId) {
      return res.status(400).json({ error: 'Missing sessionId parameter' });
    }
    
    if (!activeSessions.has(sessionId)) {
      return res.json({ success: true, message: 'Session not found' });
    }
    
    await closeBrowserSession(sessionId);
    res.json({ success: true });
  } catch (error) {
    console.error('Error in /browser/stop:', error);
    res.status(500).json({ error: String(error) });
  }
});

// Navigate to a URL
app.post('/browser/navigate', async (req: Request, res: Response) => {
  try {
    const { sessionId, url } = req.body;
    
    if (!sessionId || !url) {
      return res.status(400).json({ error: 'Missing required parameters' });
    }
    
    const session = getSession(sessionId);
    if (!session) {
      return res.status(404).json({ error: 'Session not found' });
    }
    
    await session.page.goto(url);
    
    const responseUrl = session.page.url();
    const title = await session.page.title();
    
    res.json({ success: true, url: responseUrl, title });
  } catch (error) {
    console.error('Error in /browser/navigate:', error);
    res.status(500).json({ error: String(error) });
  }
});

// Set viewport size
app.post('/browser/viewport', async (req: Request, res: Response) => {
  try {
    const { sessionId, width, height } = req.body;
    
    if (!sessionId || !width || !height) {
      return res.status(400).json({ error: 'Missing required parameters' });
    }
    
    const session = getSession(sessionId);
    if (!session) {
      return res.status(404).json({ error: 'Session not found' });
    }
    
    await session.page.setViewportSize({ width, height });
    
    res.json({ success: true });
  } catch (error) {
    console.error('Error in /browser/viewport:', error);
    res.status(500).json({ error: String(error) });
  }
});

// Inject script into page
app.post('/browser/inject', async (req: Request, res: Response) => {
  try {
    const { sessionId, script } = req.body;
    
    if (!sessionId || !script) {
      return res.status(400).json({ error: 'Missing required parameters' });
    }
    
    const session = getSession(sessionId);
    if (!session) {
      return res.status(404).json({ error: 'Session not found' });
    }
    
    await session.page.addScriptTag({ content: script });
    
    res.json({ success: true });
  } catch (error) {
    console.error('Error in /browser/inject:', error);
    res.status(500).json({ error: String(error) });
  }
});

// Execute script in page
app.post('/browser/execute', async (req: Request, res: Response) => {
  try {
    const { sessionId, script, args = [], async = false } = req.body;
    
    if (!sessionId || !script) {
      return res.status(400).json({ error: 'Missing required parameters' });
    }
    
    const session = getSession(sessionId);
    if (!session) {
      return res.status(404).json({ error: 'Session not found' });
    }
    
    let result;
    if (async) {
      // For async scripts, we wrap in a promise
      result = await session.page.evaluate(`
        (async () => {
          try {
            return await (${script})(...${JSON.stringify(args)});
          } catch (e) {
            return { error: e.toString() };
          }
        })()
      `);
    } else {
      // For synchronous scripts
      result = await session.page.evaluate(`
        (() => {
          try {
            return (${script})(...${JSON.stringify(args)});
          } catch (e) {
            return { error: e.toString() };
          }
        })()
      `);
    }
    
    res.json({ success: true, result });
  } catch (error) {
    console.error('Error in /browser/execute:', error);
    res.status(500).json({ error: String(error) });
  }
});

// Capture screenshot
app.post('/browser/screenshot', async (req: Request, res: Response) => {
  try {
    const { sessionId, fullPage = true } = req.body;
    
    if (!sessionId) {
      return res.status(400).json({ error: 'Missing sessionId parameter' });
    }
    
    const session = getSession(sessionId);
    if (!session) {
      return res.status(404).json({ error: 'Session not found' });
    }
    
    const screenshot = await session.page.screenshot({ 
      fullPage,
      type: 'jpeg',
      quality: 80
    });
    
    const base64Data = screenshot.toString('base64');
    
    res.json({ 
      success: true, 
      data: base64Data,
      mime: 'image/jpeg'
    });
  } catch (error) {
    console.error('Error in /browser/screenshot:', error);
    res.status(500).json({ error: String(error) });
  }
});

// Capture element screenshot
app.post('/browser/element-screenshot', async (req: Request, res: Response) => {
  try {
    const { sessionId, selector } = req.body;
    
    if (!sessionId || !selector) {
      return res.status(400).json({ error: 'Missing required parameters' });
    }
    
    const session = getSession(sessionId);
    if (!session) {
      return res.status(404).json({ error: 'Session not found' });
    }
    
    // Find the element
    const element = await session.page.$(selector);
    if (!element) {
      return res.status(404).json({ error: 'Element not found' });
    }
    
    const screenshot = await element.screenshot({
      type: 'jpeg',
      quality: 80
    });
    
    const base64Data = screenshot.toString('base64');
    
    res.json({ 
      success: true, 
      data: base64Data,
      mime: 'image/jpeg'
    });
  } catch (error) {
    console.error('Error in /browser/element-screenshot:', error);
    res.status(500).json({ error: String(error) });
  }
});

// Get info about current page
app.post('/browser/info', async (req: Request, res: Response) => {
  try {
    const { sessionId } = req.body;
    
    if (!sessionId) {
      return res.status(400).json({ error: 'Missing sessionId parameter' });
    }
    
    const session = getSession(sessionId);
    if (!session) {
      return res.status(404).json({ error: 'Session not found' });
    }
    
    const url = session.page.url();
    const title = await session.page.title();
    
    res.json({ 
      success: true, 
      url,
      title
    });
  } catch (error) {
    console.error('Error in /browser/info:', error);
    res.status(500).json({ error: String(error) });
  }
});

// Enable/disable network capture
app.post('/browser/network-capture', async (req: Request, res: Response) => {
  try {
    const { sessionId, enabled } = req.body;
    
    if (!sessionId || enabled === undefined) {
      return res.status(400).json({ error: 'Missing required parameters' });
    }
    
    const session = getSession(sessionId);
    if (!session) {
      return res.status(404).json({ error: 'Session not found' });
    }
    
    if (enabled) {
      // Enable network capture
      await session.page.route('**', route => {
        const request = route.request();
        console.log(`Network request: ${request.method()} ${request.url()}`);
        route.continue();
      });
    } else {
      // Disable network capture
      await session.page.unrouteAll();
    }
    
    res.json({ success: true });
  } catch (error) {
    console.error('Error in /browser/network-capture:', error);
    res.status(500).json({ error: String(error) });
  }
});

// Enable/disable console capture
app.post('/browser/console-capture', async (req: Request, res: Response) => {
  try {
    const { sessionId, enabled } = req.body;
    
    if (!sessionId || enabled === undefined) {
      return res.status(400).json({ error: 'Missing required parameters' });
    }
    
    const session = getSession(sessionId);
    if (!session) {
      return res.status(404).json({ error: 'Session not found' });
    }
    
    if (enabled) {
      // Enable console capture
      session.page.on('console', message => {
        console.log(`Console [${message.type()}]: ${message.text()}`);
      });
    } else {
      // Disabling console capture isn't directly supported in Playwright
      // We would need to use a different approach
    }
    
    res.json({ success: true });
  } catch (error) {
    console.error('Error in /browser/console-capture:', error);
    res.status(500).json({ error: String(error) });
  }
});

// Wait for a condition
app.post('/browser/wait', async (req: Request, res: Response) => {
  try {
    const { sessionId, condition, timeout = 30000 } = req.body;
    
    if (!sessionId || !condition) {
      return res.status(400).json({ error: 'Missing required parameters' });
    }
    
    const session = getSession(sessionId);
    if (!session) {
      return res.status(404).json({ error: 'Session not found' });
    }
    
    try {
      await session.page.waitForFunction(condition, { timeout });
      res.json({ success: true, result: true });
    } catch (error) {
      // Timeout or error in condition
      res.json({ success: true, result: false });
    }
  } catch (error) {
    console.error('Error in /browser/wait:', error);
    res.status(500).json({ error: String(error) });
  }
});

// Check if session is alive
app.post('/browser/ping', async (req: Request, res: Response) => {
  try {
    const { sessionId } = req.body;
    
    if (!sessionId) {
      return res.status(400).json({ error: 'Missing sessionId parameter' });
    }
    
    const session = getSession(sessionId);
    if (!session) {
      return res.status(404).json({ isAlive: false });
    }
    
    try {
      // Simple operation to check if the page is responsive
      await session.page.evaluate('1 + 1');
      res.json({ isAlive: true });
    } catch (error) {
      res.json({ isAlive: false });
    }
  } catch (error) {
    console.error('Error in /browser/ping:', error);
    res.status(500).json({ error: String(error) });
  }
});

// Parse command line arguments
const args = process.argv.slice(2);
let port = DEFAULT_SERVICE_PORT;

for (const arg of args) {
  if (arg.startsWith('--port=')) {
    const portStr = arg.split('=')[1];
    const parsedPort = parseInt(portStr);
    if (!isNaN(parsedPort)) {
      port = parsedPort;
    }
  }
}

// Start the server
app.listen(port, () => {
  console.log(`Playwright service started on port ${port}`);
}); 