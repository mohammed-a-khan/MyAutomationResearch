export enum TestStatus {
  QUEUED = 'QUEUED',
  RUNNING = 'RUNNING',
  PASSED = 'PASSED',
  FAILED = 'FAILED',
  SKIPPED = 'SKIPPED',
  ERROR = 'ERROR',
}

export enum BrowserType {
  CHROME = 'CHROME',
  FIREFOX = 'FIREFOX',
  EDGE = 'EDGE',
  SAFARI = 'SAFARI',
}

export interface ExecutionConfig {
  environment: string;
  browser: BrowserType;
  baseUrl?: string;
  headless: boolean;
  retryCount: number;
  timeoutSeconds: number;
  parallel: boolean;
  maxParallel: number;
  tags?: string[];
  customParams?: Record<string, string>;
}

export interface TestExecutionRequest {
  projectId: string;
  testIds: string[];
  suiteId?: string;
  config: ExecutionConfig;
}

export interface TestExecutionInfo {
  id: string;
  projectId: string;
  status: TestStatus;
  startTime: string;
  endTime?: string;
  duration?: number;
  environment: string;
  browser: BrowserType;
  totalTests: number;
  passedTests: number;
  failedTests: number;
  skippedTests: number;
  errorTests: number;
  runningTests: number;
  queuedTests: number;
  config: ExecutionConfig;
  createdBy: string;
}

export interface TestExecutionDetail extends TestExecutionInfo {
  results: TestResult[];
}

export interface TestResult {
  id: string;
  executionId: string;
  testId: string;
  projectId: string;
  name: string;
  status: TestStatus;
  startTime: string;
  endTime?: string;
  duration?: number;
  errorMessage?: string;
  errorStack?: string;
  stepResults: TestStepResult[];
  logs: LogEntry[];
  screenshots: Screenshot[];
  retryCount: number;
  retryIndex: number;
}

export interface TestStepResult {
  id: string;
  testResultId: string;
  stepId: string;
  name: string;
  status: TestStatus;
  startTime: string;
  endTime?: string;
  duration?: number;
  errorMessage?: string;
  screenshotId?: string;
  order: number;
}

export interface LogEntry {
  id: string;
  testResultId: string;
  timestamp: string;
  level: 'INFO' | 'WARNING' | 'ERROR' | 'DEBUG';
  message: string;
}

export interface Screenshot {
  id: string;
  testResultId: string;
  stepResultId?: string;
  timestamp: string;
  path: string;
  title: string;
  base64Data?: string;
}

export interface TestFilter {
  projectId?: string;
  suiteId?: string;
  status?: TestStatus[];
  tags?: string[];
  searchText?: string;
  dateFrom?: string;
  dateTo?: string;
}

export interface TestExecutionSummary {
  totalExecutions: number;
  passRate: number;
  avgDuration: number;
  executions: {
    id: string;
    startTime: string;
    status: TestStatus;
    passedTests: number;
    failedTests: number;
    totalTests: number;
  }[];
}

export interface ParallelExecutionStatus {
  maxParallel: number;
  currentActive: number;
  queued: number;
  browser: Record<BrowserType, number>;
  environment: Record<string, number>;
} 