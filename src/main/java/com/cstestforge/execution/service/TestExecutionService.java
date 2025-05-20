package com.cstestforge.execution.service;

import com.cstestforge.execution.model.TestExecutionInfo;
import com.cstestforge.execution.model.TestExecutionRequest;
import com.cstestforge.execution.model.TestStatus;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing test execution
 */
public interface TestExecutionService {

    /**
     * Run tests based on the provided request
     *
     * @param request Test execution request containing test IDs and configuration
     * @return Information about the created execution
     */
    TestExecutionInfo runTests(TestExecutionRequest request);

    /**
     * Get the status of an execution
     *
     * @param executionId Execution ID
     * @return Current execution information
     */
    TestExecutionInfo getExecutionStatus(String executionId);

    /**
     * Stop an ongoing execution
     *
     * @param executionId Execution ID
     * @return Updated execution information
     */
    TestExecutionInfo stopExecution(String executionId);

    /**
     * Get execution details with test results
     *
     * @param executionId Execution ID
     * @return Execution details with results
     */
    TestExecutionInfo getExecutionDetails(String executionId);

    /**
     * Get execution history for a project
     *
     * @param projectId Project ID
     * @param limit Maximum number of executions to return
     * @param offset Offset for pagination
     * @return List of executions
     */
    List<TestExecutionInfo> getExecutionHistory(String projectId, int limit, int offset);

    /**
     * Get execution history for a specific test
     *
     * @param projectId Project ID
     * @param testId Test ID
     * @param limit Maximum number of executions to return
     * @return List of executions for the test
     */
    List<TestExecutionInfo> getTestExecutionHistory(String projectId, String testId, int limit);

    /**
     * Delete an execution
     *
     * @param executionId Execution ID
     * @return true if deleted successfully
     */
    boolean deleteExecution(String executionId);

    /**
     * Get the latest execution for a test
     *
     * @param projectId Project ID
     * @param testId Test ID
     * @return Latest execution information if available
     */
    Optional<TestExecutionInfo> getLatestTestExecution(String projectId, String testId);

    /**
     * Get executions by status
     *
     * @param projectId Project ID
     * @param status Status to filter by
     * @param limit Maximum number of executions to return
     * @return List of executions with the specified status
     */
    List<TestExecutionInfo> getExecutionsByStatus(String projectId, TestStatus status, int limit);

    /**
     * Clean up old executions
     *
     * @param projectId Project ID
     * @param daysToKeep Number of days to keep executions for
     * @return Number of executions deleted
     */
    int cleanupOldExecutions(String projectId, int daysToKeep);
} 