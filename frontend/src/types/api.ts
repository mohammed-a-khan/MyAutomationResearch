/**
 * API Type definitions for CSTestForge
 */

// Common types

export interface ApiResponse<T> {
  data: T;
  status: string;
  message?: string;
  timestamp: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
}

// Project Management Types

export interface Project {
  id: string;
  name: string;
  description: string;
  createdAt: number;
  updatedAt: number;
  browserType: 'chrome' | 'firefox' | 'edge' | 'safari';
  framework: 'selenium' | 'playwright';
  tags: string[];
  settings: ProjectSettings;
}

export interface ProjectSettings {
  baseUrl: string;
  defaultTimeout: number;
  implicitWait: number;
  screenshotOnFailure: boolean;
  recordVideo: boolean;
  retryFailedTests: boolean;
  maxRetries: number;
  parallel: boolean;
  maxParallelInstances: number;
  customCapabilities: Record<string, any>;
}

// Test Recorder Types

export interface RecordingSession {
  id: string;
  projectId: string;
  name: string;
  status: 'running' | 'completed' | 'error';
  startTime: number;
  endTime?: number;
  browserType: string;
  userAgent: string;
  events: RecordedEvent[];
}

export interface RecordedEvent {
  id: string;
  timestamp: number;
  type: 'click' | 'type' | 'navigate' | 'select' | 'assertion' | 'wait' | 'custom';
  target: ElementInfo;
  value?: string;
  screenshot?: string;
}

export interface ElementInfo {
  xpath: string;
  css: string;
  id?: string;
  name?: string;
  tagName: string;
  className?: string;
  text?: string;
  attributes: Record<string, string>;
}

// Code Builder Types

export interface TestScript {
  id: string;
  projectId: string;
  name: string;
  description: string;
  createdAt: number;
  updatedAt: number;
  steps: TestStep[];
  variables: Variable[];
  imports: string[];
  tags: string[];
}

export interface TestStep {
  id: string;
  type: 'action' | 'assertion' | 'wait' | 'variable' | 'conditional' | 'loop' | 'comment';
  command: string;
  target?: string;
  value?: string;
  description?: string;
  enabled: boolean;
  screenshot?: string;
}

export interface Variable {
  id: string;
  name: string;
  value: string;
  type: 'string' | 'number' | 'boolean' | 'array' | 'object';
  scope: 'global' | 'test' | 'step';
}

export interface LocatorSuggestion {
  value: string;
  type: 'id' | 'css' | 'xpath' | 'name' | 'className' | 'tagName' | 'linkText';
  confidence: number;
  unique: boolean;
}

// Test Execution Types

export interface TestRun {
  id: string;
  projectId: string;
  testScriptId: string;
  name: string;
  status: 'queued' | 'running' | 'passed' | 'failed' | 'skipped' | 'error';
  startTime: number;
  endTime?: number;
  duration: number;
  environment: string;
  browser: string;
  results: TestStepResult[];
  logs: string[];
  screenshots: Screenshot[];
  failures: Failure[];
}

export interface TestStepResult {
  stepId: string;
  status: 'passed' | 'failed' | 'skipped' | 'error';
  duration: number;
  message?: string;
  screenshotId?: string;
  timestamp: number;
}

export interface Screenshot {
  id: string;
  timestamp: number;
  path: string;
  thumbnail?: string;
  label?: string;
}

export interface Failure {
  message: string;
  type: string;
  stepId: string;
  timestamp: number;
  stackTrace?: string;
  screenshotId?: string;
}

// API Testing Types

export interface ApiRequest {
  id: string;
  projectId: string;
  name: string;
  description: string;
  method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH' | 'HEAD' | 'OPTIONS';
  url: string;
  headers: Record<string, string>;
  queryParams: Record<string, string>;
  body?: string;
  bodyType: 'json' | 'form' | 'text' | 'xml' | 'binary' | 'none';
  assertions: ApiAssertion[];
  variables: Variable[];
  createdAt: number;
  updatedAt: number;
}

export interface ApiAssertion {
  id: string;
  type: 'status' | 'header' | 'body' | 'jsonPath' | 'responseTime';
  property?: string;
  operator: '=' | '!=' | '>' | '<' | '>=' | '<=' | 'contains' | 'not_contains' | 'exists' | 'not_exists' | 'matches';
  expected: string;
}

export interface ApiRequestResponse {
  status: number;
  statusText: string;
  headers: Record<string, string>;
  body: string;
  responseTime: number;
  size: number;
}

export interface ApiEnvironment {
  id: string;
  projectId: string;
  name: string;
  variables: Variable[];
  isDefault: boolean;
}

// Export Types

export interface ExportConfig {
  projectId: string;
  testScriptIds: string[];
  format: 'selenium-java' | 'selenium-python' | 'playwright-typescript' | 'playwright-javascript' | 'cypress' | 'cucumber-java' | 'cucumber-javascript';
  includeComments: boolean;
  includeScreenshots: boolean;
  includeDataFiles: boolean;
  options: Record<string, any>;
}

// Dashboard Types

export interface TestStats {
  totalTests: number;
  passedTests: number;
  failedTests: number;
  skippedTests: number;
  successRate: number;
  avgDuration: number;
  testsByDay: DailyTestCount[];
}

export interface DailyTestCount {
  date: string;
  total: number;
  passed: number;
  failed: number;
  skipped: number;
}

export interface RecentTest {
  id: string;
  name: string;
  status: string;
  duration: number;
  timestamp: number;
}

export interface EnvironmentStatus {
  name: string;
  status: 'available' | 'degraded' | 'unavailable';
  details?: string;
}

// Azure DevOps Integration Types

export interface AdoConnection {
  id: string;
  name: string;
  url: string;
  pat: string;
  isConnected: boolean;
  projectName: string;
  createdAt: number;
  updatedAt: number;
}

export interface AdoTestCase {
  id: string;
  testCaseId: string;
  title: string;
  state: string;
  priority: number;
  automationStatus: string;
  testScriptId?: string;
  lastSyncTime?: number;
}

export interface AdoPipeline {
  id: string;
  name: string;
  status: string;
  url: string;
  definitionId: number;
  lastRunId?: number;
  lastRunStatus?: string;
  lastRunTime?: number;
} 