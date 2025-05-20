/**
 * Execution Service - Handles API calls for test execution
 */
import api from './api';
import { buildApiUrlWithParams } from '../utils/apiClient';
import { 
  TestExecutionRequest, 
  TestExecutionInfo, 
  TestExecutionDetail, 
  TestResult, 
  Screenshot,
  TestExecutionSummary,
  ParallelExecutionStatus,
  TestFilter
} from '../types/execution';

const EXECUTION_API = '/execution';

/**
 * Service for test execution functionality
 */
export const executionService = {
  /**
   * Run selected tests
   * @param request Test execution request
   * @returns The execution information
   */
  async runTests(request: TestExecutionRequest): Promise<TestExecutionInfo> {
    try {
      return await api.post<TestExecutionInfo>(`${EXECUTION_API}/run`, request);
    } catch (error) {
      console.error('Error running tests:', error);
      throw error;
    }
  },

  /**
   * Get execution status
   * @param executionId The execution ID
   * @returns The execution information
   */
  async getExecutionStatus(executionId: string): Promise<TestExecutionInfo> {
    try {
      return await api.get<TestExecutionInfo>(`${EXECUTION_API}/status/${executionId}`);
    } catch (error) {
      console.error('Error getting execution status:', error);
      throw error;
    }
  },

  /**
   * Get execution details including test results
   * @param executionId The execution ID
   * @returns The execution details
   */
  async getExecutionDetails(executionId: string): Promise<TestExecutionDetail> {
    try {
      return await api.get<TestExecutionDetail>(`${EXECUTION_API}/results/${executionId}`);
    } catch (error) {
      console.error('Error getting execution details:', error);
      throw error;
    }
  },

  /**
   * Get a specific test result
   * @param executionId The execution ID
   * @param testId The test ID
   * @returns The test result
   */
  async getTestResult(executionId: string, testId: string): Promise<TestResult> {
    try {
      return await api.get<TestResult>(`${EXECUTION_API}/results/${executionId}/test/${testId}`);
    } catch (error) {
      console.error('Error getting test result:', error);
      throw error;
    }
  },

  /**
   * Stop an execution
   * @param executionId The execution ID
   * @returns Success status
   */
  async stopExecution(executionId: string): Promise<boolean> {
    try {
      const response = await api.post<{ success: boolean }>(`${EXECUTION_API}/stop/${executionId}`);
      return response.success;
    } catch (error) {
      console.error('Error stopping execution:', error);
      throw error;
    }
  },

  /**
   * Get screenshot
   * @param screenshotId The screenshot ID
   * @returns The screenshot data
   */
  async getScreenshot(screenshotId: string): Promise<Screenshot> {
    try {
      return await api.get<Screenshot>(`${EXECUTION_API}/results/screenshot/${screenshotId}`);
    } catch (error) {
      console.error('Error getting screenshot:', error);
      throw error;
    }
  },

  /**
   * Get execution history
   * @param filter The filter criteria
   * @param page The page number (1-based)
   * @param pageSize The page size
   * @returns The execution history items
   */
  async getExecutionHistory(
    filter: TestFilter,
    page: number = 1,
    pageSize: number = 10
  ): Promise<{
    items: TestExecutionInfo[];
    totalItems: number;
    totalPages: number;
  }> {
    try {
      return await api.post(`${EXECUTION_API}/history`, filter, {
        params: {
          page,
          size: pageSize
        }
      });
    } catch (error) {
      console.error('Error getting execution history:', error);
      throw error;
    }
  },

  /**
   * Get test execution summary
   * @param projectId The project ID
   * @returns The test execution summary
   */
  async getExecutionSummary(projectId: string): Promise<TestExecutionSummary> {
    try {
      return await api.get<TestExecutionSummary>(`${EXECUTION_API}/summary/${projectId}`);
    } catch (error) {
      console.error('Error getting execution summary:', error);
      throw error;
    }
  },

  /**
   * Get parallel execution status
   * @returns The parallel execution status
   */
  async getParallelStatus(): Promise<ParallelExecutionStatus> {
    try {
      return await api.get<ParallelExecutionStatus>(`${EXECUTION_API}/parallel/status`);
    } catch (error) {
      console.error('Error getting parallel execution status:', error);
      throw error;
    }
  },

  /**
   * Update parallel execution configuration
   * @param maxParallel The maximum parallel executions
   * @returns Success status
   */
  async updateParallelConfig(maxParallel: number): Promise<boolean> {
    try {
      const response = await api.put<{ success: boolean }>(`${EXECUTION_API}/parallel/config`, { maxParallel });
      return response.success;
    } catch (error) {
      console.error('Error updating parallel config:', error);
      throw error;
    }
  }
};

export default executionService; 