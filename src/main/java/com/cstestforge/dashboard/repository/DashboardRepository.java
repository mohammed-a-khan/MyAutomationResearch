package com.cstestforge.dashboard.repository;

import com.cstestforge.dashboard.model.DashboardStats;
import com.cstestforge.dashboard.model.EnvironmentStatus;
import com.cstestforge.dashboard.model.FailureAnalysis;
import com.cstestforge.project.model.execution.TestExecution;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Repository interface for dashboard data access
 */
public interface DashboardRepository {
    
    /**
     * Get recent test executions across all projects
     * 
     * @param limit Maximum number of executions to return
     * @return List of recent test executions
     */
    List<TestExecution> getRecentExecutions(int limit);
    
    /**
     * Get recent test executions for a specific project
     * 
     * @param projectId Project ID
     * @param limit Maximum number of executions to return
     * @return List of recent test executions
     */
    List<TestExecution> getRecentExecutions(String projectId, int limit);
    
    /**
     * Get test counts (total, passed, failed, skipped)
     * 
     * @param projectId Optional project ID
     * @return Map of test status to count
     */
    Map<String, Integer> getTestCounts(String projectId);
    
    /**
     * Get test counts by status for a specific day
     * 
     * @param date Date to count tests for
     * @param projectId Optional project ID
     * @return Map of test status to count
     */
    Map<String, Integer> getTestCountsByStatus(LocalDate date, String projectId);
    
    /**
     * Get environment configurations
     * 
     * @return List of environment configurations
     */
    List<EnvironmentStatus> getEnvironmentConfigurations();
    
    /**
     * Check health of an environment
     * 
     * @param url Environment URL
     * @return true if environment is healthy
     */
    boolean checkEnvironmentHealth(String url);
    
    /**
     * Count executions for a specific day
     * 
     * @param date Date to count executions for
     * @param projectId Optional project ID
     * @return Number of executions
     */
    int countExecutionsForDay(LocalDate date, String projectId);
    
    /**
     * Get pass rate for a specific day
     * 
     * @param date Date to get pass rate for
     * @param projectId Optional project ID
     * @return Pass rate (0-100)
     */
    double getPassRateForDay(LocalDate date, String projectId);
    
    /**
     * Get average execution time for a specific day
     * 
     * @param date Date to get average execution time for
     * @param projectId Optional project ID
     * @return Average execution time in milliseconds
     */
    double getAverageExecutionTimeForDay(LocalDate date, String projectId);
    
    /**
     * Get most common failures
     * 
     * @param cutoffDate Cutoff date for failures
     * @param projectId Optional project ID
     * @return List of common failures
     */
    List<FailureAnalysis.CommonFailure> getMostCommonFailures(LocalDateTime cutoffDate, String projectId);
    
    /**
     * Get failures by type
     * 
     * @param cutoffDate Cutoff date for failures
     * @param projectId Optional project ID
     * @return List of failures by type
     */
    List<FailureAnalysis.FailureByType> getFailuresByType(LocalDateTime cutoffDate, String projectId);
    
    /**
     * Get failures by browser
     * 
     * @param cutoffDate Cutoff date for failures
     * @param projectId Optional project ID
     * @return List of failures by browser
     */
    List<FailureAnalysis.FailureByBrowser> getFailuresByBrowser(LocalDateTime cutoffDate, String projectId);
    
    /**
     * Get unstable tests
     * 
     * @param cutoffDate Cutoff date for test executions
     * @param projectId Optional project ID
     * @return List of unstable tests
     */
    List<FailureAnalysis.UnstableTest> getUnstableTests(LocalDateTime cutoffDate, String projectId);
    
    /**
     * Get error counts by type
     * 
     * @param projectId Optional project ID
     * @return Map of error type to count
     */
    Map<String, Integer> getErrorCountsByType(String projectId);
    
    /**
     * Get total test duration
     * 
     * @param projectId Optional project ID
     * @return Total test duration in milliseconds
     */
    long getTotalTestDuration(String projectId);
    
    /**
     * Generate dashboard report
     * 
     * @param stats Dashboard statistics
     * @param format Report format (pdf, csv, excel)
     * @param projectId Optional project ID
     * @return Report data as byte array
     */
    byte[] generateReport(DashboardStats stats, String format, String projectId);
} 