/**
 * API Configuration for CSTestForge
 */

// Base API URL
// In production, this would be configured based on environment
export const API_BASE_URL = '/cstestforge';

// API timeout in milliseconds
export const API_TIMEOUT = 30000; // 30 seconds

// Default headers for API requests
export const DEFAULT_HEADERS = {
  'Content-Type': 'application/json',
  'Accept': 'application/json',
};

// Endpoints for different modules
export const ENDPOINTS = {
  // Project Management
  PROJECTS: '/api/projects',
  PROJECT_DETAILS: (id: string) => `/api/projects/${id}`,
  PROJECT_SETTINGS: (id: string) => `/api/projects/${id}/settings`,
  PROJECT_EXPORT: (id: string) => `/api/projects/${id}/export`,
  PROJECT_IMPORT: '/api/projects/import',
  PROJECT_TAGS: '/api/projects/tags',
  
  // Test Recorder
  RECORDER_START: '/api/recorder/start',
  RECORDER_STOP: '/api/recorder/stop',
  RECORDER_EVENTS: '/api/recorder/events',
  RECORDER_SCREENSHOT: '/api/recorder/screenshot',
  RECORDER_ELEMENTS: '/api/recorder/elements',
  
  // Code Builder
  CODE_GENERATE: '/api/builder/generate',
  CODE_VALIDATE: '/api/builder/validate',
  CODE_LOCATORS: '/api/builder/locators',
  CODE_VARIABLES: '/api/builder/variables',
  
  // Test Execution
  TESTS: '/api/tests',
  TEST_RUN: (id: string) => `/api/tests/${id}/run`,
  TEST_RESULT: (id: string) => `/api/tests/${id}/result`,
  TEST_RERUN: (id: string) => `/api/tests/${id}/rerun`,
  TEST_STOP: (id: string) => `/api/tests/${id}/stop`,
  
  // API Testing
  API_REQUESTS: '/api/api-test/requests',
  API_REQUEST_EXECUTE: (id: string) => `/api/api-test/requests/${id}/execute`,
  API_ENVIRONMENTS: '/api/api-test/environments',
  API_VARIABLES: '/api/api-test/variables',
  
  // Export
  EXPORT_OPTIONS: '/api/export/options',
  EXPORT_EXECUTE: '/api/export/execute',
  EXPORT_PREVIEW: '/api/export/preview',
  
  // Dashboard
  DASHBOARD_STATS: '/api/dashboard/summary',
  DASHBOARD_RECENT_TESTS: '/api/dashboard/activity',
  DASHBOARD_ENVIRONMENT: '/api/dashboard/health',
  DASHBOARD_METRICS: '/api/dashboard/metrics',
  DASHBOARD_SUMMARY: '/api/dashboard/summary',
  DASHBOARD_TIMELINE: '/api/dashboard/trends/executions',
  DASHBOARD_FAILURES: '/api/dashboard/tests/top-failing',
  DASHBOARD_COVERAGE: '/api/dashboard/metrics',
  
  // ADO Integration
  ADO_CONNECTION: '/api/ado/connection',
  ADO_TEST_CASES: '/api/ado/test-cases',
  ADO_SYNC: '/api/ado/sync',
  ADO_PIPELINES: '/api/ado/pipelines',
  
  // Auth
  LOGIN: '/api/auth/login',
  LOGOUT: '/api/auth/logout',
  CURRENT_USER: '/api/auth/user',
};

// Export as default object for easy import
export default {
  API_BASE_URL,
  API_TIMEOUT,
  DEFAULT_HEADERS,
  ENDPOINTS,
}; 