import { defineConfig, devices } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

// Read configuration from properties file
function loadConfig(): Record<string, string> {
  const config: Record<string, string> = {};
  try {
    const configPath = process.env.CSTESTFORGE_CONFIG || 'src/test/resources/cstestforge.properties';
    const content = fs.readFileSync(path.resolve(configPath), 'utf-8');
    
    content.split('\n').forEach((line: string) => {
      line = line.trim();
      if (line && !line.startsWith('#')) {
        const [key, value] = line.split('=').map((part: string) => part.trim());
        if (key && value !== undefined) {
          config[key] = value;
        }
      }
    });
    
    console.log('Loaded configuration from:', configPath);
  } catch (error) {
    console.warn('Failed to load configuration file, using defaults');
  }
  
  return config;
}

const config = loadConfig();

/**
 * See https://playwright.dev/docs/test-configuration
 */
export default defineConfig({
  testDir: './example_test/test',
  timeout: parseInt(config.pageLoadTimeout || '30') * 1000,
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: parseInt(config['retry.max'] || '2'),
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['html', { outputFolder: 'test-output/cstestforge-reports' }],
    ['list']
  ],
  use: {
    baseURL: config['custom.base.url'] || 'http://localhost:8080',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: config['report.video.enabled'] === 'true' ? 'on-first-retry' : 'off',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
  ],
  outputDir: 'test-output/test-results',
}); 