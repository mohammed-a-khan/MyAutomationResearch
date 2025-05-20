/**
 * Dashboard Service - Handles API calls for dashboard data
 */
import api from './api';
import { ENDPOINTS } from '../config/apiConfig';
import { buildApiUrlWithParams } from '../utils/apiClient';
import { 
  TestStats, 
  RecentTest, 
  EnvironmentStatus,
  DailyTestCount
} from '../types/api';

// Define dashboard-specific types
export interface TestTimelineData {
  startDate: string;
  endDate: string;
  executionsByDay: {
    date: string;
    count: number;
    passRate: number;
  }[];
}

export interface MetricsData {
  labels: string[];
  datasets: {
    label: string;
    data: number[];
    backgroundColor?: string[];
    borderColor?: string;
    fill?: boolean;
  }[];
}

export interface FailureAnalysis {
  mostCommonFailures: {
    message: string;
    count: number;
    testIds: string[];
  }[];
  failuresByType: {
    type: string;
    count: number;
    percentage: number;
  }[];
  failuresByBrowser: {
    browser: string;
    count: number;
    percentage: number;
  }[];
  unstableTests: {
    testId: string;
    testName: string;
    failureRate: number;
    lastExecuted: number;
  }[];
}

/**
 * Get test statistics for the dashboard
 * @returns Promise with test statistics
 */
export const getTestStats = async (): Promise<TestStats> => {
  try {
    return await api.get<TestStats>(ENDPOINTS.DASHBOARD_STATS);
  } catch (error) {
    console.error('Failed to fetch dashboard stats:', error);
    throw error;
  }
};

/**
 * Get recent test executions
 * @param limit Number of recent tests to retrieve
 * @returns Promise with list of recent tests
 */
export const getRecentTests = async (limit: number = 10): Promise<RecentTest[]> => {
  try {
    return await api.get<RecentTest[]>(buildApiUrlWithParams(ENDPOINTS.DASHBOARD_RECENT_TESTS, { limit }));
  } catch (error) {
    console.error(`Failed to fetch recent tests (limit ${limit}):`, error);
    throw error;
  }
};

/**
 * Get environment status for all configured environments
 * @returns Promise with list of environment statuses
 */
export const getEnvironmentStatus = async (): Promise<EnvironmentStatus[]> => {
  try {
    return await api.get<EnvironmentStatus[]>(ENDPOINTS.DASHBOARD_ENVIRONMENT);
  } catch (error) {
    console.error('Failed to fetch environment status:', error);
    throw error;
  }
};

/**
 * Get metrics data for charts
 * @param metricType Type of metric to retrieve
 * @param period Period to retrieve metrics for (day, week, month)
 * @returns Promise with metrics data
 */
export const getMetrics = async (
  metricType: 'execution' | 'performance' | 'errors',
  period: 'day' | 'week' | 'month' = 'week'
): Promise<MetricsData> => {
  try {
    return await api.get<MetricsData>(buildApiUrlWithParams(ENDPOINTS.DASHBOARD_METRICS, { type: metricType, period }));
  } catch (error) {
    console.error(`Failed to fetch metrics (type: ${metricType}, period: ${period}):`, error);
    throw error;
  }
};

/**
 * Get detailed test execution summary
 * @returns Promise with test execution summary
 */
export const getExecutionSummary = async (): Promise<TestStats> => {
  try {
    return await api.get<TestStats>(ENDPOINTS.DASHBOARD_SUMMARY);
  } catch (error) {
    console.error('Failed to fetch execution summary:', error);
    throw error;
  }
};

/**
 * Get test execution timeline data
 * @param days Number of days to retrieve data for (default: 30)
 * @returns Promise with test timeline data
 */
export const getTestTimeline = async (days: number = 30): Promise<TestTimelineData> => {
  try {
    return await api.get<TestTimelineData>(buildApiUrlWithParams(ENDPOINTS.DASHBOARD_TIMELINE, { days }));
  } catch (error) {
    console.error(`Failed to fetch test timeline (days: ${days}):`, error);
    throw error;
  }
};

/**
 * Get failure analysis data
 * @param limit Number of failures to retrieve (default: 30)
 * @returns Promise with failure analysis data
 */
export const getFailureAnalysis = async (limit: number = 30): Promise<FailureAnalysis> => {
  try {
    return await api.get<FailureAnalysis>(buildApiUrlWithParams(ENDPOINTS.DASHBOARD_FAILURES, { limit }));
  } catch (error) {
    console.error(`Failed to fetch failure analysis (limit: ${limit}):`, error);
    throw error;
  }
};

/**
 * Get test coverage by folder/feature
 * @returns Promise with test coverage data
 */
export const getTestCoverage = async (): Promise<MetricsData> => {
  try {
    return await api.get<MetricsData>(buildApiUrlWithParams(ENDPOINTS.DASHBOARD_COVERAGE, { type: 'coverage' }));
  } catch (error) {
    console.error('Failed to fetch test coverage:', error);
    throw error;
  }
};

/**
 * Rerun a specific test
 * @param testId ID of the test to rerun
 * @returns Promise with the rerun response
 */
export const rerunTest = async (testId: string): Promise<any> => {
  try {
    return await api.post(ENDPOINTS.TEST_RERUN(testId));
  } catch (error) {
    console.error(`Failed to rerun test (id: ${testId}):`, error);
    throw error;
  }
};

/**
 * Export dashboard data as a report
 * @param format Format of the report to generate
 * @returns Promise that resolves when the report is downloaded
 */
export const exportDashboardReport = async (format: 'pdf' | 'csv' | 'excel'): Promise<void> => {
  try {
    const filename = `dashboard-report-${new Date().toISOString().slice(0, 10)}.${format}`;
    return await api.downloadFile(buildApiUrlWithParams(`${ENDPOINTS.DASHBOARD_STATS}/export`, { format }), filename);
  } catch (error) {
    console.error(`Failed to export dashboard report (format: ${format}):`, error);
    throw error;
  }
}; 