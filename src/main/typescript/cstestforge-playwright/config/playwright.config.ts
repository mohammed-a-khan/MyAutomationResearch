import { PlaywrightTestConfig, devices } from '@playwright/test';
import { CSPlaywrightRunner } from '../test/CSPlaywrightRunner';

// Initialize the runner with our configuration
const runner = CSPlaywrightRunner.getInstance({
  baseUrl: process.env.BASE_URL || 'http://localhost:3000',
  screenshotOnFailure: true,
  generateHtmlReport: true,
  browserOptions: {
    headless: process.env.HEADLESS !== 'false',
    recordVideo: process.env.RECORD_VIDEO === 'true',
  }
});

// Get the base configuration from our runner
const baseConfig = runner.getPlaywrightConfig();

// Setup the report generation on completion
runner.setupGlobalTeardown();

/**
 * Read environment variables
 */
const CI = process.env.CI === 'true';
const workers = process.env.CI ? 2 : undefined;

/**
 * Complete Playwright Test configuration
 * @see https://playwright.dev/docs/test-configuration
 */
const config: PlaywrightTestConfig = {
  testDir: '../tests',
  timeout: 60_000,
  fullyParallel: !CI,
  forbidOnly: CI,
  retries: CI ? 2 : 0,
  workers: workers,
  reporter: baseConfig.reporter,
  use: {
    ...baseConfig.use,
    actionTimeout: 15_000,
  },
  projects: [
    {
      name: 'Chromium',
      use: {
        ...devices['Desktop Chrome'],
        ...baseConfig.use,
      },
    },
    {
      name: 'Firefox',
      use: {
        ...devices['Desktop Firefox'],
        ...baseConfig.use,
      },
    },
    {
      name: 'Webkit',
      use: {
        ...devices['Desktop Safari'],
        ...baseConfig.use,
      },
    },
    {
      name: 'Mobile Chrome',
      use: {
        ...devices['Pixel 5'],
        ...baseConfig.use,
      },
    },
    {
      name: 'Mobile Safari',
      use: {
        ...devices['iPhone 12'],
        ...baseConfig.use,
      },
    },
  ],
};

export default config; 