# CSTestForge Playwright Service

This service provides a REST API for controlling Playwright browser automation from Java applications.

## Prerequisites

- Node.js 16+ 
- npm or yarn

## Installation

From this directory, run:

```bash
npm install
```

## Running the Service

To start the service:

```bash
npm start
```

This will start the service on port 3500 by default. To use a different port:

```bash
node playwright-service.js --port=4000
```

## API Endpoints

The service exposes the following REST API endpoints:

- `GET /health` - Check if the service is running
- `POST /browser/start` - Launch a browser instance
- `POST /browser/stop` - Close a browser instance
- `POST /browser/navigate` - Navigate to a URL
- `POST /browser/inject` - Inject JavaScript into the page
- `POST /browser/execute` - Execute JavaScript in the page
- `POST /browser/screenshot` - Take a screenshot
- `POST /browser/element-screenshot` - Take a screenshot of a specific element
- `POST /browser/info` - Get information about the current page
- `POST /browser/wait` - Wait for a condition to be met
- `POST /browser/ping` - Check if a browser session is alive
- `POST /browser/viewport` - Set the viewport size
- `POST /browser/network-capture` - Enable/disable network request logging
- `POST /browser/console-capture` - Enable/disable console output logging

## Integration with CSTestForge

This service is used by the CSTestForge `PlaywrightBrowserInstance` class to provide browser automation capabilities for the CSTestForge recorder module. The Java code calls these REST APIs to control browser instances.

## Features

- Support for multiple browser engines (Chromium, Firefox, WebKit, and Microsoft Edge)
- Session-based browser management
- Screenshot capture
- Script injection and execution
- Network and console traffic capturing
- Element-level operations
- Support for headless and non-headless modes
- Automatic cleanup of inactive sessions

## Configuration

The service can be configured through environment variables:

- `NODE_PATH` - Path to Node.js executable
- `PLAYWRIGHT_SERVICE_PATH` - Path to the service script

## Logging

The service logs to the console. To change the log level, set the `LOG_LEVEL` environment variable to one of:

- `debug`
- `info`
- `warn`
- `error` 