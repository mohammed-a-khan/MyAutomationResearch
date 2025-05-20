package com.cstestforge.dashboard.service;

import com.cstestforge.dashboard.model.*;
import java.util.List;

/**
 * Service interface for dashboard functionality
 */
public interface DashboardService {
    
    /**
     * Get dashboard statistics
     * 
     * @param projectId Optional project ID to filter stats by project
     * @return Dashboard statistics
     */
    DashboardStats getStats(String projectId);
    
    /**
     * Get recent test executions
     * 
     * @param limit Maximum number of tests to return
     * @param projectId Optional project ID to filter by project
     * @return List of recent test executions
     */
    List<RecentTest> getRecentTests(int limit, String projectId);
    
    /**
     * Get environment status for all configured environments
     * 
     * @return List of environment statuses
     */
    List<EnvironmentStatus> getEnvironmentStatus();
    
    /**
     * Get metrics data for a specific type and period
     * 
     * @param metricType Metric type (execution, performance, errors)
     * @param period Period (day, week, month)
     * @param projectId Optional project ID to filter by project
     * @return Metrics data
     */
    MetricsData getMetrics(String metricType, String period, String projectId);
    
    /**
     * Get test execution timeline data
     * 
     * @param days Number of days to retrieve data for
     * @param projectId Optional project ID to filter by project
     * @return Test timeline data
     */
    TestTimelineData getTestTimeline(int days, String projectId);
    
    /**
     * Get failure analysis data
     * 
     * @param days Number of days to analyze failures for
     * @param projectId Optional project ID to filter by project
     * @return Failure analysis data
     */
    FailureAnalysis getFailureAnalysis(int days, String projectId);
    
    /**
     * Export dashboard data as a report
     * 
     * @param format Format of the report (pdf, csv, excel)
     * @param projectId Optional project ID to filter by project
     * @return Report data as byte array
     */
    byte[] exportDashboardReport(String format, String projectId);
    
    /**
     * Get daily test execution counts
     * 
     * @param days Number of days to retrieve data for
     * @param projectId Optional project ID to filter by project
     * @return List of daily test counts
     */
    List<DailyTestCount> getDailyTestCounts(int days, String projectId);
} 