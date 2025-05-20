package com.cstestforge.dashboard.service;

import com.cstestforge.dashboard.model.*;
import com.cstestforge.dashboard.repository.DashboardRepository;
import com.cstestforge.execution.model.TestStatus;
import com.cstestforge.execution.service.TestExecutionService;
import com.cstestforge.project.model.Project;
import com.cstestforge.project.model.execution.TestExecution;
import com.cstestforge.project.service.ProjectService;
import com.cstestforge.testing.service.TestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of the DashboardService interface
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private final TestExecutionService executionService;
    private final TestService testService;
    private final ProjectService projectService;
    private final DashboardRepository dashboardRepository;
    
    // Cache for dashboard data to improve performance
    private final Map<String, DashboardStats> statsCache = new ConcurrentHashMap<>();
    private final Map<String, TestTimelineData> timelineCache = new ConcurrentHashMap<>();
    private final Map<String, FailureAnalysis> failureAnalysisCache = new ConcurrentHashMap<>();
    private final Map<String, List<EnvironmentStatus>> environmentStatusCache = new ConcurrentHashMap<>();
    
    @Autowired
    public DashboardServiceImpl(
            TestExecutionService executionService,
            TestService testService,
            ProjectService projectService,
            DashboardRepository dashboardRepository) {
        this.executionService = executionService;
        this.testService = testService;
        this.projectService = projectService;
        this.dashboardRepository = dashboardRepository;
    }
    
    @Override
    public DashboardStats getStats(String projectId) {
        logger.debug("Getting dashboard stats for project: {}", projectId);
        
        // Check cache first
        String cacheKey = projectId != null ? "stats_" + projectId : "stats_all";
        if (statsCache.containsKey(cacheKey)) {
            return statsCache.get(cacheKey);
        }
        
        // Retrieve statistics
        DashboardStats stats = calculateStats(projectId);
        
        // Update cache
        statsCache.put(cacheKey, stats);
        
        return stats;
    }
    
    @Override
    public List<RecentTest> getRecentTests(int limit, String projectId) {
        logger.debug("Getting {} recent tests for project: {}", limit, projectId);
        
        List<RecentTest> recentTests = new ArrayList<>();
        
        try {
            // Get recent test executions from the repository
            List<TestExecution> executions;
            if (projectId != null) {
                executions = dashboardRepository.getRecentExecutions(projectId, limit);
            } else {
                executions = dashboardRepository.getRecentExecutions(limit);
            }
            
            // Convert to RecentTest objects
            for (TestExecution execution : executions) {
                RecentTest recentTest = new RecentTest();
                recentTest.setId(execution.getId());
                
                // Set test name (if available)
                String testId = extractTestId(execution);
                if (testId != null) {
                    recentTest.setTestId(testId);
                    testService.getTestById(execution.getProjectId(), testId)
                        .ifPresent(test -> recentTest.setName(test.getName()));
                }
                
                // If test name is still null, use a default
                if (recentTest.getName() == null) {
                    recentTest.setName("Test Execution " + execution.getId().substring(0, 8));
                }
                
                // Set other properties
                recentTest.setStatus(mapStatus(execution.getStatus().toString()));
                recentTest.setProjectId(execution.getProjectId());
                recentTest.setEnvironment(execution.getEnvironment());
                recentTest.setBrowser(execution.getBrowser());
                
                // Calculate duration
                if (execution.getStartTime() != null && execution.getEndTime() != null) {
                    long duration = java.time.Duration.between(
                        execution.getStartTime(), execution.getEndTime()).toMillis();
                    recentTest.setDuration(duration);
                }
                
                // Convert start time to timestamp
                if (execution.getStartTime() != null) {
                    recentTest.setTimestamp(execution.getStartTime().toInstant(ZoneOffset.UTC).toEpochMilli());
                } else {
                    recentTest.setTimestamp(System.currentTimeMillis());
                }
                
                recentTests.add(recentTest);
            }
        } catch (Exception e) {
            logger.error("Error getting recent tests", e);
            // Return empty list on error
            return Collections.emptyList();
        }
        
        return recentTests;
    }
    
    @Override
    @Cacheable(value = "environmentStatus", key = "'envStatus'", unless = "#result.isEmpty()")
    public List<EnvironmentStatus> getEnvironmentStatus() {
        logger.debug("Getting environment status");
        
        // Check cache first
        String cacheKey = "environments";
        if (environmentStatusCache.containsKey(cacheKey)) {
            return environmentStatusCache.get(cacheKey);
        }
        
        List<EnvironmentStatus> environments;
        try {
            // Get environment configurations from repository
            environments = dashboardRepository.getEnvironmentConfigurations();
            
            // For each environment, check its status
            for (EnvironmentStatus env : environments) {
                try {
                    // Check environment health and update status
                    boolean isHealthy = dashboardRepository.checkEnvironmentHealth(env.getUrl());
                    env.setStatus(isHealthy ? "online" : "offline");
                    env.setLastChecked(LocalDateTime.now());
                } catch (Exception e) {
                    logger.warn("Error checking environment status for {}: {}", env.getName(), e.getMessage());
                    env.setStatus("unknown");
                    env.setLastChecked(LocalDateTime.now());
                }
            }
            
            // Update cache
            environmentStatusCache.put(cacheKey, environments);
            
        } catch (Exception e) {
            logger.error("Error getting environment status", e);
            environments = Collections.emptyList();
        }
        
        return environments;
    }
    
    @Override
    public MetricsData getMetrics(String metricType, String period, String projectId) {
        logger.debug("Getting {} metrics for period {} and project: {}", metricType, period, projectId);
        
        MetricsData metricsData = new MetricsData();
        List<String> labels = new ArrayList<>();
        List<Number> data = new ArrayList<>();
        
        try {
            switch (metricType) {
                case "execution":
                    return getExecutionMetrics(period, projectId);
                case "performance":
                    return getPerformanceMetrics(period, projectId);
                case "errors":
                    return getErrorMetrics(projectId);
                default:
                    logger.warn("Unknown metric type: {}", metricType);
                    return new MetricsData();
            }
        } catch (Exception e) {
            logger.error("Error getting metrics data", e);
            return new MetricsData();
        }
    }
    
    @Override
    public TestTimelineData getTestTimeline(int days, String projectId) {
        logger.debug("Getting test timeline for {} days and project: {}", days, projectId);
        
        // Check cache first
        String cacheKey = (projectId != null ? projectId : "all") + "_" + days;
        if (timelineCache.containsKey(cacheKey)) {
            return timelineCache.get(cacheKey);
        }
        
        TestTimelineData timelineData = new TestTimelineData();
        
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(days - 1);
            
            timelineData.setStartDate(startDate.format(DATE_FORMATTER));
            timelineData.setEndDate(endDate.format(DATE_FORMATTER));
            
            // Get daily execution counts
            List<TestTimelineData.ExecutionByDay> executionsByDay = new ArrayList<>();
            
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                String dateStr = date.format(DATE_FORMATTER);
                
                // Count executions for this day
                int count = dashboardRepository.countExecutionsForDay(date, projectId);
                
                // Calculate pass rate
                double passRate = dashboardRepository.getPassRateForDay(date, projectId);
                
                executionsByDay.add(new TestTimelineData.ExecutionByDay(dateStr, count, passRate));
            }
            
            timelineData.setExecutionsByDay(executionsByDay);
            
            // Update cache
            timelineCache.put(cacheKey, timelineData);
            
        } catch (Exception e) {
            logger.error("Error getting test timeline", e);
            // Return empty timeline on error
            timelineData.setExecutionsByDay(Collections.emptyList());
        }
        
        return timelineData;
    }
    
    @Override
    public FailureAnalysis getFailureAnalysis(int days, String projectId) {
        logger.debug("Getting failure analysis for {} days and project: {}", days, projectId);
        
        // Check cache first
        String cacheKey = (projectId != null ? projectId : "all") + "_" + days;
        if (failureAnalysisCache.containsKey(cacheKey)) {
            return failureAnalysisCache.get(cacheKey);
        }
        
        FailureAnalysis failureAnalysis = new FailureAnalysis();
        
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
            
            // Get most common failures
            List<FailureAnalysis.CommonFailure> commonFailures = dashboardRepository.getMostCommonFailures(cutoffDate, projectId)
                .stream()
                .map(failure -> new FailureAnalysis.CommonFailure(
                    failure.getMessage(), 
                    failure.getCount(), 
                    failure.getTestIds()
                ))
                .collect(Collectors.toList());
            failureAnalysis.setMostCommonFailures(commonFailures);
            
            // Get failures by type
            List<FailureAnalysis.FailureByType> failuresByType = dashboardRepository.getFailuresByType(cutoffDate, projectId)
                .stream()
                .map(failure -> new FailureAnalysis.FailureByType(
                    failure.getType(), 
                    failure.getCount(), 
                    failure.getPercentage()
                ))
                .collect(Collectors.toList());
            failureAnalysis.setFailuresByType(failuresByType);
            
            // Get failures by browser
            List<FailureAnalysis.FailureByBrowser> failuresByBrowser = dashboardRepository.getFailuresByBrowser(cutoffDate, projectId)
                .stream()
                .map(failure -> new FailureAnalysis.FailureByBrowser(
                    failure.getBrowser(), 
                    failure.getCount(), 
                    failure.getPercentage()
                ))
                .collect(Collectors.toList());
            failureAnalysis.setFailuresByBrowser(failuresByBrowser);
            
            // Get unstable tests
            List<FailureAnalysis.UnstableTest> unstableTests = dashboardRepository.getUnstableTests(cutoffDate, projectId)
                .stream()
                .map(test -> new FailureAnalysis.UnstableTest(
                    test.getTestId(),
                    test.getTestName(),
                    test.getFailureRate(),
                    test.getLastExecuted()
                ))
                .collect(Collectors.toList());
            failureAnalysis.setUnstableTests(unstableTests);
            
            // Update cache
            failureAnalysisCache.put(cacheKey, failureAnalysis);
            
        } catch (Exception e) {
            logger.error("Error getting failure analysis", e);
            // Initialize empty lists to avoid null pointers
            failureAnalysis.setMostCommonFailures(Collections.emptyList());
            failureAnalysis.setFailuresByType(Collections.emptyList());
            failureAnalysis.setFailuresByBrowser(Collections.emptyList());
            failureAnalysis.setUnstableTests(Collections.emptyList());
        }
        
        return failureAnalysis;
    }
    
    @Override
    public byte[] exportDashboardReport(String format, String projectId) {
        logger.debug("Exporting dashboard report in {} format for project: {}", format, projectId);
        
        try {
            // Get dashboard stats
            DashboardStats stats = getStats(projectId);
            
            // Generate report based on format
            return dashboardRepository.generateReport(stats, format, projectId);
            
        } catch (Exception e) {
            logger.error("Error exporting dashboard report", e);
            return new byte[0];
        }
    }
    
    @Override
    public List<DailyTestCount> getDailyTestCounts(int days, String projectId) {
        logger.debug("Getting daily test counts for {} days and project: {}", days, projectId);
        
        List<DailyTestCount> dailyCounts = new ArrayList<>();
        
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(days - 1);
            
            // Get test counts for each day
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                String dateStr = date.format(DATE_FORMATTER);
                
                // Get test counts for this day
                Map<String, Integer> counts = dashboardRepository.getTestCountsByStatus(date, projectId);
                
                DailyTestCount dailyCount = new DailyTestCount(
                    dateStr,
                    counts.getOrDefault("total", 0),
                    counts.getOrDefault("passed", 0),
                    counts.getOrDefault("failed", 0),
                    counts.getOrDefault("skipped", 0)
                );
                
                dailyCounts.add(dailyCount);
            }
        } catch (Exception e) {
            logger.error("Error getting daily test counts", e);
            // Return empty list on error
            return Collections.emptyList();
        }
        
        return dailyCounts;
    }
    
    /**
     * Calculate dashboard statistics
     * 
     * @param projectId Optional project ID
     * @return Dashboard statistics
     */
    private DashboardStats calculateStats(String projectId) {
        DashboardStats stats = new DashboardStats();
        
        try {
            // Get test counts
            Map<String, Integer> testCounts = dashboardRepository.getTestCounts(projectId);
            
            // Set stats
            stats.setTotalTests(testCounts.getOrDefault("total", 0));
            stats.setPassedTests(testCounts.getOrDefault("passed", 0));
            stats.setFailedTests(testCounts.getOrDefault("failed", 0));
            stats.setSkippedTests(testCounts.getOrDefault("skipped", 0));
            
            // Calculate success rate
            int total = stats.getTotalTests();
            if (total > 0) {
                double successRate = (double) stats.getPassedTests() / total * 100;
                stats.setSuccessRate(Math.round(successRate * 10.0) / 10.0); // Round to 1 decimal
            } else {
                stats.setSuccessRate(0);
            }
            
            // Calculate average duration
            long totalDuration = dashboardRepository.getTotalTestDuration(projectId);
            if (total > 0) {
                stats.setAvgDuration(totalDuration / total);
            } else {
                stats.setAvgDuration(0);
            }
            
            // Get daily test counts for last week
            stats.setTestsByDay(getDailyTestCounts(7, projectId));
            
        } catch (Exception e) {
            logger.error("Error calculating dashboard stats", e);
            // Return empty stats on error
            stats = new DashboardStats(0, 0, 0, 0, 0, 0);
            stats.setTestsByDay(Collections.emptyList());
        }
        
        return stats;
    }
    
    /**
     * Get execution metrics data
     * 
     * @param period Time period (day, week, month)
     * @param projectId Optional project ID
     * @return Metrics data
     */
    private MetricsData getExecutionMetrics(String period, String projectId) {
        MetricsData metricsData = new MetricsData();
        List<String> labels = new ArrayList<>();
        List<Number> values = new ArrayList<>();
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;
        
        // Determine date range
        switch (period) {
            case "day":
                startDate = endDate;
                break;
            case "week":
                startDate = endDate.minusDays(6);
                break;
            case "month":
                startDate = endDate.minusDays(29);
                break;
            default:
                startDate = endDate.minusDays(6);
                break;
        }
        
        // Get daily execution counts
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String dateStr = date.format(DATE_FORMATTER);
            labels.add(dateStr);
            
            // Count executions for this day
            int count = dashboardRepository.countExecutionsForDay(date, projectId);
            values.add(count);
        }
        
        metricsData.setLabels(labels);
        
        // Create dataset
        MetricsData.Dataset dataset = new MetricsData.Dataset(
            "Test Executions",
            values,
            "#94196B",  // Purple line color
            false      // Don't fill under the line
        );
        
        metricsData.setDatasets(Collections.singletonList(dataset));
        
        return metricsData;
    }
    
    /**
     * Get performance metrics data
     * 
     * @param period Time period (day, week, month)
     * @param projectId Optional project ID
     * @return Metrics data
     */
    private MetricsData getPerformanceMetrics(String period, String projectId) {
        MetricsData metricsData = new MetricsData();
        List<String> labels = new ArrayList<>();
        List<Number> executionTimes = new ArrayList<>();
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;
        
        // Determine date range
        switch (period) {
            case "day":
                startDate = endDate;
                break;
            case "week":
                startDate = endDate.minusDays(6);
                break;
            case "month":
                startDate = endDate.minusDays(29);
                break;
            default:
                startDate = endDate.minusDays(6);
                break;
        }
        
        // Get daily average execution times
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String dateStr = date.format(DATE_FORMATTER);
            labels.add(dateStr);
            
            // Get average execution time for this day
            double avgTime = dashboardRepository.getAverageExecutionTimeForDay(date, projectId);
            executionTimes.add(avgTime);
        }
        
        metricsData.setLabels(labels);
        
        // Create dataset for execution times
        MetricsData.Dataset dataset = new MetricsData.Dataset(
            "Avg. Execution Time (ms)",
            executionTimes,
            generateColorList(executionTimes.size(), "#2196f3")  // Blue bars
        );
        
        metricsData.setDatasets(Collections.singletonList(dataset));
        
        return metricsData;
    }
    
    /**
     * Get error metrics data
     * 
     * @param projectId Optional project ID
     * @return Metrics data
     */
    private MetricsData getErrorMetrics(String projectId) {
        MetricsData metricsData = new MetricsData();
        
        try {
            // Get error counts by type
            Map<String, Integer> errorCounts = dashboardRepository.getErrorCountsByType(projectId);
            
            if (errorCounts.isEmpty()) {
                return metricsData;
            }
            
            List<String> labels = new ArrayList<>(errorCounts.keySet());
            List<Number> values = labels.stream()
                .map(errorCounts::get)
                .collect(Collectors.toList());
            
            metricsData.setLabels(labels);
            
            // Create dataset
            List<String> colors = Arrays.asList(
                "#f44336",  // Red
                "#ff9800",  // Orange
                "#2196f3",  // Blue
                "#9c27b0",  // Purple
                "#e91e63",  // Pink
                "#795548"   // Brown
            );
            
            List<String> backgroundColor = new ArrayList<>();
            for (int i = 0; i < labels.size(); i++) {
                backgroundColor.add(colors.get(i % colors.size()));
            }
            
            MetricsData.Dataset dataset = new MetricsData.Dataset(
                "Error Distribution",
                values,
                backgroundColor
            );
            
            metricsData.setDatasets(Collections.singletonList(dataset));
            
        } catch (Exception e) {
            logger.error("Error getting error metrics", e);
        }
        
        return metricsData;
    }
    
    /**
     * Schedule periodic cache invalidation
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    private void invalidateCache() {
        logger.debug("Invalidating dashboard cache");
        
        statsCache.clear();
        timelineCache.clear();
        failureAnalysisCache.clear();
        environmentStatusCache.clear();
    }
    
    /**
     * Map test execution status to frontend status
     * 
     * @param status Repository status
     * @return Frontend status
     */
    private String mapStatus(String status) {
        try {
            TestStatus testStatus = TestStatus.valueOf(status);
            
            switch (testStatus) {
                case PASSED:
                    return "passed";
                case FAILED:
                    return "failed";
                case SKIPPED:
                    return "skipped";
                case ERROR:
                    return "failed";
                case RUNNING:
                    return "running";
                case QUEUED:
                    return "queued";
                case ABORTED:
                    return "failed";
                default:
                    return "unknown";
            }
        } catch (Exception e) {
            return status.toLowerCase();
        }
    }
    
    /**
     * Extract test ID from execution metadata
     * 
     * @param execution Test execution
     * @return Test ID or null
     */
    private String extractTestId(TestExecution execution) {
        if (execution == null || execution.getMetadata() == null) {
            return null;
        }
        
        // Try to find test ID in metadata
        Map<String, Object> metadata = execution.getMetadata();
        if (metadata.containsKey("testId")) {
            return metadata.get("testId").toString();
        }
        
        return null;
    }
    
    /**
     * Generate a list of colors for charts
     * 
     * @param count Number of colors
     * @param baseColor Base color
     * @return List of color strings
     */
    private List<String> generateColorList(int count, String baseColor) {
        List<String> colors = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            colors.add(baseColor);
        }
        return colors;
    }
} 