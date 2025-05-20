package com.cstestforge.storage.repository;

import com.cstestforge.project.model.execution.TestExecution;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for managing test executions.
 * Test executions are stored at "projects/{projectId}/executions/"
 */
public interface TestExecutionRepository {

    /**
     * Find a test execution by its ID
     *
     * @param projectId Project ID
     * @param executionId Execution ID
     * @return Optional containing the execution if found
     */
    Optional<TestExecution> findById(String projectId, String executionId);

    /**
     * Find all executions for a project
     *
     * @param projectId Project ID
     * @return List of executions
     */
    List<TestExecution> findAll(String projectId);
    
    /**
     * Find executions by filter criteria
     *
     * @param projectId Project ID
     * @param filters Map of filter criteria
     * @return List of matching executions
     */
    List<TestExecution> findByFilters(String projectId, Map<String, Object> filters);
    
    /**
     * Create a new test execution
     *
     * @param projectId Project ID
     * @param execution Execution to create
     * @return Created execution with ID
     */
    TestExecution create(String projectId, TestExecution execution);
    
    /**
     * Update an existing test execution
     *
     * @param projectId Project ID
     * @param executionId Execution ID
     * @param execution Updated execution data
     * @return Updated execution
     */
    TestExecution update(String projectId, String executionId, TestExecution execution);
    
    /**
     * Delete a test execution
     *
     * @param projectId Project ID
     * @param executionId Execution ID
     * @return true if deleted successfully
     */
    boolean delete(String projectId, String executionId);
    
    /**
     * Find executions for a specific test
     *
     * @param projectId Project ID
     * @param testId Test ID
     * @return List of executions for the test
     */
    List<TestExecution> findByTestId(String projectId, String testId);
    
    /**
     * Find executions within a date range
     *
     * @param projectId Project ID
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of executions in the date range
     */
    List<TestExecution> findByDateRange(String projectId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find executions for a specific test run
     *
     * @param projectId Project ID
     * @param testRunId Test run ID
     * @return List of executions in the test run
     */
    List<TestExecution> findByTestRunId(String projectId, String testRunId);
    
    /**
     * Get the latest execution for a test
     *
     * @param projectId Project ID
     * @param testId Test ID
     * @return Optional containing the latest execution if found
     */
    Optional<TestExecution> findLatestByTestId(String projectId, String testId);
    
    /**
     * Get the count of executions in a project
     *
     * @param projectId Project ID
     * @return Number of executions
     */
    int count(String projectId);
    
    /**
     * Get execution statistics for a project
     *
     * @param projectId Project ID
     * @return Map of statistics
     */
    Map<String, Object> getStatistics(String projectId);
    
    /**
     * Delete old executions beyond a retention period
     *
     * @param projectId Project ID
     * @param cutoffDate Delete executions before this date
     * @return Number of executions deleted
     */
    int deleteOlderThan(String projectId, LocalDateTime cutoffDate);
} 